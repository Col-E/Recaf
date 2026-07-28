package software.coley.recaf.services.workspace.io;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import software.coley.recaf.info.Info;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.util.ClassDefiner;
import software.coley.recaf.util.io.ByteSources;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BasicClassPatcher}.
 */
class ClassPatcherTest {
	// TODO: Additional tests for the patcher, primarily relating to patch levels and how they affect the output of the patcher.
	//  - Actual patching efficacy is tested upstream in cafedude, so we don't need to re-test that here.

	@Nested
	class Bomb {
		private static final int BOMB_DEPTH = 24;

		@Test
		void bootstrapBombDoesNotExplode() {
			byte[] sample = createSample(BOMB_DEPTH);
			assertDoesNotThrow(() -> executeSample(sample));
		}

		@Test
		void importerFiltersBootstrapMethodExpansionBombBeforeAsmValidation() {
			byte[] sample = createSample(BOMB_DEPTH);
			BasicInfoImporter importer = new BasicInfoImporter(new InfoImporterConfig(), new TextFormatConfig(), new BasicClassPatcher());

			Info info = assertTimeoutPreemptively(Duration.ofSeconds(2),
					() -> importer.readInfo("BootstrapBomb.class", ByteSources.wrap(sample)));
			assertTrue(info.isClass());
			assertDoesNotThrow(() -> new ClassWriter(info.asClass().asJvmClass().getClassReader(), 0));
			assertDoesNotThrow(() -> executeSample(info.asClass().asJvmClass().getBytecode()));
		}

		private static void executeSample(byte[] bytecode) throws Exception {
			ClassDefiner definer = new ClassDefiner("BootstrapBomb", bytecode);
			Class<?> type = definer.findClass("BootstrapBomb");
			Method sampleMethod = type.getMethod("sample");
			sampleMethod.invoke(null);
		}

		/**
		 * @param depth
		 * 		Number of dynamic constants to create in the sample class.
		 *
		 * @return Bytecode of a class that has a bootstrap-method expansion bomb.
		 */
		private static byte[] createSample(int depth) {
			try {
				// We can't exactly use ASM for this considering how ASM is what explodes when operating on this class...
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				DataOutputStream out = new DataOutputStream(output);
				out.writeInt(0xCAFEBABE);
				out.writeShort(0);
				out.writeShort(55);

				int firstDynamicIndex = 13;
				int codeNameIndex = firstDynamicIndex + depth + 1;
				int sampleNameIndex = codeNameIndex + 1;
				int voidDescriptorIndex = sampleNameIndex + 1;
				int mainNameIndex = voidDescriptorIndex + 1;
				int mainDescriptorIndex = mainNameIndex + 1;
				int bootstrapMethodsNameIndex = mainDescriptorIndex + 1;
				out.writeShort(bootstrapMethodsNameIndex + 1);

				writeUtf8(out, "BootstrapBomb");
				writeClass(out, 1);
				writeUtf8(out, "java/lang/Object");
				writeClass(out, 3);
				writeUtf8(out, "bootstrap");
				writeUtf8(out, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;");
				writeNameAndType(out, 5, 6);
				writeMethodRef(out, 2, 7);
				writeMethodHandle(out, 6, 8);
				writeUtf8(out, "value");
				writeUtf8(out, "Ljava/lang/Object;");
				writeNameAndType(out, 10, 11);
				for (int i = 0; i <= depth; i++)
					writeDynamic(out, i, 12);
				writeUtf8(out, "Code");                              // codeNameIndex
				writeUtf8(out, "sample");                            // sampleNameIndex
				writeUtf8(out, "()V");                               // voidDescriptorIndex
				writeUtf8(out, "main");                              // mainNameIndex
				writeUtf8(out, "([Ljava/lang/String;)V");             // mainDescriptorIndex
				writeUtf8(out, "BootstrapMethods");                  // bootstrapMethodsNameIndex

				out.writeShort(0x0021); // ACC_PUBLIC | ACC_SUPER
				out.writeShort(2);
				out.writeShort(4);
				out.writeShort(0); // interfaces
				out.writeShort(0); // fields
				out.writeShort(3); // methods

				// sample method
				writeMethod(out, 0x0089, 5, 6, codeNameIndex, 1, 4, new byte[]{1, (byte) 176});
				int finalDynamicIndex = firstDynamicIndex + depth;
				writeMethod(out, 0x0009, sampleNameIndex, voidDescriptorIndex, codeNameIndex, 1, 0,
						new byte[]{19, (byte) (finalDynamicIndex >>> 8), (byte) finalDynamicIndex, 87, (byte) 177});
				writeMethod(out, 0x0009, mainNameIndex, mainDescriptorIndex, codeNameIndex, 0, 1,
						new byte[]{(byte) 177});

				out.writeShort(1); // class attributes
				out.writeShort(bootstrapMethodsNameIndex);
				out.writeInt(6 + depth * 8);
				out.writeShort(depth + 1);
				out.writeShort(9);
				out.writeShort(0);
				for (int i = 1; i <= depth; i++) {
					out.writeShort(9);
					out.writeShort(2);
					out.writeShort(firstDynamicIndex + i - 1);
					out.writeShort(firstDynamicIndex + i - 1);
				}
				return output.toByteArray();
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		private static void writeMethod(DataOutputStream out, int access, int nameIndex, int descriptorIndex,
		                                int codeNameIndex, int maxStack, int maxLocals, byte[] code) throws Exception {
			out.writeShort(access);
			out.writeShort(nameIndex);
			out.writeShort(descriptorIndex);
			out.writeShort(1);
			out.writeShort(codeNameIndex);
			out.writeInt(12 + code.length);
			out.writeShort(maxStack);
			out.writeShort(maxLocals);
			out.writeInt(code.length);
			out.write(code);
			out.writeShort(0);
			out.writeShort(0);
		}

		private static void writeUtf8(DataOutputStream out, String value) throws Exception {
			out.writeByte(1);
			out.writeUTF(value);
		}

		private static void writeClass(DataOutputStream out, int nameIndex) throws Exception {
			out.writeByte(7);
			out.writeShort(nameIndex);
		}

		private static void writeNameAndType(DataOutputStream out, int nameIndex, int descriptorIndex) throws Exception {
			out.writeByte(12);
			out.writeShort(nameIndex);
			out.writeShort(descriptorIndex);
		}

		private static void writeMethodRef(DataOutputStream out, int classIndex, int nameAndTypeIndex) throws Exception {
			out.writeByte(10);
			out.writeShort(classIndex);
			out.writeShort(nameAndTypeIndex);
		}

		private static void writeMethodHandle(DataOutputStream out, int kind, int referenceIndex) throws Exception {
			out.writeByte(15);
			out.writeByte(kind);
			out.writeShort(referenceIndex);
		}

		private static void writeDynamic(DataOutputStream out, int bootstrapIndex, int nameAndTypeIndex) throws Exception {
			out.writeByte(17);
			out.writeShort(bootstrapIndex);
			out.writeShort(nameAndTypeIndex);
		}
	}
}
