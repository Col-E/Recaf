package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.parser.BytecodeParser;
import me.coley.recaf.assemble.transformer.AntlrToAstTransformer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExpressionParseTests extends TestUtil {
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
		handle("name(Z boolArg)V\n" + "EXPR " + expression, unit -> {
			List<Expression> x = unit.getCode().getExpressions();
			assertEquals(1, x.size());
			assertEquals(expression, x.get(0).getCode());
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{ \n    System.out.println(\"hello\");  \n  }",
			"{ \n    if (boolArg) { \n         System.out.println(\"arg-true\");\n    }\n\n  }",
	})
	public void testMultiLine(String expression) {
		handle("name(Z boolArg)V\n" + "EXPR " + expression, unit -> {
			List<Expression> x = unit.getCode().getExpressions();
			assertEquals(1, x.size());
			assertEquals(expression, x.get(0).getCode());
		});
	}

	private static void handle(String original, Consumer<Unit> handler) {
		BytecodeParser parser = parser(original);

		BytecodeParser.UnitContext unitCtx = parser.unit();
		assertNotNull(unitCtx, "Parser did not find unit context with input: " + original);

		AntlrToAstTransformer visitor = new AntlrToAstTransformer();
		Unit unit = visitor.visitUnit(unitCtx);

		handler.accept(unit);
	}
}
