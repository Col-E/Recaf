package me.coley.recaf.assemble.validation;

import java.util.List;

/**
 * Validates input.
 *
 * @param <E>
 * 		Validation exception type.
 *
 * @author Matt Coley
 */
public interface Validator<E extends Exception> {
	/**
	 * For results see: {@link #getMessages()}
	 *
	 * @throws E
	 * 		Propagated from the visitors used. See {@link ValidationVisitor}.
	 */
	void visit() throws E;

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
