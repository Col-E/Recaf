package me.coley.recaf;

import java.io.File;

import org.objectweb.asm.tree.ClassNode;

import javafx.application.Application.Parameters;
import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.bytecode.Agent;
import me.coley.recaf.event.ClassOpenEvent;
import me.coley.recaf.event.NewInputEvent;
import me.coley.recaf.event.UiInitEvent;
import me.coley.recaf.plugins.Plugins;
import me.coley.recaf.util.Threads;
import picocli.CommandLine;

public class InitListener {
	private String[] launchArgs;

	public InitListener(String[] launchArgs) {
		this.launchArgs = launchArgs;
	}

	@Listener
	private void onInit(UiInitEvent event) {
		try {
			// run update check (if enabled)
			Updater.run(Recaf.args);
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
				Threads.runLaterFx(10, () -> {
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
