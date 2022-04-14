package me.coley.recaf.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.CompositeBootClassLoader;
import dev.xdark.ssvm.fs.FileDescriptorManager;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.util.VMSymbols;
import dev.xdark.ssvm.value.Value;
import me.coley.recaf.TestUtils;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.transformer.BytecodeToAstTransformer;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.decompile.DecompileResult;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.decompile.cfr.CfrDecompiler;
import me.coley.recaf.ssvm.loader.RuntimeBootClassLoader;
import me.coley.recaf.ssvm.loader.WorkspaceBootClassLoader;
import me.coley.recaf.ssvm.processing.PeepholeProcessors;
import me.coley.recaf.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A variety of tests for SSVM. See each category for different features.
 */
public class SsvmTests extends TestUtils implements Opcodes {
	private Workspace workspace;
	private VirtualMachine vm;

	@BeforeEach
	public void setup() throws IOException {
		workspace = createWorkspace(jarsDir.resolve("BinarySearchObf.jar"));
		vm = new VirtualMachine() {
			@Override
			protected FileDescriptorManager createFileDescriptorManager() {
				return new DenyingFileDescriptorManager();
			}

			@Override
			protected BootClassLoader createBootClassLoader() {
				return new CompositeBootClassLoader(Arrays.asList(
						new WorkspaceBootClassLoader(workspace),
						new RuntimeBootClassLoader()
				));
			}
		};
		vm.bootstrap();
	}

	@Nested
	class Processing {
		@Test
		public void foldVisitedMathOperations() throws Exception {
			try {
				VMHelper helper = vm.getHelper();
				VMSymbols symbols = vm.getSymbols();
				InstanceJavaClass target = (InstanceJavaClass) vm.findBootstrapClass("sample/math/BinarySearch");
				// Register peephole processors
				PeepholeProcessors.installValuePushing(vm);
				PeepholeProcessors.installOperationFolding(vm);
				// Invoke the main method
				helper.invokeStatic(target, "main", "([Ljava/lang/String;)V", new Value[0], new Value[]{helper.emptyArray(symbols.java_lang_String)});
				// Modified code
				ClassWriter writer = new ClassWriter(0);
				target.getNode().accept(writer);
				byte[] modified = writer.toByteArray();
				workspace.getResources().getPrimary()
						.getClasses().put(target.getInternalName(), ClassInfo.read(modified));
				// Decompile
				ClassInfo info = ClassInfo.read(modified);
				Decompiler decompiler = new CfrDecompiler();
				DecompileResult decompile = decompiler.decompile(workspace, info);
				String code = decompile.getValue();
				printDebug(target);
				System.out.println(code);
				// Wrong output:   System.out.println(Fields$1.indexOf("obfuscated", (8 + 8 + (8 << 2) + (4 + 4)) / (6 << 2)) + result);
				// Correct output: System.out.println(Fields$1.indexOf("obfuscated", 2) + result);
				assertTrue(code.contains("\", 2)"), "Integer math operations not folded! Value expected to be '2'");
			} catch (Throwable t) {
				fail(t);
			}
		}

		@Test
		public void foldStringDecryptCall() throws Exception {
			try {
				VMHelper helper = vm.getHelper();
				VMSymbols symbols = vm.getSymbols();
				InstanceJavaClass target = (InstanceJavaClass) vm.findBootstrapClass("sample/math/BinarySearch");
				// Register peephole processors
				PeepholeProcessors.installValuePushing(vm);
				PeepholeProcessors.installOperationFolding(vm);
				PeepholeProcessors.installReturnValueFolding(vm);
				// Invoke the main method
				helper.invokeStatic(target, "main", "([Ljava/lang/String;)V", new Value[0], new Value[]{helper.emptyArray(symbols.java_lang_String)});
				// Modified code
				ClassWriter writer = new ClassWriter(0);
				target.getNode().accept(writer);
				byte[] modified = writer.toByteArray();
				workspace.getResources().getPrimary()
						.getClasses().put(target.getInternalName(), ClassInfo.read(modified));
				// Decompile
				ClassInfo info = ClassInfo.read(modified);
				Decompiler decompiler = new CfrDecompiler();
				DecompileResult decompile = decompiler.decompile(workspace, info);
				String code = decompile.getValue();
				printDebug(target);
				System.out.println(code);
				// String should be decrypted for the visited branch
				assertTrue(code.contains("\"Element found at index \""), "String decrypt call not folded");
			} catch (Throwable t) {
				fail(t);
			}
		}
	}


	/**
	 * Used to disassemble the "main" method in the sample.
	 *
	 * @param target
	 * 		SSVM instance of the obfuscated main class.
	 */
	private static void printDebug(InstanceJavaClass target) {
		BytecodeToAstTransformer transformer = new BytecodeToAstTransformer(target.getStaticMethod("main", "([Ljava/lang/String;)V").getNode());
		transformer.visit();
		Unit unit = transformer.getUnit();
		System.out.println(unit.print());
	}
}
