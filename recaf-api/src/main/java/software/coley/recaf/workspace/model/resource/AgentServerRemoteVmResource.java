package software.coley.recaf.workspace.model.resource;

import com.sun.tools.attach.VirtualMachine;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import software.coley.instrument.ApiConstants;
import software.coley.instrument.Client;
import software.coley.instrument.data.ClassData;
import software.coley.instrument.data.ClassLoaderInfo;
import software.coley.instrument.message.MessageConstants;
import software.coley.instrument.message.broadcast.BroadcastClassMessage;
import software.coley.instrument.message.broadcast.BroadcastClassloaderMessage;
import software.coley.instrument.message.request.RequestClassMessage;
import software.coley.instrument.message.request.RequestClassloaderClassesMessage;
import software.coley.instrument.message.request.RequestClassloadersMessage;
import software.coley.instrument.message.request.RequestRedefineMessage;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.properties.builtin.RemoteClassloaderProperty;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.BundleListener;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

/**
 * Implementation of {@link WorkspaceRemoteVmResource} via {@link Client}.
 *
 * @author Matt Coley
 */
public class AgentServerRemoteVmResource extends BasicWorkspaceResource implements WorkspaceRemoteVmResource {
	private static final DebuggingLogger logger = Logging.get(AgentServerRemoteVmResource.class);
	private final Map<Integer, RemoteJvmClassBundle> remoteBundleMap = new HashMap<>();
	private final Map<Integer, ClassLoaderInfo> remoteLoaders = new HashMap<>();
	private final Set<String> queuedRedefines = new ConcurrentSkipListSet<>();
	private final VirtualMachine virtualMachine;
	private final Client client;
	private boolean closed;

	/**
	 * @param virtualMachine
	 * 		Instance of remote VM.
	 * @param client
	 * 		Client to communicate to the remote VM.
	 */
	public AgentServerRemoteVmResource(VirtualMachine virtualMachine, Client client) {
		super(new WorkspaceResourceBuilder());
		this.virtualMachine = virtualMachine;
		this.client = client;

		// Call the parent setup method.
		super.setup();
	}

	@Override
	protected void setup() {
		// No-op here so the constructor doesn't call it before the fields in THIS class are initialized
	}

	@Nonnull
	@Override
	public VirtualMachine getVirtualMachine() {
		return virtualMachine;
	}

