package me.coley.recaf.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.CompositeBootClassLoader;
import dev.xdark.ssvm.execution.Result;
import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.fs.FileDescriptorManager;
import dev.xdark.ssvm.fs.SimpleFileDescriptorManager;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.symbol.VMSymbols;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.IntValue;
import me.coley.recaf.TestUtils;
import me.coley.recaf.ssvm.loader.RuntimeBootClassLoader;
import me.coley.recaf.ssvm.loader.WorkspaceBootClassLoader;
import me.coley.recaf.workspace.Workspace;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sample tests demonstrating sandboxing capabilities of SSVM.
 *
 * @author Matt Coley
 */
@Execution(ExecutionMode.SAME_THREAD)
public class SsvmSandboxingTests extends TestUtils implements Opcodes {
	private final static List<String> filesRead = new ArrayList<>();
	private final static List<String> filesWritten = new ArrayList<>();
	private static Workspace workspace;
	private static VirtualMachine vm;

	@BeforeEach
	public void setup() throws IOException {
		workspace = createWorkspace(jarsDir.resolve("DemoExec.jar"));
		vm = new VirtualMachine() {
			@Override
			protected FileDescriptorManager createFileDescriptorManager() {
				return new TestFileDescriptorManager();
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
		VMInterface vmi = vm.getInterface();
		InstanceJavaClass unsafe = (InstanceJavaClass) vm.findBootstrapClass("jdk/internal/misc/Unsafe");
		vmi.setInvoker(unsafe, "pageSize", "()I", ctx -> {
			ctx.setResult(IntValue.of(vm.getMemoryAllocator().pageSize()));
			return Result.ABORT;
		});
	}

	private static class TestFileDescriptorManager extends SimpleFileDescriptorManager {
		@Override
		public long open(String path, int mode) {
			switch (mode) {
				case READ:
					filesRead.add(path);
					System.out.println("READ:" + path);
					break;
				case WRITE:
					filesWritten.add(path);
					System.out.println("WRITE:" + path);
					break;
				case APPEND:
					filesWritten.add(path);
					System.out.println("APPEND:" + path);
					break;
			}
			return 0;
		}
	}

	@AfterEach
	public void cleanup() {
		vm = null;
		filesRead.clear();
		filesWritten.clear();
	}

	@Nested
	@Disabled("Pending SSVM Runtime implementation")
	class Exec {
		@Test
		public void testInterceptRuntimeExec() {
			invokeMain("demo/exec/ExecRunner");
		}
	}

	@Nested
	@Disabled("Enable once SSVM is updated")
	class IO {
		@Test
		public void testInterceptFileRead() {
			// 'FileRead' reads from the JVM 'classlist' file
			File target = new File(System.getProperty("java.home"), "/lib/classlist");
			invokeMain("demo/io/FileRead");
			assertTrue(filesRead.contains(target.getAbsolutePath()));
		}

		@Test
		@Disabled("Pending controllable network (dummy logic that we define)")
		public void testInterceptFileDownload() {
			// 'FileDownload' downloads text from online, and writes it to './hello.bat'
			File target = new File("hello.bat");
			invokeMain("demo/io/FileDownload");
			assertTrue(filesWritten.contains(target.getAbsolutePath()));
		}

		@Test
		@Disabled("Pending resolving monitor exception on simulated object wait")
		public void testInterceptFileDelete() {
			// Creates 10 temp files, then deletes them
			invokeMain("demo/io/FileDelete");
			assertEquals(10, filesWritten.size());
		}
	}

	@Nested
	@Disabled("Pending SSVM support for NIO")
	class NIO {
		@Test
		public void testInterceptFileRead() {
			// 'FileRead' reads from the JVM 'classlist' file
			File target = new File(System.getProperty("java.home"), "/lib/classlist");
			invokeMain("demo/nio/FileRead");
			assertTrue(filesRead.contains(target.getAbsolutePath()));
		}

		@Test
		public void testInterceptFileDownload() {
			// 'FileDownload' downloads text from online, and writes it to './hello.bat'
			File target = new File("hello.bat");
			invokeMain("demo/nio/FileDownload");
			assertTrue(filesWritten.contains(target.getAbsolutePath()));
		}
	}

	/**
	 * Starts SSVM.
	 *
	 * @param main
	 * 		Name of main class.
	 */
	private void invokeMain(String main) {
		try {
			InstanceJavaClass target = (InstanceJavaClass) vm.findBootstrapClass(main);
			if (target == null)
				fail("Could not find class: " + main);
			VMHelper helper = vm.getHelper();
			VMSymbols symbols = vm.getSymbols();
			VmUtil util = VmUtil.create(vm);
			util.invokeStatic(target, "main", "([Ljava/lang/String;)V", helper.emptyArray(symbols.java_lang_String()));
		} catch (VMException ex) {
			fail(ex);
		}
	}
}
