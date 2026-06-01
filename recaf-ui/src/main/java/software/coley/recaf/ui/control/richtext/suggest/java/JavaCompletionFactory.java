package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;

import java.util.Comparator;
import java.util.Objects;

/**
 * Shared completion construction helpers.
 *
 * @author Matt Coley
 */
public final class JavaCompletionFactory {
	/**
	 * Default ordering for completions. Ranks are sorted first, then sort keys, then display text.
	 */
	public static final Comparator<JavaCompletion> DEFAULT_COMPLETION_ORDER = Comparator
			.comparingInt(JavaCompletion::rank)
			.thenComparing(JavaCompletion::sortKey, String.CASE_INSENSITIVE_ORDER)
			.thenComparing(JavaCompletion::displayText, String.CASE_INSENSITIVE_ORDER);

	private JavaCompletionFactory() {}

	/**
	 * @param candidate
	 * 		Type candidate to create a completion for.
	 * @param rank
	 * 		Rank to assign to the completion, where lower is better.
	 *
	 * @return Completion for the given type candidate.
	 */
	@Nonnull
	public static JavaCompletion typeCompletion(@Nonnull TypeCandidate candidate, int rank) {
		return new JavaCompletion(
				CompletionKind.TYPE,
				candidate.packageName().isEmpty() ? candidate.simpleName() : candidate.simpleName() + " - " + candidate.packageName(),
				candidate.simpleName(),
				rank,
				candidate.path(),
				candidate.simpleName(),
				0,
				""
		);
	}

	/**
	 * @param path
	 * 		Path to the type being completed.
	 * 		If {@code null}, a completion will still be created but with no path reference.
	 * @param simpleName
	 * 		Simple name of the type being completed.
	 * 		The fully qualified name will be derived from the {@code path} if provided.
	 * @param rank
	 * 		Rank to assign to the completion, where lower is better.
	 *
	 * @return Completion for the given type information.
	 */
	@Nonnull
	public static JavaCompletion typeCompletion(@Nullable ClassPathNode path, @Nonnull String simpleName, int rank) {
		if (path != null) {
			TypeCandidate candidate = new TypeCandidate(simpleName,
					path.getValue().getName().replace('/', '.').replace('$', '.'),
					path.getValue().getName(),
					Objects.requireNonNullElse(path.getValue().getPackageName(), "").replace('/', '.'),
					path.getValue().getAccess() != 0 && (path.getValue().getAccess() & org.objectweb.asm.Opcodes.ACC_ANNOTATION) != 0,
					path.getValue().getAccess(),
					path);
			return typeCompletion(candidate, rank);
		}
		return new JavaCompletion(CompletionKind.TYPE, simpleName, simpleName, rank, null, simpleName, 0, "");
	}

	/**
	 * @param name
	 * 		Name of the method being completed.
	 * @param descriptor
	 * 		Method descriptor for the method being completed.
	 * @param rank
	 * 		Rank to assign to the completion, where lower is better.
	 * @param path
	 * 		Path to the method being completed. If {@code null}, a completion will still be created but with no path reference.
	 * @param insertMethodCall
	 *        {@code true} to insert parentheses after the method name, indicating a method call.
	 *        {@code false} to insert just the method name.
	 *
	 * @return Completion for the given method information.
	 */
	@Nonnull
	public static JavaCompletion methodCompletion(@Nonnull String name, @Nonnull String descriptor, int rank,
	                                              @Nullable ClassMemberPathNode path, boolean insertMethodCall) {
		boolean appendStatementTerminator = insertMethodCall &&
				Type.getMethodType(descriptor).getReturnType().getSort() == Type.VOID;
		return new JavaCompletion(
				CompletionKind.METHOD,
				displayMethod(name, descriptor),
				insertMethodCall ? name + "()" : name,
				rank,
				path,
				name,
				insertMethodCall ? (appendStatementTerminator ? 2 : 1) : 0,
				appendStatementTerminator ? ";" : ""
		);
	}

	/**
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 *
	 * @return Display text for a field completion, combining the field name and its type.
	 */
	@Nonnull
	public static String displayField(@Nonnull String name, @Nonnull String descriptor) {
		return name + " : " + simpleTypeName(Type.getType(descriptor));
	}

	/**
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 *
	 * @return Display text for a method completion, combining the method name and its argument types.
	 */
	@Nonnull
	public static String displayMethod(@Nonnull String name, @Nonnull String descriptor) {
		Type methodType = Type.getMethodType(descriptor);
		StringBuilder sb = new StringBuilder(name).append('(');
		Type[] argumentTypes = methodType.getArgumentTypes();
		for (int i = 0; i < argumentTypes.length; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(simpleTypeName(argumentTypes[i]));
		}
		return sb.append(')').toString();
	}

	/**
	 * @param type
	 * 		Type to get a simple name for.
	 *
	 * @return Simple name of type.
	 */
	@Nonnull
	private static String simpleTypeName(@Nonnull Type type) {
		return switch (type.getSort()) {
			case Type.VOID -> "void";
			case Type.BOOLEAN -> "boolean";
			case Type.CHAR -> "char";
			case Type.BYTE -> "byte";
			case Type.SHORT -> "short";
			case Type.INT -> "int";
			case Type.FLOAT -> "float";
			case Type.LONG -> "long";
			case Type.DOUBLE -> "double";
			case Type.ARRAY -> simpleTypeName(type.getElementType()) + "[]".repeat(type.getDimensions());
			case Type.OBJECT -> {
				String className = type.getClassName();
				int lastDot = className.lastIndexOf('.');
				yield lastDot >= 0 ? className.substring(lastDot + 1) : className;
			}
			default -> type.getClassName();
		};
	}

	/**
	 * @param name
	 * 		Name to check for prefix match.
	 * @param partial
	 * 		Partial text to match as a prefix.
	 *
	 * @return {@code true} if the name starts with the partial text, or if the partial text is empty. {@code false} otherwise.
	 */
	public static boolean matchesPrefix(@Nonnull String name, @Nonnull String partial) {
		if (partial.isEmpty())
			return true;
		return name.startsWith(partial);
	}

	/**
	 * @param name
	 * 		Name to check for prefix match.
	 * @param partial
	 * 		Partial text to match as a prefix.
	 *
	 * @return Penalty score for the name based on prefix matching.
	 */
	public static int prefixPenalty(@Nonnull String name, @Nonnull String partial) {
		return name.startsWith(partial) ? 0 : 1;
	}
}
