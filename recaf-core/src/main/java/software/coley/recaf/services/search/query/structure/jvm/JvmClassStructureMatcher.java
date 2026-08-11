package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.search.query.structure.ClassQuery;
import software.coley.recaf.services.search.query.structure.InsnMatcher;
import software.coley.recaf.services.search.query.structure.MethodMatcher;
import software.coley.recaf.services.search.query.structure.TryCatchBlockMatcher;

import java.util.ArrayList;
import java.util.List;

import static software.coley.recaf.services.search.query.structure.StructMatchUtils.matchList;

/**
 * Evaluates the JVM side of a {@link ClassQuery}.
 *
 * @author Matt Coley
 * @see ClassQuery
 */
public final class JvmClassStructureMatcher {
	private JvmClassStructureMatcher() {}

	/**
	 * @param query
	 * 		Structure query.
	 * @param classInfo
	 * 		Candidate class.
	 *
	 * @return {@code true} when the complete query matches.
	 */
	public static boolean matches(@Nonnull ClassQuery query, @Nonnull JvmClassInfo classInfo) {
		try {
			if (!query.matchesCommon(classInfo))
				return false;

			// Read into node structure for matching.
			ClassNode classNode = new ClassNode();
			classInfo.getClassReader().accept(classNode, classInfo.getClassReaderFlags());

			// Read methods into a list of candidates for matching.
			List<JvmMethodCandidate> methods = new ArrayList<>();
			for (MethodNode methodNode : classNode.methods) {
				MethodMember member = classInfo.getDeclaredMethod(methodNode.name, methodNode.desc);
				if (member == null)
					return false;
				methods.add(new JvmMethodCandidate(methodNode, member));
			}
			return matchList(query.methods(), methods, JvmClassStructureMatcher::matchesMethod);
		} catch (Throwable ignored) {
			// A malformed candidate must not prevent other workspace classes from matching.
			return false;
		}
	}

	/**
	 * Matches shared method metadata, JVM instructions, and handlers.
	 *
	 * @param matcher
	 * 		Method constraint.
	 * @param candidate
	 * 		JVM method candidate.
	 *
	 * @return {@code true} when every method constraint matches.
	 */
	private static boolean matchesMethod(@Nonnull MethodMatcher matcher, @Nonnull JvmMethodCandidate candidate) {
		if (!matcher.matchesMetadata(candidate.member()))
			return false;

		// Read instructions into a list for matching.
		MethodNode method = candidate.node();
		List<AbstractInsnNode> instructions = new ArrayList<>();
		if (method.instructions != null)
			for (AbstractInsnNode instruction : method.instructions)
				instructions.add(instruction);

		// Check for instruction matches.
		if (!matchList(matcher.instructions(), instructions, InsnMatcher::matchesJvm))
			return false;

		// Check for try/catch matches.
		List<TryCatchBlockNode> catches = method.tryCatchBlocks == null ? List.of() : method.tryCatchBlocks;
		return matchList(matcher.tryCatchBlocks(), catches, TryCatchBlockMatcher::matchesJvm);
	}

	/**
	 * JVM method paired with its shared Recaf metadata.
	 *
	 * @param node
	 * 		ASM method node.
	 * @param member
	 * 		Shared method metadata.
	 */
	private record JvmMethodCandidate(@Nonnull MethodNode node, @Nonnull MethodMember member) {}
}
