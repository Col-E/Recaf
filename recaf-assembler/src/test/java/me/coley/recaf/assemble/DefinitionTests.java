package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.arch.*;
import me.coley.recaf.assemble.ast.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for definitions and other arch nodes.
 */
public class DefinitionTests extends JasmUtils {
	@Test
	public void testSimpleDefinition() {
		handle("method simple ()V\nend", unit -> {
			Definition def = unit.getDefinition();
			assertEquals("simple", def.getName());
			assertEquals("()V", def.getDesc());
			assertEquals(0, def.getModifiers().value());
			assertTrue(def.isMethod());
			assertFalse(def.isField());
		});
	}

	@Test
	public void testDefinitionWithParams() {
		handle("method simple (Ljava/lang/String; param1,  I param2)V\nend", unit -> {
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

	@ParameterizedTest
	@ValueSource(strings = {
			"()Ljava/util/Set<TV;>;",
			"()Ljava/util/Map<TT;TV;>;",
			"(TT;)TV;",
			"(I[ITT;ITT;J[TT;)V",
			"Ljava/util/Set<Ljava/util/Set<Ljava/util/Set<Ljava/util/Set<Ljava/util/Set<TV;>;>;>;>;>;",
			"Ljava/util/Map<TT;TV;>;",
	})
	public void testSignature(String sig) {
		handle("signature " + sig + "\nmethod simple ()V\nend", unit -> {
			MethodDefinition method = unit.getDefinitionAsMethod();
			assertEquals(sig, method.getSignature().getSignature());
		});
	}

	@Test
	public void testConstValInt() {
		handle("field simple I\n" + "0", unit -> {
			FieldDefinition field = unit.getDefinitionAsField();
			assertNotNull(field.getConstVal());
			assertEquals(0, field.getConstVal().getValue());
		});
		handle("field simple I\n" + "-5000", unit -> {
			FieldDefinition field = unit.getDefinitionAsField();
			assertNotNull(field.getConstVal());
			assertEquals(-5000, field.getConstVal().getValue());
		});
	}


	@Test
	public void testConstValLong() {
		handle("field simple J\n" + "0L", unit -> {
			FieldDefinition field = unit.getDefinitionAsField();
			assertNotNull(field.getConstVal());
			assertEquals(0L, field.getConstVal().getValue());
		});
		handle("field simple J\n" + "-5000000000L", unit -> {
			FieldDefinition field = unit.getDefinitionAsField();
			assertNotNull(field.getConstVal());
			assertEquals(-5000000000L, field.getConstVal().getValue());
		});
	}

	@Test
	public void testConstValFloat() {
		handle("field simple F\n" + "0.5F", unit -> {
			FieldDefinition field = unit.getDefinitionAsField();
			assertNotNull(field.getConstVal());
			assertEquals(0.5F, field.getConstVal().getValue());
		});
		handle("field simple F\n" + "-50.7F", unit -> {
			FieldDefinition field = unit.getDefinitionAsField();
			assertNotNull(field.getConstVal());
			assertEquals(-50.7F, field.getConstVal().getValue());
		});
	}

	@Test
	public void testConstValDouble() {
		handle("field simple D\n" + "0.54321", unit -> {
			FieldDefinition field = unit.getDefinitionAsField();
			assertNotNull(field.getConstVal());
			assertEquals(0.54321, field.getConstVal().getValue());
		});
		handle("field simple D\n" + " -5000000000000000000000000000000000000000.5", unit -> {
			FieldDefinition field = unit.getDefinitionAsField();
			assertNotNull(field.getConstVal());
			assertEquals(-5000000000000000000000000000000000000000.5, field.getConstVal().getValue());
		});
	}

	@Test
	public void testConstValString() {
		handle("field simple Ljava/lang/String;\n" + "\"hello\"", unit -> {
			FieldDefinition field = unit.getDefinitionAsField();
			assertNotNull(field.getConstVal());
			assertEquals("hello", field.getConstVal().getValue());
		});
	}

	@Test
	public void testThrownException() {
		handle("throws java/lang/Exception" + "\nmethod simple ()V\nend" , unit -> {
			MethodDefinition method = unit.getDefinitionAsMethod();
			assertNotNull(method.getCode());
			assertEquals(1, method.getThrownExceptions().size());
			assertEquals("java/lang/Exception", method.getThrownExceptions().get(0).getExceptionType());
		});
	}

	@Test
	public void testThrownExceptionShadowsPrimitive() {
		handle("throws I" + "\nmethod simple ()V\nend", unit -> {
			MethodDefinition method = unit.getDefinitionAsMethod();
			assertNotNull(method.getCode());
			assertEquals(1, method.getThrownExceptions().size());
			assertEquals("I", method.getThrownExceptions().get(0).getExceptionType());
		});
	}

	@Test
	public void testThrownExceptionShadowsKeyword() {
		handle("throws throws" + "\nmethod simple ()V\nend", unit -> {
			MethodDefinition method = unit.getDefinitionAsMethod();
			assertNotNull(method.getCode());
			assertEquals(1, method.getThrownExceptions().size());
			assertEquals("throws", method.getThrownExceptions().get(0).getExceptionType());
		});
	}

	@Test
	public void testAnno() {
		handle("annotation com/example/MyAnno numArg 500 strArg \"hello\" end" + "\nmethod simple ()V\nend", unit -> {
			MethodDefinition method = unit.getDefinitionAsMethod();
			assertNotNull(method.getCode());
			assertEquals(1, method.getAnnotations().size());
			Annotation annotation = method.getAnnotations().get(0);
			assertTrue(annotation.isVisible());
			assertEquals("com/example/MyAnno", annotation.getType());
			assertEquals(2, annotation.getArgs().size());
			assertEquals(500, annotation.getArgs().get("numArg").getValue());
			assertEquals("hello", annotation.getArgs().get("strArg").getValue());
		});
	}

	@Test
	public void testAnnoWithEnum() {
		handle("annotation com/example/MyAnno v annotation-enum com/example/Example NAME end" + "\nmethod simple ()V\nend", unit -> {
			MethodDefinition method = unit.getDefinitionAsMethod();
			assertNotNull(method.getCode());
			assertEquals(1, method.getAnnotations().size());
			Annotation annotation = method.getAnnotations().get(0);
			assertTrue(annotation.isVisible());
			assertEquals("com/example/MyAnno", annotation.getType());
			assertEquals(1, annotation.getArgs().size());
			Annotation.AnnoEnum annoEnum = (Annotation.AnnoEnum) annotation.getArgs().get("v");
			assertEquals("com/example/Example", annoEnum.getEnumType());
			assertEquals("NAME", annoEnum.getEnumName());
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testAnnoWithList() {
		handle("annotation com/example/MyAnno list args 1 2 3 end end" + "\nmethod simple ()V\nend", unit -> {
			MethodDefinition method = unit.getDefinitionAsMethod();
			assertNotNull(method.getCode());
			assertEquals(1, method.getAnnotations().size());
			Annotation annotation = method.getAnnotations().get(0);
			assertTrue(annotation.isVisible());
			assertEquals("com/example/MyAnno", annotation.getType());
			assertEquals(1, annotation.getArgs().size());
			List<Annotation.AnnoArg> list = (List<Annotation.AnnoArg>) annotation.getArgs().get("list").getValue();
			assertEquals(3, list.size());
			assertEquals(1, list.get(0).getValue());
			assertEquals(2, list.get(1).getValue());
			assertEquals(3, list.get(2).getValue());
		});
	}

	private static void handle(String original, Consumer<Unit> handler) {
		Unit unit = createSilentUnit(DEFAULT_KEYWORDS, original);
		assertNotNull(unit, "Parser did not find unit context with input: " + original);

		handler.accept(unit);
	}
}
