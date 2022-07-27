package me.coley.recaf.ssvm;

/**
 * Supplier of {@link IntegratedVirtualMachine} instances for {@link SsvmIntegration}.
 *
 * @author Matt Coley
 * @see RemoteVmFactory
 * @see LocalVmFactory
 */
public interface VmFactory {
	/**
	 * @param integration
	 * 		Integration instance.
	 *
	 * @return New VM instance.
	 */
	IntegratedVirtualMachine create(SsvmIntegration integration);
}
