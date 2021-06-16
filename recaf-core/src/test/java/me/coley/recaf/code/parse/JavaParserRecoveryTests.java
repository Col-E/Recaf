package me.coley.recaf.code.parse;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import me.coley.recaf.Controller;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.presentation.EmptyPresentation;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;
import me.coley.recaf.workspace.resource.RuntimeResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JavaParserHelper}
 */
public class JavaParserRecoveryTests {
	private static final String helloWorld = "class HelloWorld {\n" +
			"    public static void main(String[] args){\n" +
			"        System.out.println(\"Hello\");\n" +
			"        return;\n" +
			"    }\n" +
			"}";
	private static Workspace workspace;
	private static Controller controller;

	@BeforeAll
	static void setup() {
		workspace = new Workspace(new Resources(RuntimeResource.get()));
		controller = new Controller(new EmptyPresentation());
		controller.setWorkspace(workspace);
	}

	@Test
	void missingSemicolonOnStatement() {
		// TODO: Create more cases of this to see if the error reported is too general to assume
		String code = helloWorld.replace(");", ")");
		run(code, unit -> "Recovered missing semicolon from 'println':\n" + unit.toString());
	}

	@Test
	void missingCloseQuote() {
		String code = helloWorld.replace("\");", ");");
		run(code, unit -> "Recovered missing close quote from 'println':\n" + unit.toString());
	}

	private static void run(String code, Function<CompilationUnit, String> recoverMessage) {
		JavaParserHelper helper = JavaParserHelper.create(controller);
		ParseResult<CompilationUnit> result = helper.parseClass(code, true);
		if (result.getResult().isPresent()) {
			CompilationUnit unit = result.getResult().get();
			System.out.println(recoverMessage.apply(unit));
			// Resolve the 'println' symbol
			Optional<ItemInfo> value = helper.at(unit, 3, 21); // Should point to println
			assertTrue(value.isPresent(), "Failed to resolve 'System.out.println()'?");
			assertEquals("println", value.get().getName(), "Failed to resolve 'System.out'?");
		} else {
			fail("Failed to recover");
		}
	}
}
