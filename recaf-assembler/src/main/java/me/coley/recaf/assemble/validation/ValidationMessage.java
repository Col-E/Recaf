package me.coley.recaf.assemble.validation;

import me.coley.recaf.assemble.ast.Element;

import java.util.Objects;

/**
 * Validation feedback item. May represent warnings about dodgy code, errors, etc.
 *
 * @author Matt Coley
 */
public class ValidationMessage {
	////// Message type ID's
	// Constant values
	public static final int CV_VAL_ON_METHOD = 100;
	public static final int CV_VAL_ON_NON_STATIC = 101;
	public static final int CV_VAL_NOT_ALLOWED = 102;
	public static final int CV_VAL_WRONG_TYPE = 103;
	public static final int CV_VAL_TOO_BIG = 104;
	// Variable usage
	public static final int VAR_USE_BEFORE_DEF = 202;
	public static final int VAR_USE_OF_DIFF_TYPE = 203;
	// Label usage
	public static final int LBL_UNDEFINED = 300;
	// Integer usage
	public static final int INT_VAL_TOO_BIG = 400;
	// Generic
	public static final int ILLEGAL_DESC = 500;
	public static final int ILLEGAL_FMT = 501;
	////// Instance data
	private final Element source;
	private final MessageLevel level;
	private final String message;
	private final int messageType;

	private ValidationMessage(Element source, int messageType, MessageLevel level, String message) {
		this.source = source;
		this.messageType = messageType;
		this.level = level;
		this.message = message;
	}

	/**
	 * @param identifier
	 * 		Unique type id.
	 * @param source
	 * 		Causing element of the message.
	 * @param message
	 * 		Info message.
	 *
	 * @return Instance.
	 */
	public static ValidationMessage info(int identifier, Element source, String message) {
		return new ValidationMessage(source, identifier, MessageLevel.INFO, message);
	}

	/**
	 * @param identifier
	 * 		Unique type id.
	 * @param source
	 * 		Causing element of the message.
	 * @param message
	 * 		Warning message.
	 *
	 * @return Instance.
	 */
	public static ValidationMessage warn(int identifier, Element source, String message) {
		return new ValidationMessage(source, identifier, MessageLevel.WARN, message);
	}

	/**
	 * @param identifier
	 * 		Unique type id.
	 * @param source
	 * 		Causing element of the message.
	 * @param message
	 * 		Error message.
	 *
	 * @return Instance.
	 */
	public static ValidationMessage error(int identifier, Element source, String message) {
		return new ValidationMessage(source, identifier, MessageLevel.ERROR, message);
	}

	/**
	 * @return Cause of the validation message.
	 */
	public Element getSource() {
		return source;
	}

	/**
	 * @return Unique type id.
	 */
	public int getMessageType() {
		return messageType;
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
		return messageType == message1.messageType && level == message1.level && message.equals(message1.message);
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
