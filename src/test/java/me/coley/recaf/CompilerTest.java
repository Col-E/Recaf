package me.coley.recaf;

import me.coley.recaf.compiler.*;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for the compiler.
 *
 * @author Matt
 */
public class CompilerTest {
	private final static DiagnosticListener<VirtualJavaFileObject> FAIL_ON_ERROR = message -> {
		assertNotSame(message.getKind(), Diagnostic.Kind.ERROR);
	};

	/**
	 * Tests generation of inner class units.
	 */
	@Test
	public void testInner() {
		// source code
		String s = "public class HelloWorld {" +
				"  public static void main(String args[])" +
				"  {" +
				"    A.print(\"Hello from an inner class\");" +
				"  }" +
				"  public" +
				" static class A {" +
				"    public static void print(String s){" +
				"        System.out.println(s);" +
				"    }" +
				"  }" +
				"}";
		// create the compiler, add the code
		JavacCompiler c = new JavacCompiler();
		c.addUnit("HelloWorld", s);
		c.setCompileListener(FAIL_ON_ERROR);
		assertTrue(c.compile());
		// compiled code
		byte[] outer = c.getUnitCode("HelloWorld");
		byte[] inner = c.getUnitCode("HelloWorld$A");
		assertNotNull(outer);
		assertNotNull(inner);
	}

	/**
	 * Tests generation of inner class units.
	 */
	@Test
	public void testDebug() {
		// source code
		StringBuilder s = new StringBuilder();
		s.append("public class HelloWorld {" +
				"  public static void main(String args[])" +
				"  {" +
				"    String a = \"Hello \";" +
				"    String b = \"World!\";" +
				"    System.out.print(a + b);" +
				"  }" +
				"}");
		// create the compilers, add the code
		JavacCompiler cDebug = new JavacCompiler();
		cDebug.addUnit("HelloWorld", s.toString());
		cDebug.options().lineNumbers = true;
		cDebug.options().variables = true;
		cDebug.options().sourceName = true;
		cDebug.options().setTarget(JavacTargetVersion.V8);
		cDebug.setCompileListener(FAIL_ON_ERROR);
		assertTrue(cDebug.compile());
		JavacCompiler cNone = new JavacCompiler();
		cNone.addUnit("HelloWorld", s.toString());
		cNone.setCompileListener(FAIL_ON_ERROR);
		assertTrue(cNone.compile());
		// compiled code
		byte[] debug = cDebug.getUnitCode("HelloWorld");
		byte[] nodebug = cNone.getUnitCode("HelloWorld");
		assertNotNull(debug);
		assertNotNull(nodebug);
		assertTrue(debug.length > nodebug.length);
	}
}