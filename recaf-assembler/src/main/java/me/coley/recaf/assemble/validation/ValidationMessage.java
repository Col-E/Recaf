package me.coley.recaf.assemble.validation;

import java.util.Objects;

/**
 * Validation feedback item. May represent warnings about dodgy code, errors, etc.
 *
 * @author Matt Coley
 */
public class ValidationMessage {
	// Constant values
	public static final int CV_VAL_ON_METHOD = 100;
	public static final int CV_VAL_ON_NON_STATIC = 101;
	public static final int CV_VAL_NOT_ALLOWED = 102;
	public static final int CV_VAL_WRONG_TYPE = 103;
	public static final int CV_VAL_TOO_BIG = 104;
	// Variable usage
	public static final int VAR_ILLEGAL_DESC = 200;
	public static final int VAR_USE_BEFORE_DEF = 202;
	public static final int VAR_USE_OF_DIFF_TYPE = 203;
	// Label usage
	public static final int LBL_UNDEFINED = 300;
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
