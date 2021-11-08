package me.coley.recaf.assemble.validation;

import java.util.List;

/**
 * Validates input.
 *
 * @author Matt Coley
 */
public interface Validator {
	/**
	 * For results see: {@link #getMessages()}
	 */
	void visit();

	/**
	 * @param message
	 * 		Message to add.
	 */
	void addMessage(ValidationMessage message);

	/**
	 * @return Validation messages.
	 */
	List<ValidationMessage> getMessages();
}
