package me.coley.recaf;

import java.io.File;
import java.util.*;

import me.coley.recaf.util.*;
import org.objectweb.asm.tree.ClassNode;

import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.bytecode.Agent;
import me.coley.recaf.event.ClassOpenEvent;
import me.coley.recaf.event.NewInputEvent;
import me.coley.recaf.event.UiInitEvent;
import me.coley.recaf.plugin.Launchable;
import me.coley.recaf.plugin.Plugins;
import picocli.CommandLine;

/**
 * Initialization handler, does:
 * <ul>
 * <li>Update checking</li>
 * <li>Command line argument parsing</li>
 * <li>Invokes 'Launchable' plugins</li>
 * <li>Initializes 'Input' instance if running in AgentMode</li>
 * </ul>
 * 
 * @author Matt
 */
public class InitListener {
	private String[] launchArgs;

	public InitListener(String[] launchArgs) {
		this.launchArgs = launchArgs;
	}

	@Listener
	private void onInit(UiInitEvent event) {
		try {
			// run update check (if enabled)
			Updater.updateFromRelease(launchArgs);
			// run plugins on pre-parse phase
			Collection<Launchable> launchables = Plugins.instance().plugins(Launchable.class);
			launchables.forEach(l -> l.preparse(launchArgs));
			// parse arguments
			LaunchParams params = new LaunchParams();
			new CommandLine(params).execute(launchArgs);
			Recaf.argsSerialized = params;
			// Load input if given
			if (Agent.isActive()) {
				NewInputEvent.call(Agent.inst);
			} else {
				// load file & class if specified
				File file = params.initialFile;
				if (file != null && file.exists()) {
					NewInputEvent.call(file);
					Threads.runLaterFx(10, () -> {
						Input in = Input.get();
						String clazz = params.initialClass;
						if (clazz != null && in.classes.contains(clazz)) {
							ClassNode cn = in.getClass(clazz);
							Bus.post(new ClassOpenEvent(cn));
						}
					});
				}
			}
			// run plugins on post-parse phase
			launchables.forEach(l -> l.postparse(launchArgs));
		} catch (Exception e) {
			Logging.fatal(e);
		}
	}
}
