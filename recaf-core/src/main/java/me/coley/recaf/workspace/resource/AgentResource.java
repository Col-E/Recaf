package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.OuterMethodInfo;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.source.ContentCollection;
import me.coley.recaf.workspace.resource.source.ContentSource;
import me.coley.recaf.workspace.resource.source.SourceType;
import org.slf4j.Logger;
import software.coley.instrument.Client;
import software.coley.instrument.command.impl.ClassLoaderClassesCommand;
import software.coley.instrument.command.impl.GetClassCommand;
import software.coley.instrument.command.impl.LoadedClassesCommand;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Workspace unit that works through an agent {@link software.coley.instrument.Client}.
 *
 * @author Matt Coley
 */
public class AgentResource extends Resource {
	private static final Logger logger = Logging.get(AgentResource.class);
	private final Set<String> ignored = new HashSet<>();
	private final Client client;

	/**
	 * @param client
	 * 		Instrumentation client.
	 */
	public AgentResource(Client client) {
		super(new AgentContentSource());
		this.client = client;
	}

	/**
	 * Populates the names of classes that should be ignored.
	 * <br>
	 * The idea is to query the server for all classes loaded under the bootloader.
	 * This includes things like the classes in the provided JVM modules (java.base, etc).
	 * Anything in here we should visually ignore so we can focus just on the application logic.
	 */
	public void populateSystemIgnores() {
		// Ignore all JDK classes from the boot classloader.
		client.sendSynchronous(new ClassLoaderClassesCommand(0), reply -> {
			ClassLoaderClassesCommand systemClassResponse = (ClassLoaderClassesCommand) reply;
			ignored.addAll(systemClassResponse.getClassNames());
		});
	}

	/**
	 * Requests new loaded classes.
	 */
	public void updateClassList() {
		try {
			// The 'LoadedClassesCommand' responds with new classes rather than everything every request.
			// So this basically asks the server 'what new things got loaded?'
			client.sendSynchronous(new LoadedClassesCommand(), namesReply -> {
				if (namesReply instanceof LoadedClassesCommand) {
					LoadedClassesCommand classesResponse = (LoadedClassesCommand) namesReply;
					classesResponse.getClassNames().stream()
							.filter(name -> !isRuntimeClass(name))
							.map(this::getClass)
							.filter(Objects::nonNull)
							.forEach(getClasses()::put);
				} else {
					// TODO: Why are the replies sometimes not the expected types?
					//  - Is the client read buffer confused, or is the server write buffer confused?
					logger.info("Expected 'LoadedClassesCommand' reply, got '{}' instead!",
							namesReply.getClass().getSimpleName());
				}
			});
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Order of operations:
	 * <ol>
	 *     <li>Check for being runtime class, return {@code null} if so</li>
	 *     <li>Check local cache for instance</li>
	 *     <li>Request remote data from server</li>
	 * </ol>
	 *
	 * @param className
	 * 		Name of class to get.
	 *
	 * @return Current class information.
	 */
	private ClassInfo getClass(String className) {
		if (className.indexOf('.') >= 0)
			className = className.replace('.', '/');
		if (ignored.contains(className))
			return null;
		// Get local cached version
		ClassInfo present = getClasses().get(className);
		if (present != null)
			return present;
		// Can't do "computeIfAbsent" since we also want to store null values.
		return requestClass(className);
	}

	/**
	 * @param className
	 * 		Name of class to get.
	 *
	 * @return Latest bytecode for class on remote server.
	 */
	private ClassInfo requestClass(String className) {
		byte[][] wrapper = new byte[1][];
		client.sendSynchronous(new GetClassCommand(className, null), reply -> {
			GetClassCommand responseClass = (GetClassCommand) reply;
			wrapper[0] = responseClass.getCode();
		});
		byte[] value = wrapper[0];
		if (value == null || value.length < 20) {
			ignored.add(className);
			return null;
		}
		ClassInfo info = ClassInfo.read(value);
		if (isRuntimeClass(info)) {
			// Ignore runtime classes
			ignored.add(className);
			return null;
		}
		return info;
	}

	/**
	 * @param name
	 * 		Class name to check.
	 *
	 * @return {@code true} when recognized by the current runtime.
	 */
	private boolean isRuntimeClass(String name) {
		return RuntimeResource.get().getClasses().containsKey(name);
	}

	/**
	 * @param info
	 * 		Class info to check.
	 *
	 * @return {@code true} when recognized by the current runtime.
	 * For inner classes, the outer class name is checked as well.
	 */
	private boolean isRuntimeClass(ClassInfo info) {
		// Obvious check
		if (isRuntimeClass(info.getName()))
			return true;
		// Check for outer-class of inner classes
		OuterMethodInfo outerMethod = info.getOuterMethod();
		if (outerMethod != null && isRuntimeClass(outerMethod.getOwner()))
			return true;
		List<String> outerClassBreadcrumbs = info.getOuterClassBreadcrumbs();
		if (outerClassBreadcrumbs.isEmpty())
			return false;
		return isRuntimeClass(outerClassBreadcrumbs.get(0));
	}

	private static class AgentContentSource extends ContentSource {
		protected AgentContentSource() {
			super(SourceType.INSTRUMENTATION);
		}

		@Override
		protected void onRead(ContentCollection collection) {
			// no-op
		}
	}
}
