package me.coley.recaf;

import me.coley.recaf.parse.assembly.LineParseException;
import me.coley.recaf.parse.assembly.visitors.AssemblyVisitor;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.*;

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
		public void testMultiANewArray() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("MULTIANEWARRAY java/lang/String 2");
				// Two simple InsnNode instructions
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				MultiANewArrayInsnNode insn = (MultiANewArrayInsnNode) insns.get(0);
				assertEquals(MULTIANEWARRAY, insn.getOpcode());
				assertEquals("java/lang/String", insn.desc);
				assertEquals(2, insn.dims);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testBadMultiANewArray() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			assertThrows(LineParseException.class, () -> visitor.visit("MULTIANEWARRAY java/lang/String 2.0"));
			assertThrows(LineParseException.class, () -> visitor.visit("MULTIANEWARRAY java/lang/String 0"));
			assertThrows(LineParseException.class, () -> visitor.visit("MULTIANEWARRAY java/lang/String -1"));
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
			assertThrows(LineParseException.class, () -> visitor.visit("GETFIELD Dummy " + "Ljava" + "/lang/Stri"));
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
		public void testFieldOnlyOwner() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// Invalid because field only specifies owner
			assertThrows(LineParseException.class, () -> visitor.visit("GETFIELD Dummy"));
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
			assertThrows(LineParseException.class, () -> visitor.visit("INVOKESTATIC Dummy.call" + "(I"));
		}

		@Test
		public void testLdcString() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("LDC \"\"Hello\\nWorld\"\"");
				//
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				LdcInsnNode min = (LdcInsnNode) insns.get(0);
				assertEquals("\"Hello\nWorld\"", min.cst);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testLdcEmptyString() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("LDC \"\"");
				//
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				LdcInsnNode min = (LdcInsnNode) insns.get(0);
				assertEquals("", min.cst);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testLdcInt() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("LDC -100");
				//
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				LdcInsnNode min = (LdcInsnNode) insns.get(0);
				assertEquals(-100, min.cst);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testLdcFloat() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("LDC 1.2F");
				//
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				LdcInsnNode min = (LdcInsnNode) insns.get(0);
				assertEquals(1.2F, min.cst);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testLdcDouble() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("LDC 1.2");
				//
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				LdcInsnNode min = (LdcInsnNode) insns.get(0);
				assertEquals(1.2, min.cst);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testLdcType() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("LDC Ljava/lang/String;");
				//
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				LdcInsnNode min = (LdcInsnNode) insns.get(0);
				Type typeExpected = Type.getType("Ljava/lang/String;");
				Type typeActual = (Type) min.cst;
				assertEquals(typeExpected.getSort(), typeActual.getSort());
				assertEquals(typeExpected.toString(), typeActual.toString());
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testVarThis() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.setupMethod(ACC_PUBLIC, "()V");
				visitor.visit("ALOAD this");
				//
				Set<String> names = visitor.getVariables().names();
				Collection<Integer> indices = visitor.getVariables().indices();
				assertEquals(1, names.size());
				assertEquals(1, indices.size());
				assertEquals("this", names.iterator().next());
				assertEquals(0, indices.iterator().next());
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testVarFromDescriptor() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				// 0 = this
				// 1 = int
				// 2 = double
				// 4 = double (double takes two spaces)
				// 6 = object (double takes two spaces)
				visitor.setupMethod(ACC_PUBLIC, "(IDDLjava/lang/String;)V");
				visitor.visit("ALOAD this\nILOAD 1\nDLOAD 2\nDLOAD 4\nALOAD 6");
				// verify only "this" was specified as a name, all other vars are nameless
				Set<String> names = visitor.getVariables().names();
				assertEquals("this", names.iterator().next());
				assertEquals(1, names.size());
				// verify the parameter variables were registered properly
				Set<Integer> indices = visitor.getVariables().indices();
				assertEquals(5, indices.size());
				Integer[] expected = {0, 1, 2, 4, 6};
				Integer[] expectedSorts = {Type.OBJECT, Type.INT, Type.DOUBLE, Type.DOUBLE, Type.OBJECT};
				Integer[] actual = indices.toArray(new Integer[0]);
				assertArrayEquals(expected, actual);
				for (int i = 0; i < expected.length; i++)  {
					// type verification
					int index = expected[i];
					assertEquals(expectedSorts[i], visitor.getVariables().getSort(index), "Mismatch on index: " + index);
				}
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testVarFromInsns() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				// 0 = this
				// 1 = int
				// 2 = double
				// 4 = double (double takes two spaces)
				// 6 = object (double takes two spaces)
				visitor.setupMethod(ACC_PUBLIC, "()V");
				visitor.visit("ALOAD this\nILOAD 1\nDLOAD 2\nDLOAD 4\nALOAD 6");
				// verify only "this" was specified as a name, all other vars are nameless
				Set<String> names = visitor.getVariables().names();
				assertEquals("this", names.iterator().next());
				assertEquals(1, names.size());
				// verify the parameter variables were registered properly
				Set<Integer> indices = visitor.getVariables().indices();
				assertEquals(5, indices.size());
				Integer[] expected = {0, 1, 2, 4, 6};
				Integer[] expectedSorts = {Type.OBJECT, Type.INT, Type.DOUBLE, Type.DOUBLE, Type.OBJECT};
				Integer[] actual = indices.toArray(new Integer[0]);
				assertArrayEquals(expected, actual);
				for (int i = 0; i < expected.length; i++)  {
					// type verification
					int index = expected[i];
					assertEquals(expectedSorts[i], visitor.getVariables().getSort(index), "Mismatch on index: " + index);
				}
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testVarNoDupesCreated() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				// 0 = object
				visitor.setupMethod(ACC_STATIC, "()V");
				visitor.visit("ACONST_NULL\nASTORE 0\nALOAD 0\nPOP\nALOAD 0\nPOP");
				assertEquals(0, visitor.getVariables().names().size());
				// verify the parameter variables were registered properly
				Set<Integer> indices = visitor.getVariables().indices();
				assertEquals(1, indices.size());
				assertEquals(Type.OBJECT, visitor.getVariables().getSort(0));
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testVarNoDupesCreatedNamedIndex() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				// name = object
				visitor.setupMethod(ACC_STATIC, "()V");
				visitor.visit("ACONST_NULL\nASTORE name\nALOAD name\nPOP\nALOAD name\nPOP");
				// verify "name" was used
				Set<String> names = visitor.getVariables().names();
				assertEquals(1, names.size());
				assertEquals("name", names.iterator().next());
				// verify the parameter variables were registered properly
				Set<Integer> indices = visitor.getVariables().indices();
				assertEquals(1, indices.size());
				assertEquals(Type.OBJECT, visitor.getVariables().getSort(0));
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testVarsClearedAfterRevisit() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.setupMethod(ACC_STATIC, "()V");
				visitor.visit("ASTORE first");
				// verify "name" was used
				Set<String> names = visitor.getVariables().names();
				Set<Integer> indices = visitor.getVariables().indices();
				assertEquals(1, names.size());
				assertEquals(1, indices.size());
				assertEquals("first", names.iterator().next());
				assertEquals(0, indices.iterator().next());
				// revisit with different code
				visitor.visit("ASTORE second");
				names = visitor.getVariables().names();
				assertEquals(1, names.size());
				assertEquals(1, indices.size());
				assertEquals("second", names.iterator().next());
				assertEquals(0, indices.iterator().next());
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testVarConsistentType() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			visitor.setupMethod(ACC_PUBLIC, "()V");
			// Invalid because "this" is already registered as an object type in virtual methods
			assertThrows(LineParseException.class, () -> visitor.visit("ILOAD this"));
			assertThrows(LineParseException.class, () -> visitor.visit("LLOAD this"));
			assertThrows(LineParseException.class, () -> visitor.visit("FLOAD this"));
			assertThrows(LineParseException.class, () -> visitor.visit("DLOAD this"));
		}

		@Test
		public void testIinc() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				// name = int
				visitor.setupMethod(ACC_STATIC, "()V");
				visitor.visit("ICONST_1\nISTORE name\nIINC name 1");
				// verify "name" was used
				Set<String> names = visitor.getVariables().names();
				assertEquals(1, names.size());
				assertEquals("name", names.iterator().next());
				// verify the parameter variables were registered properly
				Set<Integer> indices = visitor.getVariables().indices();
				assertEquals(1, indices.size());
				assertEquals(Type.INT, visitor.getVariables().getSort(0));
				// verify instructions
				InsnList insns = visitor.getInsnList();
				assertEquals(3, insns.size());
				assertEquals(ICONST_1, insns.get(0).getOpcode());
				VarInsnNode vin = (VarInsnNode) insns.get(1);
				assertEquals(ISTORE, vin.getOpcode());
				assertEquals(0, vin.var);
				IincInsnNode iinc = (IincInsnNode) insns.get(2);
				assertEquals(0, iinc.var);
				assertEquals(1, iinc.incr);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testBadIinc() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			visitor.setupMethod(ACC_PUBLIC, "()V");
			// Invalid because 0 = "this" (Object type)
			assertThrows(LineParseException.class, () -> visitor.visit("IINC this 1"));
			assertThrows(LineParseException.class, () -> visitor.visit("IINC 0 1"));
		}

		@Test
		public void testLine() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("LABEL one\nLINE 1 one");
				//
				InsnList insns = visitor.getInsnList();
				assertEquals(2, insns.size());
				LabelNode lbl = (LabelNode) insns.get(0);
				LineNumberNode lln = (LineNumberNode) insns.get(1);
				assertEquals(1, lln.line);
				assertEquals(lbl, lln.start);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testBadLine() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// Invalid because "two" is not a label
			assertThrows(LineParseException.class, () -> visitor.visit("LABEL one\nLINE 1 two"));
			// Invalid because line number is negative
			assertThrows(LineParseException.class, () -> visitor.visit("LABEL one\nLINE -1 one"));
			// Invalid because "two" is not a label
			assertThrows(LineParseException.class, () -> visitor.visit("LINE 1 two"));
			// Invalid because no label is specified
			assertThrows(LineParseException.class, () -> visitor.visit("LABEL one\nLINE 1"));
			// Invalid because order is swapped
			assertThrows(LineParseException.class, () -> visitor.visit("LABEL one\nLINE one 1"));
		}

		@Test
		public void testLabelsClearedAfterRevisit() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.setupMethod(ACC_STATIC, "()V");
				visitor.visit("LABEL first");
				// verify "name" was used
				Set<String> names = visitor.getLabels().names();
				assertEquals(1, names.size());
				assertEquals("first", names.iterator().next());
				// revisit with different code
				visitor.visit("LABEL second");
				names = visitor.getLabels().names();
				assertEquals(1, names.size());
				assertEquals("second", names.iterator().next());
			} catch(LineParseException ex) {
				fail(ex);
			}
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
		public void testMultiANewArraySuggestType() {
			// I know you'll never do "new System[]" but it gets the point across
			List<String> suggestions = suggest("MULTIANEWARRAY java/lang/Sys");
			assertEquals(4, suggestions.size());
			assertEquals("java/lang/System", suggestions.get(0));
			assertEquals("java/lang/System$1", suggestions.get(1));
			assertEquals("java/lang/System$2", suggestions.get(2));
			assertEquals("java/lang/SystemClassLoaderAction", suggestions.get(3));
		}

		@Test
		public void testMultiANewArraySuggestNoDims() {
			List<String> suggestions = suggest("MULTIANEWARRAY java/lang/System 2");
			// Nothing to suggest when specifying dimensions
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
			List<String> suggestions = suggest("GETFIELD Dummy.field Lcalc/Ca");
			// Suggested class desc should be:
			// - Lcalc/Calculator;
			assertEquals(1, suggestions.size());
			assertEquals("Lcalc/Calculator;", suggestions.get(0));
		}

		@Test
		public void testFieldMissingVarNameButHasDesc() {
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
				return Collections.emptyList();
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
