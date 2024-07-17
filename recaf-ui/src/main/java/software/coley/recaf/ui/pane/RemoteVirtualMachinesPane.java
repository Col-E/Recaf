package software.coley.recaf.ui.pane;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import com.sun.tools.attach.VirtualMachineDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.collections.func.UncheckedSupplier;
import software.coley.observables.ObservableObject;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.attach.AttachManager;
import software.coley.recaf.services.attach.JmxBeanServerConnection;
import software.coley.recaf.services.attach.NamedMBeanInfo;
import software.coley.recaf.services.attach.PostScanListener;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.window.RemoteVirtualMachinesWindow;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceRemoteVmResource;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanFeatureInfo;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static software.coley.recaf.util.Lang.getBinding;

/**
 * Pane for displaying available remote JVMs from {@link AttachManager}.
 *
 * @author Matt Coley
 * @see RemoteVirtualMachinesWindow
 */
@Dependent
public class RemoteVirtualMachinesPane extends BorderPane implements PostScanListener, WorkspaceCloseListener {
	private static final Logger logger = Logging.get(RemoteVirtualMachinesPane.class);
	private final ObservableObject<VirtualMachineDescriptor> connectedVm = new ObservableObject<>(null);
	private final Map<VirtualMachineDescriptor, VmPane> vmCellMap = new HashMap<>();
	private final Map<VirtualMachineDescriptor, Button> vmButtonMap = new HashMap<>();
	private final VBox vmButtonsList = new VBox();
	private final BorderPane vmDisplayPane = new BorderPane();
	private final AttachManager attachManager;
	private final WorkspaceManager workspaceManager;
	private VmPane currentVmPane;

	@Inject
	public RemoteVirtualMachinesPane(@Nonnull AttachManager attachManager,
									 @Nonnull WorkspaceManager workspaceManager) {
		this.attachManager = attachManager;
		this.workspaceManager = workspaceManager;

		// Register this class as scan listener so we can update the UI live as updates come in.
		attachManager.addPostScanListener(this);

		// Register this class as a close listener so we can know when the current attached resource is closed.
		// We'll reset any UI state after doing this associated with being connected.
		workspaceManager.addWorkspaceCloseListener(this);

		// Setup UI
		if (attachManager.canAttach())
			initialize();
		else
			initializeWithoutAttach();
	}

	/**
	 * Sets up the UI, and binds passive scanning to only occur while this pane is displayed.
	 */
	private void initialize() {
		// Layout
		vmButtonsList.setPadding(new Insets(5));
		vmButtonsList.setAlignment(Pos.TOP_LEFT);
		vmButtonsList.setSpacing(5);
		ScrollPane scroll = new ScrollPane(vmButtonsList);
		scroll.setFitToWidth(true);
		SplitPane split = new SplitPane(scroll, vmDisplayPane);
		SplitPane.setResizableWithParent(scroll, false);
		split.setDividerPositions(0.3);
		setCenter(split);
	}

	/**
	 * Place a warning box stating that the feature is not available.
	 */
	private void initializeWithoutAttach() {
		Label graphic = new Label();
		graphic.setGraphic(new FontIconView(CarbonIcons.ERROR, 128, Color.RED));
		graphic.setAlignment(Pos.CENTER);

		Label title = new Label();
		title.getStyleClass().add(Styles.TITLE_1);
		title.textProperty().bind(getBinding("attach.unsupported"));
		title.setAlignment(Pos.CENTER);

		Label description = new Label();
		description.getStyleClass().add(Styles.TITLE_4);
		description.textProperty().bind(getBinding("attach.unsupported.detail"));
		description.setAlignment(Pos.CENTER);

		VBox box = new VBox(graphic, title, description);
		box.setMaxHeight(Double.MAX_VALUE);
		box.setMaxWidth(Double.MAX_VALUE);
		box.setMinHeight(250);
		box.setMinWidth(300);
		box.setAlignment(Pos.CENTER);
		box.getStyleClass().add("tooltip");

		// Layout 'box' centered on the pane
		VBox vwrap = new VBox(box);
		vwrap.setAlignment(Pos.CENTER);
		vwrap.setMaxHeight(Double.MAX_VALUE);
		vwrap.setMaxWidth(Double.MAX_VALUE);
		HBox hwrap = new HBox(vwrap);
		hwrap.setAlignment(Pos.CENTER);
		hwrap.setMaxHeight(Double.MAX_VALUE);
		hwrap.setMaxWidth(Double.MAX_VALUE);
		hwrap.setMouseTransparent(true);

		setCenter(hwrap);
	}

