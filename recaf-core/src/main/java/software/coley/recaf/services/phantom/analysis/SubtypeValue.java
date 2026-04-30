package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.Value;
import software.coley.recaf.util.Types;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Minimal recreation of {@link Value} for subtype relationship inference in {@link PhantomMethodConstraintAnalysis}.
 *
 * @author Matt Coley
 * @see SubtypeFrameState
 */
public class SubtypeValue implements Value {
	private static final Type NULL_TYPE = Type.getObjectType("null");
	private static final SubtypeValue UNINITIALIZED = new SubtypeValue(null, Set.of());
	private final Type type;
	private final Set<String> referenceNames;

	private SubtypeValue(@Nullable Type type, @Nonnull Set<String> referenceNames) {
		this.type = type;
		this.referenceNames = referenceNames;
	}

	/**
	 * @return Uninitialized placeholder value.
	 */
	@Nonnull
	public static SubtypeValue uninitialized() {
		return UNINITIALIZED;
	}

	/**
	 * @return Null-like reference value.
	 */
	@Nonnull
	public static SubtypeValue nullValue() {
		return new SubtypeValue(NULL_TYPE, Set.of());
	}

	/**
	 * @param type
	 * 		Value type.
	 *
	 * @return Typed value with reference evidence inferred from the type itself.
	 */
	@Nonnull
	public static SubtypeValue typed(@Nonnull Type type) {
		if (type.getSort() == Type.OBJECT)
			return new SubtypeValue(type, Set.of(type.getInternalName()));
		return new SubtypeValue(type, Set.of());
	}

	/**
	 * @param type
	 * 		Value type.
	 * @param referenceNames
	 * 		Reference evidence carried by the value.
	 *
	 * @return Typed value.
	 */
	@Nonnull
	public static SubtypeValue typed(@Nonnull Type type, @Nonnull Set<String> referenceNames) {
		return new SubtypeValue(type, Set.copyOf(referenceNames));
	}

	/**
	 * Merges two values at a control-flow join.
	 *
	 * @param value1
	 * 		First value.
	 * @param value2
	 * 		Second value.
	 *
	 * @return Merged value.
	 */
	@Nonnull
	public static SubtypeValue merge(@Nullable SubtypeValue value1, @Nullable SubtypeValue value2) {
		if (Objects.equals(value1, value2))
			return value1 == null ? uninitialized() : value1;
		if (value1 == null)
			return value2 == null ? uninitialized() : value2;
		if (value2 == null)
			return value1;
		if (!value1.isReferenceLike() || !value2.isReferenceLike()) {
			if (value1.type != null && value1.type.equals(value2.type))
				return value1;
			return typed(Types.OBJECT_TYPE);
		}

		Set<String> mergedReferences = new HashSet<>(value1.referenceNames);
		mergedReferences.addAll(value2.referenceNames);
		Type mergedType;
		if (value1.type == NULL_TYPE)
			mergedType = value2.type;
		else if (value2.type == NULL_TYPE)
			mergedType = value1.type;
		else if (Objects.equals(value1.type, value2.type))
			mergedType = value1.type;
		else
			mergedType = Types.OBJECT_TYPE;
		return typed(mergedType, mergedReferences);
	}

	/**
	 * @return Underlying ASM type.
	 */
	@Nullable
	public Type getType() {
		return type;
	}

	/**
	 * @return Reference evidence collected for the value.
	 */
	@Nonnull
	public Set<String> getReferenceNames() {
		return referenceNames;
	}

	/**
	 * @return {@code true} when the value behaves like a reference during merging.
	 */
	public boolean isReferenceLike() {
		if (type == null)
			return false;
		if (type == NULL_TYPE)
			return true;
		int sort = type.getSort();
		return sort == Type.OBJECT || sort == Type.ARRAY;
	}

	@Override
	public int getSize() {
		return type == null ? 1 : type.getSize();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SubtypeValue other))
			return false;
		return Objects.equals(type, other.type) && referenceNames.equals(other.referenceNames);
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(type);
		result = 31 * result + referenceNames.hashCode();
		return result;
	}
}
