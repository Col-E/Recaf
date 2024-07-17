package software.coley.recaf.workspace.model.resource;

import com.sun.tools.attach.VirtualMachine;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import java.util.*;
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
	private final Map<Integer, Set<ClassData>> queuedClasses = new HashMap<>();
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

	@Nonnull
	@Override
	public Stream<JvmClassBundle> jvmClassBundleStream() {
		return Stream.concat(super.jvmClassBundleStream(), new ArrayList<>(remoteBundleMap.values()).stream());
	}

	@Override
	public void close() {
		try {
			super.close();
		} finally {
			closed = true;

			// Close client connection
			try {
				client.close();
			} catch (IOException ex) {
				logger.info("Failed to close client connection to remote VM: {}", virtualMachine.id());
			}
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
					int loaderId = loaderInfo.getId();
					remoteLoaders.put(loaderId, loaderInfo);

					// If the loader was one that we saw classes for earlier, now we can
					// link those to this loader.
					Set<ClassData> pendingClasses = queuedClasses.remove(loaderId);
					if (pendingClasses != null) {
						for (ClassData pendingClass : pendingClasses) {
							handleReceiveClassData(pendingClass, null);
						}
					}
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
		try {
			client.connectThrowing();
		} catch (Exception ex) {
			throw new IOException("Could not connect to remote JVM " + virtualMachine.id() +
					" over " + client.getIp() + ":" + client.getPort(), ex);
		}

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
						.computeIfAbsent(loaderId, id -> createRemoteBundle(loaderInfo));

				// Request all classes from classloader
				logger.info("Sending initial request for class names in classloader {}...", loader.getName());
				client.sendAsync(new RequestClassloaderClassesMessage(loaderId), classesReply -> {
					Collection<String> classes = classesReply.getClasses();
					logger.info("Received initial response for class names in classloader {}, count={}",
							loader.getName(), classes.size());
					for (String className : classes) {
						// If class does not exist in bundle, then request it from remote server
						if (bundle.get(className) == null) {
							client.sendAsync(new RequestClassMessage(loaderId, className), reply -> {
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
				if (loaderInfo == null) {
					queuedClasses.computeIfAbsent(loaderId, i -> new HashSet<>()).add(data);
					return;
				}
				bundle = remoteBundleMap.computeIfAbsent(loaderId, id -> createRemoteBundle(loaderInfo));
			}

			// Add the class
			JvmClassInfo classInfo = new JvmClassInfoBuilder(data.getCode()).build();
			RemoteClassloaderProperty.set(classInfo, loaderId);
			bundle.put(classInfo);
		}
	}

	/**
	 * Creates a new {@link RemoteJvmClassBundle} and sets up listener delegation.
	 *
	 * @param loaderInfo
	 * 		Loader to associate with some classes in a bundle.
	 *
	 * @return New bundle for classes within that loader.
	 */
	@Nonnull
	private RemoteJvmClassBundle createRemoteBundle(@Nonnull ClassLoaderInfo loaderInfo) {
		RemoteJvmClassBundle bundle = new RemoteJvmClassBundle(loaderInfo);
		delegateJvmClassBundle(this, bundle);
		return bundle;
	}

	/**
	 * JVM bundle extension adding a listener to handle syncing local changes with the remote server.
	 */
	public class RemoteJvmClassBundle extends BasicJvmClassBundle {
		private final ClassLoaderInfo loaderInfo;

		private RemoteJvmClassBundle(@Nonnull ClassLoaderInfo loaderInfo) {
			this.loaderInfo = loaderInfo;

			addBundleListener(new BundleListener<>() {
				@Override
				public void onNewItem(@Nonnull String key, @Nonnull JvmClassInfo value) {
					// Should occur when we get data from the client.
					// No action needed.
				}

				@Override
				public void onUpdateItem(@Nonnull String key, @Nonnull JvmClassInfo oldValue, @Nonnull JvmClassInfo newValue) {
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
				public void onRemoveItem(@Nonnull String key, @Nonnull JvmClassInfo value) {
					// Should not occur
					throw new IllegalStateException("Remove operations should not occur for remote VM resource!");
				}
			});
		}

		/**
		 * @return Loader information for this bundle.
		 */
		@Nonnull
		public ClassLoaderInfo getLoaderInfo() {
			return loaderInfo;
		}
	}
}
