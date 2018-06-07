package me.coley.recaf;

import java.io.File;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;

import javafx.application.Application.Parameters;
import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.bytecode.Agent;
import me.coley.recaf.event.ClassOpenEvent;
import me.coley.recaf.event.NewInputEvent;
import me.coley.recaf.event.UiInitEvent;
import me.coley.recaf.plugins.Plugins;
import me.coley.recaf.ui.FxWindow;
import picocli.CommandLine;

public class Recaf {
	public static void main(String[] args) {
		Bus.subscribe(new Recaf());
		FxWindow.init(args);
	}

	@Listener
	private void onInit(UiInitEvent event) {
		try {
			// convert parameters to string array so picocli can parse it
			Parameters paramsFx = event.getLaunchParameters();
			Plugins.getLaunchables().forEach(l -> l.call(paramsFx, false));
			List<String> jfxArgs = paramsFx.getRaw();
			String[] args = jfxArgs.toArray(new String[0]);
			LaunchParams params = new LaunchParams();
			CommandLine.call(params, System.out, args);
			if (params.agent) {
				Bus.post(new NewInputEvent(Agent.inst));
				return;
			}
			// load file & class if specified
			File file = params.initialFile;
			if (file != null && file.exists()) {
				Bus.post(new NewInputEvent(file));
				//
				Input in = Input.get();
				String clazz = params.initialClass;
				if (clazz != null && in.classes.contains(clazz)) {
					ClassNode cn = in.getClass(clazz);
					Bus.post(new ClassOpenEvent(cn));
				}
			}
			Plugins.getLaunchables().forEach(l -> l.call(paramsFx, true));
		} catch (Exception e) {
			Logging.fatal(e);
		}
	}
}