	@Override
	public void onScanCompleted(@Nonnull Set<VirtualMachineDescriptor> added,
								@Nonnull Set<VirtualMachineDescriptor> removed) {
		FxThreadUtil.run(() -> {
			// Add new VM's found
			for (VirtualMachineDescriptor descriptor : added) {
				if (vmCellMap.containsKey(descriptor))
					continue;
				VmPane cell = new VmPane(descriptor);
				vmCellMap.put(descriptor, cell);

				// Button to display the cell
				Button button = new ActionButton(cell.getLabel(), () -> {
					vmDisplayPane.setCenter(cell);
					currentVmPane = cell;

					// Mark button as 'selected'
					for (Node child : vmButtonsList.getChildren())
						child.getStyleClass().remove(Styles.BUTTON_OUTLINED);
					vmButtonMap.get(descriptor).getStyleClass().add(Styles.BUTTON_OUTLINED);
				});
				button.setFocusTraversable(false);
				button.setMaxWidth(Double.MAX_VALUE);
				button.setAlignment(Pos.CENTER_LEFT);
				button.getStyleClass().add("muted");
				vmButtonMap.put(descriptor, button);
				vmButtonsList.getChildren().add(button);

				// Highlight the button bright green if it is the current attached VM.
				connectedVm.addChangeListener((ob, old, cur) -> {
					if (cur == descriptor) {
						button.getStyleClass().addAll(Styles.SUCCESS);
					} else {
						button.getStyleClass().removeAll(Styles.SUCCESS);
					}
				});
			}

			// Remove VM's that are no longer alive.
			for (VirtualMachineDescriptor descriptor : removed) {
				Button removedButton = vmButtonMap.remove(descriptor);
				vmButtonsList.getChildren().remove(removedButton);

				VmPane cell = vmCellMap.remove(descriptor);
				if (cell != null) cell.setDisable(true);
			}

			// Refresh current cell.
			if (currentVmPane != null && !currentVmPane.isDisabled())
				currentVmPane.update();
		});
	}

	@Override
	public void onWorkspaceClosed(@Nonnull Workspace workspace) {
		connectedVm.setValue(null);
	}

	/**
	 * Display for a remote JVM.
	 */
	private class VmPane extends VBox {
		private final ContentTabs contentGrid;
		private final String label;

		/**
		 * @param descriptor
		 * 		Associated descriptor.
		 */
		VmPane(VirtualMachineDescriptor descriptor) {
			this.contentGrid = new ContentTabs(attachManager, descriptor);
			VBox.setVgrow(contentGrid, Priority.ALWAYS);

			// Remote VM info
			int pid = attachManager.getVirtualMachinePid(descriptor);
			String mainClass = attachManager.getVirtualMachineMainClass(descriptor);
			this.label = pid + ": " + mainClass;

			// Create title controls
			boolean canConnect = attachManager.getVirtualMachineConnectionFailure(descriptor) == null;
			CarbonIcons titleIcon = canConnect ? CarbonIcons.DEBUG : CarbonIcons.ERROR_FILLED;
			FontIconView titleGraphic = new FontIconView(titleIcon, 28, canConnect ? Color.LIME.brighter() : Color.RED);
			Button connectButton = new ActionButton(titleGraphic, getBinding("attach.connect"), () -> {
				if (workspaceManager.closeCurrent()) {
					ThreadUtil.run(() -> {
						try {
							WorkspaceRemoteVmResource vmResource = attachManager.createRemoteResource(descriptor);
							vmResource.connect();
							workspaceManager.setCurrent(new BasicWorkspace(vmResource));
							connectedVm.setValue(descriptor);
						} catch (IOException ex) {
							logger.error("Failed to connect to remote VM: {}", label, ex);
							ErrorDialogs.show(getBinding("dialog.error.attach.title"),
									getBinding("dialog.error.attach.header"),
									getBinding("dialog.error.attach.content"),
									ex);
						}
					});
				}
			});
			// Give the button a rounded appearance, which becomes solid
			connectButton.setMinWidth(120);
			connectButton.getStyleClass().add(Styles.ROUNDED);
			connectButton.setFocusTraversable(false);
			connectedVm.addChangeListener((obs, old, cur) -> {
				if (cur == descriptor) {
					connectButton.getStyleClass().addAll(Styles.ACCENT, Styles.SUCCESS);
					connectButton.setMouseTransparent(true); // Disable clicking again until disconnected
				} else {
					connectButton.getStyleClass().removeAll(Styles.ACCENT, Styles.SUCCESS);
					connectButton.setMouseTransparent(false);
				}
			});

			// Layout
			Label title = new Label(label);
			title.getStyleClass().add(Styles.TEXT_CAPTION);
			title.setPadding(new Insets(10));
			HBox titleWrapper = new HBox(connectButton, title);
			titleWrapper.setAlignment(Pos.CENTER_LEFT);
			titleWrapper.setPadding(new Insets(10));
			titleWrapper.setSpacing(10);

			// Layout
			getChildren().addAll(titleWrapper, contentGrid);
		}

