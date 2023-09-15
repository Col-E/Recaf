package software.coley.recaf.services.mapping.data;

import java.util.Objects;

/**
 * Outlines mappings for a variable.
 *
 * @author Matt Coley
 */
public class VariableMapping {
	private final String ownerName;
	private final String methodName;
	private final String methodDesc;
	// Variable data
	private final String oldName;
	private final String desc;
	private final int index;
	private final String newName;

	/**
	 * @param ownerName
	 * 		Name of class defining the method.
	 * @param methodName
	 * 		Pre-mapping method name.
	 * @param methodDesc
	 * 		Descriptor type of the method.
	 * @param desc
	 * 		Variable descriptor.
	 * @param oldName
	 * 		Variable old name.
	 * @param index
	 * 		Variable index.
	 * @param newName
	 * 		Post-mapping method name.
	 */
	public VariableMapping(String ownerName, String methodName, String methodDesc,
						   String desc, String oldName, int index,
						   String newName) {
		this.ownerName = Objects.requireNonNull(ownerName, "Mapping entries cannot be null");
		this.methodDesc = Objects.requireNonNull(methodDesc, "Mapping entries cannot be null");
		this.methodName = Objects.requireNonNull(methodName, "Mapping entries cannot be null");
		this.newName = Objects.requireNonNull(newName, "Mapping entries cannot be null");
		// Variable info, may be null
		this.desc = desc;
		this.oldName = oldName;
		this.index = index;
	}

	/**
	 * @return Name of class defining the method.
	 */
	public String getOwnerName() {
		return ownerName;
	}

	/**
	 * @return Method name.
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * @return Method descriptor.
	 */
	public String getMethodDesc() {
		return methodDesc;
	}

	/**
	 * @return Old variable name. May be {@code null}.
	 */
	public String getOldName() {
		return oldName;
	}

	/**
	 * @return Variable descriptor. May be {@code null}.
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * @return Variable descriptor. May be {@code -1} for unknown values.
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return New variable name.
	 */
	public String getNewName() {
		return newName;
	}

	@Override
	public String toString() {
		return oldName + " ==> " + newName;
	}
}
