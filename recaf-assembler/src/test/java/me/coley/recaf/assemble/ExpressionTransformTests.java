package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.assemble.ast.arch.MethodParameters;
import me.coley.recaf.assemble.ast.arch.Modifiers;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.transformer.ExpressionToAsmTransformer;
import me.coley.recaf.assemble.transformer.ExpressionToAstTransformer;
import me.coley.recaf.assemble.transformer.Variables;
import me.coley.recaf.assemble.util.ClassSupplier;
import me.coley.recaf.util.IOUtil;
import org.junit.jupiter.api.RepeatedTest;
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
			String formatted = code.print(PrintContext.DEFAULT_CTX);
			assertEquals("A:\n" +
					"	getstatic java/lang/System.out Ljava/io/PrintStream;\n" +
					"	aload text\n" +
					"	invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V\n" +
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
			String formatted = code.print(PrintContext.DEFAULT_CTX);
			assertEquals(1, code.getTryCatches().size());
			assertEquals("java/io/IOException", code.getTryCatches().get(0).getExceptionType());
			// 'ex' should not be stored in local slot 0 and should retain its defined name of 'ex'
			assertFalse(formatted.contains("astore this"));
			assertTrue(formatted.contains("astore ex"));
		} catch (Exception ex) {
			fail(ex);
		}
	}

	@RepeatedTest(20)
	public void testArrayParameterUsage() {
		// This test is run repeatedly to ensure that a 'random-order' regression does not resurface,
		// In the JavassistCompiler, variable population in a random order can lead to failures.
		ExpressionToAstTransformer transformer = setup(new MethodParameter("[Ljava/lang/String;", "xargs"));
		transformer.setLabelPrefixFunction(e -> "");
		try {
			Code code = transformer.transform(new Expression(
					"if (xargs != null){\n" +
							"  int length = xargs.length;\n" +
							"  switch(length) {\n" +
							"   case 0: System.out.println(\"One arg\"); break;\n" +
							"   case 1: System.out.println(\"Two args\"); break;\n" +
							"   default: System.out.println(\"Multiple args\"); break; \n" +
							"  }\n" +
							"} else {\n" +
							"  System.out.println(\"Null args\");\n" +
							"}"));
			String formatted = code.print(PrintContext.DEFAULT_CTX);
			// The argument 'args' should be read
			assertTrue(formatted.contains("aload xargs"));
			assertTrue(formatted.contains("istore length"));
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
		MethodDefinition definition = new MethodDefinition(modifiers, "exampleMethod", params, "V", new Code());
		Variables variables = new Variables();
		try {
			variables.visitImplicitThis(selfType, definition);
			variables.visitParams(definition);
		} catch (Exception ex) {
			fail(ex);
		}
		ExpressionToAsmTransformer toAsmTransformer = new ExpressionToAsmTransformer(classSupplier, definition, variables, selfType);
		return new ExpressionToAstTransformer(definition, variables, toAsmTransformer);
	}
}
