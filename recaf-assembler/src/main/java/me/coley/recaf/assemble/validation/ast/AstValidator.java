package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the code in the given {@link Unit} isn't garbage.
 *
 * @author Matt Coley
 */
public class AstValidator implements Validator<AstException> {
	private static final List<AstValidationVisitor> validators = new ArrayList<>();
	private final List<ValidationMessage> messages = new ArrayList<>();
	private final Unit unit;

	/**
	 * @param unit
	 * 		Unit to validate.
	 */
	public AstValidator(Unit unit) {
		this.unit = unit;
	}

	@Override
	public void visit() throws AstException {
		for (AstValidationVisitor validator : validators)
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
	 * @return Associated unit to validate.
	 */
	public Unit getUnit() {
		return unit;
	}

	static {
		validators.add(new AstDescriptorValidator());
		validators.add(new AstLabelValidator());
		validators.add(new AstFieldConstValidator());
		validators.add(new AstVariableValidator());
		validators.add(new AstIntInsnValidator());
		validators.add(new AstArrayValidator());
	}
}
