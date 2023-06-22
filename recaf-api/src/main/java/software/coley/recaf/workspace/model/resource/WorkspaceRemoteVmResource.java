package software.coley.recaf.workspace.model.resource;

import com.sun.tools.attach.VirtualMachine;
import jakarta.annotation.Nonnull;
import software.coley.instrument.data.ClassLoaderInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.RemoteClassloaderProperty;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

import java.io.IOException;
import java.util.Map;

/**
 * A resource sourced from a remote {@link VirtualMachine}.
 *
 * @author Matt Coley
 * @see RemoteClassloaderProperty Property on {@link JvmClassInfo} instances, indcating which {@link ClassLoaderInfo}
 * the info is associated with in {@link #getRemoteLoaders()} and {@link #getJvmClassloaderBundles()}.
 */
public interface WorkspaceRemoteVmResource extends WorkspaceResource {
	/**
	 * Connects to the remote VM.
	 *
	 * @throws IOException
	 * 		When the connection fails.
	 */
	void connect() throws IOException;

	/**
	 * @return Virtual machine of the remote process attached to.
	 */
	@Nonnull
	VirtualMachine getVirtualMachine();

	/**
	 * @return Map of remote classloaders.
	 */
	@Nonnull
	Map<Integer, ClassLoaderInfo> getRemoteLoaders();

	/**
	 * @return Map of {@code ClassLoader} id to the classes defined by the loader.
	 *
	 * @see #getRemoteLoaders() Classloader values, keys of which are {@link ClassLoaderInfo#getId()}.
	 */
	@Nonnull
	Map<Integer, JvmClassBundle> getJvmClassloaderBundles();
}
