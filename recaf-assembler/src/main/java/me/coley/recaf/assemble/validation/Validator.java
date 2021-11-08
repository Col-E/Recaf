package me.coley.recaf.assemble.validation;

import me.coley.recaf.assemble.ast.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the code in the given unit isn't garbage.
 *
 * @author Matt Coley
 */
public class Validator {
	private static final List<ValidationVisitor> validators = new ArrayList<>();
	private final List<ValidationMessage> messages = new ArrayList<>();
	private final Unit unit;

	/**
	 * @param unit
	 * 		Unit to validate.
	 */
	public Validator(Unit unit) {
		this.unit = unit;
	}

	/**
	 * Populates validation messages based on the validator's {@link #getUnit() unit}.
	 * <br>
	 * For results see: {@link #getMessages()}
	 */
	public void visit() {
		for (ValidationVisitor validator : validators)
			validator.visit(this);
	}

	/**
	 * @param message
	 * 		Message to add.
	 */
	public void addMessage(ValidationMessage message) {
		messages.add(message);
	}

	/**
	 * @return Associated unit to validate.
	 */
	public Unit getUnit() {
		return unit;
	}

	/**
	 * @return Validation messages.
	 */
	public List<ValidationMessage> getMessages() {
		return messages;
	}

	static {
		validators.add(new LabelValidator());
		validators.add(new FieldConstValidator());
		validators.add(new VariableValidator());
	}
}
