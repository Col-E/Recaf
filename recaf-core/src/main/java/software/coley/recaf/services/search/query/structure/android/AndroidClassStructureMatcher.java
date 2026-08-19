package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.visitor.DexClassVisitor;
import me.darknet.dex.tree.visitor.DexMethodVisitor;
import me.darknet.dex.tree.visitor.DexTreeWalker;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.search.query.structure.ClassQuery;
import software.coley.recaf.services.search.query.structure.InsnMatcher;
import software.coley.recaf.services.search.query.structure.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

import static software.coley.recaf.services.search.query.structure.StructMatchUtils.matchList;

/**
 * Evaluates the Android side of a {@link ClassQuery}.
 *
 * @author Matt Coley
 * @see ClassQuery
 */
public final class AndroidClassStructureMatcher {
	private AndroidClassStructureMatcher() {}

	/**
	 * @param query
	 * 		Structure query.
	 * @param classInfo
	 * 		Candidate class.
	 *
	 * @return {@code true} when the complete query matches.
	 */
	public static boolean matches(@Nonnull ClassQuery query, @Nonnull AndroidClassInfo classInfo) {
		try {
			if (!query.matchesCommon(classInfo))
				return false;

			// Read methods into a list of candidates for matching.
			List<DexMethodCandidate> methods = new ArrayList<>();
			DexTreeWalker.accept(classInfo.getBackingDefinition(), new DexClassVisitor() {
				/**
				 * @param method
				 * 		Native DEX method.
				 *
				 * @return Visitor for the method, or {@code null} when metadata is unavailable.
				 */
				@Override
				public DexMethodVisitor visitMethod(@Nonnull me.darknet.dex.tree.definitions.MethodMember method) {
					MethodMember member = classInfo.getDeclaredMethod(method.getName(), method.getType().descriptor());
					if (member == null) return null;
					methods.add(new DexMethodCandidate(method, member));
					return new DexMethodVisitor() {
						/**
						 * @param ignored
						 * 		Native DEX method supplied by the walker.
						 */
						@Override
						public void visit(@Nonnull me.darknet.dex.tree.definitions.MethodMember ignored) {
							// The method and code are read from the native definition below.
						}
					};
				}
			});

			return matchList(query.methods(), methods, AndroidClassStructureMatcher::matchesMethod);
		} catch (Throwable ignored) {
			// A malformed DEX candidate must not cancel the workspace search.
			return false;
		}
	}

	/**
	 * Matches shared method metadata, native instructions, and handlers.
	 *
	 * @param matcher
	 * 		Method constraint.
	 * @param candidate
	 * 		Native method candidate.
	 *
	 * @return {@code true} when every method constraint matches.
	 */
	private static boolean matchesMethod(@Nonnull MethodMatcher matcher, @Nonnull DexMethodCandidate candidate) {
		if (!matcher.matchesMetadata(candidate.member()))
			return false;

		// Read instructions into a list for matching.
		Code code = candidate.nativeMethod().getCode();
		List<Instruction> instructions = code == null ? List.of() : code.getInstructions();

		// Check for instruction matches.
		if (!matchList(matcher.instructions(), instructions, InsnMatcher::matchesAndroid))
			return false;

		// Read try-catch blocks into a list for matching.
		List<DexCatchCandidate> catches = new ArrayList<>();
		if (code != null) {
			for (TryCatch tryCatch : code.tryCatch())
				for (Handler handler : tryCatch.handlers()) catches.add(new DexCatchCandidate(tryCatch, handler));
		}

		// Check for try/catch matches.
		return matchList(matcher.tryCatchBlocks(), catches, (blockMatcher, candidateValue) ->
				blockMatcher.matchesAndroid(candidateValue.tryCatch(), candidateValue.handler()));
	}

	/**
	 * Native method paired with its shared Recaf metadata.
	 *
	 * @param nativeMethod
	 * 		Native DEX method.
	 * @param member
	 * 		Shared method metadata.
	 */
	private record DexMethodCandidate(@Nonnull me.darknet.dex.tree.definitions.MethodMember nativeMethod,
	                                  @Nonnull MethodMember member) {}

	/**
	 * Native try/catch pair used for matching.
	 *
	 * @param tryCatch
	 * 		Protected DEX range.
	 * @param handler
	 * 		DEX exception handler.
	 */
	private record DexCatchCandidate(@Nonnull TryCatch tryCatch, @Nonnull Handler handler) {}
}
