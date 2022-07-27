package me.coley.recaf.ssvm;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.execution.ExecutionContext;
import dev.xdark.ssvm.fs.FileDescriptorManager;
import dev.xdark.ssvm.fs.Handle;
import dev.xdark.ssvm.fs.HostFileDescriptorManager;
import me.coley.recaf.ssvm.processing.DataTracking;
import me.coley.recaf.ssvm.processing.FlowRevisiting;
import me.coley.recaf.ssvm.processing.peephole.MathOperationFolder;
import me.coley.recaf.ssvm.processing.peephole.MethodInvokeFolder;
import me.coley.recaf.ssvm.processing.peephole.StringFolder;
import me.coley.recaf.ssvm.util.DummyAttributes;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Random;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;

/**
 * Virtual machine that can pull classes from a {@link Workspace} and perform optimizations on executed bytecode.
 *
 * @author Matt Coley
 */
public abstract class IntegratedVirtualMachine extends VirtualMachine {
	private static final int RECAF_LIVE_ZIP_HANDLE = new Random().nextInt();
	private static final Logger logger = Logging.get(IntegratedVirtualMachine.class);
	private final VmUtil vmUtil;
	// applied processors
	private boolean dataTracking;
	private boolean flowRevisit;
	private boolean peepholeMathFolding;
	private boolean peepholeStringFolding;
	private boolean peepholeMethodInvokeFolding;

	public IntegratedVirtualMachine() {
		vmUtil = VmUtil.create(this);
	}

	/**
	 * Exists so that the {@link Workspace} reference can be used in the constructor.
	 * Supplied via an outer class.
	 *
	 * @return Associated SSVM integration instance.
	 */
	protected abstract SsvmIntegration integration();

	/**
	 * @return VM's associated workspace.
	 */
	protected Workspace getWorkspace() {
		return integration().getWorkspace();
	}

	/**
	 * @return VM utils.
	 */
	public final VmUtil getVmUtil() {
		return vmUtil;
	}

	@Override
	public void bootstrap() {
		super.bootstrap();
		// TODO: Replace workspace bootloader with fake classpath jar
		// VirtualMachineUtil.addUrl(this, RECAF_LIVE_ZIP);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected FileDescriptorManager createFileDescriptorManager() {
		return new HostFileDescriptorManager() {

			@Override
			public synchronized ZipEntry getZipEntry(long handle) {
				return super.getZipEntry(handle);
			}

			@Override
			public synchronized boolean freeZipEntry(long handle) {
				return super.freeZipEntry(handle);
			}

			@Override
			public <A extends BasicFileAttributes> A getAttributes(String path, Class<A> attrType, LinkOption... options) throws IOException {
				if (WorkspaceZipFile.RECAF_LIVE_ZIP.equals(path)) {
					return (A) new DummyAttributes(path);
				}
				return super.getAttributes(path, attrType, options);
			}

			@Override
			public synchronized long openZipFile(String path, int mode) throws IOException {
				if (WorkspaceZipFile.RECAF_LIVE_ZIP.equals(path)) {
					zipFiles.put(Handle.of(RECAF_LIVE_ZIP_HANDLE), new WorkspaceZipFile(RECAF_LIVE_ZIP_HANDLE, getWorkspace()));
					return RECAF_LIVE_ZIP_HANDLE;
				}
				return super.openZipFile(path, mode);
			}

			@Override
			public long open(String path, int mode) throws IOException {
				if (WorkspaceZipFile.RECAF_LIVE_ZIP.equals(path)) {
					return RECAF_LIVE_ZIP_HANDLE;
				}
				long fd = newFD();
				if ((integration().doAllowRead() && mode == READ) || (integration().doAllowWrite() && (mode == WRITE || mode == APPEND)))
					logger.trace("VM file handle[{}:{}]: {}",
							SsvmUtil.describeFileMode(mode), fd, path);
				switch (mode) {
					case READ: {
						InputStream in;
						if (integration().doAllowRead())
							in = new FileInputStream(path);
						else
							in = new ByteArrayInputStream(new byte[0]);
						inputs.put(Handle.of(fd), in);
						return fd;
					}
					case WRITE:
					case APPEND: {
						OutputStream out;
						if (integration().doAllowWrite())
							out = new FileOutputStream(path, mode == APPEND);
						else
							out = new ByteArrayOutputStream();
						outputs.put(Handle.of(fd), out);
						return fd;
					}
					default:
						throw new IOException("Unknown mode: " + mode);
				}
			}
		};
	}

	/**
	 * Install processors to track values at runtime.
	 */
	public void installDataTracking() {
		if (!dataTracking) {
			DataTracking.install(this);
			dataTracking = true;
		}
	}

	/**
	 * Install processors to ensure all control flow paths are taken.
	 *
	 * @param whitelist
	 * 		Whitelist filter.
	 */
	public void installFlowRevisiting(Predicate<ExecutionContext> whitelist) {
		if (!flowRevisit) {
			FlowRevisiting.install(this, whitelist);
			flowRevisit = true;
		}
	}

	/**
	 * Install processors to fold math operations on constant values.
	 *
	 * @param whitelist
	 * 		Whitelist filter.
	 */
	public void installMathFolding(Predicate<ExecutionContext> whitelist) {
		if (!peepholeMathFolding) {
			installDataTracking();
			MathOperationFolder.install(this, whitelist);
			peepholeMathFolding = true;
		}
	}

	/**
	 * Install processors to fold method calls on constant values if the result is also a constantly deterministic value.
	 *
	 * @param whitelist
	 * 		Whitelist filter.
	 */
	public void installMethodFolding(Predicate<ExecutionContext> whitelist) {
		if (!peepholeMethodInvokeFolding) {
			installDataTracking();
			MethodInvokeFolder.install(this, whitelist);
			peepholeMethodInvokeFolding = true;
		}
	}

	/**
	 * Install processors to fold string creation.
	 *
	 * @param whitelist
	 * 		Whitelist filter.
	 */
	public void installStringFolding(Predicate<ExecutionContext> whitelist) {
		if (!peepholeStringFolding) {
			installDataTracking();
			StringFolder.installStringFolding(this, whitelist);
			peepholeStringFolding = true;
		}
	}
}