	@Nonnull
	@Override
	public Map<Integer, ClassLoaderInfo> getRemoteLoaders() {
		return remoteLoaders;
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public Map<Integer, JvmClassBundle> getJvmClassloaderBundles() {
		return (Map<Integer, JvmClassBundle>) (Object) remoteBundleMap;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Stream<JvmClassBundle> jvmClassBundleStream() {
		return (Stream<JvmClassBundle>) (Object) remoteBundleMap.values().stream();
	}

	@Override
	public void close() {
		super.close();
		closed = true;

		// Close client connection
		try {
			client.close();
		} catch (IOException ex) {
			logger.info("Failed to close client connection to remote VM: {}", virtualMachine.id());
		}
	}

	@Override
	public void connect() throws IOException {
		if (closed)
			throw new IOException("Cannot re-connect to closed resource. Please create a new one.");
		client.setBroadcastListener((messageType, message) -> {
			switch (messageType) {
				case MessageConstants.ID_BROADCAST_LOADER:
					// New loader reported
					BroadcastClassloaderMessage loaderMessage = (BroadcastClassloaderMessage) message;
					ClassLoaderInfo loaderInfo = loaderMessage.getClassLoader();
					remoteLoaders.put(loaderInfo.getId(), loaderInfo);
					break;
				case MessageConstants.ID_BROADCAST_CLASS:
					// New class, or update to existing class reported
					BroadcastClassMessage classMessage = (BroadcastClassMessage) message;
					ClassData data = classMessage.getData();
					handleReceiveClassData(data, null);
					break;
				default:
					// unknown broadcast packet
					break;
			}
		});

		// Try to connect
		logger.info("Connecting to remote JVM '{}' over port {}", virtualMachine.id(), client.getPort());
		if (!client.connect())
			throw new IOException("Client connection failed");

		// Request known classloaders
		logger.info("Sending initial request for classloaders & initial classes...");
		client.sendAsync(new RequestClassloadersMessage(), loaderReply -> {
			Collection<ClassLoaderInfo> classLoaders = loaderReply.getClassLoaders();
			logger.info("Received initial response for classloaders, count={}", classLoaders.size());
			for (ClassLoaderInfo loader : classLoaders) {
				if (loader.isBootstrap())
					continue;
				int loaderId = loader.getId();
				remoteLoaders.put(loaderId, loader);

				// Get/create bundle for loader
				ClassLoaderInfo loaderInfo = remoteLoaders.get(loaderId);
				RemoteJvmClassBundle bundle = remoteBundleMap
						.computeIfAbsent(loaderId, id -> new RemoteJvmClassBundle(loaderInfo));

				// Request all classes from classloader
				logger.info("Sending initial request for class names in classloader {}...", loader.getName());
				client.sendAsync(new RequestClassloaderClassesMessage(loaderId), classesReply -> {
					Collection<String> classes = classesReply.getClasses();
					logger.info("Received initial response for class names in classloader {}, count={}",
							loader.getName(), classes.size());
					for (String className : classes) {
						// If class does not exist in bundle, then request it from remote server
						if (bundle.get(className) == null) {
							client.sendBlocking(new RequestClassMessage(loaderId, className), reply -> {
								if (reply.hasData()) {
									ClassData data = reply.getData();
									handleReceiveClassData(data, bundle);
								}
							});
						}
					}
				});
			}
		});
	}

	/**
	 * @param data
	 * 		Class data to handle adding to the resource.
	 * @param bundle
	 * 		Bundle to check within.
	 * 		May be {@code null} to be lazily fetched in this method.
	 */
	private void handleReceiveClassData(@Nonnull ClassData data, @Nullable RemoteJvmClassBundle bundle) {
		// If it belongs to the bootstrap classloader, it's a core JVM class.
		if (data.getClassLoaderId() == ApiConstants.BOOTSTRAP_CLASSLOADER_ID)
			return;

		// If this class broadcast isn't for one of our redefine requests, it's a new class.
		if (!queuedRedefines.remove(data.getName())) {
			int loaderId = data.getClassLoaderId();

			// Get the bundle for the remote classloader if not specified by parameter
			if (bundle == null) {
				ClassLoaderInfo loaderInfo = remoteLoaders.get(loaderId);
				bundle = remoteBundleMap
						.computeIfAbsent(loaderId, id -> new RemoteJvmClassBundle(loaderInfo));
			}

			// Add the class
			JvmClassInfo classInfo = new JvmClassInfoBuilder(new ClassReader(data.getCode())).build();
			RemoteClassloaderProperty.set(classInfo, loaderId);
			bundle.initialPut(classInfo);
		}
	}

	/**
	 * JVM bundle extension adding a listener to handle syncing local changes with the remote server.
	 */
	private class RemoteJvmClassBundle extends BasicJvmClassBundle {
		private RemoteJvmClassBundle(ClassLoaderInfo loaderInfo) {
			this.addBundleListener(new BundleListener<>() {
				@Override
				public void onNewItem(String key, JvmClassInfo value) {
					// Should occur when we get data from the client.
					// No action needed.
				}

				@Override
				public void onUpdateItem(String key, JvmClassInfo oldValue, JvmClassInfo newValue) {
					// Should occur when the user makes changes to a class from recaf.
					// We need to send this definition to the remote server.

					// Record that we expect acknowledgement from the remote server for our redefine request.
					queuedRedefines.add(key);

					// Request class update
					byte[] definition = newValue.getBytecode();
					client.sendAsync(new RequestRedefineMessage(loaderInfo.getId(), key, definition), reply -> {
						if (reply.isSuccess()) {
							logger.debug("Redefine '{}' success", key);
						} else {
							logger.debug("Redefine '{}' failed: {}", key, reply.getMessage());
						}
					});
				}

				@Override
				public void onRemoveItem(String key, JvmClassInfo value) {
					// Should not occur
					throw new IllegalStateException("Remove operations should not occur for remote VM resource!");
				}
			});
		}
	}
}
