package software.coley.recaf.services.source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utils for manipulating OpenRewrite values.
 *
 * @author Matt Coley
 */
public class AstUtils {
	/**
	 * @param type
	 * 		Some type to get descriptor of.
	 *
	 * @return Descriptor of type.
	 */
	@Nonnull
	public static String toDesc(@Nonnull JavaType type) {
		return toInternal(type, true);
	}

	/**
	 * @param type
	 * 		Some type to get internal name of.
	 *
	 * @return Internal name of type.
	 */
	@Nonnull
	public static String toInternal(@Nonnull JavaType type) {
		return toInternal(type, false);
	}

	/**
	 * @param type
	 * 		Some type to get internal name of.
	 * @param desc
	 * 		Flag to map to descriptor format.
	 *
	 * @return Internal name <i>(or descriptor)</i> of requested type.
	 */
	@Nonnull
	private static String toInternal(@Nonnull JavaType type, boolean desc) {
		if (type instanceof JavaType.FullyQualified qualified) {
			String internalName = toInternal(qualified);
			return desc ? "L" + internalName + ";" : internalName;
		} else if (type instanceof JavaType.Array array) {
			return toInternal(array);
		} else if (type instanceof JavaType.Primitive primitive) {
			if (primitive == JavaType.Primitive.String)
				return "Ljava/lang/String;";
			return toInternal(primitive);
		} else if (type instanceof JavaType.Method method) {
			return "(" + method.getParameterTypes().stream()
					.map(t -> toInternal(t, true))
					.collect(Collectors.joining()) + ")" + toInternal(method.getReturnType(), true);
		} else if (type instanceof JavaType.Variable variable) {
			return toInternal(variable.getType(), desc);
		} else {
			throw new UnsupportedOperationException("Unhandled type: " + type);
		}
	}

	/**
	 * @param primitive
	 * 		Primitive type.
	 *
	 * @return Internal name of primitive.
	 */
	@Nonnull
	public static String toInternal(@Nonnull JavaType.Primitive primitive) {
		return switch (primitive) {
			case Boolean -> "Z";
			case Byte -> "B";
			case Char -> "C";
			case Double -> "D";
			case Float -> "F";
			case Int -> "I";
			case Long -> "J";
			case Short -> "S";
			case Void -> "V";
			case String -> "java/lang/String";
			default -> throw new IllegalArgumentException("Invalid primitive: " + primitive.name());
		};
	}

	/**
	 * @param qualified
	 * 		Qualified type.
	 *
	 * @return Internal name of type.
	 */
	@Nonnull
	public static String toInternal(@Nonnull JavaType.FullyQualified qualified) {
		return qualified.getFullyQualifiedName().replace('.', '/');
	}

	/**
	 * @param array
	 * 		Array to get internal name of.
	 *
	 * @return Array descriptor.
	 */
	@Nonnull
	public static String toInternal(@Nonnull JavaType.Array array) {
		// Array internal types are descriptors.
		JavaType elemType = array.getElemType();
		if (elemType instanceof JavaType.FullyQualified qualified)
			return "[L" + toInternal(qualified) + ";";
		else
			return "[" + toInternal(elemType);
	}

	/**
	 * @param offset
	 * 		Offset to fetch contents of.
	 * @param unit
	 * 		Unit to look in.
	 * @param backingText
	 * 		Optional backing text that is the origin of the tree.
	 * 		Can be used for detecting dropped tokens.
	 *
	 * @return Hierarchy of {@link Tree} elements at the given offset.
	 * First item is the most specific node, last item is the {@link J.CompilationUnit}.
	 */
	@Nonnull
	public static List<Tree> getAstPathAtOffset(int offset, @Nonnull J.CompilationUnit unit, @Nullable String backingText) {
		List<Tree> path = new ArrayList<>();
		Map<Range, Tree> map = AstRangeMapper.computeRangeToTreeMapping(unit, backingText);
		for (Map.Entry<Range, Tree> entry : map.entrySet()) {
			Range range = entry.getKey();
			int start = range.getStart().getOffset();
			int end = range.getEnd().getOffset();
			if (offset >= start && offset <= end) {
				Tree tree = entry.getValue();
				path.add(0, tree);
			}
		}
		return path;
	}
}
