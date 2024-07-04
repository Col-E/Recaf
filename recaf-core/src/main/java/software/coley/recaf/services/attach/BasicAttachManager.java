package software.coley.recaf.services.attach;

import com.sun.tools.attach.*;
import com.sun.tools.attach.spi.AttachProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.collections.observable.ObservableList;
import software.coley.instrument.BuildConfig;
import software.coley.instrument.Client;
import software.coley.instrument.Extractor;
import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.message.MessageFactory;
import software.coley.instrument.sock.SocketAvailability;
import software.coley.instrument.util.Discovery;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.DevDetection;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.resource.AgentServerRemoteVmResource;
import software.coley.recaf.workspace.model.resource.WorkspaceRemoteVmResource;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarFile;

/**
 * Manager for handling instrumentation of remote JVMs.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicAttachManager implements AttachManager {
	private static final Logger logger = Logging.get(BasicAttachManager.class);
	private static final long currentPid = ProcessHandle.current().pid();
	private static final String JMX_AGENT_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
	private static ExtractState extractState = ExtractState.DEFAULT;

	private final DescriptorComparator descriptorComparator = new DescriptorComparator();
	private final Map<VirtualMachineDescriptor, VirtualMachine> virtualMachineMap = new ConcurrentHashMap<>();
	private final Map<VirtualMachineDescriptor, Exception> virtualMachineFailureMap = new ConcurrentHashMap<>();
	private final Map<VirtualMachineDescriptor, Integer> virtualMachinePidMap = new ConcurrentHashMap<>();
	private final Map<VirtualMachineDescriptor, Properties> virtualMachinePropertiesMap = new ConcurrentHashMap<>();
	private final Map<VirtualMachineDescriptor, String> virtualMachineMainClassMap = new ConcurrentHashMap<>();
	private final Map<VirtualMachineDescriptor, JmxBeanServerConnection> virtualMachineJmxConnMap = new ConcurrentHashMap<>();
	private final ObservableList<VirtualMachineDescriptor> virtualMachineDescriptors = new ObservableList<>();
	private final List<PostScanListener> postScanListeners = new CopyOnWriteArrayList<>();
	private final AttachManagerConfig config;
	private ScheduledFuture<?> future;

	@Inject
	public BasicAttachManager(AttachManagerConfig config) {
		this.config = config;
		extractAgent();
	}

	/**
	 * Extracts the agent jar from Recaf to its own file.
	 */
	private void extractAgent() {
		if (extractState == ExtractState.DEFAULT) {
			Path agentPath = getAgentJarPath();
			if (!Files.isRegularFile(agentPath)) {
				// Not extracted already
				try {
					logger.debug("Extracting agent jar to Recaf directory: {}", agentPath.getFileName());
					Files.createDirectories(config.getAgentDirectory());
					Extractor.extractToPath(agentPath);
					future = ThreadUtil.scheduleAtFixedRate(this::passiveScanUpdate, 0, 1, TimeUnit.SECONDS);
					extractState = ExtractState.SUCCESS;
				} catch (IOException ex) {
					logger.error("Failed to extract agent jar to Recaf directory", ex);
					extractState = ExtractState.FAILURE;
				}
			} else {
				// Already extracted before
				extractState = ExtractState.SUCCESS;
				future = ThreadUtil.scheduleAtFixedRate(this::passiveScanUpdate, 0, 1, TimeUnit.SECONDS);
			}
		}
	}

	/**
	 * Cancel passive scan loop when shutting down.
	 */
	@PreDestroy
	private void onShutdown() {
		future.cancel(true);
	}

	/**
	 * Check for new virtual machines in the background.
	 */
	private void passiveScanUpdate() {
		try {
			if (config.getPassiveScanning().getValue())
				scan();
		} catch (Throwable t) {
			logger.error("Unhandled exception in JVM scan", t);
		}
	}

	/**
	 * @param descriptor
	 * 		VM descriptor.
	 *
	 * @return Main class of VM, if possible to resolve.
	 */
	@Nonnull
	private String mapToMainClass(@Nonnull VirtualMachineDescriptor descriptor) {
		// Get source string to find main class name from
		String source = descriptor.displayName();
		if (source == null || source.isBlank() || source.toLowerCase().contains(".jar")) {
			Properties properties = virtualMachinePropertiesMap.get(descriptor);
			if (properties != null) {
				// Check if we can get the main class from the command.
				// It may start with it, such as "com.example.Main args..."
				// it may be a file path, such as from "-jar <path> <args...>"
				String command = properties.getProperty("sun.java.command", "");

				// Check if the command is a path.
				String commandLower = command.toLowerCase();
				int jarIndex = commandLower.indexOf(".jar");
				if (jarIndex > 0) {
					// Depending on the invocation, it may not be the full path.
					// We may need to pre-pend the current directory
					String commandJarName = command.substring(0, jarIndex + 4);
					try {
						Path jarPath = Paths.get(commandJarName);
						if (!Files.isRegularFile(jarPath)) {
							// Prepend remote vm's user directory
							String commandUserDir = properties.getProperty("user.dir");
							if (commandUserDir.endsWith("/"))
								commandUserDir = commandUserDir.substring(0, commandUserDir.length() - 1);
							if (commandJarName.startsWith("/"))
								commandJarName = commandJarName.substring(1);
							jarPath = Paths.get(commandUserDir + "/" + commandJarName);
						}

						// Read main class attribute from jar manifest
						if (Files.isRegularFile(jarPath)) {
							try (JarFile jar = new JarFile(jarPath.toFile())) {
								source = jar.getManifest().getMainAttributes().getValue("Main-Class");
							} catch (IOException ignored) {
								// Can't read from jar, oh well
							}
						}
					} catch (InvalidPathException ignored) {
						// Expected for cases like 'com.example.Main foo.jar'
						// In this case we know the substring up to the '.jar' isn't a path in totality.
						// Only a section of it is, so likely the '.jar' match is part of an argument.
					}
				}
			}
		}

		// Still null/missing? Give up
		if (source == null || source.isEmpty())
			return "<unknown main-class>";

		// Some 'display name' values are '<class> <args...>' so strip out the args
		String trim = source.trim();
		int end = trim.indexOf(' ');
		if (end == -1)
			end = trim.length();
		return trim.substring(0, end);
		// Alternative idea for later: Use 'sun/launcher/LauncherHelper' as mentioned by xxDark
		//  - reliable source for main-class
	}

	/**
	 * @param descriptor
	 * 		VM descriptor.
	 *
	 * @return PID of VM process.
	 */
	private int mapToPid(@Nonnull VirtualMachineDescriptor descriptor) {
		String id = descriptor.id();
		if (id.matches("\\d+")) {
			return Integer.parseInt(descriptor.id());
		} else {
			return -1;
		}
	}

	/**
	 * @return {@code true} when the agent jar is extracted and ready to be used.
	 * {@code false} means instrumentation cannot be done since the agent is not available.
	 */
	public static boolean isAgentReady() {
		return extractState == ExtractState.SUCCESS;
	}

	/**
	 * @return Path to agent jar file.
	 */
	@Nonnull
	public Path getAgentJarPath() {
		String jarName = "agent-" + BuildConfig.VERSION + ".jar";
		return config.getAgentDirectory().resolve(jarName);
	}

	@Override
	public boolean canAttach() {
		return isAgentReady();
	}

	@Override
	public void scan() {
		int numDescriptors = virtualMachineDescriptors.size();
		List<VirtualMachineDescriptor> remoteVmList = VirtualMachine.list();
		Set<VirtualMachineDescriptor> toRemove = new HashSet<>(virtualMachineDescriptors);
		Set<VirtualMachineDescriptor> toAdd = new HashSet<>();
		List<CompletableFuture<?>> attachFutures = new ArrayList<>();
		for (VirtualMachineDescriptor descriptor : remoteVmList) {
			// Still active in VM list, keep it.
			toRemove.remove(descriptor);

			// Add if not in the list.
			if (!virtualMachineDescriptors.contains(descriptor)) {
				String label = descriptor.id() + " - " + StringUtil.withEmptyFallback(descriptor.displayName(), "?");
				int pid = mapToPid(descriptor);
				if (pid == currentPid) // skip self
					continue;

				// Using futures for attach in case one of the VM's decides to hang on response.
				// Using 'orTimeout' we can prevent such hangs from affecting us.
				attachFutures.add(ThreadUtil.run(() -> {
					try {
						AttachProvider provider = descriptor.provider();
						return provider.attachVirtualMachine(descriptor);
					} catch (IOException ex) {
						virtualMachineFailureMap.put(descriptor, ex);
						logger.debug("Remote JVM descriptor found (attach-success, read-failure): " + label);
					} catch (AttachNotSupportedException ex) {
						virtualMachineFailureMap.put(descriptor, ex);
						logger.debug("Remote JVM descriptor found (attach-failure): " + label);
					} catch (Throwable t) {
						logger.error("Unhandled exception populating remote VM info", t);
					}
					return null;
				}).orTimeout(500, TimeUnit.MILLISECONDS).thenAccept(machine -> {
					// Get information from machine if it is available.
					if (machine != null) {
						virtualMachineMap.put(descriptor, machine);
						logger.debug("Remote JVM descriptor found (attach-success): " + label);

						// Extract additional information
						try {
							Properties systemProperties = machine.getSystemProperties();
							virtualMachinePropertiesMap.put(descriptor, systemProperties);
							virtualMachinePidMap.put(descriptor, pid);
							virtualMachineMainClassMap.put(descriptor, mapToMainClass(descriptor));

							// Enable optional JMX agent
							if (config.getAttachJmxAgent().getValue()) {
								try {
									Properties agentProperties = machine.getAgentProperties();
									String serviceUrl = agentProperties.getProperty(JMX_AGENT_ADDRESS);
									if (serviceUrl == null) {
										serviceUrl = machine.startLocalManagementAgent();
									}

									if (serviceUrl != null) {
										JMXServiceURL url = new JMXServiceURL(serviceUrl);
										@SuppressWarnings("resource") // Do NOT wrap this in a try-with-resource. It will close the connection.
										JMXConnector connector = JMXConnectorFactory.connect(url);
										MBeanServerConnection connection = connector.getMBeanServerConnection();
										virtualMachineJmxConnMap.put(descriptor, new JmxBeanServerConnection(connection));
									} else {
										logger.warn("Could fetch JMX agent address, skipping connection for: {}", label);
									}
								} catch (Exception ex) {
									logger.error("Failed to attach JMX agent to remote JVM: {}", label, ex);
								}
							}
						} catch (IOException ex) {
							logger.error("Could not read system properties from remote JVM: " + label, ex);
						}
					}

					// Add to list for listener call later.
					toAdd.add(descriptor);

					// Insert descriptor in sorted order.
					int lastComparison = 1;
					synchronized (virtualMachineDescriptors) {
						for (int i = 0; i < numDescriptors; i++) {
							VirtualMachineDescriptor other = virtualMachineDescriptors.get(i);
							int comparison = descriptorComparator.compare(descriptor, other);
							if (comparison < lastComparison) {
								virtualMachineDescriptors.add(i, descriptor);
								return;
							}
						}

						// Greater than all entries, append to end
						virtualMachineDescriptors.add(descriptor);
					}
				}));
			}
		}
		// When all attach attachFutures complete, update the observable list to update the UI
		ThreadUtil.allOf(attachFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
			// Remove entries not visited in this pass
			virtualMachineDescriptors.removeAll(toRemove);
			for (VirtualMachineDescriptor descriptor : toRemove) {
				String label = descriptor.id() + " - " + StringUtil.withEmptyFallback(descriptor.displayName(), "?");
				logger.debug("Remote JVM descriptor removed: " + label);
			}

			// Call listeners
			Unchecked.checkedForEach(postScanListeners, listener -> listener.onScanCompleted(toAdd, toRemove),
					(listener, t) -> logger.error("Exception thrown after scan completion", t));
		});
	}

	@Nonnull
	@Override
	public WorkspaceRemoteVmResource createRemoteResource(VirtualMachineDescriptor item) throws IOException {
		VirtualMachine virtualMachine = virtualMachineMap.get(item);
		try {
			// Will initialize agent server with default arguments
			Properties properties = virtualMachinePropertiesMap.get(item);
			int port = Discovery.extractPort(properties);
			if (port <= 0) {
				// Port not found, server is not running on remote VM.
				// Load the agent to start it.
				try {
					port = SocketAvailability.findAvailable();
					String agentAbsolutePath = StringUtil.pathToAbsoluteString(getAgentJarPath());
					String agentArgs = "port=" + port;
					if (DevDetection.isDevEnv())
						agentArgs += ",debug";
					else
						agentArgs += ",namelessThreads";
					virtualMachine.loadAgent(agentAbsolutePath, agentArgs);

					// The agent server will update some properties to indicate its active.
					// We need to update our map so that we can see this indicator so that we can extract
					// the port that the server is running on if we want to reconnect.
					Properties systemProperties = virtualMachine.getSystemProperties();
					virtualMachinePropertiesMap.put(item, systemProperties);
				} catch (AgentLoadException ex) {
					// The agent jar file is written in Java 8. But Recaf uses Java 11+.
					// This is a problem on OUR side because Java 11+ handles agent interactions differently.
					//  - https://stackoverflow.com/a/54454418/
					// Basically in Java 10 they added a prefix string requirement.
					// But the Java 8 VM doesn't have that so our VM will mark it as invalid.
					if (!ex.getMessage().equals("0"))
						// If the result we get back is '0' then that means it was actually a success
						throw ex;
				}
			}

			// Connect with client
			Client client = new Client("localhost", port, ByteBufferAllocator.HEAP, MessageFactory.create());
			return new AgentServerRemoteVmResource(virtualMachine, client);
		} catch (AgentLoadException ex) {
			logger.error("Agent on remote VM '{}' could not be loaded", item, ex);
			throw new IOException("Failed remote load", ex);
		} catch (AgentInitializationException ex) {
			logger.error("Agent on remote VM '{}' crashed on initialization", item, ex);
			throw new IOException("Failed remote initialization", ex);
		} catch (IOException ex) {
			logger.error("IO error when loading agent to remote VM '{}'", item, ex);
			throw ex;
		}
	}

	@Override
	public VirtualMachine getVirtualMachine(@Nonnull VirtualMachineDescriptor descriptor) {
		return virtualMachineMap.get(descriptor);
	}

	@Override
	public Exception getVirtualMachineConnectionFailure(@Nonnull VirtualMachineDescriptor descriptor) {
		return virtualMachineFailureMap.get(descriptor);
	}

	@Override
	public int getVirtualMachinePid(@Nonnull VirtualMachineDescriptor descriptor) {
		return virtualMachinePidMap.getOrDefault(descriptor, -1);
	}

	@Nullable
	@Override
	public Properties getVirtualMachineProperties(@Nonnull VirtualMachineDescriptor descriptor) {
		return virtualMachinePropertiesMap.get(descriptor);
	}

	@Nullable
	@Override
	public String getVirtualMachineMainClass(@Nonnull VirtualMachineDescriptor descriptor) {
		return virtualMachineMainClassMap.get(descriptor);
	}

	@Nullable
	@Override
	public JmxBeanServerConnection getJmxServerConnection(@Nonnull VirtualMachineDescriptor descriptor) {
		return virtualMachineJmxConnMap.get(descriptor);
	}

	@Nonnull
	@Override
	public ObservableList<VirtualMachineDescriptor> getVirtualMachineDescriptors() {
		return virtualMachineDescriptors;
	}

	@Override
	public void addPostScanListener(@Nonnull PostScanListener listener) {
		postScanListeners.add(listener);
	}

	@Override
	public void removePostScanListener(@Nonnull PostScanListener listener) {
		postScanListeners.remove(listener);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public AttachManagerConfig getServiceConfig() {
		return config;
	}

	/**
	 * Comparator for {@link VirtualMachineDescriptor} using {@link #mapToMainClass(VirtualMachineDescriptor)}.
	 */
	class DescriptorComparator implements Comparator<VirtualMachineDescriptor> {
		@Override
		public int compare(VirtualMachineDescriptor o1, VirtualMachineDescriptor o2) {
			String k1 = virtualMachineMainClassMap.getOrDefault(o1, o1.displayName());
			String k2 = virtualMachineMainClassMap.getOrDefault(o2, o2.displayName());
			return k1.compareTo(k2);
		}
	}

	enum ExtractState {
		DEFAULT,
		SUCCESS,
		FAILURE
	}
}
