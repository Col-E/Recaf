package me.coley.recaf.code;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Common elements between {@link ClassInfo} and {@link DexClassInfo}.
 *
 * @author Matt Coley
 */
public interface CommonClassInfo extends AccessibleInfo, ItemInfo {
	/**
	 * @return Class's name.
	 */
	String getName();

	/**
	 * @return Class's parent name.
	 */
	String getSuperName();

	/**
	 * @return Class's generic signature. May be {@code null}.
	 */
	@Nullable
	String getSignature();

	/**
	 * @return Class's implemented interfaces.
	 */
	List<String> getInterfaces();

	/**
	 * @return Class's access modifiers.
	 */
	int getAccess();

	/**
	 * @return Outer method where the class is in.
	 */
	@Nullable
	OuterMethodInfo getOuterMethod();

	/**
	 * @return Class's declared fields.
	 */
	List<FieldInfo> getFields();

	/**
	 * @return Class's declared methods.
	 */
	List<MethodInfo> getMethods();

	/**
	 * @return Class's inner classes.
	 */
	List<InnerClassInfo> getInnerClasses();

	/**
	 * Breadcrumbs of the outer class. For example: A, B for the class C which has B as an outer class.
	 * This List <strong>MUST</strong> be sorted in order of the outest first. The last element is the outer of the class itself.
	 * @return breadcrumbs of the outer class.
	 */
	List<String> getOuterClassBreadcrumbs();

	/**
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field type descriptor.
	 *
	 * @return The matched field if any exists within the class.
	 */
	default FieldInfo findField(String name, String descriptor) {
		// We treat the parameters as optional, so you can do 'field by name X' and ignore the descriptor.
		for (FieldInfo field : getFields()) {
			if (name != null && !field.getName().equals(name))
				continue;
			if (descriptor != null && !field.getDescriptor().equals(descriptor))
				continue;
			return field;
		}
		return null;
	}

	/**
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method type descriptor.
	 *
	 * @return The matched method if any exists within the class.
	 */
	default MethodInfo findMethod(String name, String descriptor) {
		for (MethodInfo method : getMethods()) {
			if (method.getName().equals(name) && method.getDescriptor().equals(descriptor))
				return method;
		}
		return null;
	}
}
