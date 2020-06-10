package me.coley.recaf.parse.javadoc;

import java.util.List;

/**
 * Javadoc field wrapper.
 *
 * @author Matt
 */
public class DocField extends DocMember {
	private String type;

	/**
	 * @param modifiers
	 * 		Field's access modifiers.
	 * @param name
	 * 		Field's name.
	 * @param description
	 * 		Field's purpose.
	 * @param type
	 * 		Field's declared type.
	 */
	public DocField(List<String> modifiers, String name, String description, String type) {
		super(modifiers, name, description);
		this.type = type;
	}

	/**
	 * @return Field's declared type.
	 */
	public String getType() {
		return type;
	}
}