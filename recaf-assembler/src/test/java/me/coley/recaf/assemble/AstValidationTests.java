package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.parser.BytecodeParser;
import me.coley.recaf.assemble.transformer.AntlrToAstTransformer;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.ast.AstValidator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * These are basic AST validation tests. The logic expressed by assembly snippets may be incorrect but this is ok
 * here because we are not testing the runtime correctness steps here. These only validate that we <i>can</i> generate
 * code from the AST, not if that code will be correct.
 */
public class AstValidationTests extends TestUtil {
	// TODO: In VariableUsage, once the instruction visit process isn't linear in the variable validator,
	//       check for allowed scope changes.

	@Nested
	class Int {
		@Test
		public void testCorrect() {
			for (int i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++)
				assertCorrect("method()V\n" + "BIPUSH " + i);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"method()V\n" + "BIPUSH " + (Byte.MIN_VALUE - 1),
				"method()V\n" + "BIPUSH " + (Byte.MAX_VALUE + 1),
				"method()V\n" + "SIPUSH " + (Short.MIN_VALUE - 1),
				"method()V\n" + "SIPUSH " + (Short.MAX_VALUE + 1),
		})
		public void testMissingTryCatchLabels(String original) {
			assertMatch(original, ValidationMessage.INT_VAL_TOO_BIG);
		}
	}

	@Nested
	class LabelRefs {
		@Test
		public void testCorrect() {
			assertCorrect("method()V\n" + "TRY a b CATCH(*) c\na:\nb:\nc:");
		}

		@Test
		public void testMissingTryCatchLabels() {
			assertMatch("method()V\n" + "TRY a b CATCH(*) c",
					ValidationMessage.LBL_UNDEFINED);
		}
	}

	@Nested
	class VariableUsage {
		@ParameterizedTest
		@ValueSource(strings = {
				"method()V\n" + "ASTORE newVariable",
				"method()V\n" + "ISTORE newVariable",
				"method()V\n" + "FSTORE newVariable",
				"method(I param)V\n" + "ILOAD param",
				"method(Ljava/lang/Object; param)V\n" + "ALOAD param"
		})
		public void testCorrect(String original) {
			assertCorrect(original);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"method(Ljava/lang/String param)V\n",
				"method(java/lang/String param)V\n"
		})
		public void testIllegalParamDesc(String original) {
			assertMatch(original, ValidationMessage.ILLEGAL_DESC);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"method()V\n" + "ALOAD doesnotexist",
				"method()V\n" + "ILOAD doesnotexist",
				"method()V\n" + "FLOAD doesnotexist",
				"method()V\n" + "DLOAD doesnotexist",
				"method()V\n" + "LLOAD doesnotexist",
				"method()V\n" + "IINC doesnotexist 1"
		})
		public void testUsedBeforeDefined(String original) {
			assertMatch(original, ValidationMessage.VAR_USE_BEFORE_DEF);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"method(Ljava/lang/String; param)V\nILOAD param",
				"method(I param)V\nALOAD param",
				"method(I param)V\nFLOAD param",
				"method(I param)V\nDLOAD param",
				"method(I param)V\nLLOAD param",
		})
		public void testUsageOfVarOfDifferentTypeFromParameters(String orginal) {
			assertMatch(orginal, ValidationMessage.VAR_USE_OF_DIFF_TYPE);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"method()V\n" + "ASTORE param\n" + "ILOAD param",
				"method()V\n" + "ASTORE param\n" + "FLOAD param",
				"method()V\n" + "ASTORE param\n" + "DLOAD param",
				"method()V\n" + "ASTORE param\n" + "LLOAD param",
				"method()V\n" + "ISTORE param\n" + "ALOAD param",
				"method()V\n" + "ISTORE param\n" + "FLOAD param",
				"method()V\n" + "ISTORE param\n" + "DLOAD param",
				"method()V\n" + "ISTORE param\n" + "LLOAD param",
		})
		public void testUsageOfVarOfDifferentTypeFromCode(String original) {
			assertMatch(original, ValidationMessage.VAR_USE_OF_DIFF_TYPE);
		}
	}

	@Nested
	class ConstValues {
		@ParameterizedTest
		@ValueSource(strings = {
				"STATIC FINAL field I\n" + "VALUE 0",
				"STATIC       field J\n" + "VALUE 9000000000L",
				"STATIC       field F\n" + "VALUE 10.5F",
				"STATIC       field D\n" + "VALUE 10.5",
				"STATIC       field Ljava/lang/String;\n" + "VALUE \"text\""
		})
		public void testCorrect(String original) {
			assertCorrect(original);
		}

		@Test
		public void testConstOnMethod() {
			assertMatch("STATIC FINAL method()V\n" + "VALUE 0",
					ValidationMessage.CV_VAL_ON_METHOD);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"FINAL field I\n" + "VALUE 0",
				"field I\n" + "VALUE 0",
		})
		public void testConstOnNonStatic(String original) {
			assertMatch(original, ValidationMessage.CV_VAL_ON_NON_STATIC);
		}

		@Test
		public void testIntStoredOnByte() {
			assertMatch("STATIC FINAL field B\n" + "VALUE 90000000",
					ValidationMessage.CV_VAL_TOO_BIG);
		}

		@Test
		public void testIntStoredOnChar() {
			assertMatch("STATIC FINAL field C\n" + "VALUE 90000000",
					ValidationMessage.CV_VAL_TOO_BIG);
		}
	}


	private static void assertCorrect(String original) {
		handle(original, false, new DelegatedMessageConsumer(message -> fail(message.getMessage())));
	}

	private static void assertMatch(String original, int id) {
		handle(original, true, new DelegatedMessageConsumer(message -> {
			System.out.println(message);
			assertEquals(id, message.getMessageType());
		}));
	}

	private static void handle(String original, boolean expectMessages, DelegatedMessageConsumer handler) {
		BytecodeParser parser = parser(original);

		BytecodeParser.UnitContext unitCtx = parser.unit();
		assertNotNull(unitCtx, "Parser did not find unit context with input: " + original);

		AntlrToAstTransformer visitor = new AntlrToAstTransformer();
		Unit unit = visitor.visitUnit(unitCtx);

		AstValidator validator = new AstValidator(unit);
		try {
			validator.visit();
		} catch (AstException ex) {
			fail(ex);
		}
		for (ValidationMessage message : validator.getMessages())
			handler.accept(message);
		if (expectMessages && !handler.hasConsumed()) {
			fail("Expected messages, but received none!");
		} else if (!expectMessages && handler.hasConsumed()) {
			fail("Expected no messages, but received " + handler.getConsumed());
		}
	}

	private static class DelegatedMessageConsumer implements Consumer<ValidationMessage> {
		private final Consumer<ValidationMessage> delegate;
		private int consumed;

		private DelegatedMessageConsumer(Consumer<ValidationMessage> delegate) {
			this.delegate = delegate;
		}

		@Override
		public void accept(ValidationMessage message) {
			consumed++;
			delegate.accept(message);
		}

		public boolean hasConsumed() {
			return consumed > 0;
		}

		public int getConsumed() {
			return consumed;
		}
	}
}
