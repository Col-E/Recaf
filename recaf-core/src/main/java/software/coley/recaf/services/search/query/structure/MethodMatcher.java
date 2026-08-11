package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.search.match.AccessFlagMatcher;
import software.coley.recaf.services.search.match.Matcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches method metadata and backend code constraints.
 *
 * @param name
 * 		Method name matcher.
 * @param returnType
 * 		Return type matcher.
 * @param accessFlags
 * 		Access constraint.
 * @param parameterTypes
 * 		Parameter type constraint.
 * @param annotations
 * 		Annotation constraint.
 * @param exceptions
 * 		Thrown exception constraint.
 * @param tryCatchBlocks
 * 		Try/catch constraint.
 * @param instructions
 * 		Instruction constraint.
 *
 * @author Matt Coley
 * @see FieldMatcher
 * @see InsnMatcher
 */
public record MethodMatcher(@Nonnull Matcher<String> name,
                            @Nonnull Matcher<Type> returnType,
                            @Nonnull AccessFlagMatcher accessFlags,
                            @Nonnull ListMatcher<Matcher<Type>> parameterTypes,
                            @Nonnull ListMatcher<AnnotationMatcher> annotations,
                            @Nonnull ListMatcher<Matcher<Type>> exceptions,
                            @Nonnull ListMatcher<TryCatchBlockMatcher> tryCatchBlocks,
                            @Nonnull ListMatcher<InsnMatcher> instructions) implements Matcher<MethodMember> {
	@Override
	public boolean matches(@Nullable MethodMember method) {
		return matchesMetadata(method);
	}

	/**
	 * Matches the parts available from the shared Recaf method model.
	 *
	 * @param method
	 * 		Method to test.
	 *
	 * @return {@code true} when shared metadata matches.
	 */
	public boolean matchesMetadata(@Nullable MethodMember method) {
		// Reject missing members and cheap metadata mismatches before parsing descriptors.
		if (method == null || !name.matches(method.getName()) || !accessFlags.matches(method.getAccess()))
			return false;

		// Parse shared signature metadata before evaluating annotations and code constraints.
		try {
			Type methodType = Type.getMethodType(method.getDescriptor());
			if (!returnType.matches(methodType.getReturnType()) ||
					!parameterTypes.matches(List.of(methodType.getArgumentTypes())))
				return false;

			// Convert thrown internal names so exception constraints use the same ASM type model.
			List<Type> thrownTypes = new ArrayList<>();
			for (String exception : method.getThrownTypes())
				thrownTypes.add(toType(exception));
			return annotations.matches(method.getAnnotations()) && exceptions.matches(thrownTypes);
		} catch (RuntimeException ex) {
			// Malformed candidate metadata is a non-match, not a workspace-wide search failure.
			return false;
		}
	}

	/**
	 * Converts either a descriptor or an internal class name to an ASM type.
	 *
	 * @param value
	 * 		Descriptor or internal class name.
	 *
	 * @return Parsed ASM type.
	 */
	@Nonnull
	private static Type toType(@Nonnull String value) {
		return value.startsWith("[") || value.startsWith("L") || value.length() == 1 ?
				Type.getType(value) : Type.getObjectType(value);
	}
}
