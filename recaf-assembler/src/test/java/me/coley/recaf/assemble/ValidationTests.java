package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.parser.BytecodeParser;
import me.coley.recaf.assemble.parser.BytecodeVisitorImpl;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.Validator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ValidationTests extends TestUtil {
	// TODO: Warn
	//  - redundant THROWS statements

	// TODO: Error
	//  - enforced int ranges for smaller types (bipush can't push 1028 for example)
	//  - references variable names that dont exist
	//  - references to label names that dont exist
	//  - other things from 2.x (document here for proper checklist)

	@Nested
	class ConstValues {
		@Test
		public void testConstOnMethod() {
			handle("STATIC FINAL method()V\n" + "CONST-VALUE 0",
					assertMatch(ValidationMessage.CV_VAL_ON_METHOD));
		}

		@Test
		public void testConstOnNonConstant() {
			handle("FINAL field I\n" + "CONST-VALUE 0",
					assertMatch(ValidationMessage.CV_VAL_ON_NON_STATIC));
			handle("field I\n" + "CONST-VALUE 0",
					assertMatch(ValidationMessage.CV_VAL_ON_NON_STATIC));
		}

		@Test
		public void testIntStoredOnByte() {
			handle("STATIC FINAL field B\n" + "CONST-VALUE 90000000",
					assertMatch(ValidationMessage.CV_VAL_TOO_BIG));
		}

		@Test
		public void testIntStoredOnChar() {
			handle("STATIC FINAL field C\n" + "CONST-VALUE 90000000",
					assertMatch(ValidationMessage.CV_VAL_TOO_BIG));
		}
	}

	private static Consumer<ValidationMessage> assertMatch(int id) {
		return message -> {
			System.out.println(message);
			assertEquals(id, message.getIdentifier());
		};
	}

	private static void handle(String original, Consumer<ValidationMessage> handler) {
		BytecodeParser parser = parser(original);

		BytecodeParser.UnitContext unitCtx = parser.unit();
		assertNotNull(unitCtx, "Parser did not find unit context with input: " + original);

		BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
		Unit unit = visitor.visitUnit(unitCtx);

		Validator validator = new Validator(unit);
		validator.visit();
		for (ValidationMessage message : validator.getMessages())
			handler.accept(message);
	}
}
