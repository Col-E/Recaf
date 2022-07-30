package me.coley.recaf.parse;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import me.coley.recaf.Controller;
import me.coley.recaf.presentation.EmptyPresentation;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;
import me.coley.recaf.workspace.resource.RuntimeResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JavaParserHelper}
 */
@Execution(ExecutionMode.SAME_THREAD)
public class JavaParserRecoveryTests {
	private static final String helloWorld = "class HelloWorld {\n" +
			"    public static void main(String[] args){\n" +
			"        System.out.println(\"Hello\");\n" +
			"        return;\n" +
			"    }\n" +
			"}";
	private static final String demoException = "class DemoException extends Exception {\n" +
			"    public Demo(String message){\n" +
			"        super(message);\n" +
			"        System.out.println(\"Post-super\");\n" +
			"    }\n" +
			"}";
	private static final String controlFlow = "class Flow {\n" +
			"    public static void main(String[] args){\n" +
			"        if (args == null) {\n" +
			"            System.err.println(\"No args!\");\n" +
			"            return;\n" +
			"        }\n" +
			"        handle(args);\n" +
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
		String code = helloWorld.replace(");", ")");
		runHello(code, unit -> "Recovered missing semicolon from 'println':\n" + unit.toString());
	}

	@Test
	void missingSemicolonOnAssignment() {
		String code = helloWorld.replace("return;", "args = null\n        return;");
		runHello(code, unit -> "Recovered missing semicolon from variable assignment:\n" + unit.toString());
	}

	@Test
	void missingCloseQuote() {
		String code = helloWorld.replace("\");", ");");
		runHello(code, unit -> "Recovered missing close quote from 'println':\n" + unit.toString());
	}

	@Test
	void missingValueAssign() {
		String code = helloWorld.replace("return;", "args = \n        return;");
		runHello(code, unit -> "Recovered missing completion of variable assignment:\n" + unit.toString());
	}

	@Test
	void testCfrPseudoGoto() {
		String code = controlFlow
				.replace("if ", "block: if ")
				.replace("return;", "** GOTO block");
		runCFlow(code, unit -> "Recovered broken CFR pseudo-goto:\n" + unit.toString());
	}

	@Test
	void illegalCodeBeforeSuper() {
		String code = demoException.replace("super(message);",
				"System.out.println(\"Pre-super\");\n" +
						"        super(message);"
		);
		runDemoException(code, unit -> "Recovered illegal code before 'super()' call in constructor:\n" + unit.toString());
	}

	@Test
	void illegalCodeBeforeThis() {
		String code = demoException.replace("super(message);",
				"System.out.println(\"Pre-super\");\n" +
						"        this(message);"
		);
		runDemoException(code, unit -> "Recovered illegal code before 'super()' call in constructor:\n" + unit.toString());
	}

	private static void runHello(String code, Function<CompilationUnit, String> recoverMessage) {
		int line = 3;
		int column = 21;
		String expectedResolvedName = "println";
		String resolveFailMessage = "Failed to resolve 'System.out.println()'?";
		template(code, recoverMessage, line, column, expectedResolvedName, resolveFailMessage);
	}

	private static void runCFlow(String code, Function<CompilationUnit, String> recoverMessage) {
		int line = 4;
		int column = 26;
		String expectedResolvedName = "println";
		String resolveFailMessage = "Failed to resolve 'System.err.println()'?";
		template(code, recoverMessage, line, column, expectedResolvedName, resolveFailMessage);
	}

	private static void runDemoException(String code, Function<CompilationUnit, String> recoverMessage) {
		int line = 5;
		int column = 21;
		String expectedResolvedName = "println";
		String resolveFailMessage = "Failed to resolve 'System.out.println()'?";
		template(code, recoverMessage, line, column, expectedResolvedName, resolveFailMessage);
	}

	private static void template(String code, Function<CompilationUnit, String> recoverMessage,
								 int line, int column, String expectedResolvedName, String resolveFailMessage) {
		System.out.println("============= ORIGINAL CODE =============");
		System.out.println(code);
		JavaParserHelper helper = JavaParserHelper.create(controller);
		ParseResult<CompilationUnit> result = helper.parseClass(code, true);
		if (result.getResult().isPresent()) {
			CompilationUnit unit = result.getResult().get();
			System.out.println("=============== FIXED CODE ==============");
			System.out.println("// " + recoverMessage.apply(unit));
			// Resolve the 'println' symbol
			Optional<ParseHitResult> value = helper.at(unit, line, column); // Should point to println
			assertTrue(value.isPresent(), resolveFailMessage);
			assertEquals(expectedResolvedName, value.get().getInfo().getName(), resolveFailMessage);
		} else {
			fail("Failed to recover");
		}
	}
}
