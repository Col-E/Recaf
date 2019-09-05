package me.coley.recaf;

import me.coley.recaf.parse.assembly.*;
import me.coley.recaf.parse.assembly.visitors.AssemblyVisitor;
import me.coley.recaf.workspace.*;
import org.junit.jupiter.api.*;
import org.objectweb.asm.Handle;
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
		public void testCommentInsn() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("ACONST_NULL\n// Comment line\nARETURN");
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
		public void testTypeWithInternalName() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("NEW java/lang/String");
				// One type instruction
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
		public void testTypeWithArrayDescriptor() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("CHECKCAST [Ljava/lang/String;");
				// One type instruction
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				TypeInsnNode insn = (TypeInsnNode) insns.get(0);
				assertEquals(CHECKCAST, insn.getOpcode());
				assertEquals("[Ljava/lang/String;", insn.desc);
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
		public void testBadField() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// Invalid because no name could be matched
			assertThrows(LineParseException.class, () -> visitor.visit("GETFIELD Dummy " + "Ljava" + "/lang/Stri"));
			// Invalid because field descriptor is not complete
			assertThrows(LineParseException.class, () -> visitor.visit("GETFIELD Dummy.in Ljava/lang/Stri"));
			// Invalid because field descriptor is not complete
			assertThrows(LineParseException.class, () -> visitor.visit("GETFIELD Dummy.in [[Ljava/lang/Stri"));
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
		public void testBadMethod() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// missing variable name, but has desc
			assertThrows(LineParseException.class, () -> visitor.visit("INVOKESTATIC Dummy.(I)V"));
			// missing return type
			assertThrows(LineParseException.class, () -> visitor.visit("INVOKESTATIC Dummy.call(I)"));
			// descriptor is incomplete
			assertThrows(LineParseException.class, () -> visitor.visit("INVOKESTATIC Dummy.call(I"));

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
		public void testLdcAliasString() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("ALIAS HELLO \"Hello World!\"\nLDC \"${HELLO}\"");
				//
				InsnList insns = visitor.getInsnList();
				assertEquals(1, insns.size());
				LdcInsnNode min = (LdcInsnNode) insns.get(0);
				assertEquals("Hello World!", min.cst);
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
		public void testGeneratedVarNodes() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.setDoAddVariables(true);
				// 0 = this
				// 1 = num
				// 2 = decimal
				// 4 = obj (double takes two spaces)
				visitor.setupMethod(ACC_PUBLIC, "()V");
				visitor.visit("ALOAD this\nILOAD num\nDLOAD decimal\nALOAD obj");
				// verify 4 names exist
				int visited = 0;
				MethodNode method = visitor.getMethod();
				for (LocalVariableNode lvn : method.localVariables) {
					visited++;
					switch(lvn.name) {
						case "this":
							assertEquals(0, lvn.index);
							assertEquals("Ljava/lang/Object;", lvn.desc);
							break;
						case "num":
							assertEquals(1, lvn.index);
							assertEquals("I", lvn.desc);
							break;
						case "decimal":
							assertEquals(2, lvn.index);
							assertEquals("D", lvn.desc);
							break;
						case "obj":
							assertEquals(4, lvn.index);
							assertEquals("Ljava/lang/Object;", lvn.desc);
							break;
						default:
							fail("Unknown var: " + lvn.name);
					}
				}
				assertEquals(4, visited);
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

		@Test
		public void testTable() {
			try {
				// Same code just with list identifiers present/not-present
				String[] codes = {
						"TABLESWITCH range[0-1] labels[A, B] default[C]\nLABEL A\nLABEL B\nLABEL C",
						"TABLESWITCH [0-1] [A, B] [C]\nLABEL A\nLABEL B\nLABEL C"
				};
				AssemblyVisitor visitor = new AssemblyVisitor();
				for (String code : codes ) {
					visitor.visit(code);
					//
					InsnList insns = visitor.getInsnList();
					assertEquals(4, insns.size());
					LabelNode lblA = (LabelNode) insns.get(1);
					LabelNode lblB = (LabelNode) insns.get(2);
					LabelNode lblC = (LabelNode) insns.get(3);
					TableSwitchInsnNode table = (TableSwitchInsnNode) insns.get(0);
					assertEquals(0, table.min);
					assertEquals(1, table.max);
					assertEquals(lblC, table.dflt);
					assertEquals(2, table.labels.size());
					assertEquals(lblA, table.labels.get(0));
					assertEquals(lblB, table.labels.get(1));
				}
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testBadTable() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// Invalid because missing range/labels/default
			assertThrows(LineParseException.class, () -> visitor.visit("TABLESWITCH"));
			// Invalid because missing labels/default
			assertThrows(LineParseException.class, () -> visitor.visit("TABLESWITCH range[0-1]"));
			// Invalid because missing default
			assertThrows(LineParseException.class, () -> visitor.visit("TABLESWITCH range[0-1] labels[A, B]\nLABEL A\nLABEL B"));
			// Invalid because missing range size != label count
			assertThrows(LineParseException.class, () -> visitor.visit("TABLESWITCH range[0-1] labels[B] default[C]\nLABEL A\nLABEL B\nLABEL C"));
			// Invalid because missing range size != label count
			assertThrows(LineParseException.class, () -> visitor.visit("TABLESWITCH r[0-1] l[A, B, C] d[D]\nLABEL A\nLABEL B\nLABEL C\nLABEL D"));
			// Invalid because range format invalid
			assertThrows(LineParseException.class, () -> visitor.visit("TABLESWITCH r[0] l[A, B] d[C]\nLABEL A\nLABEL B\nLABEL C"));
			// Invalid because default empty
			assertThrows(LineParseException.class, () -> visitor.visit("TABLESWITCH r[0] l[A, B] d[]\nLABEL A\nLABEL B"));
		}

		@Test
		public void testLookup() {
			try {
				// Same code just with list identifiers present/not-present
				String[] codes = {
						"LOOKUPSWITCH mapping[0=A, 1=B] default[C]\nLABEL A\nLABEL B\nLABEL C",
						"LOOKUPSWITCH [0=A, 1=B] [C]\nLABEL A\nLABEL B\nLABEL C",
				};
				AssemblyVisitor visitor = new AssemblyVisitor();
				for (String code : codes ) {
					visitor.visit(code);
					//
					InsnList insns = visitor.getInsnList();
					assertEquals(4, insns.size());
					LabelNode lblA = (LabelNode) insns.get(1);
					LabelNode lblB = (LabelNode) insns.get(2);
					LabelNode lblC = (LabelNode) insns.get(3);
					LookupSwitchInsnNode lookup = (LookupSwitchInsnNode) insns.get(0);
					assertEquals(Arrays.asList(0, 1), lookup.keys);
					assertEquals(Arrays.asList(lblA, lblB), lookup.labels);
					assertEquals(lblC, lookup.dflt);
				}
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testBadLookup() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			// Invalid because missing mapping/default
			assertThrows(LineParseException.class, () -> visitor.visit("LOOKUPSWITCH"));
			// Invalid because missing default
			assertThrows(LineParseException.class, () -> visitor.visit("LOOKUPSWITCH [0=A, 1=B]\nLABEL A\nLABEL B"));
			// Invalid because mapping format invalid
			assertThrows(LineParseException.class, () -> visitor.visit("LOOKUPSWITCH [0=A, 1] [C]\nLABEL A\nLABEL B\nLABEL C"));
			// Invalid because default empty
			assertThrows(LineParseException.class, () -> visitor.visit("LOOKUPSWITCH [0=A, 1=B] []\nLABEL A\nLABEL B"));
		}

		@Test
		public void testAddException() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("THROWS test");
				List<String> exceptions = visitor.getMethod().exceptions;
				assertEquals(1, exceptions.size());
				assertEquals("test", exceptions.get(0));
			} catch(LineParseException ex) {
				fail(ex);
			}
		}


		@Test
		public void testAddCatch() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.visit("CATCH test A B C\nLABEL A\nLABEL B\nLABEL C");
				// Only the labels are actual insns
				// CATCH is a pseudo-op
				InsnList insns = visitor.getInsnList();
				assertEquals(3, insns.size());
				LabelNode lblA = (LabelNode) insns.get(0);
				LabelNode lblB = (LabelNode) insns.get(1);
				LabelNode lblC = (LabelNode) insns.get(2);
				List<TryCatchBlockNode> catchBlocks = visitor.getMethod().tryCatchBlocks;
				assertEquals(1, catchBlocks.size());
				TryCatchBlockNode block = catchBlocks.get(0);
				assertEquals("test", block.type);
				assertEquals(lblA, block.start);
				assertEquals(lblB, block.end);
				assertEquals(lblC, block.handler);
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testInvokeDynamic() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				// "H_META" is an alias for the super common LambdaMetafactory.metafactory(...) call
				visitor.visit("ALIAS H_META \"" + H_META + "\"\nINVOKEDYNAMIC handle (Lgame/SnakeController;)" +
						"Ljavafx/event/EventHandler; ${H_META} args[handle[H_INVOKESTATIC game/FxMain" +
						" lambda$start$0 (Lgame/SnakeController;Ljavafx/scene/input/KeyEvent;)V], " +
						"(Ljavafx/event/Event;)V, (Ljavafx/scene/input/KeyEvent;)V]");
				// check base values
				InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) visitor.getInsnList().get(0);
				assertEquals("handle", indy.name);
				assertEquals("(Lgame/SnakeController;)Ljavafx/event/EventHandler;", indy.desc);
				// bsm handle is aliased by "H_META"
				assertEquals(H_INVOKESTATIC, indy.bsm.getTag());
				assertEquals("java/lang/invoke/LambdaMetafactory", indy.bsm.getOwner());
				assertEquals("metafactory", indy.bsm.getName());
				assertEquals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
						"Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;" +
						"Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)" +
						"Ljava/lang/invoke/CallSite;", indy.bsm.getDesc());
				// check args
				assertEquals(3, indy.bsmArgs.length);
				Handle h = (Handle) indy.bsmArgs[0];
				assertEquals(H_INVOKESTATIC, h.getTag());
				assertEquals("game/FxMain", h.getOwner());
				assertEquals("lambda$start$0", h.getName());
				assertEquals("(Lgame/SnakeController;Ljavafx/scene/input/KeyEvent;)V", h.getDesc());
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

	}

	@Nested
	public class Disass {
		@Test
		public void testInsn() {
			same("ACONST_NULL");
		}

		@Test
		public void testTwoInsns() {
			same("ACONST_NULL\nARETURN");
		}

		@Test
		public void testInt() {
			same("BIPUSH 127");
		}

		@Test
		public void testType() {
			same("NEW java/lang/String");
			same("CHECKCAST [Ljava/lang/String;");
		}

		@Test
		public void testMultiANewArray() {
			same("MULTIANEWARRAY java/lang/String 2");
		}

		@Test
		public void testField() {
			same("GETFIELD java/lang/System.out Ljava/io/PrintStream;");
		}

		@Test
		public void testMethod() {
			same("INVOKESTATIC Dummy.call(I)Ljava/lang/String;");
		}

		@Test
		public void testLdc() {
			// Quotes in string and newline
			same("LDC \"\"Hello\\nWorld\"\"");
			same("LDC \"\"");
			same("LDC -100");
			same("LDC 10.5F");
			same("LDC 10.5D");
		}

		@Test
		public void testVar() {
			// Variable emitting is off in the assembler for this test
			//
			// Test virtual 'this'
			same("ALOAD this");
			// Test non-named vars
			same("ALOAD 1");
		}

		@Test
		public void testVarWithCustomName() {
			try {
				// Assemble the text
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.setupMethod(ACC_PUBLIC, "()V");
				visitor.setDoAddVariables(true);
				visitor.visit("ALOAD second");
				MethodNode method = visitor.getMethod();
				// Disassemble the assembled method
				// They should be the same text.
				String text = new Disassembler().disassemble(method);
				assertTrue(text.contains("ALOAD second"));
			} catch(LineParseException ex) {
				fail(ex);
			}
		}

		@Test
		public void testIinc() {
			// Use non-named variable since variable emitting is off
			same("IINC 1 1");
			same("IINC 1 -1");
		}

		@Test
		public void testLine() {
			// Using label 'A' since names are lost on assemble
			// - 'A' is the first default generated name
			same("LABEL A\nLINE 1 A");
		}

		@Test
		public void testTable() {
			same("TABLESWITCH range[0-1] labels[A, B] default[C]\nLABEL A\nLABEL B\nLABEL C");
		}

		@Test
		public void testLookup() {
			same("LOOKUPSWITCH mapping[0=A, 1=B] default[C]\nLABEL A\nLABEL B\nLABEL C");
		}

		@Test
		public void testInvokeDynamic() {
			// And this is why I put off Indy support for so long...
			same("INVOKEDYNAMIC handle (Lgame/SnakeController;)Ljavafx/event/EventHandler; " + H_META +
					" args[handle[H_INVOKESTATIC game/FxMain " +
					"lambda$start$0 (Lgame/SnakeController;Ljavafx/scene/input/KeyEvent;)V], " +
					"(Ljavafx/event/Event;)V, (Ljavafx/scene/input/KeyEvent;)V]", false);
		}


		private void same(String text) {
			same(text, false);
		}

		private void same(String text, boolean simplify) {
			try {
				// Assemble the text
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.setupMethod(ACC_PUBLIC, "()V");
				visitor.visit(text);
				MethodNode method = visitor.getMethod();
				// Disassemble the assembled method
				// They should be the same text.
				// - Do not simplify invokeDynamic's handles
				Disassembler d = new Disassembler();
				d.setUseIndyAlias(simplify);
				assertEquals(text, d.disassemble(method));
			} catch(LineParseException ex) {
				fail(ex);
			}
		}
	}

	@Nested
	public class Verify {
		@Test
		public void testBasicPass() {
			try {
				AssemblyVisitor visitor = new AssemblyVisitor();
				visitor.setupMethod(ACC_PUBLIC, "()Ljava/lang/Object;");
				// simple "return null" method
				visitor.visit("ACONST_NULL\nARETURN");
				visitor.verify();
			} catch(LineParseException | VerifyException ex) {
				// This should NOT occur, code is valid
				fail(ex);
			}
		}

		@Test
		public void testMissingRetOnVoidMethod() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			visitor.setupMethod(ACC_PUBLIC, "()V");
			try {
				visitor.visit("NOP");
				// No return on void type
				visitor.verify();
			} catch(LineParseException ex) {
				fail(ex);
			} catch(VerifyException ex) {
				// Assert that the RETURN is the "cause" instruction
				assertEquals(null, ex.getInsn());
			}
		}

		@Test
		public void testPopEmptyStack() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			visitor.setupMethod(ACC_PUBLIC, "()V");
			try {
				visitor.visit("POP\nRETURN");
				// Nothing to pop
				visitor.verify();
			} catch(LineParseException ex) {
				fail(ex);
			} catch(VerifyException ex) {
				// Assert that the POP is the "cause" instruction
				assertEquals(visitor.getInsnList().get(0), ex.getInsn());
			}
		}
		@Test
		public void testPop2SmallStack() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			visitor.setupMethod(ACC_PUBLIC, "()V");
			try {
				visitor.visit("ACONST_NULL\nPOP2\nRETURN");
				// Try to pop 2 off stack, but only 1 exists
				visitor.verify();
			} catch(LineParseException ex) {
				fail(ex);
			} catch(VerifyException ex) {
				// Assert that the POP2 is the "cause" instruction
				assertEquals(visitor.getInsnList().get(1), ex.getInsn());
			}
		}

		@Test
		public void testSaveDoubleAsIntVar() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			visitor.setupMethod(ACC_PUBLIC, "()V");
			try {
				visitor.visit("DCONST_1\nISTORE 1\nRETURN");
				// Treating a double as an int
				visitor.verify();
			} catch(LineParseException ex) {
				fail(ex);
			} catch(VerifyException ex) {
				// Assert that the ISTORE is the "cause" instruction
				assertEquals(visitor.getInsnList().get(1), ex.getInsn());
			}
		}

		@Test
		public void testMissingMethodArg() {
			AssemblyVisitor visitor = new AssemblyVisitor();
			visitor.setupMethod(ACC_PUBLIC, "()V");
			try {
				visitor.visit("ICONST_1\nINVOKESTATIC Owner.method(II)V\nRETURN");
				// Method desc calls for two ints, only one is given
				visitor.verify();
			} catch(LineParseException ex) {
				fail(ex);
			} catch(VerifyException ex) {
				// Assert that the INVOKESTATIC is the "cause" instruction
				assertEquals(visitor.getInsnList().get(1), ex.getInsn());
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
			List<String> suggestions = suggest("MULTIANEWARRAY java/util/regex/Mat");
			// Suggested classes should be:
			// - java/util/regex/MatchResult
			// - java/util/regex/Matcher
			// And in java 9+
			// - java/util/regex/Matcher$1MatchResultIterator
			// - java/util/regex/Matcher$ImmutableMatchResult
			float vmVersion = Float.parseFloat(System.getProperty("java.class.version")) - 44;
			boolean higher = vmVersion >= 9;
			assertEquals(higher ? 4 : 2, suggestions.size());
			assertEquals("java/util/regex/MatchResult", suggestions.get(0));
			assertEquals("java/util/regex/Matcher", suggestions.get(1));
			if(higher) {
				assertEquals("java/util/regex/Matcher$1MatchResultIterator", suggestions.get(2));
				assertEquals("java/util/regex/Matcher$ImmutableMatchResult", suggestions.get(3));
			}
		}

		@Test
		public void testMultiANewArraySuggestNoDims() {
			List<String> suggestions = suggest("MULTIANEWARRAY java/lang/System 2");
			// Nothing to suggest when specifying dimensions
			assertEquals(0, suggestions.size());
		}

		@Test
		public void testTypeSuggestRuntime() {
			List<String> suggestions = suggest("NEW java/util/regex/Mat");
			// Suggested classes should be:
			// - java/util/regex/MatchResult
			// - java/util/regex/Matcher
			// And in java 9+
			// - java/util/regex/Matcher$1MatchResultIterator
			// - java/util/regex/Matcher$ImmutableMatchResult
			float vmVersion = Float.parseFloat(System.getProperty("java.class.version")) - 44;
			boolean higher = vmVersion >= 9;
			assertEquals(higher ? 4 : 2, suggestions.size());
			assertEquals("java/util/regex/MatchResult", suggestions.get(0));
			assertEquals("java/util/regex/Matcher", suggestions.get(1));
			if(higher) {
				assertEquals("java/util/regex/Matcher$1MatchResultIterator", suggestions.get(2));
				assertEquals("java/util/regex/Matcher$ImmutableMatchResult", suggestions.get(3));
			}
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
			List<String> suggestions = suggest("GETFIELD java/util/regex/Mat");
			// Suggested classes should be:
			// - java/util/regex/MatchResult
			// - java/util/regex/Matcher
			// And in java 9+
			// - java/util/regex/Matcher$1MatchResultIterator
			// - java/util/regex/Matcher$ImmutableMatchResult
			float vmVersion = Float.parseFloat(System.getProperty("java.class.version")) - 44;
			boolean higher = vmVersion >= 9;
			assertEquals(higher ? 4 : 2, suggestions.size());
			assertEquals("java/util/regex/MatchResult", suggestions.get(0));
			assertEquals("java/util/regex/Matcher", suggestions.get(1));
			if(higher) {
				assertEquals("java/util/regex/Matcher$1MatchResultIterator", suggestions.get(2));
				assertEquals("java/util/regex/Matcher$ImmutableMatchResult", suggestions.get(3));
			}
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
	// Hiding the ugly constant down here
	private static final String H_META = "handle[H_INVOKESTATIC java/lang/invoke/LambdaMetafactory " +
			"metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
			"Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;" +
			"Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)" +
			"Ljava/lang/invoke/CallSite;]";
}
