package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.UpdateSourcePositions;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Range;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.StringDiff;
import software.coley.recaf.util.StringDiff.DiffHelper;

import java.util.*;

import static org.openrewrite.Tree.randomId;

/**
 * Mostly copied from {@link UpdateSourcePositions}, but with a few modifications.
 *
 * @author Matt Coley
 */
public class AstRangeMapper {
	private static final DebuggingLogger logger = Logging.get(AstRangeMapper.class);

	/**
	 * @param tree
	 * 		Tree to map ranges of.
	 * @param backingText
	 * 		Optional backing text that is the origin of the tree.
	 * 		Can be used for detecting dropped tokens, which OpenRewrite does when it detects invalid code in some cases.
	 * 		Technically, the behavior is undefined, but that's how it works currently <i>(which is ideal actually,
	 * 		as opposed to just crashing/refusing to parse)</i>
	 *
	 * @return Sorted map of ranges to tree nodes.
	 * Ranges that appear first in the AST are first as keys.
	 * If two tree nodes have the same start position, the range is used to differentiate, such that the containing node is first.
	 */
	@Nonnull
	public static SortedMap<Range, Tree> computeRangeToTreeMapping(@Nonnull Tree tree, @Nullable String backingText) {
		// Check if there's a cached range mapping.
		Optional<AstRangeMappingMarker> opt = tree.getMarkers().findFirst(AstRangeMappingMarker.class);
		if (opt.isPresent())
			return opt.get().rangeMap;

		// Nope, gotta compute the mapping.
		SortedMap<Range, Tree> rangeMap = new TreeMap<>((o1, o2) -> {
			if (o1 == o2) return 0;
			int off1 = o1.getStart().getOffset();
			int off2 = o2.getStart().getOffset();
			int cmp = Integer.compare(off1, off2);
			if (cmp == 0) {
				off1 = o1.getEnd().getOffset();
				off2 = o2.getEnd().getOffset();
				cmp = -Integer.compare(off1, off2);
			}
			return cmp;
		});
		DiffHelper helper = backingText == null ? null : Unchecked.getOr(() -> new DiffHelper(backingText, tree), null);
		PositionPrintOutputCapture ppoc = new PositionPrintOutputCapture(helper);
		JavaPrinter<ExecutionContext> printer = new JavaPrinter<>() {
			final JavaPrinter<ExecutionContext> spacePrinter = new JavaPrinter<>();

			@Nullable
			@Override
			public J visit(@Nullable Tree tree, @Nonnull PrintOutputCapture<ExecutionContext> outputCapture) {
				if (tree == null) {
					return null;
				}

				J t = (J) tree;

				PositionPrintOutputCapture prefix = new PositionPrintOutputCapture(ppoc);
				spacePrinter.visitSpace(t.getPrefix(), Space.Location.ANY, prefix);

				Range.Position startPosition = new Range.Position(prefix.posInBacking, prefix.line, prefix.column);
				t = Unchecked.getOr(() ->super.visit(tree, outputCapture), null);
				if (t == null) {
					return null;
				}
				Range.Position endPosition = new Range.Position(ppoc.posInBacking, ppoc.line, ppoc.column);
				Range range = new Range(randomId(), startPosition, endPosition);
				rangeMap.put(range, t);

				return t;
			}

			@Override
			public J visitModifier(@Nonnull J.Modifier modifier, PrintOutputCapture<ExecutionContext> p) {
				PositionPrintOutputCapture prefix = new PositionPrintOutputCapture(ppoc);
				spacePrinter.visitSpace(modifier.getPrefix(), Space.Location.ANY, prefix);

				Range.Position startPosition = new Range.Position(prefix.posInBacking, prefix.line, prefix.column);
				J ret = super.visitModifier(modifier, p);
				Range.Position endPosition = new Range.Position(ppoc.posInBacking, ppoc.line, ppoc.column);
				Range range = new Range(randomId(), startPosition, endPosition);
				rangeMap.put(range, modifier);
				return ret;
			}
		};
		printer.visit(tree, ppoc);

		// Save mapping for potential repeated operations.
		tree.getMarkers().add(new AstRangeMappingMarker(UUID.randomUUID(), rangeMap));
		return rangeMap;
	}

	private record AstRangeMappingMarker(@Nonnull UUID uuid,
										 @Nonnull SortedMap<Range, Tree> rangeMap) implements Marker {
		@Nonnull
		@Override
		public UUID getId() {
			return uuid;
		}

		@Nonnull
		@Override
		@SuppressWarnings("unchecked")
		public <M extends Marker> M withId(@Nonnull UUID id) {
			return (M) new AstRangeMappingMarker(id, rangeMap);
		}
	}

