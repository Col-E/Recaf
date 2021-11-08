package me.coley.recaf.assemble.validation.bytecode;

import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the given method bytecode is <i>(probably)</i> verifiable.
 *
 * @author Matt Coley
 */
public class BytecodeValidator implements Validator {
	private static final List<BytecodeValidationVisitor> validators = new ArrayList<>();
	private final List<ValidationMessage> messages = new ArrayList<>();

	@Override
	public void visit() {
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

	static {
		// TODO: populate validators
	}
}
