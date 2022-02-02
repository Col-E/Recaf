package me.coley.recaf.code;

import java.util.List;

/**
 * Common elements between {@link ClassInfo} and {@link DexClassInfo}.
 *
 * @author Matt Coley
 */
public interface CommonClassInfo extends ItemInfo {
	/**
	 * @return Class's name.
	 */
	String getName();

	/**
	 * @return Class's parent name.
	 */
	String getSuperName();

	/**
	 * @return Class's implemented interfaces.
	 */
	List<String> getInterfaces();

	/**
	 * @return Class's access modifiers.
	 */
	int getAccess();

	/**
	 * @return Class's declared fields.
	 */
	List<FieldInfo> getFields();

	/**
	 * @return Class's declared methods.
	 */
	List<MethodInfo> getMethods();

	/**
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field type descriptor.
	 *
	 * @return The matched field if any exists within the class.
	 */
	default FieldInfo findField(String name, String descriptor) {
		for (FieldInfo field : getFields()) {
			if (field.getName().equals(name) && field.getDescriptor().equals(descriptor))
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
