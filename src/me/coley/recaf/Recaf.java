package me.coley.recaf;

import java.io.File;

import me.coley.recaf.util.Threads;
import org.objectweb.asm.tree.ClassNode;

import javafx.application.Application.Parameters;
import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.bytecode.Agent;
import me.coley.recaf.bytecode.analysis.Hierarchy;
import me.coley.recaf.event.ClassOpenEvent;
import me.coley.recaf.event.NewInputEvent;
import me.coley.recaf.event.UiInitEvent;
import me.coley.recaf.plugins.Plugins;
import me.coley.recaf.ui.FxWindow;
import picocli.CommandLine;

public class Recaf {
	public static final String VERSION = "1.5.2";
	public static String[] launchArgs;

	public static void main(String[] args) {
		launchArgs = args;
		// Register hierarchy listeners by calling an arbitrary method in the
		// class. This will load it.
		Hierarchy.getStatus();
		// run update check (if enabled)
		Updater.run(args);
		// start main window
		Bus.subscribe(new Recaf());
		FxWindow.init(args);
	}

	@Listener
	private void onInit(UiInitEvent event) {
		try {
			// convert parameters to string array so picocli can parse it
			Parameters paramsFx = event.getLaunchParameters();
			Plugins.getLaunchables().forEach(l -> l.call(paramsFx, false));
			LaunchParams params = new LaunchParams();
			CommandLine.call(params, System.out, launchArgs);
			if (params.agent) {
				NewInputEvent.call(Agent.inst);
				return;
			}
			// load file & class if specified
			File file = params.initialFile;
			if (file != null && file.exists()) {
				NewInputEvent.call(file);
				Threads.runFx(() -> {
					Input in = Input.get();
					String clazz = params.initialClass;
					if (clazz != null && in.classes.contains(clazz)) {
						ClassNode cn = in.getClass(clazz);
						Bus.post(new ClassOpenEvent(cn));
					}
				});
			}
			Plugins.getLaunchables().forEach(l -> l.call(paramsFx, true));
		} catch (Exception e) {
			Logging.fatal(e);
		}
	}
}