		/**
		 * @return Display title.
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * Updates the content display.
		 */
		void update() {
			contentGrid.update();
		}

		/**
		 * Wrapper of multiple content titles.
		 */
		private static class ContentTabs extends TabPane {
			private final List<AbstractContentTile> tiles = new ArrayList<>();

			public ContentTabs(AttachManager attachManager, VirtualMachineDescriptor descriptor) {
				// Property tile
				add(new AbstractContentTile() {
					private final TableView<String> propertyTable = new TableView<>();
					private Properties lastProperties;

					@Override
					void setup() {
						TableColumn<String, String> keyColumn = new TableColumn<>("Key");
						keyColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()));
						TableColumn<String, String> valueColumn = new TableColumn<>("Value");
						valueColumn.setCellValueFactory(param -> new SimpleStringProperty(Objects.toString(lastProperties.get(param.getValue()))));

						ObservableList<TableColumn<String, ?>> columns = propertyTable.getColumns();
						columns.add(keyColumn);
						columns.add(valueColumn);

						propertyTable.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE);
						propertyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
						keyColumn.setMaxWidth(1f * Integer.MAX_VALUE * 25);
						valueColumn.setMaxWidth(1f * Integer.MAX_VALUE * 75);

						setCenter(propertyTable);
					}

					@Override
					void update() {
						// Update property table if there are changes
						Properties properties = attachManager.getVirtualMachineProperties(descriptor);
						if (Objects.equals(lastProperties, properties)) return;
						lastProperties = properties;

						// Update table
						ObservableList<String> items = propertyTable.getItems();
						List<String> keys = properties.keySet().stream().map(Object::toString).sorted().toList();
						items.clear();
						items.addAll(keys);
					}

					@Override
					Tab tab() {
						Tab tab = new Tab();
						tab.setClosable(false);
						tab.setGraphic(new FontIconView(CarbonIcons.SETTINGS));
						tab.textProperty().bind(getBinding("attach.tab.properties"));
						return tab;
					}
				});

