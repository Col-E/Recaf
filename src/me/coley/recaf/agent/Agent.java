package me.coley.recaf.agent;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;

import me.coley.recaf.LaunchParams;
import me.coley.recaf.Recaf;
import me.coley.recaf.asm.Asm;

/**
 * Agent entry-point for Recaf.
 * 
 * <pre>
java -javaagent:/path/to/agent.jar -cp Test.jar TestMain
 * </pre>
 * 
 * @author Matt
 */
public class Agent {
	/**
	 * Map of loaded classes.
	 */
	public static final Map<String, byte[]> classes = new HashMap<>();
	/**
	 * Instrumentation instance.
	 */
	public static Instrumentation instrument;
	/**
	 * Exclude core java classes from being loaded.
	 */
	private static boolean excludeInternals = true;

	/**
	 * Called when attached through javaagent jvm arg.
	 * 
	 * @param agentArgs
	 * @param inst
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		agent(agentArgs, inst);
	}

	/**
	 * Called when attached to a jvm externally.
	 * 
	 * @param agentArgs
	 * @param inst
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		agent(agentArgs, inst);
	}

	/**
	 * Populate {@link #instrument} and invoke Recaf.
	 * 
	 * @param agentArgs
	 * @param inst
	 */
	private static void agent(String agentArgs, Instrumentation inst) {
		instrument = inst;
		if (agentArgs == null) {
			agentArgs = "";
		} else {
			// TODO: Lets say for some reason the user wishes to edit some
			// core-classes. They would want to set this boolean to false so
			// core classes are loaded... But if this boolean is set to false,
			// some wacky things happen.
			// Screenshot of issue: https://my.mixtape.moe/tptxwl.jpg
			// I have no idea why the instrumentation API is being such a child,
			// but I don't know why this is happening.
			if (agentArgs.equals("keep")) {
				System.out.println(Recaf.class);
				excludeInternals = false;
			}
			if (agentArgs.equals("keep2")) {
				excludeInternals = false;
			}
		}
		try {
			// Add transformer and invoke retransform so already loaded classes
			// are transformed.
			ClassPopulator transformer = new ClassPopulator();
			instrument.addTransformer(transformer, true);
			instrument.retransformClasses(getModifable());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		// Manually set params agent value.
		// I keep getting mis-matched args via picoli when attaching to other
		// processes.
		LaunchParams params = new LaunchParams();
		params.isAgent = true;
		Recaf.start(new String[0], params);
	}

	/**
	 * Creates an array of modifable loaded classes.
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static Class[] getModifable() {
		Set<Class> set = new HashSet<>();
		for (Class clazz : instrument.getAllLoadedClasses()) {
			if (valid(clazz.getName()) && instrument.isModifiableClass(clazz)) {
				set.add(clazz);
			}
		}
		return set.toArray(new Class[set.size()]);
	}

	/**
	 * Update edits made to classes in the current VM.
	 */
	public static void apply() {
		try {
			ClassDefinition[] definitions = Marker.getDefinitions();
			instrument.redefineClasses(definitions);
		} catch (Exception e) {
			Recaf.INSTANCE.logging.error(e);
		}
	}

	/**
	 * Retrieve the map of ClassNodes using {@link #instrument}.
	 * 
	 * @return Map of JVM classes.
	 * @throws IOException
	 *             Thrown if classes could not be read.
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String, ClassNode> getNodesViaInst() {
		Map<String, ClassNode> map = new HashMap<>();
		for (Class clazz : instrument.getAllLoadedClasses()) {
			try {
				ClassNode node = getNode(clazz.getName().replace(".", "/"));
				if (node != null) {
					map.put(node.name, node);
					Recaf.INSTANCE.logging.fine("Loaded runtime class: " + node.name, 1);
				}
			} catch (Exception e) {
				Recaf.INSTANCE.logging.fine("Failed loading runtime class: " + clazz.getName().replace(".", "/"), 1);
			}
		}
		return map;
	}

	/**
	 * Get node via its internal class name.
	 * 
	 * @param name
	 * @return
	 * @throws IOException
	 */
	private static ClassNode getNode(String name) throws IOException {
		byte[] bytes = classes.get(name);
		if (bytes == null) {
			return null;
		}
		return Asm.getNode(bytes);
	}

	/**
	 * Add class to map and current class tree if recaf's UI is already open.
	 * 
	 * @param name
	 * @param bytes
	 */
	public static void addClass(String name, byte[] bytes) {
		classes.put(name, bytes);
		Recaf rc = Recaf.INSTANCE;
		if (rc.ui != null) {
			try {
				rc.jarData.classes.put(name, getNode(name));
				if (rc.configs.agent.autoRefresh) {
					rc.ui.refreshTree();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Determine if the given name should be loaded.
	 * 
	 * @param name
	 * @return
	 */
	public static boolean valid(String name) {
		if (excludeInternals) {
			if (name.startsWith("java")) return false;
			else if (name.startsWith("sun")) return false;
			else if (name.startsWith("com/sun")) return false;
			else if (name.startsWith("jdk")) return false;
		}
		if (name.startsWith("[")) return false;
		else if (name.startsWith("me/coley")) return false;
		return true;
	}

	public static boolean active() {
		return classes.size() > 0;
	}
}
