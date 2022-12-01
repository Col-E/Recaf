package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.ast.AstValidator;
import org.junit.jupiter.api.Disabled;
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
public class AstValidationTests extends JasmUtils {
	@Disabled("JASM will catch this before it gets to validation")
	@Nested
	class Int {
		@Test
		public void testCorrect() {
			for (int i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++)
				assertCorrect("method dummy ()V\n" + "bipush " + i + "\nend");
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"method dummy ()V\n" + "bipush " + (Byte.MIN_VALUE - 1) + "\nend",
				"method dummy ()V\n" + "bipush " + (Byte.MAX_VALUE + 1) + "\nend",
				"method dummy ()V\n" + "sipush " + (Short.MIN_VALUE - 1) + "\nend",
				"method dummy ()V\n" + "sipush " + (Short.MAX_VALUE + 1) + "\nend",
		})
		public void testMissingTryCatchLabels(String original) {
			assertMatch(original, ValidationMessage.INT_VAL_TOO_BIG);
		}
	}

	@Nested
	class VariableUsage {
		@ParameterizedTest
		@ValueSource(strings = {
				"method dummy ()V\n" + "astore newVariable" + "\nend",
				"method dummy ()V\n" + "istore newVariable" + "\nend",
				"method dummy ()V\n" + "fstore newVariable" + "\nend",
				"method dummy (I param)V\n" + "iload param" + "\nend",
				"method dummy (Ljava/lang/Object; param)V\n" + "aload param" + "\nend"
		})
		public void testCorrect(String original) {
			assertCorrect(original);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"method dummy ()V\n" + "aload doesnotexist" + "\nend",
				"method dummy ()V\n" + "iload doesnotexist" + "\nend",
				"method dummy ()V\n" + "fload doesnotexist" + "\nend",
				"method dummy ()V\n" + "dload doesnotexist" + "\nend",
				"method dummy ()V\n" + "lload doesnotexist" + "\nend",
				"method dummy ()V\n" + "iinc doesnotexist 1" + "\nend"
		})
		public void testUsedBeforeDefined(String original) {
			assertMatch(original, ValidationMessage.VAR_USE_BEFORE_DEF);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"method dummy (Ljava/lang/String; param)V\niload param" + "\nend",
				"method dummy (I param)V\naload param" + "\nend",
				"method dummy (I param)V\nfload param" + "\nend",
				"method dummy (I param)V\ndload param" + "\nend",
				"method dummy (I param)V\nlload param" + "\nend",
		})
		public void testUsageOfVarOfDifferentTypeFromParameters(String orginal) {
			assertMatch(orginal, ValidationMessage.VAR_USE_OF_DIFF_TYPE);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"method dummy ()V\n" + "astore param\n" + "iload param" + "\nend",
				"method dummy ()V\n" + "astore param\n" + "fload param" + "\nend",
				"method dummy ()V\n" + "astore param\n" + "dload param" + "\nend",
				"method dummy ()V\n" + "astore param\n" + "lload param" + "\nend",
				"method dummy ()V\n" + "istore param\n" + "aload param" + "\nend",
				"method dummy ()V\n" + "istore param\n" + "fload param" + "\nend",
				"method dummy ()V\n" + "istore param\n" + "dload param" + "\nend",
				"method dummy ()V\n" + "istore param\n" + "lload param" + "\nend",
		})
		public void testUsageOfVarOfDifferentTypeFromCode(String original) {
			assertMatch(original, ValidationMessage.VAR_USE_OF_DIFF_TYPE);
		}
	}

	@Nested
	class ConstValues {
		@ParameterizedTest
		@ValueSource(strings = {
				"field static final dummy I\n" + " 0",
				"field static       dummy J\n" + " 9000000000L",
				"field static       dummy C\n" + " 'A'",
				"field static       dummy F\n" + " 10.5F",
				"field static       dummy D\n" + " 10.5",
				"field static       dummy Ljava/lang/String;\n" + " \"text\""
		})
		public void testCorrect(String original) {
			assertCorrect(original);
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"field final dummy I " + " 0",
				"field dummy I" + " 0",
		})
		public void testConstOnNonStatic(String original) {
			assertMatch(original, ValidationMessage.CV_VAL_ON_NON_STATIC);
		}

		@Test
		public void testIntStoredOnByte() {
			assertMatch("field static final dummy B " + "90000000",
					ValidationMessage.CV_VAL_TOO_BIG);
		}

		@Test
		public void testIntStoredOnChar() {
			assertMatch("field static final dummy C " + "90000000",
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
		Unit unit = createSilentUnit(DEFAULT_KEYWORDS, original);
		assertNotNull(unit, "Parser did not find unit context with input: " + original);

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
