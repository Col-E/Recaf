package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.services.search.query.structure.CountConstraint;
import software.coley.recaf.services.search.query.structure.ListMatcher;
import software.coley.recaf.services.search.query.structure.MethodDebugInfoMatcher;

import java.util.List;

/**
 * Matches DEX method line-number and local-variable debug metadata.
 *
 * @param lineNumbers
 * 		Line-number entry matcher.
 * @param localVariables
 * 		Local-variable entry matcher.
 *
 * @author Matt Coley
 */
public record DexDebugInfoMatcher(@Nonnull ListMatcher<DexLineNumberMatcher> lineNumbers,
                                  @Nonnull ListMatcher<DexLocalVariableMatcher> localVariables)
		implements MethodDebugInfoMatcher {
	/** Matcher which accepts any DEX debug information, including its absence. */
	public static final DexDebugInfoMatcher ANY = new DexDebugInfoMatcher(ListMatcher.any(), ListMatcher.any());

	/**
	 * @param line
	 * 		Source line to find.
	 *
	 * @return Matcher requiring at least one entry for the given source line and allowing all other entries.
	 */
	@Nonnull
	public static DexDebugInfoMatcher line(int line) {
		return new DexDebugInfoMatcher(
				ListMatcher.bag(List.of(DexLineNumberMatcher.line(line)), CountConstraint.atLeast(1)),
				ListMatcher.any());
	}

	/**
	 * @param register
	 * 		Register index.
	 * @param name
	 * 		Local name.
	 * @param descriptor
	 * 		Local type descriptor.
	 *
	 * @return Matcher requiring at least one matching local variable and allowing all other entries.
	 */
	@Nonnull
	public static DexDebugInfoMatcher local(int register, @Nonnull String name, @Nonnull String descriptor) {
		return new DexDebugInfoMatcher(ListMatcher.any(),
				ListMatcher.bag(List.of(DexLocalVariableMatcher.exact(register, name, descriptor)),
						CountConstraint.atLeast(1)));
	}

	@Override
	public boolean matchesJvm(@Nonnull MethodNode method) {
		return false;
	}

	@Override
	public boolean matchesAndroid(@Nullable DebugInformation debugInfo) {
		List<DebugInformation.LineNumber> lines = debugInfo == null ? List.of() : debugInfo.lineNumbers();
		List<DebugInformation.LocalVariable> locals = debugInfo == null ? List.of() : debugInfo.locals();
		return lineNumbers.matches(lines) && localVariables.matches(locals);
	}
}
