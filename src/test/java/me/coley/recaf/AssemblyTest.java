package me.coley.recaf;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.visitors.AssemblyVisitor;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for the assembly.
 *
 * @author Matt
 */
public class AssemblyTest extends Base {
	@Nested
	public class Format {
		@Test
		public void testInsn() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("ACONST_NULL\nARETURN");
				// Two simple InsnNode instructions
				InsnList insns = visitor.getInsnList();
				assertEquals(2, insns.size());
				assertEquals(ACONST_NULL, insns.get(0).getOpcode());
				assertEquals(ARETURN, insns.get(1).getOpcode());
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testBadInsn() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// AssemblyVisitor won't be able to resolve this fake opcode, thus throws a line
			// parser err
			assertThrows(LineParseException.class, () -> visitor.visit("ACONST_NOT_REAL"));
		}

		@Test
		public void testInt() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("BIPUSH 127");
				// Two simple InsnNode instructions
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				IntInsnNode insn = (IntInsnNode) insns.get(0);
				assertEquals(BIPUSH, insn.getOpcode());
				assertEquals(127, insn.operand);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testBadInt() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// Can't put a float where int required
			assertThrows(LineParseException.class, () -> visitor.visit("BIPUSH 127F"));
			assertThrows(LineParseException.class, () -> visitor.visit("BIPUSH 127.0"));
		}

		@Test
		public void testType() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("NEW java/lang/String");
				// Two simple InsnNode instructions
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				TypeInsnNode insn = (TypeInsnNode) insns.get(0);
				assertEquals(NEW, insn.getOpcode());
				assertEquals("java/lang/String", insn.desc);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testField() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("GETFIELD Dummy.in Ljava/lang/String;");
				//
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				FieldInsnNode fin = (FieldInsnNode) insns.get(0);
				assertEquals(GETFIELD, fin.getOpcode());
				assertEquals("Dummy", fin.owner);
				assertEquals("in", fin.name);
				assertEquals("Ljava/lang/String;", fin.desc);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testFieldMissingVarNameButHasDesc() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// Invalid because no name could be matched
			assertThrows(LineParseException.class,
					() -> visitor.visit("GETFIELD Dummy " + "Ljava" + "/lang/Stri"));
		}

		@Test
		public void testFieldIncompleteDesc() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// Invalid because field descriptor is not complete
			assertThrows(LineParseException.class, () -> visitor.visit("GETFIELD Dummy.in " +
					"Ljava/lang/Stri"));
		}

		@Test
		public void testFieldIncompleteArrayDesc() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// Invalid because field descriptor is not complete
			assertThrows(LineParseException.class, () -> visitor.visit("GETFIELD Dummy.in " +
					"[[Ljava/lang/Stri"));
		}

		@Test
		public void testMethod() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("INVOKESTATIC Dummy.call(I)Ljava/lang/String;");
				//
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				MethodInsnNode min = (MethodInsnNode) insns.get(0);
				assertEquals(INVOKESTATIC, min.getOpcode());
				assertEquals("Dummy", min.owner);
				assertEquals("call", min.name);
				assertEquals("(I)Ljava/lang/String;", min.desc);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testMethodMissingVarNameButHasDesc() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			assertThrows(LineParseException.class, () -> visitor.visit("INVOKESTATIC Dummy.(I)V"));
		}

		@Test
		public void testMethodIncompleteDescNoRet() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			assertThrows(LineParseException.class, () -> visitor.visit("INVOKESTATIC Dummy.call(I)"
			));
		}

		@Test
		public void testMethodIncompleteDescUnclosed() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			assertThrows(LineParseException.class,
					() -> visitor.visit("INVOKESTATIC Dummy.call" + "(I"));
		}
	}

	@Nested
	public class Suggestions {
		@AfterEach
		public void cleanup() {
			Recaf.setCurrentWorkspace(null);
		}

		@Test
		public void testInsnSuggest() {
			List<String> suggestions = suggest("ACONST_");
			// Only opcode to match should be ACONST_NULL
			assertEquals(1, suggestions.size());
			assertEquals("ACONST_NULL", suggestions.get(0));
		}

		@Test
		public void testBadInsnSuggest() {
			List<String> suggestions = suggest("ACONST_NOT_REAL");
			// Suggest doesn't care the opcode is bad.
			// We'll just return nothing
			assertEquals(0, suggestions.size());
		}

		@Test
		public void testTypeSuggestRuntime() {
			// I know you'll never do "new System" but it gets the point across
			List<String> suggestions = suggest("NEW java/lang/Sys");
			assertEquals(4, suggestions.size());
			assertEquals("java/lang/System", suggestions.get(0));
			assertEquals("java/lang/System$1", suggestions.get(1));
			assertEquals("java/lang/System$2", suggestions.get(2));
			assertEquals("java/lang/SystemClassLoaderAction", suggestions.get(3));
		}

		@Test
		public void testTypeSuggestWorkspace() {
			workspace("calc.jar");
			//
			List<String> suggestions = suggest("NEW calc/P");
			// Suggested class should be:
			// - calc/Parenthesis
			assertEquals(1, suggestions.size());
			assertEquals("calc/Parenthesis", suggestions.get(0));
		}

		@Test
		public void testFieldSuggestRuntimeOwner() {
			List<String> suggestions = suggest("GETFIELD java/lang/Sys");
			// Suggested classes should be:
			// - java/lang/System
			// - java/lang/System$1
			// - java/lang/System$2
			// - java/lang/SystemClassLoaderAction
			assertEquals(4, suggestions.size());
			assertEquals("java/lang/System", suggestions.get(0));
			assertEquals("java/lang/System$1", suggestions.get(1));
			assertEquals("java/lang/System$2", suggestions.get(2));
			assertEquals("java/lang/SystemClassLoaderAction", suggestions.get(3));
		}

		@Test
		public void testFieldSuggestWorkspaceOwner() {
			workspace("calc.jar");
			//
			List<String> suggestions = suggest("GETFIELD calc/P");
			// Suggested class should be:
			// - calc/Parenthesis
			assertEquals(1, suggestions.size());
			assertEquals("calc/Parenthesis", suggestions.get(0));
		}

		@Test
		public void testFieldNoSuggestFullWorkspaceOwner() {
			workspace("calc.jar");
			//
			List<String> suggestions = suggest("GETFIELD calc/Calculator");
			// Name is filled already
			assertEquals(0, suggestions.size());
		}

		@Test
		public void testFieldSuggestWorkspaceRefs() {
			workspace("calc.jar");
			//
			List<String> suggestions = suggest("GETFIELD calc/Calculator.M");
			// Suggested field should be:
			// - calc/Parenthesis.MAX_DEPTH I
			assertEquals(1, suggestions.size());
			assertEquals("MAX_DEPTH I", suggestions.get(0));
		}

		@Test
		public void testFieldSuggestNoDescWithPrimitive() {
			String[] types = {"I", "J", "B", "Z", "F", "D", "C"};
			for(String type : types) {
				List<String> suggestions = suggest("GETFIELD Dummy." + type);
				// No suggestions due to primitive type
				assertEquals(0, suggestions.size());
			}
		}

		@Test
		public void testFieldSuggestRuntimeDesc() {
			List<String> suggestions = suggest("GETFIELD java/lang/System.out Ljava/io/PrintStr");
			// Suggested class desc should be:
			// - Ljava/io/PrintStream;
			assertEquals(1, suggestions.size());
			assertEquals("Ljava/io/PrintStream;", suggestions.get(0));
		}

		@Test
		public void testFieldSuggestWorkspaceDesc() {
			workspace("calc.jar");
			//
			AssemblyVisitor visitor = new AssemblyVisitor();
			List<String> suggestions = suggest("GETFIELD Dummy.field Lcalc/Ca");
			// Suggested class desc should be:
			// - Lcalc/Calculator;
			assertEquals(1, suggestions.size());
			assertEquals("Lcalc/Calculator;", suggestions.get(0));
		}

		@Test
		public void testFieldMissingVarNameButHasDesc() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			List<String> suggestions = suggest("GETFIELD Dummy Ljava/lang/Stri");
			// Invalid because no name could be matched
			assertEquals(0, suggestions.size());

		}

		@Test
		public void testMethodSuggestRuntimeDesc() {
			List<String> suggestions = suggest("INVOKESTATIC java/io/PrintStream.printl");
			// Suggested method descs should be:
			// - java/io/PrintStream.println(...)V
			assertEquals(10, suggestions.size());
			for(String s : suggestions)
				assertTrue(s.matches("println\\(.*\\)V"));
		}

		private List<String> suggest(String code) {
			try {
				return new AssemblyVisitor().suggest(code);
			} catch(LineParseException ex) {
				fail(ex);
				return null;
			}
		}

		private void workspace(String file) {
			try {
				JavaResource res = new JarResource(getClasspathFile(file));
				Workspace workspace = new Workspace(res);
				Recaf.setCurrentWorkspace(workspace);
			} catch(IOException ex) {
				fail(ex);
			}
		}
	}
}
