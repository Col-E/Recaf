package software.coley.recaf.services.attach;

import com.sun.tools.attach.VirtualMachineDescriptor;
import jakarta.annotation.Nonnull;

import java.util.Set;

/**
 * Listener called after {@link AttachManager#scan()} completion.
 *
 * @author Matt Coley
 */
public interface PostScanListener {
	/**
	 * Called when scan is completed.
	 *
	 * @param added
	 * 		Newly found VMs.
	 * @param removed
	 * 		Old VMs that are no longer available.
	 */
	void onScanCompleted(@Nonnull Set<VirtualMachineDescriptor> added,
						 @Nonnull Set<VirtualMachineDescriptor> removed);
}
