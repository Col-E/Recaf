package me.coley.recaf.workspace.resource;

import java.util.List;

/**
 * Common elements between {@link ClassInfo} and {@link DexClassInfo}.
 *
 * @author Matt Coley
 */
public interface CommonClassInfo {
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
	List<MemberInfo> getFields();

	/**
	 * @return Class's declared methods.
	 */
	List<MemberInfo> getMethods();
}
