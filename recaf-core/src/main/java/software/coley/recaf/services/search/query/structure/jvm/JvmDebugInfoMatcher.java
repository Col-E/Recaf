package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.services.search.query.structure.CountConstraint;
import software.coley.recaf.services.search.query.structure.ListMatcher;
import software.coley.recaf.services.search.query.structure.MethodDebugInfoMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches JVM method line-number and local-variable debug metadata.
 *
 * @param lineNumbers
 * 		Line-number node matcher.
 * @param localVariables
 * 		Local-variable entry matcher.
 *
 * @author Matt Coley
 */
public record JvmDebugInfoMatcher(@Nonnull ListMatcher<JvmLineNumberNodeMatcher> lineNumbers,
                                  @Nonnull ListMatcher<JvmLocalVariableMatcher> localVariables)
		implements MethodDebugInfoMatcher {
	/** Matcher which accepts any JVM debug information, including its absence. */
	public static final JvmDebugInfoMatcher ANY = new JvmDebugInfoMatcher(ListMatcher.any(), ListMatcher.any());

	/**
	 * @param line
	 * 		Source line to find.
	 *
	 * @return Matcher requiring at least one node for the given source line and allowing all other nodes.
	 */
	@Nonnull
	public static JvmDebugInfoMatcher line(int line) {
		return new JvmDebugInfoMatcher(
				ListMatcher.bag(List.of(JvmLineNumberNodeMatcher.line(line)), CountConstraint.atLeast(1)),
				ListMatcher.any());
	}

	/**
	 * @param index
	 * 		Local slot index.
	 * @param name
	 * 		Local name.
	 * @param descriptor
	 * 		Local type descriptor.
	 *
	 * @return Matcher requiring at least one matching local variable and allowing all other entries.
	 */
	@Nonnull
	public static JvmDebugInfoMatcher local(int index, @Nonnull String name, @Nonnull String descriptor) {
		return new JvmDebugInfoMatcher(ListMatcher.any(),
				ListMatcher.bag(List.of(JvmLocalVariableMatcher.exact(index, name, descriptor)), CountConstraint.atLeast(1)));
	}

	@Override
	public boolean matchesJvm(@Nonnull MethodNode method) {
		List<LineNumberNode> lines = new ArrayList<>();
		if (method.instructions != null) {
			for (AbstractInsnNode instruction : method.instructions) {
				if (instruction instanceof LineNumberNode line)
					lines.add(line);
			}
		}
		List<LocalVariableNode> locals = method.localVariables == null ? List.of() : method.localVariables;
		return lineNumbers.matches(lines) && localVariables.matches(locals);
	}

	@Override
	public boolean matchesAndroid(@Nullable DebugInformation debugInfo) {
		return false;
	}
}