				// JMX tiles
				JmxBeanServerConnection jmxConnection = attachManager.getJmxServerConnection(descriptor);
				if (jmxConnection == null) {
					logger.warn("Failed to get JMX connection for descriptor: {}", descriptor);
					return;
				}
				List<JmxWrapper> beanSuppliers = List.of(
						new JmxWrapper(CarbonIcons.OBJECT_STORAGE, "attach.tab.classloading", jmxConnection::getClassloadingBeanInfo),
						new JmxWrapper(CarbonIcons.QUERY_QUEUE, "attach.tab.compilation", jmxConnection::getCompilationBeanInfo),
						new JmxWrapper(CarbonIcons.SCREEN, "attach.tab.system", jmxConnection::getOperatingSystemBeanInfo),
						new JmxWrapper(CarbonIcons.METER, "attach.tab.runtime", jmxConnection::getRuntimeBeanInfo),
						new JmxWrapper(CarbonIcons.PARENT_CHILD, "attach.tab.thread", jmxConnection::getThreadBeanInfo)
				);
				for (JmxWrapper wrapper : beanSuppliers) {
					add(new AbstractContentTile() {
						private final TableView<String> propertyTable = new TableView<>();
						private Map<String, String> lastAttributeMap;

						@Override
						void setup() {
							TableColumn<String, String> keyColumn = new TableColumn<>("Key");
							keyColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue()));

							TableColumn<String, String> valueColumn = new TableColumn<>("Value");
							valueColumn.setCellValueFactory(param -> new SimpleStringProperty(Objects.toString(lastAttributeMap.get(param.getValue()))));

							ObservableList<TableColumn<String, ?>> columns = propertyTable.getColumns();
							columns.add(keyColumn);
							columns.add(valueColumn);

							propertyTable.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE);
							propertyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
							keyColumn.setMaxWidth(1f * Integer.MAX_VALUE * 25);
							valueColumn.setMaxWidth(1f * Integer.MAX_VALUE * 75);

							setCenter(propertyTable);
						}

						@Override
						void update() {
							try {
								// Update attribute table if there are changes.
								//
								// The bean supplier can block, and if the connection has died this block will go until
								// a timeout period elapses. So we want to do this in the background.
								CompletableFuture.supplyAsync(() -> {
									NamedMBeanInfo beanInfo = wrapper.beanSupplier().get();
									MBeanAttributeInfo[] attributes = beanInfo.getAttributes();
									return Arrays.stream(attributes)
											.collect(Collectors.toMap(MBeanFeatureInfo::getDescription, attribute -> {
												try {
													Object value = beanInfo.getAttributeValue(jmxConnection, attribute);
													if (value != null) {
														if (value.getClass().isArray()) {
															value = Arrays.toString(convertToObjectArray(value));
														}
													}
													return Objects.toString(value);
												} catch (Exception ex) {
													return "?";
												}
											}));

								}).whenCompleteAsync((attributeMap, error) -> {
									if (Objects.equals(lastAttributeMap, attributeMap)) return;
									lastAttributeMap = attributeMap;

									// Update table
									ObservableList<String> items = propertyTable.getItems();
									List<String> keys = attributeMap.keySet().stream().map(Object::toString).sorted().toList();
									items.clear();
									items.addAll(keys);

									// Enable on success
									setDisable(false);
								}, FxThreadUtil.executor());
							} catch (Exception ex) {
								// Disable on failure
								setDisable(true);
							}
						}

						@Override
						Tab tab() {
							Tab tab = new Tab();
							tab.setClosable(false);
							tab.setGraphic(new FontIconView(wrapper.icon()));
							tab.textProperty().bind(getBinding(wrapper.langKey()));
							return tab;
						}
					});
				}
			}

			/**
			 * @param tile
			 * 		Tile to add to the content grid.
			 */
			private void add(AbstractContentTile tile) {
				tile.setup();
				tiles.add(tile);
				Tab tab = tile.tab();
				tab.setContent(tile);
				getTabs().add(tab);
			}

			/**
			 * Updates all blocks.
			 */
			public void update() {
				for (AbstractContentTile block : tiles)
					block.update();
			}

			@SuppressWarnings("all")
			static Object[] convertToObjectArray(Object array) {
				Class componentType = array.getClass().getComponentType();
				if (componentType.isPrimitive()) {
					int length = Array.getLength(array);
					Object[] boxedArray = new Object[length];
					for (int i = 0; i < length; i++)
						boxedArray[i] = Array.get(array, i);
					return boxedArray;
				} else {
					return (Object[]) array;
				}
			}
		}

		/**
		 * Simple content tile.
		 */
		private static abstract class AbstractContentTile extends BorderPane {
			abstract void setup();

			abstract void update();

			abstract Tab tab();
		}

		/**
		 * Wrapper to hold both the given values.
		 *
		 * @param icon
		 * 		Graphic icon representation.
		 * @param langKey
		 * 		Identifier for language name lookup.
		 * @param beanSupplier
		 * 		Supplier to the current bean info.
		 */
		private record JmxWrapper(Ikon icon, String langKey,
								  UncheckedSupplier<NamedMBeanInfo> beanSupplier) {
		}
	}
}
