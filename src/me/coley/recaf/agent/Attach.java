package me.coley.recaf.agent;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;

import me.coley.recaf.Recaf;
import sun.tools.attach.*;

public class Attach {
	public static boolean fail;

	/**
	 * Spawn recaf in the JVM by the given descriptor.
	 * 
	 * @param vm
	 */
	public static void attach(VirtualMachineDescriptor vm) {
		try {
			// Run attach
			VirtualMachine target = VirtualMachine.attach(vm);
			String agentPath = getAgentJar();
			if (!agentPath.endsWith(".jar")) {
				Recaf.INSTANCE.logging.error(new RuntimeException("Recaf could not resolve a path to itself."));
				return;
			}
			File agent = new File(agentPath);
			if (agent.exists()) {
				Recaf.INSTANCE.logging.info("Attempting to attach to '" + vm.displayName() + "' with agent '" + agent.getAbsolutePath() + "'.");
				target.loadAgent(agent.getAbsolutePath());
				target.detach();
			} else {
				Recaf.INSTANCE.logging.error(new RuntimeException("Recaf could not resolve a path to itself, attempt gave: " + agent.getAbsolutePath()));
			}
		} catch (Exception e) {
			Recaf.INSTANCE.logging.error(e);
		}
	}

	/**
	 * @return Path to self (Recaf jar / execution directory).
	 * @throws Exception
	 */
	private static String getAgentJar() throws Exception {
		return Attach.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().toString();
	}

	/**
	 * Set java's attach providers programatically since I can't be bothered to
	 * do multi-release with maven.
	 * 
	 * @throws Exception
	 */
	public static void setProviders() throws Exception {
		Field providers = AttachProvider.class.getDeclaredField("providers");
		providers.setAccessible(true);
		List<AttachProvider> list = new ArrayList<>();
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			list.add(new WindowsAttachProvider());
		} else if (os.contains("mac")) {
			list.add(new BsdAttachProvider());
		} else if (os.contains("nux")) {
			list.add(new LinuxAttachProvider());
		} else {
			Recaf.INSTANCE.logging.error(new RuntimeException(
					"Recaf could not select an attach provider, please open an issue on Github. Your OS was: '" + os + "'."));
		}
		providers.set(null, list);
	}

	/**
	 * Ensures the attach libraries have been loaded.
	 * 
	 * @throws Exception
	 */
	public static void load() throws Exception {
		String os = System.getProperty("os.name").toLowerCase();
		String path = System.getProperty("java.home");
		// C:\Program Files\Java\jdk1.8.0_131\jre\bin\attach.dll
		if (os.contains("win")) {
			// Enforce JDK path
			if (path.contains("a\\jre")) {
				path = path.replace("a\\jre", "a\\jdk");
			}
			// Look in jre sub-directory
			if (path.contains("jdk") && !path.contains("jre")) {
				path += "\\jre";
			}
			path += "\\bin\\attach.dll";
			Recaf.INSTANCE.logging.info("Loading attach.dll from: " + path, 2);
			System.load(path);
		}
	}
}
