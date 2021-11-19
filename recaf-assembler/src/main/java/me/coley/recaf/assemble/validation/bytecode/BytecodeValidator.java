package me.coley.recaf.assemble.validation.bytecode;

import me.coley.recaf.assemble.BytecodeException;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.Validator;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the given field/method bytecode is <i>(probably)</i> verifiable.
 *
 * @author Matt Coley
 */
public class BytecodeValidator implements Validator<BytecodeException> {
	private static final List<BytecodeValidationVisitor> validators = new ArrayList<>();
	private final List<ValidationMessage> messages = new ArrayList<>();
	private final String selfType;
	private final FieldNode field;
	private final MethodNode method;

	/**
	 * @param selfType
	 * 		Class the member being validated.
	 * @param method
	 * 		Method to validate.
	 */
	public BytecodeValidator(String selfType, MethodNode method) {
		this(selfType, null, method);
	}

	/**
	 * @param selfType
	 * 		Class the member being validated.
	 * @param field
	 * 		Field to validate.
	 */
	public BytecodeValidator(String selfType, FieldNode field) {
		this(selfType, field, null);
	}

	private BytecodeValidator(String selfType, FieldNode field, MethodNode method) {
		this.selfType = selfType;
		this.field = field;
		this.method = method;
	}

	@Override
	public void visit() throws BytecodeException {
		for (BytecodeValidationVisitor validator : validators)
			validator.visit(this);
	}

	@Override
	public void addMessage(ValidationMessage message) {
		messages.add(message);
	}

	@Override
	public List<ValidationMessage> getMessages() {
		return messages;
	}

	/**
	 * @return Associated class the member being validated.
	 */
	public String getSelfType() {
		return selfType;
	}

	/**
	 * @return Associated field to validate.
	 * May be {@code null}, implying a {@link #getMethod() method} is present instead.
	 */
	public FieldNode getField() {
		return field;
	}

	/**
	 * @return Associated method to validate.
	 * May be {@code null}, implying a {@link #getField() field} is present instead.
	 */
	public MethodNode getMethod() {
		return method;
	}

	static {
		// TODO: populate validators
	}
}
