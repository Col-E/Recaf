package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.assemble.ast.arch.MethodParameters;
import me.coley.recaf.assemble.ast.arch.Modifiers;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.compiler.ClassSupplier;
import me.coley.recaf.assemble.transformer.ExpressionToAsmTransformer;
import me.coley.recaf.assemble.transformer.ExpressionToAstTransformer;
import me.coley.recaf.assemble.transformer.Variables;
import me.coley.recaf.util.IOUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionTransformTests {
	@Test
	public void testPrintParameterArg() {
		ExpressionToAstTransformer transformer = setup(new MethodParameter("Ljava/lang/String;", "text"));
		transformer.setLabelPrefixFunction(e -> "");
		try {
			Code code = transformer.transform(new Expression("System.out.println(text);"));
			String formatted = code.print();
			assertEquals("A:\n" +
					"GETSTATIC java/lang/System.out Ljava/io/PrintStream;\n" +
					"ALOAD text\n" +
					"INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V\n" +
					"B:", formatted);
		} catch (Exception ex) {
			fail(ex);
		}
	}

	@Test
	public void testTryCatch() {
		ExpressionToAstTransformer transformer = setup(new MethodParameter("Ljava/lang/String;", "path"));
		transformer.setLabelPrefixFunction(e -> "");
		try {
			Code code = transformer.transform(new Expression(
					"try { new java.io.File(path).createNewFile(); } " +
							"catch(java.io.IOException ex){}"));
			String formatted = code.print();
			assertEquals(1, code.getTryCatches().size());
			assertEquals("java/io/IOException", code.getTryCatches().get(0).getExceptionType());
			// 'ex' should not be stored in local slot 0 and should retain its defined name of 'ex'
			assertFalse(formatted.contains("ASTORE this"));
			assertTrue(formatted.contains("ASTORE ex"));
		} catch (Exception ex) {
			fail(ex);
		}
	}

	private static ExpressionToAstTransformer setup(MethodParameter... parameters) {
		ClassSupplier classSupplier = name -> {
			try {
				InputStream stream = ClassLoader.getSystemResourceAsStream(name.replace('.', '/') + ".class");
				if (stream == null)
					throw new IOException("Missing: " + name);
				return IOUtil.toByteArray(stream);
			} catch (IOException ex) {
				fail(ex);
				return null;
			}
		};
		String selfType = "java/lang/System";
		Modifiers modifiers = new Modifiers();
		MethodParameters params = new MethodParameters();
		for (MethodParameter parameter : parameters)
			params.add(parameter);
		MethodDefinition definition = new MethodDefinition(modifiers, "exampleMethod", params, "V");
		Variables variables = new Variables();
		try {
			variables.visitDefinition(selfType, definition);
			variables.visitParams(definition);
		} catch (Exception ex) {
			fail(ex);
		}
		ExpressionToAsmTransformer toAsmTransformer = new ExpressionToAsmTransformer(classSupplier, definition, variables, selfType);
		return new ExpressionToAstTransformer(definition, variables, toAsmTransformer);
	}
}
