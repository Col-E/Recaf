package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MemberDefinition;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.assemble.parser.BytecodeParser;
import me.coley.recaf.assemble.ast.BytecodeAstGenerator;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for definitions and other arch nodes.
 */
public class DefinitionArchTests extends TestUtil {
	@Test
	public void testSimpleDefinition() {
		handle("simple()V\n", unit -> {
			MemberDefinition def = unit.getDefinition();
			assertEquals("simple", def.getName());
			assertEquals("()V", def.getDesc());
			assertEquals(0, def.getModifiers().value());
			assertTrue(def.isMethod());
			assertFalse(def.isField());
		});
	}

	@Test
	public void testDefinitionWithParams() {
		handle("simple(Ljava/lang/String; param1, I param2)V\n", unit -> {
			MethodDefinition def = (MethodDefinition) unit.getDefinition();
			assertEquals(2, def.getParams().getParameters().size());
			MethodParameter parameter1 = def.getParams().getParameters().get(0);
			MethodParameter parameter2 = def.getParams().getParameters().get(1);
			assertEquals("param1", parameter1.getName());
			assertEquals("param2", parameter2.getName());
			assertEquals("Ljava/lang/String;", parameter1.getDesc());
			assertEquals("I", parameter2.getDesc());
			assertEquals("(Ljava/lang/String;I)", def.getParams().getDesc());
		});
	}

	@Test
	public void testConstValInt() {
		handle("simple I\n" + "CONST-VALUE 0", unit -> {
			assertNotNull(unit.getCode());
			assertNotNull(unit.getCode().getConstVal());
			assertEquals(0, unit.getCode().getConstVal().getValue());
		});
		handle("simple I\n" + "CONST-VALUE -5000", unit -> {
			assertNotNull(unit.getCode());
			assertNotNull(unit.getCode().getConstVal());
			assertEquals(-5000, unit.getCode().getConstVal().getValue());
		});
	}


	@Test
	public void testConstValLong() {
		handle("simple J\n" + "CONST-VALUE 0L", unit -> {
			assertNotNull(unit.getCode());
			assertNotNull(unit.getCode().getConstVal());
			assertEquals(0L, unit.getCode().getConstVal().getValue());
		});
		handle("simple J\n" + "CONST-VALUE -5000000000L", unit -> {
			assertNotNull(unit.getCode());
			assertNotNull(unit.getCode().getConstVal());
			assertEquals(-5000000000L, unit.getCode().getConstVal().getValue());
		});
	}

	@Test
	public void testConstValFloat() {
		handle("simple F\n" + "CONST-VALUE 0.5F", unit -> {
			assertNotNull(unit.getCode());
			assertNotNull(unit.getCode().getConstVal());
			assertEquals(0.5F, unit.getCode().getConstVal().getValue());
		});
		handle("simple F\n" + "CONST-VALUE -50.7F", unit -> {
			assertNotNull(unit.getCode());
			assertNotNull(unit.getCode().getConstVal());
			assertEquals(-50.7F, unit.getCode().getConstVal().getValue());
		});
	}

	@Test
	public void testConstValDouble() {
		handle("simple D\n" + "CONST-VALUE 0.54321", unit -> {
			assertNotNull(unit.getCode());
			assertNotNull(unit.getCode().getConstVal());
			assertEquals(0.54321, unit.getCode().getConstVal().getValue());
		});
		handle("simple D\n" + "CONST-VALUE -5000000000000000000000000000000000000000.5", unit -> {
			assertNotNull(unit.getCode());
			assertNotNull(unit.getCode().getConstVal());
			assertEquals(-5000000000000000000000000000000000000000.5, unit.getCode().getConstVal().getValue());
		});
	}

	@Test
	public void testConstValString() {
		handle("simple Ljava/lang/String;\n" + "CONST-VALUE \"hello\"", unit -> {
			assertNotNull(unit.getCode());
			assertNotNull(unit.getCode().getConstVal());
			assertEquals("hello", unit.getCode().getConstVal().getValue());
		});
	}

	@Test
	public void testThrownException() {
		handle("simple()V\n" + "THROWS java/lang/Exception", unit -> {
			assertNotNull(unit.getCode());
			assertEquals(1, unit.getCode().getThrownExceptions().size());
			assertEquals("java/lang/Exception", unit.getCode().getThrownExceptions().get(0).getExceptionType());
		});
	}

	@Test
	public void testThrownExceptionShadowsPrimitive() {
		handle("simple()V\n" + "THROWS I", unit -> {
			assertNotNull(unit.getCode());
			assertEquals(1, unit.getCode().getThrownExceptions().size());
			assertEquals("I", unit.getCode().getThrownExceptions().get(0).getExceptionType());
		});
	}

	@Test
	public void testThrownExceptionShadowsKeyword() {
		handle("simple()V\n" + "THROWS THROWS", unit -> {
			assertNotNull(unit.getCode());
			assertEquals(1, unit.getCode().getThrownExceptions().size());
			assertEquals("THROWS", unit.getCode().getThrownExceptions().get(0).getExceptionType());
		});
	}

	private static void handle(String original, Consumer<Unit> handler) {
		BytecodeParser parser = parser(original);

		BytecodeParser.UnitContext unitCtx = parser.unit();
		assertNotNull(unitCtx, "Parser did not find unit context with input: " + original);

		BytecodeAstGenerator visitor = new BytecodeAstGenerator();
		Unit unit = visitor.visitUnit(unitCtx);

		handler.accept(unit);
	}
}
