package me.coley.recaf.assemble.validation;

import java.util.Objects;

/**
 * Validation feedback item. May represent warnings about dodgy code, errors, etc.
 *
 * @author Matt Coley
 */
public class ValidationMessage {
	// Constant values
	public static final int CV_VAL_ON_METHOD = 1000;
	public static final int CV_VAL_ON_NON_STATIC = 1001;
	public static final int CV_VAL_NOT_ALLOWED = 1002;
	public static final int CV_VAL_WRONG_TYPE = 1003;
	public static final int CV_VAL_TOO_BIG = 1004;
	// Variable usage
	public static final int VAR_ILLEGAL_DESC = 2000;
	public static final int VAR_USE_BEFORE_DEF = 2002;
	public static final int VAR_USE_OF_DIFF_TYPE = 2003;
	// Instance data
	private final MessageLevel level;
	private final String message;
	private final int identifier;

	private ValidationMessage(int identifier, MessageLevel level, String message) {
		this.identifier = identifier;
		this.level = level;
		this.message = message;
	}

	/**
	 * @param identifier
	 * 		Unique message id.
	 * @param message
	 * 		Info message.
	 *
	 * @return Instance.
	 */
	public static ValidationMessage info(int identifier, String message) {
		return new ValidationMessage(identifier, MessageLevel.INFO, message);
	}

	/**
	 * @param identifier
	 * 		Unique message id.
	 * @param message
	 * 		Warning message.
	 *
	 * @return Instance.
	 */
	public static ValidationMessage warn(int identifier, String message) {
		return new ValidationMessage(identifier, MessageLevel.WARN, message);
	}

	/**
	 * @param identifier
	 * 		Unique message id.
	 * @param message
	 * 		Error message.
	 *
	 * @return Instance.
	 */
	public static ValidationMessage error(int identifier, String message) {
		return new ValidationMessage(identifier, MessageLevel.ERROR, message);
	}

	/**
	 * @return Unique message id.
	 */
	public int getIdentifier() {
		return identifier;
	}

	/**
	 * @return Message severity level.
	 */
	public MessageLevel getLevel() {
		return level;
	}

	/**
	 * @return Message content.
	 */
	public String getMessage() {
		return message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ValidationMessage message1 = (ValidationMessage) o;
		return level == message1.level && message.equals(message1.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(level, message);
	}

	@Override
	public String toString() {
		return level.name() + ": " + message;
	}
}
