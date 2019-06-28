package me.coley.recaf.ui;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import me.coley.recaf.Logging;
import me.coley.recaf.config.Conf;
import me.coley.recaf.ui.component.ActionButton;
import me.coley.recaf.ui.component.ReflectivePropertySheet;
import me.coley.recaf.util.*;
import org.controlsfx.control.PropertySheet;

/**
 * Window for handling attaching to external processes.
 * 
 * @author Matt
 */
public class FxAttach extends Stage {
	private final static FxAttach INSTANCE = new FxAttach();
	private final ListView<VirtualMachineDescriptor> list = new ListView<>();
	private final AgentSheet properties = new AgentSheet();
	private VirtualMachineDescriptor selected;

	private FxAttach() {
		setTitle(Lang.get("ui.attach"));
		getIcons().add(Icons.ATTACH);
		Button btn = new ActionButton(Lang.get("ui.attach.prompt"), () -> {
			attach(selected);
		});
		btn.setDisable(true);
		list.setCellFactory(param -> new ListCell<VirtualMachineDescriptor>() {
			@Override
			public void updateItem(VirtualMachineDescriptor item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
					setText(null);
				} else {
					setGraphic(null);
					setText(name(item));
				}
			}
		});
		list.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			selected = newValue;
			boolean set = selected != null;
			btn.setDisable(!set);
			if (set) {
				btn.setText(name(selected));
			} else {
				btn.setText(Lang.get("ui.attach.prompt"));
			}
		});
		list.setMaxHeight(400);
		BorderPane bp = new BorderPane();
		bp.setTop(list);
		bp.setCenter(properties);
		bp.setBottom(btn);
		refresh();
		setScene(JavaFX.scene(bp, 700, 500));
	}

	private void refresh() {
		String mxName = ManagementFactory.getRuntimeMXBean().getName();
		String pid = mxName.substring(0, mxName.indexOf("@"));
		List<VirtualMachineDescriptor> vms = VirtualMachine.list();
		vms = vms.stream()
				.filter(vm -> !vm.toString().contains(pid))
				.collect(Collectors.toList());
		list.getItems().clear();
		list.getItems().addAll(vms);
	}

	private void attach(VirtualMachineDescriptor vmDesc) {
		// The attach process is threaded so that the target vm's agent does not
		// lock up the current instance of Recaf.
		// Without threading, the client locks until the agent is terminated.
		Threads.run(()->{
			AtomicBoolean cancelShutdown = new AtomicBoolean();
			try {
				// Schedule shutdown
				if (properties.doQuit()) {
					Threads.runLater(500, () -> {
						if(!cancelShutdown.get()) {
							// We can die happy now
							System.exit(0);
						}
					});
				}
				// Attach
				VirtualMachine vm = VirtualMachine.attach(vmDesc);
				vm.loadAgent(SelfReference.get().getPath(), "--skip=" + properties.getIgnoreList());
				vm.detach();
			} catch (Exception e) {
				cancelShutdown.set(true);
				Logging.error(e);
			}
		});
	}

	private static String name(VirtualMachineDescriptor item) {
		String str = item.toString();
		str = str.substring(str.indexOf(":") + 1);
		return str;
	}

	/**
	 * Display attach window.
	 */
	public static void open() {
		if (INSTANCE.isShowing()) {
			INSTANCE.toFront();
		} else {
			INSTANCE.show();
		}
		// refresh VM list
		INSTANCE.refresh();
	}

	/**
	 * Agent property-sheet used to pass arguments to the Recaf agent.
	 */
	public static class AgentSheet extends ReflectivePropertySheet
	{
		private final Args args = new Args();

		AgentSheet() {
			setModeSwitcherVisible(false);
			setSearchBoxVisible(false);
			setupItems(args);
		}

		@Override
		protected void setupItems(Object instance) {
			for (Field field : Reflect.fields(instance.getClass())) {
				// Require conf annotation
				Conf conf = field.getDeclaredAnnotation(Conf.class);
				if (conf == null) continue;
				else if (conf.hide()) continue;
				// Setup item & add to list
				getItems().add(new AgentItem(instance, field, conf.category(), conf.key()));
			}
		}

		class AgentItem extends ReflectiveItem {
			public AgentItem(Object owner, Field field, String categoryKey, String translationKey) {
				super(owner, field, categoryKey, translationKey);
			}

			@Override
			protected Class<?> getEditorType() {
				// Copy pasted from the search property sheet, it works fine though so whatever
				Field f = getField();
				if (f.getName().equals("ignored")) {
					return FxSearch.IgnoreList.class;
				}
				return null;
			}
		}

		public String getIgnoreList() {
			return args.ignored.stream().reduce((a, b) -> a + "," + b).get();
		}

		public boolean doQuit() {
			return args.quitOnAttach;
		}

		public static class Args {
			@Conf(category = "params", key = "ignored")
			public List<String> ignored = new ArrayList<>();
			@Conf(category = "ui", key = "quitonattach")
			public boolean quitOnAttach = true;

			/**
			 * Add default ignored packages
			 */
			Args() {
				// core jvm
				ignored.add("java/");
				ignored.add("javax/");
				ignored.add("javafx/");
				ignored.add("com/oracle/");
				ignored.add("com/sun/");
				ignored.add("sun/");
				ignored.add("jdk/");
				// recaf dependencies
				ignored.add("me/coley/");
				ignored.add("com/eclipsesource/");
				ignored.add("com/google/common/");
				ignored.add("com/github/javaparser/");
				ignored.add("org/commonmark/");
				ignored.add("org/slf4j/");
				ignored.add("org/reactfx/");
				ignored.add("org/plugface/");
				ignored.add("org/fxmisc/");
				ignored.add("org/benf/");
				ignored.add("org/controlsfx/");
				ignored.add("impl/org/controlsfx/");
				ignored.add("jregex/");
				ignored.add("picocli/");
			}
		}
	}
}
