package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.BytecodeAstGenerator;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.generation.MethodBytecodeGenerator;
import me.coley.recaf.assemble.parser.BytecodeParser;
import me.coley.recaf.assemble.validation.ValidationMessage;
import me.coley.recaf.assemble.validation.ast.AstValidator;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.Types;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic setup to validate all bytecode assembly files in the test resources directory complete the entire
 * assembly process, generating verifiable methods.
 */
public class MethodParseTests extends TestUtil {
	private static final String SELF_CLASS = "com/example/FooBar";

	@ParameterizedTest
	@MethodSource("paths")
	public void testFoo(Path file) {
		handle(read(file), pass());
	}

	private static Consumer<MethodNode> pass() {
		return method -> {
			if (!AccessFlag.isAbstract(method.access))
				assertTrue(method.instructions.size() > 0, "Must have instructions if not abstract!");
			// The method should be compilable and verifiable
			ClassNode node = new ClassNode();
			node.name = SELF_CLASS;
			node.superName = Types.OBJECT_TYPE.getInternalName();
			node.version = Opcodes.V11;
			node.methods.add(method);
			try {
				// Verify method and see if it can be written
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
				node.accept(new CheckClassAdapter(cw));
			} catch (Throwable t) {
				fail("Method failed verification: " + method.name, t);
			}
		};
	}

	private static void handle(String original, Consumer<MethodNode> handler) {
		// ANTLR parse
		BytecodeParser parser = parser(original);
		BytecodeParser.UnitContext unitCtx = parser.unit();
		assertNotNull(unitCtx, "Parser did not find unit context with input: " + original);

		// Transform to our AST
		BytecodeAstGenerator visitor = new BytecodeAstGenerator();
		Unit unit = visitor.visitUnit(unitCtx);

		// Validate
		AstValidator validator = new AstValidator(unit);
		validator.visit();
		for (ValidationMessage message : validator.getMessages())
			System.err.println(message);
		assertEquals(0, validator.getMessages().size());

		// Generate
		MethodBytecodeGenerator generator = new MethodBytecodeGenerator(SELF_CLASS, unit);
		try {
			generator.visit();
			handler.accept(generator.get());
		} catch (MethodCompileException ex) {
			fail(ex);
		}
	}

	private static String read(Path path) {
		try {
			return new String(Files.readAllBytes(path));
		} catch (IOException e) {
			fail(e);
			throw new IllegalStateException();
		}
	}

	public static List<Path> paths() throws IOException {
		return Files.walk(Paths.get("src/test/resources"))
				.filter(p -> p.toString().endsWith(".txt"))
				.collect(Collectors.toList());
	}
}
