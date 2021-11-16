package me.coley.recaf.assemble;

import com.google.common.reflect.ClassPath;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.parser.BytecodeParser;
import me.coley.recaf.assemble.transformer.AntlrToAstTransformer;
import me.coley.recaf.assemble.transformer.AstToMethodTransformer;
import me.coley.recaf.assemble.transformer.BytecodeToAstTransformer;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.ast.AstValidator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This will attempt to assemble <i><b>EVERY</b></i> method in the classpath.
 * Only run this manually if you think you broke something and want over 8000 test cases to look at.
 */
@Disabled
public class AssembleEntireClasspathTests extends TestUtil {
	@ParameterizedTest
	@MethodSource("lookup")
	public void test(String name) {
		String debug = "?";
		try {
			ClassReader cr = new ClassReader(name);
			ClassNode node = new ClassNode();
			cr.accept(node, ClassReader.SKIP_FRAMES);
			for (MethodNode method : node.methods) {
				String location = node.name + "." + method.name + method.desc;
				debug = "// " + location;
				BytecodeToAstTransformer transformer = new BytecodeToAstTransformer(node.name, method);
				transformer.visit();
				Unit unit = transformer.getUnit();
				assertNotNull(unit, "Failed to create unit from: " + location);
				String code = unit.print();
				debug += "\n" + code;
				assertNotNull(code, "Failed to disassemble: " + location);
				// ANTLR parse
				BytecodeParser parser = parser(code);
				BytecodeParser.UnitContext unitCtx = parser.unit();
				assertNotNull(unitCtx, "Parser did not find unit context! Input: " + location);

				// Transform to our AST
				AntlrToAstTransformer antlrToAstTransformer = new AntlrToAstTransformer();
				Unit unitAssembled = antlrToAstTransformer.visitUnit(unitCtx);

				// Validate
				AstValidator validator = new AstValidator(unitAssembled);
				validator.visit();
				for (ValidationMessage message : validator.getMessages()) {
					int line = message.getSource().getLine();
					String errMessage = "// " + line + " : " + message;
					System.err.println(errMessage);
				}
				if (!validator.getMessages().isEmpty()) {
					System.err.println(debug);
					assertEquals(0, validator.getMessages().size(), "There were validation errors");
				}

				// Generate
				AstToMethodTransformer generator = new AstToMethodTransformer(node.name, unitAssembled);
				try {
					generator.visit();
					MethodNode methodAssembled = generator.get();
					assertNotNull(methodAssembled);
				} catch (MethodCompileException ex) {
					System.err.println("// " + ex.getSource().getLine() + " : " + ex.getMessage());
					System.err.println(code);
					ex.printStackTrace();
					fail();
					return;
				}
			}
			// Sleep call is so if you try and stop it'll actually have a moment to acknowledge the request
			Thread.sleep(1);
		} catch (Exception ex) {
			fail(debug, ex);
		}
	}

	@SuppressWarnings("UnstableApiUsage")
	public static List<String> lookup() throws IOException {
		return ClassPath.from(AssembleEntireClasspathTests.class.getClassLoader())
				.getAllClasses()
				.stream()
				.map(ClassPath.ClassInfo::getName)
				.filter(name -> name.indexOf('$') == -1)
				.collect(Collectors.toList());
	}
}
