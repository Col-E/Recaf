package me.coley.recaf.ui;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import me.coley.recaf.Logging;
import me.coley.recaf.ui.component.ActionButton;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.SelfReference;

/**
 * Window for handling attaching to external processes.
 * 
 * @author Matt
 */
public class FxAttach extends Stage {
	private final static FxAttach INSTANCE = new FxAttach();
	private final ListView<VirtualMachineDescriptor> list = new ListView<>();
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
					String str = item.toString();
					str = str.substring(str.indexOf(":") + 1);
					setText(str);
				}
			}
		});
		list.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			selected = newValue;
			boolean set = selected != null;
			btn.setDisable(!set);
			if (set) {
				btn.setText(selected.toString());
			} else {
				btn.setText(Lang.get("ui.attach.prompt"));
			}
		});
		BorderPane bp = new BorderPane();
		bp.setCenter(list);
		bp.setBottom(btn);
		refresh();
		setScene(JavaFX.scene(bp, 700, 200));
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

	private static void attach(VirtualMachineDescriptor vmDesc) {
		// The attach process is threaded so that the target vm's agent does not
		// lock up the current instance of Recaf.
		// Without threading, the client locks until the agent is terminated.
		new Thread(() -> {
			try {
				VirtualMachine vm = VirtualMachine.attach(vmDesc);
				vm.loadAgent(SelfReference.get().getPath(), "-agent");
				vm.detach();
			} catch (Exception e) {
				Logging.error(e);
			}
		}).start();
	}

	/**
	 * Display config window.
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
}
