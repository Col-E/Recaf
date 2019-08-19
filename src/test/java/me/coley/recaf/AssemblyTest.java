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
			// AssemblyVisitor won't be able to resolve this fake opcode, thus throws a line parser err
			assertThrows(LineParseException.class, () -> visitor.visit("ACONST_NOT_REAL"));
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
			assertThrows(LineParseException.class, () -> visitor.visit("GETFIELD Dummy Ljava/lang/Stri"));
		}

		@Test
		public void testFieldIncompleteDesc() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// Invalid because field descriptor is not complete
			assertThrows(LineParseException.class, () -> visitor.visit("GETFIELD Dummy.in Ljava/lang/Stri"));
		}

		@Test
		public void testFieldIncompleteArrayDesc() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// Invalid because field descriptor is not complete
			assertThrows(LineParseException.class, () -> visitor.visit("GETFIELD Dummy.in [[Ljava/lang/Stri"));
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
			assertThrows(LineParseException.class, () -> visitor.visit("INVOKESTATIC Dummy.call(I)"));
		}

		@Test
		public void testMethodIncompleteDescUnclosed() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			assertThrows(LineParseException.class, () -> visitor.visit("INVOKESTATIC Dummy.call(I"));
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
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				List<String> suggestions = visitor.suggest("ACONST_");
				// Only opcode to match should be ACONST_NULL
				assertEquals(1, suggestions.size());
				assertEquals("ACONST_NULL", suggestions.get(0));
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testBadInsnSuggest() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				List<String> suggestions = visitor.suggest("ACONST_NOT_REAL");
				// Suggest doesn't care the opcode is bad.
				// We'll just return nothing
				assertEquals(0, suggestions.size());
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testFieldSuggestRuntimeOwner() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				List<String> suggestions = visitor.suggest("GETFIELD java/lang/Sys");
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
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testFieldSuggestWorkspaceOwner() {
			try {
				// Setup workspace
				JavaResource res = new JarResource(getClasspathFile("calc.jar"));
				Workspace workspace = new Workspace(res);
				Recaf.setCurrentWorkspace(workspace);
				//
				AssemblyVisitor visitor = new AssemblyVisitor();
				List<String> suggestions = visitor.suggest("GETFIELD calc/P");
				// Suggested class should be:
				// - calc/Parenthesis
				assertEquals(1, suggestions.size());
				assertEquals("calc/Parenthesis", suggestions.get(0));
			} catch(LineParseException | IOException ex) {
				fail(ex);
			}
		}

		@Test
		public void testFieldNoSuggestFullWorkspaceOwner() {
			try {
				// Setup workspace
				JavaResource res = new JarResource(getClasspathFile("calc.jar"));
				Workspace workspace = new Workspace(res);
				Recaf.setCurrentWorkspace(workspace);
				//
				AssemblyVisitor visitor = new AssemblyVisitor();
				List<String> suggestions = visitor.suggest("GETFIELD calc/Calculator");
				// Name is filled already
				assertEquals(0, suggestions.size());
			} catch(LineParseException | IOException ex) {
				fail(ex);
			}
		}

		@Test
		public void testFieldSuggestWorkspaceRefs() {
			try {
				// Setup workspace
				JavaResource res = new JarResource(getClasspathFile("calc.jar"));
				Workspace workspace = new Workspace(res);
				Recaf.setCurrentWorkspace(workspace);
				//
				AssemblyVisitor visitor = new AssemblyVisitor();
				List<String> suggestions = visitor.suggest("GETFIELD calc/Calculator.M");
				// Suggested field should be:
				// - calc/Parenthesis.MAX_DEPTH I
				assertEquals(1, suggestions.size());
				assertEquals("MAX_DEPTH I", suggestions.get(0));
			} catch(LineParseException | IOException ex) {
				fail(ex);
			}
		}

		@Test
		public void testFieldSuggestNoDescWithPrimitive() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				String[] types = {"I", "J", "B", "Z", "F", "D", "C"};
				for (String type :types) {
					List<String> suggestions = visitor.suggest("GETFIELD Dummy." + type);
					// No suggestions due to primitive type
					assertEquals(0, suggestions.size());
				}
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testFieldSuggestRuntimeDesc() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				List<String> suggestions = visitor.suggest("GETFIELD java/lang/System.out Ljava/io/PrintStr");
				// Suggested class desc should be:
				// - Ljava/io/PrintStream;
				assertEquals(1, suggestions.size());
				assertEquals("Ljava/io/PrintStream;", suggestions.get(0));
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testFieldSuggestWorkspaceDesc() {
			try {
				// Setup workspace
				JavaResource res = new JarResource(getClasspathFile("calc.jar"));
				Workspace workspace = new Workspace(res);
				Recaf.setCurrentWorkspace(workspace);
				//
				AssemblyVisitor visitor = new AssemblyVisitor();
				List<String> suggestions = visitor.suggest("GETFIELD Dummy.field Lcalc/Ca");
				// Suggested class desc should be:
				// - Lcalc/Calculator;
				assertEquals(1, suggestions.size());
				assertEquals("Lcalc/Calculator;", suggestions.get(0));
			} catch(LineParseException | IOException ex) {
				fail(ex);
			}
		}

		@Test
		public void testFieldMissingVarNameButHasDesc() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				List<String> suggestions = visitor.suggest("GETFIELD Dummy Ljava/lang/Stri");
				// Invalid because no name could be matched
				assertEquals(0, suggestions.size());
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testMethodSuggestRuntimeDesc() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				List<String> suggestions = visitor.suggest("INVOKESTATIC java/io/PrintStream.printl");
				// Suggested method descs should be:
				// - java/io/PrintStream.println(...)V
				assertEquals(10, suggestions.size());
				for (String s : suggestions)
					assertTrue(s.matches("println\\(.*\\)V"));
			} catch(LineParseException ex) {
				fail(ex);
			}
		}
	}
}
