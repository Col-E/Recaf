package me.coley.recaf.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.CompositeBootClassLoader;
import dev.xdark.ssvm.fs.FileDescriptorManager;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.symbol.VMSymbols;
import dev.xdark.ssvm.util.VMHelper;
import me.coley.recaf.TestUtils;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.transformer.BytecodeToAstTransformer;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.decompile.DecompileResult;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.decompile.cfr.CfrDecompiler;
import me.coley.recaf.ssvm.loader.RuntimeBootClassLoader;
import me.coley.recaf.ssvm.loader.WorkspaceBootClassLoader;
import me.coley.recaf.ssvm.processing.DataTracking;
import me.coley.recaf.ssvm.processing.FlowRevisiting;
import me.coley.recaf.ssvm.processing.peephole.MathOperationFolder;
import me.coley.recaf.ssvm.processing.peephole.MethodInvokeFolder;
import me.coley.recaf.workspace.Workspace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for deobfuscation using SSVM processors.
 *
 * @author Matt Coley
 */
@Execution(ExecutionMode.SAME_THREAD)
public class SsvmDeobfuscationTests extends TestUtils implements Opcodes {
	private static Workspace workspace;
	private static VirtualMachine vm;

	@BeforeEach
	public void setup() throws IOException {
		workspace = createWorkspace(jarsDir.resolve("DemoObf.jar"));
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

	@AfterEach
	public void cleanup() {
		vm = null;
	}

	@Test
	public void foldVisitedMathOperations() {
		try {
			// Register peephole processors
			DataTracking.installValuePushing(vm);
			DataTracking.installStackManipulationInstructionTracking(vm);
			MathOperationFolder.install(vm, c -> true);
			// Invoke the main method
			invokeMain();
			// Decompile
			String code = decompile();
			String asm = disassemble();
			System.out.println(asm);
			System.out.println(code);
			// Wrong output:   System.out.println(Fields$1.indexOf("obfuscated", (8 + 8 + (8 << 2) + (4 + 4)) / (6 << 2)) + result);
			// Correct output: System.out.println(Fields$1.indexOf("obfuscated", 2) + result);
			assertTrue(code.contains("\", 2)"), "Integer math operations not folded! Value expected to be '2'");
		} catch (Throwable t) {
			fail(t);
		}
	}

	@Test
	public void foldStringDecryptCall() {
		try {
			// Register peephole processors
			DataTracking.installValuePushing(vm);
			DataTracking.installStackManipulationInstructionTracking(vm);
			MathOperationFolder.install(vm, c -> true);
			MethodInvokeFolder.install(vm, c -> true);
			// Invoke the main method
			invokeMain();
			// Decompile
			String code = decompile();
			String asm = disassemble();
			System.out.println(asm);
			System.out.println(code);
			// String should be decrypted for the visited branch
			assertTrue(code.contains("\"Element found at index \""), "String decrypt call not folded");
		} catch (Throwable t) {
			fail(t);
		}
	}

	@Test
	public void visitAllPaths() {
		try {
			// Register peephole processors
			DataTracking.installValuePushing(vm);
			DataTracking.installStackManipulationInstructionTracking(vm);
			MathOperationFolder.install(vm, c -> true);
			MethodInvokeFolder.install(vm, c -> true);
			FlowRevisiting.install(vm, ctx -> ctx.getMethod().getName().equals("main"));
			// Invoke the main method
			invokeMain();
			// Decompile
			String code = decompile();
			String asm = disassemble();
			System.out.println(asm);
			System.out.println(code);
			// String should be decrypted for all branches.
			// No math obfuscation should remain.
			assertTrue(code.contains("\"Element found at index \""), "String decrypt call not folded");
			assertTrue(code.contains("\"Element not present\""), "String decrypt call not folded");
			assertFalse(code.contains(" << "), "Bitwise math not removed");
		} catch (Throwable t) {
			fail(t);
		}
	}

	private InstanceJavaClass getTargetClass() {
		return (InstanceJavaClass) vm.findBootstrapClass("sample/math/BinarySearch");
	}

	/**
	 * Starts SSVM.
	 */
	private void invokeMain() {
		InstanceJavaClass target = getTargetClass();
		if (target == null)
			fail("Could not find target class");
		VMHelper helper = vm.getHelper();
		VMSymbols symbols = vm.getSymbols();
		VmUtil util = VmUtil.create(vm);
		util.invokeStatic(target, "main", "([Ljava/lang/String;)V", helper.emptyArray(symbols.java_lang_String()));
	}

	/**
	 * Used to decompile the "main" method in the sample.
	 *
	 * @return Decompiled code.
	 */
	private String decompile() {
		InstanceJavaClass target = getTargetClass();
		ClassWriter writer = new ClassWriter(0);
		target.getNode().accept(writer);
		byte[] modified = writer.toByteArray();
		workspace.getResources().getPrimary()
				.getClasses().put(target.getInternalName(), ClassInfo.read(modified));
		// Decompile
		ClassInfo info = ClassInfo.read(modified);
		Decompiler decompiler = new CfrDecompiler();
		DecompileResult decompile = decompiler.decompile(workspace, info);
		return decompile.getValue();
	}

	/**
	 * Used to disassemble the "main" method in the sample.
	 *
	 * @return Disassembled code.
	 */
	private String disassemble() {
		InstanceJavaClass target = getTargetClass();
		MethodNode method = target.getStaticMethod("main", "([Ljava/lang/String;)V").getNode();
		// Patch out SSVM modifications to the ASM instructions
		SsvmUtil.restoreMethod(method);
		// Map to AST
		BytecodeToAstTransformer transformer = new BytecodeToAstTransformer(method);
		transformer.visit();
		Unit unit = transformer.getUnit();
		return unit.print(PrintContext.DEFAULT_CTX);
	}
}
