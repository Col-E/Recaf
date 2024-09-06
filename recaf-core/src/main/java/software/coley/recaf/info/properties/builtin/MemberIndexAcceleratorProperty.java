package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Used to accelerate the {@link ClassMember} to {@code int index} lookup process in extreme scenarios.
 *
 * @author Matt Coley
 */
public class MemberIndexAcceleratorProperty extends BasicProperty<Map<ClassMember, Integer>> {
	public static final String KEY = "member-index-accel";
	/**
	 * Number of elements required to enable use of this property.
	 */
	public static final int CUTOFF = 1000;

	/**
	 * @param classInfo
	 * 		Target class to pull fields/methods from.
	 */
	public MemberIndexAcceleratorProperty(@Nonnull ClassInfo classInfo) {
		super(KEY, new IdentityHashMap<>());
		Map<ClassMember, Integer> value = Objects.requireNonNull(value());
		List<FieldMember> fields = classInfo.getFields();
		for (int i = 0; i < fields.size(); i++)
			value.put(fields.get(i), i);
		List<MethodMember> methods = classInfo.getMethods();
		for (int i = 0; i < methods.size(); i++)
			value.put(methods.get(i), i);
	}

	/**
	 * Get or creates an {@link MemberIndexAcceleratorProperty} for the given {@link ClassInfo}.
	 *
	 * @param classInfo
	 * 		Target class to pull fields/methods from.
	 *
	 * @return Accelerator property for class.
	 */
	@Nonnull
	public static MemberIndexAcceleratorProperty get(@Nonnull ClassInfo classInfo) {
		Property<?> property = classInfo.getProperty(KEY);
		if (property instanceof MemberIndexAcceleratorProperty acceleratorProperty)
			return acceleratorProperty;
		MemberIndexAcceleratorProperty acceleratorProperty = new MemberIndexAcceleratorProperty(classInfo);
		classInfo.setProperty(acceleratorProperty);
		return acceleratorProperty;
	}

	/**
	 * @param member
	 * 		Member to get an index of. Must match an exact instance of the member in the containing {@link ClassInfo}.
	 *
	 * @return Index of member, or {@code -1} if not present in the class.
	 */
	public int indexOf(@Nonnull ClassMember member) {
		return Objects.requireNonNull(value()).getOrDefault(member, -1);
	}

	@Override
	public boolean persistent() {
		// Member instances change between class info containers, so we dont want to persist this.
		return false;
	}
}
