package me.coley.recaf.parse.javadoc;

import java.util.List;

/**
 * Base javadoc member wrapper.
 *
 * @author Matt
 */
public class DocMember {
	private List<String> modifiers;
	private String name;
	private String description;

	/**
	 * @param modifiers
	 * 		Member's access modifiers.
	 * @param name
	 * 		Member's name.
	 * @param description
	 * 		Member's purpose.
	 */
	DocMember(List<String> modifiers, String name, String description) {
		this.modifiers = modifiers;
		this.name = name;
		this.description = description;
	}

	/**
	 * @return Member's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Member's purpose.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return Access modifiers of the member.
	 */
	public List<String> getModifiers() {
		return modifiers;
	}
}