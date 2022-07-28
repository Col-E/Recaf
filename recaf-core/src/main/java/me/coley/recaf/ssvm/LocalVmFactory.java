package me.coley.recaf.ssvm;

import dev.xdark.ssvm.api.MethodInvoker;
import dev.xdark.ssvm.api.VMInterface;
import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.CompositeBootClassLoader;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import me.coley.recaf.ssvm.loader.RuntimeBootClassLoader;
import me.coley.recaf.ssvm.loader.WorkspaceBootClassLoader;
import me.coley.recaf.workspace.Workspace;

import java.util.Arrays;

/**
 * Factory implementation that provides workspace class access via the VM bootloader.
 *
 * @author Matt Coley
 * @see RemoteVmFactory
 */
public class LocalVmFactory implements VmFactory {
	private final Workspace workspace;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 */
	public LocalVmFactory(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public IntegratedVirtualMachine create(SsvmIntegration integration) {
		IntegratedVirtualMachine vm = new IntegratedVirtualMachine() {
			@Override
			protected SsvmIntegration integration() {
				return integration;
			}

			@Override
			protected BootClassLoader createBootClassLoader() {
				return new CompositeBootClassLoader(Arrays.asList(
						new WorkspaceBootClassLoader(workspace),
						new RuntimeBootClassLoader()
				));
			}
		};
		vm.initialize();
		VMInterface vmi = vm.getInterface();
		InstanceJavaClass cl = (InstanceJavaClass) vm.findBootstrapClass("java/lang/Shutdown");
		vmi.setInvoker(cl, "beforeHalt", "()V", MethodInvoker.noop());
		vmi.setInvoker(cl, "halt0", "(I)V", MethodInvoker.noop());
		return vm;
	}
}
