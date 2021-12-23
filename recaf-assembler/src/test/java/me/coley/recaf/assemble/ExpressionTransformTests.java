package me.coley.recaf.assemble;

import javassist.CannotCompileException;
import javassist.bytecode.BadBytecode;
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
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionTransformTests {

	// TODO: More varied test cases

	@Test
	public void test() {
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
		params.add(new MethodParameter("Ljava/lang/String;", "text"));
		MethodDefinition definition = new MethodDefinition(modifiers, "exampleMethod", params, "V");
		Variables variables = new Variables();
		try {
			variables.visitDefinition(selfType, definition);
			variables.visitParams(definition);
		} catch (Exception ex) {
			fail(ex);
		}
		ExpressionToAsmTransformer toAsmTransformer = new ExpressionToAsmTransformer(classSupplier, definition, variables, selfType);
		ExpressionToAstTransformer transformer = new ExpressionToAstTransformer(definition, variables, toAsmTransformer);
		// Ok now the setup is done, we can actually test things out
		try {
			Code code = transformer.transform(new Expression("System.out.println(text);"));
			System.out.println(code.print());
		} catch (Exception ex) {
			fail(ex);
		}
	}
}
