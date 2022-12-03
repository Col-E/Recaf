package me.coley.recaf.ui.pane;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.WindowEvent;
import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.instrument.InstrumentationManager;
import me.coley.recaf.ui.behavior.WindowCloseListener;
import me.coley.recaf.ui.behavior.WindowShownListener;
import me.coley.recaf.ui.control.ActionButton;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.AgentResource;
import me.coley.recaf.workspace.resource.Resources;
import org.slf4j.Logger;

import java.util.Properties;

/**
 * Displays various {@link VirtualMachine} instances that can be attached to.
 *
 * @author Matt Coley
 */
public class AttachPane extends BorderPane implements WindowCloseListener, WindowShownListener {
	private static final Logger logger = Logging.get(AttachPane.class);
	private final ObservableList<VirtualMachineDescriptor> virtualMachineDescriptors = FXCollections.observableArrayList();
	private final InstrumentationManager instrumentationManager;

	public AttachPane(InstrumentationManager instrumentationManager) {
		this.instrumentationManager = instrumentationManager;
		// Setup UI
		BorderPane listWrapper = new BorderPane();
		listWrapper.setPadding(new Insets(5));
		ListView<VirtualMachineDescriptor> machineListView = new ListView<>(virtualMachineDescriptors);
		machineListView.setCellFactory(param -> new VMCell());
		listWrapper.setCenter(machineListView);
		setCenter(listWrapper);
		instrumentationManager.getVirtualMachineDescriptors().addChangeListener((source, change) -> {
			FxThreadUtil.run(() -> {
				if (change.wasAddition()) {
					virtualMachineDescriptors.addAll(change.getStart(), change.getAdded());
				} else {
					virtualMachineDescriptors.removeAll(change.getRemoved());
				}
			});
		});
		// Disable UI if the agent jar could not be extracted
		if (!InstrumentationManager.isAgentReady())
			setDisable(true);
	}

	@Override
	public void onShown(WindowEvent e) {
		instrumentationManager.setScanning(true);
	}

	@Override
	public void onClose(WindowEvent e) {
		instrumentationManager.setScanning(false);
	}

	class VMCell extends ListCell<VirtualMachineDescriptor> {
		@Override
		protected void updateItem(VirtualMachineDescriptor item, boolean empty) {
			super.updateItem(item, empty);
			if (item == null || empty) {
				setGraphic(null);
			} else {
				setGraphic(createGraphic(item));
			}
		}

		private Node createGraphic(VirtualMachineDescriptor item) {
			VirtualMachine virtualMachine = instrumentationManager.getVirtualMachine(item);
			Exception attachException = instrumentationManager.getVirtualMachineConnectionFailure(item);
			int virtualMachinePid = instrumentationManager.getVirtualMachinePid(item);
			String virtualMachineMainClass = instrumentationManager.getVirtualMachineMainClass(item);
			VBox vertical = new VBox();
			vertical.setPadding(new Insets(5));
			vertical.setSpacing(5);
			HBox horizontal = new HBox();
			horizontal.setAlignment(Pos.CENTER_LEFT);
			horizontal.setSpacing(15);
			VBox buttons = new VBox();
			Label header = new Label(virtualMachinePid + " - " + virtualMachineMainClass);
			header.getStyleClass().add("h2");
			vertical.getChildren().addAll(header, horizontal);
			Circle circle = new Circle();
			circle.radiusProperty().bind(horizontal.heightProperty().divide(3));
			String message;
			// TODO: Translatable text
			if (virtualMachine != null) {
				Properties properties = instrumentationManager.getVirtualMachineProperties(item);
				if (properties != null) {
					circle.setFill(Color.GREEN);
					String vmName = properties.getProperty("java.vm.name", "Unknown VM distribution");
					String vmVersion = properties.getProperty("java.version", "Unknown VM version");
					message = "VM: " + vmName + "\n" +
							"Version: " + vmVersion;
				} else {
					circle.setFill(Color.ORANGE);
					message = "VM supports attach\nFailed to read remote properties";
				}
			} else if (attachException != null) {
				boolean attachFailure = attachException instanceof AttachNotSupportedException;
				circle.setFill(attachFailure ? Color.RED : Color.ORANGE);
				String cause = attachFailure ? "VM does not support attach" : "IO error reading from VM";
				message = "Failed to attach:\n" + cause;
				buttons.setDisable(true);
			} else {
				throw new IllegalStateException("VM Desc has no associated VM, but also no attach failure reason");
			}
			horizontal.getChildren().addAll(buttons, circle, new Label(message));
			buttons.getChildren().add(new ActionButton("Connect", () -> {
				AgentResource resource = instrumentationManager.connect(item);
				int port = resource.getPort();
				Workspace workspace = new Workspace(new Resources(resource));
				Controller controller = RecafUI.getController();
				if (resource.setup() && controller.setWorkspace(workspace)) {
					logger.info("Connected to remote process '{}' over port {}", item.id(), port);
				} else {
					resource.close();
				}
			}));
			return vertical;
		}
	}
}
