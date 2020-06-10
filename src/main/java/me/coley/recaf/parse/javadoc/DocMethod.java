package me.coley.recaf.parse.javadoc;

import java.util.List;

/**
 * Javadoc method wrapper.
 *
 * @author Matt
 */
public class DocMethod extends DocMember {
	private final String returnType;
	private final String returnDescription;
	private final List<DocParameter> parameters;

	/**
	 * @param modifiers
	 * 		Method's access modifiers.
	 * @param name
	 * 		Method's name.
	 * @param description
	 * 		Method's purpose.
	 * @param returnType
	 * 		Method's declared type.
	 * @param returnDescription
	 * 		Method's return value purpose.
	 * @param parameters
	 * 		Method's parameter information.
	 */
	public DocMethod(List<String> modifiers, String name, String description,
					 String returnDescription, String returnType,
					 List<DocParameter> parameters) {
		super(modifiers, name, description);
		if (returnDescription == null || returnDescription.trim().isEmpty())
			returnDescription = "n/a";
		this.returnType = returnType;
		this.returnDescription = returnDescription;
		this.parameters = parameters;
	}

	/**
	 * @return Method's return type.
	 */
	public String getReturnType() {
		return returnType;
	}

	/**
	 * @return Method's return value purpose.
	 */
	public String getReturnDescription() {
		return returnDescription;
	}

	/**
	 * @return Method's parameter information.
	 */
	public List<DocParameter> getParameters() {
		return parameters;
	}
}