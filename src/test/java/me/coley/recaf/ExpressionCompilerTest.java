package me.coley.recaf;

import javassist.*;
import javassist.bytecode.*;
import me.coley.recaf.compiler.JavassistASMTranslator;
import me.coley.recaf.parse.bytecode.Disassembler;
import me.coley.recaf.compiler.JavassistCompiler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class ExpressionCompilerTest extends Base {
	private final ClassPool pool = ClassPool.getDefault();
	private final JavassistASMTranslator translator = new JavassistASMTranslator();

	@Nested
	class MethodCompilations {
		@Test
		public void testClone() throws Exception {
			CtClass ctPoint = pool.makeClass(getClasspathUrl("Point.class").openStream());
			System.out.println(disassembleMethod(ctPoint, "public Point clone() { return new Point(x, y); }"));
		}

		@Test
		public void testMoveX() throws Exception {
			CtClass ctPoint = pool.makeClass(getClasspathUrl("Point.class").openStream());
			System.out.println(disassembleMethod(ctPoint, "public void moveX(int dx) { x += dx; }"));
		}

		@Test
		public void testDistanceTo() throws Exception {
			CtClass ctPoint = pool.makeClass(getClasspathUrl("Point.class").openStream());
			System.out.println(disassembleMethod(ctPoint, "public double distanceTo(Point p) { " +
					"double dx2 = Math.pow((double)x-p.x, 2.0);" +
					"double dy2 = Math.pow((double)y-p.y, 2.0);" +
					"double d = Math.sqrt(dx2 + dy2);" +
					"return d;" +
					" }"));
		}
	}

	@Nested
	class BodyCompilations {
		@Test
		public void testMovePrint() throws Exception {
			CtClass ctPoint = pool.makeClass(getClasspathUrl("Point.class").openStream());
			CtMethod ctMethod = ctPoint.getMethod("move", "(II)V");
			System.out.println(disassembleStatement(ctPoint, ctMethod, "System.out.println(\"Moving by \" + dx + \",\" + dy);"));
			System.out.println();
			System.out.println(disassembleStatement(ctPoint, ctMethod, "System.out.println(\"Moved to \" + x + \",\" + y);"));
		}

		@Test
		public void testConditionalMovePrint() throws Exception {
			CtClass ctPoint = pool.makeClass(getClasspathUrl("Point.class").openStream());
			CtMethod ctMethod = ctPoint.getMethod("move", "(II)V");
			System.out.println(disassembleStatement(ctPoint, ctMethod, "if (dx > 0 && dy > 0) System.out.println(\"Moving by \" + dx + \",\" + dy);"));
		}
	}

	private String disassembleMethod(CtClass owner, String src) throws CannotCompileException, BadBytecode {
		CtMethod compiled = JavassistCompiler.compileMethod(owner, src);
		return translate(owner, compiled, compiled.getMethodInfo().getCodeAttribute());
	}

	private String disassembleStatement(CtClass owner, CtBehavior method, String src) throws CannotCompileException, BadBytecode {
		Bytecode compiled = JavassistCompiler.compileExpression(owner, method, src, Collections.emptyList(), null).getBytecode();
		return translate(owner, method, compiled.toCodeAttribute());
	}

	private String translate(CtClass owner, CtBehavior method, CodeAttribute code) throws CannotCompileException, BadBytecode {
		translator.visit(owner, code);
		return new Disassembler().disassemble(translator.toAsmMethod(method));
	}
}
