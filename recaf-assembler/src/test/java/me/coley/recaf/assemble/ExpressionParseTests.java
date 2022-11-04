package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.meta.Expression;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that validate {@link Expression} values can be parsed from source text.
 */
public class ExpressionParseTests extends JasmUtils {
	@ParameterizedTest
	@ValueSource(strings = {
			"value = foo << bar;",
			"value = foo + bar;",
			"value = foo - bar;",
			"value = foo / bar;",
			"value = foo ^ bar;",
			"System.out.println(\"Hello\");",
			"if (foo && bar) System.out.println(\"Hello\");",
			"if (foo || bar) System.out.println(\"Hello\");"
	})
	public void testSingleLine(String expression) {
		handle("method name (Z boolArg)V\n" + "expr " + expression + " end\nend", unit -> {
			List<Expression> x = unit.getCode().getExpressions();
			assertEquals(1, x.size());
			assertEquals(expression, x.get(0).getCode().trim());
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"System.out.println(\"hello\");",
			"if (boolArg) { \n         System.out.println(\"arg-true\");\n    }",
	})
	public void testMultiLine(String expression) {
		handle("method name (Z boolArg)V\n" + "expr " + expression + " end\nend", unit -> {
			List<Expression> x = unit.getCode().getExpressions();
			assertEquals(1, x.size());
			assertEquals(expression, x.get(0).getCode().trim());
		});
	}

	private static void handle(String original, Consumer<MethodDefinition> handler) {
		Unit unit = createSilentUnit(DEFAULT_KEYWORDS, original);

		assertNotNull(unit, "Parser did not find unit context with input: " + original);

		handler.accept(unit.getDefinitionAsMethod());
	}
}
