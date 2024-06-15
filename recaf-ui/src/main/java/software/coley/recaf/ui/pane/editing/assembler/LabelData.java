package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import software.coley.collections.box.Box;
import software.coley.collections.box.IntBox;

import java.util.*;

/**
 * Models a variable.
 *
 * @param name
 * 		Name of label.
 * @param usage
 * 		Usages of the loabel in the AST.
 */
public record LabelData(@Nonnull String name,   @Nonnull AstUsages usage,
                        @Nonnull IntBox lineSlot, @Nonnull Box<List<LabelData>> overlapping) {
	@Nonnull
	public Range range() {
		IntSummaryStatistics summary = usage.readersAndWriters()
				.mapToInt(e -> Objects.requireNonNull(e.location()).line())
				.summaryStatistics();
		return new Range(summary.getMin(), summary.getMax());
	}

	@Nonnull
	public ASTLabel labelDeclaration() {
		return (ASTLabel) usage.readers().getFirst();
	}


	public long countRefsOnLine(int line) {
		return usage.readersAndWriters()
				.filter(u -> u.location().line() == line)
				.count();
	}

	public List<LabelData> computeOverlapping(@Nonnull Collection<LabelData> labelDatum) {
		return overlapping.computeIfAbsent(() -> {
			Range range = range();
			List<LabelData> overlap = new ArrayList<>();
			for (LabelData data : labelDatum) {
				// Skip self
				if (name.equals(data.name)) continue;

				// Skip labels that don't have references
				if (data.usage().writers().isEmpty()) continue;

				Range otherRange = data.range();
				if (Math.max(range.start(), otherRange.start()) <= Math.min(range.end(), otherRange.end()))
					overlap.add(data);
			}
			return overlap;

		});
	}

	public boolean isInRange(int line) {
		ASTLabel declaration = labelDeclaration();
		Location declarationLoc = declaration.location();
		if (declarationLoc == null) return false;
		int declarationLine = declarationLoc.line();

		// Base case, range included declaration line.
		if (declarationLine == line) return true;

		// Check if inside range:
		//  goto X
		//    ..   <---- line somewhere in here
		//  X:
		for (ASTElement referrer : usage.writers()) {
			Location referrerLoc = referrer.location();
			if (referrerLoc == null) continue;
			int referrerLine = referrerLoc.line();

			// Base case, range included referrer's line.
			if (referrerLine == line) return true;

			// Otherwise check if the line is between the range of the reference and the declaration.
			// The range bounds are swapped based on if the reference is forwards or backwards.
			if ((declarationLine > referrerLine) ?
					(line > referrerLine && line < declarationLine) :
					(line > declarationLine && line < referrerLine)) return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LabelData labelData = (LabelData) o;

		return name.equals(labelData.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
