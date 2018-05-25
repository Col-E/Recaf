package me.coley.recaf;

import java.io.File;
import java.util.List;

import org.objectweb.asm.tree.ClassNode;

import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.event.ClassOpenEvent;
import me.coley.recaf.event.NewInputEvent;
import me.coley.recaf.event.UiInitEvent;
import me.coley.recaf.ui.FxWindow;
import picocli.CommandLine;

public class Recaf {
	public static void main(String[] args) {
		//Bus.INSTANCE.subscribe(new Recaf());
		FxWindow.init(args);
	}

	@Listener
	private void onInit(UiInitEvent event) {
		try {
			// convert parameters to string array so picocli can parse it
			List<String> jfxArgs = event.getLaunchParameters().getRaw();
			String[] args = jfxArgs.toArray(new String[jfxArgs.size()]);
			LaunchParams params = new LaunchParams();
			CommandLine.call(params, System.out, args);
			// load file & class if specified
			File file = params.initialFile;
			if (file.exists()) {
				Bus.INSTANCE.post(new NewInputEvent(file));				
				//
				Input in = Input.get();
				String clazz = params.initialClass;
				if (clazz != null && in.classes.contains(clazz)) {
					ClassNode cn = in.getClass(clazz);
					Bus.INSTANCE.post(new ClassOpenEvent(cn));
				}
			}
		} catch (Exception e) {
			Logging.fatal(e);
		}

	}
}
