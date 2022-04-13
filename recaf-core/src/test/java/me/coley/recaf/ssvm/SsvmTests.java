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
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.decompile.DecompileResult;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.decompile.cfr.CfrDecompiler;
import me.coley.recaf.ssvm.loader.RuntimeBootClassLoader;
import me.coley.recaf.ssvm.loader.WorkspaceBootClassLoader;
import me.coley.recaf.ssvm.processing.PeepholeProcessors;
import me.coley.recaf.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SsvmTests extends TestUtils implements Opcodes {
	@Test
	public void foldVisitedConstants() throws Exception {
		Workspace workspace = createWorkspace(jarsDir.resolve("BinarySearchObf.jar"));
		VirtualMachine vm = new VirtualMachine() {
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
		VMHelper helper = vm.getHelper();
		try {
			vm.bootstrap();
			VMSymbols symbols = vm.getSymbols();
			InstanceJavaClass target = (InstanceJavaClass) vm.findBootstrapClass("sample/math/BinarySearch");
			// Register peephole processors
			PeepholeProcessors.install(vm);
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
			// Wrong output:   System.out.println(Fields$1.indexOf("obfuscated", (8 + 8 + (8 << 2) + (4 + 4)) / (6 << 2)) + result);
			// Correct output: System.out.println(Fields$1.indexOf("obfuscated", 2) + result);
			assertTrue(code.contains("\", 2)"), "Integer math operations not folded! Value expected to be '2'");
		} catch (Throwable t) {
			fail(t);
		}
	}
}