	private static class PositionPrintOutputCapture extends PrintOutputCapture<ExecutionContext> {
		private final Set<StringDiff.Diff> diffsApplied;
		private final DiffHelper diffHelper;
		private int posInBacking = 0;
		private int posInCurrent = 0;
		private int column = 0;
		private int line = 1;
		private boolean lineBoundary;

		public PositionPrintOutputCapture(@Nullable DiffHelper diffHelper) {
			super(new InMemoryExecutionContext());
			this.diffHelper = diffHelper;
			this.diffsApplied = new HashSet<>();
		}

		public PositionPrintOutputCapture(@Nonnull PositionPrintOutputCapture ppoc) {
			super(new InMemoryExecutionContext());
			this.posInBacking = ppoc.posInBacking;
			this.posInCurrent = ppoc.posInCurrent;
			this.line = ppoc.line;
			this.column = ppoc.column;
			this.diffHelper = ppoc.diffHelper;
			this.diffsApplied = ppoc.diffsApplied;
		}

		@Nonnull
		@Override
		public PrintOutputCapture<ExecutionContext> append(char c) {
			next(c);
			return this;
		}

		@Nonnull
		@Override
		public PrintOutputCapture<ExecutionContext> append(@Nullable String text) {
			if (text != null) {
				int length = text.length();
				for (int i = 0; i < length; i++)
					next(text.charAt(i));
			}
			return this;
		}

		private boolean hasNotAppliedDiff(StringDiff.Diff diff) {
			return diffsApplied.add(diff);
		}

		private void next(char c) {
			// Error correction. OpenRewrite will drop tokens it deems invalid/not-fitting the Java model at the given
			// point. As such we should assume a move forward in the position should skip over the content in the
			// original backing text that got skipped by OpenRewrite.
			if (diffHelper != null) {
				int start = posInBacking;

				// If text is present in the original but not in the AST, something got removed.
				// We want to move the offset for the backing text over the text removed, but keep
				// the current offset for the current text.
				//
				// This will keep the backing offset synchronized with what is next in the current text.
				StringDiff.Diff removed = diffHelper.getRemoved(posInBacking);
				if (removed != null) {
					if (hasNotAppliedDiff(removed)) {
						int removedLength = removed.textA().length();
						posInBacking += removedLength;
					}
				} else {
					// If text is present in the AST but not the original, something got inserted.
					// We want to move the offset for the backing text to before the insertion, but keep
					// the current offset for the current text.
					//
					// This will keep the backing offset synchronized with what is next in the current text.
					StringDiff.Diff inserted = diffHelper.getInserted(posInCurrent);
					if (inserted != null) {
						if (hasNotAppliedDiff(inserted)) {
							int length = inserted.textB().length();
							posInBacking -= length;
						}
					} else {
						// If text is present in both but with some changes, we want to move the offset for
						// the backing text to after the change, but only up to the difference in size of the length.
						//
						// If we have change:
						//   void foo() {}
						// To:
						//   void xyz(int p) {}
						// Then the change is 'foo()' --> 'xyz(int p)'
						//
						// The current offsets should be sync'd at the start of the change, which would be:
						// 'foo' and 'xyz' in this case. The change swaps 'foo' to 'xyz' which does not require
						// any offset changes, but it does insert 'int p' which is a 5 character insertion.
						// Thus, in order to keep things synced, we will move the backing offset backwards by 5.
						//
						// Alternatively if we had the reverse, where 'xyz' is changed to 'foo' then we would
						// want to move the offset forwards by 5.
						StringDiff.Diff replaced = diffHelper.getReplaced(posInCurrent);
						if (replaced != null) {
							if (hasNotAppliedDiff(replaced)) {
								int aLength = replaced.textA().length();
								int bLength = replaced.textB().length();
								int lengthDiff = (bLength - aLength);
								posInBacking -= lengthDiff;
							}
						}
					}
				}

				if (posInBacking != start)
					logger.debugging(l -> l.info("Text adjustments: {} --> {}", start, posInBacking));
			}

			// Standard positioning updates
			posInBacking++;
			moveNext(c);
		}

		private void moveNext(char c) {
			posInCurrent++;
			if (lineBoundary) {
				line++;
				column = 0;
				lineBoundary = false;
			} else {
				column++;
			}
			if (c == '\n') {
				lineBoundary = true;
			}
		}
	}
}
