package me.coley.recaf.parse;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import me.coley.recaf.Controller;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.presentation.EmptyPresentation;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;
import me.coley.recaf.workspace.resource.RuntimeResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JavaParserHelper}
 */
public class JavaParserHelperTests {
	private static Workspace workspace;
	private static Controller controller;

	@BeforeAll
	static void setup() {
		workspace = new Workspace(new Resources(RuntimeResource.get()));
		controller = new Controller(new EmptyPresentation());
		controller.setWorkspace(workspace);
	}

	@Test
	void resolveSystemOut() {
		String code = "import java.io.PrintStream;\n" +
				"class Demo extends Serializable {\n" +
				"    private PrintStream stream = System.out;\n" + // line 3, "out" starts at 40
				"    private String text = \"Hello\";\n" +
				"}";
		// Setup JP helper
		JavaParserHelper helper = JavaParserHelper.create(controller);
		ParseResult<CompilationUnit> result = helper.parseClass(code);
		CompilationUnit unit = result.getResult().get();
		// Resolve the 'out' symbol
		Optional<ParseHitResult> value = helper.at(unit, 3, 41);
		if (value.isPresent()) {
			ItemInfo item = value.get().getInfo();
			if (item instanceof FieldInfo) {
				FieldInfo fieldInfo = (FieldInfo) item;
				assertEquals("java/lang/System", fieldInfo.getOwner());
				assertEquals("out", fieldInfo.getName());
				assertEquals("Ljava/io/PrintStream;", fieldInfo.getDescriptor());
				assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, fieldInfo.getAccess());
			} else {
				fail("Resolved value should have been a field");
			}
		} else {
			fail("No value found at location, expected 'System.out'");
		}
	}
}
