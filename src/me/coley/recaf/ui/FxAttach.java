package me.coley.recaf.ui;

import java.io.File;
import java.security.CodeSource;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import me.coley.recaf.Logging;
import me.coley.recaf.Recaf;
import me.coley.recaf.ui.component.ActionButton;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;
import net.bytebuddy.agent.ByteBuddyAgent;

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
					setText(item.displayName());
				}
			}
		});
		list.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<VirtualMachineDescriptor>() {
			@Override
			public void changed(ObservableValue<? extends VirtualMachineDescriptor> observable, VirtualMachineDescriptor oldValue,
					VirtualMachineDescriptor newValue) {
				selected = newValue;
				boolean set = selected != null;
				btn.setDisable(!set);
				if (set) {
					btn.setText(selected.displayName());
				} else {
					btn.setText(Lang.get("ui.attach.prompt"));
				}
			}

		});
		BorderPane bp = new BorderPane();
		bp.setCenter(list);
		bp.setBottom(btn);
		refresh();
		setScene(JavaFX.scene(bp, 700, 200));
	}

	private void refresh() {
		list.getItems().clear();
		list.getItems().addAll(VirtualMachine.list());
	}

	private static void attach(VirtualMachineDescriptor vm) {
		try {
			ByteBuddyAgent.attach(getSelf(), vm.id(), "-agent");
		} catch (Exception e) {
			Logging.error(e);
		}
	}
	
	private static File getSelf() throws Exception {
		CodeSource codeSource = Recaf.class.getProtectionDomain().getCodeSource();
		File jarFile = new File(codeSource.getLocation().toURI().getPath());
		if (!jarFile.getName().toLowerCase().endsWith(".jar")) {
			throw new RuntimeException("Please run recaf as a jar file to attach. You ran from: " + jarFile.getAbsolutePath());
		}
		return jarFile;
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
	}
}
