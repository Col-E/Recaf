package me.coley.recaf.compile;

import me.coley.recaf.TestUtils;
import me.coley.recaf.compile.javac.JavacCompiler;
import me.coley.recaf.code.ClassInfo;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class JavacTests extends TestUtils implements Opcodes {
	@Test
	void compileSampleDefaults() throws IOException {
		// input
		Path source = sourcesDir.resolve("Sample.java");
		String name = "Sample";
		String code = new String(Files.readAllBytes(source));
		// compile
		JavacCompiler compiler = new JavacCompiler();
		CompilerResult result = compiler.compile(name, code);
		// validate
		assertTrue(result.wasSuccess());
		assertEquals(1, result.getValue().size());
		byte[] compiled = result.getValue().get(name);
		assertNotNull(compiled);
		ClassInfo info = ClassInfo.read(compiled);
		assertEquals(name, info.getName());
		assertEquals(0, info.getFields().size());
		assertEquals(2, info.getMethods().size()); // main + ctor
	}

	@Test
	void compileSampleFailing() throws IOException {
		// input
		Path source = sourcesDir.resolve("SampleFailing.java");
		String name = "Sample";
		String code = new String(Files.readAllBytes(source));
		// compile
		JavacCompiler compiler = new JavacCompiler();
		CompilerResult result = compiler.compile(name, code);
		// validate
		assertFalse(result.wasSuccess());
		assertEquals(2, result.getErrors().size());
	}
}
