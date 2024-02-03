package software.coley.recaf.services.attach;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.collections.observable.ObservableList;
import software.coley.recaf.services.Service;
import software.coley.recaf.workspace.model.resource.WorkspaceRemoteVmResource;

import java.io.IOException;
import java.util.Properties;

/**
 * Outline for attach service.
 *
 * @author Matt Coley
 */
public interface AttachManager extends Service {
	String SERVICE_ID = "attach";

	/**
	 * @return {@code true} when attach is supported.
	 * Typically only {@code false} when the agent fails to extract onto the local file system.
	 */
	boolean canAttach();

	/**
	 * Refresh available remote JVMs.
	 */
	void scan();

	/**
	 * Create a {@link WorkspaceRemoteVmResource} for the given VM.
	 * Users must call {@link WorkspaceRemoteVmResource#connect()} to <i>'enable'</i> the resource.
	 *
	 * @param item
	 * 		VM descriptor for VM to connect to.
	 *
	 * @return Agent client resource, not yet connected.
	 *
	 * @throws IOException
	 * 		When the remote resource couldn't be created.
	 * 		Causing exceptions depend on implementation.
	 */
	@Nonnull
	WorkspaceRemoteVmResource createRemoteResource(VirtualMachineDescriptor item) throws IOException;

	/**
	 * @param descriptor
	 * 		Lookup descriptor.
	 *
	 * @return Remote VM, if known. Otherwise {@code null}.
	 */
	@Nullable
	VirtualMachine getVirtualMachine(@Nonnull VirtualMachineDescriptor descriptor);

	/**
	 * @param descriptor
	 * 		Lookup descriptor.
	 *
	 * @return Exception when attempting to connect to remote VM, if there was one. Otherwise {@code null}.
	 */
	@Nullable
	Exception getVirtualMachineConnectionFailure(@Nonnull VirtualMachineDescriptor descriptor);

	/**
	 * @param descriptor
	 * 		Lookup descriptor.
	 *
	 * @return Remote VM PID, or {@code -1} if no PID is known for the remote VM.
	 */
	int getVirtualMachinePid(@Nonnull VirtualMachineDescriptor descriptor);

	/**
	 * @param descriptor
	 * 		Lookup descriptor.
	 *
	 * @return Remote VM {@link System#getProperties()} if known, otherwise {@code null}.
	 */
	@Nullable
	Properties getVirtualMachineProperties(@Nonnull VirtualMachineDescriptor descriptor);

	/**
	 * @param descriptor
	 * 		Lookup descriptor.
	 *
	 * @return Remote main class of VM.
	 */
	@Nullable
	String getVirtualMachineMainClass(@Nonnull VirtualMachineDescriptor descriptor);

	/**
	 * @param descriptor
	 * 		Lookup descriptor.
	 *
	 * @return JMX bean server connection to remote VM.
	 */
	@Nullable
	JmxBeanServerConnection getJmxServerConnection(@Nonnull VirtualMachineDescriptor descriptor);

	/**
	 * @return Observable list of virtual machine descriptors.
	 */
	@Nonnull
	ObservableList<VirtualMachineDescriptor> getVirtualMachineDescriptors();

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	void addPostScanListener(@Nonnull PostScanListener listener);

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	void removePostScanListener(@Nonnull PostScanListener listener);
}
