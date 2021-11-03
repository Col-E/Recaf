package me.coley.recaf.ui.pane;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.decompile.DecompileManager;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.ErrorDisplay;
import me.coley.recaf.ui.control.code.ProblemIndicatorInitializer;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.util.Threads;
import me.coley.recaf.util.visitor.ClearableThreadPool;
import me.coley.recaf.workspace.Workspace;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.concurrent.Future;

/**
 * Decompiler wrapper of {@link JavaArea}.
 *
 * @author Matt Coley
 */
public class DecompilePane extends BorderPane implements ClassRepresentation, Cleanable {
	private final ClearableThreadPool threadPool = new ClearableThreadPool(1, true, "Decompile");
	private final JavaArea javaArea;
	private Decompiler decompiler;
	private CommonClassInfo lastClass;
	private boolean ignoreNextDecompile;

	/**
	 * Create and set up the panel.
	 */
	public DecompilePane() {
		ProblemTracking tracking = new ProblemTracking();
		tracking.setIndicatorInitializer(new ProblemIndicatorInitializer(tracking));
		this.javaArea = new JavaArea(tracking);
		Node node = new VirtualizedScrollPane<>(javaArea);
		Node errorDisplay = new ErrorDisplay(javaArea, tracking);
		StackPane stack = new StackPane();
		StackPane.setAlignment(errorDisplay, Pos.TOP_LEFT);
		StackPane.setMargin(errorDisplay, new Insets(16, 25, 25, 53));
		stack.getChildren().add(node);
		stack.getChildren().add(errorDisplay);
		setCenter(stack);
		setBottom(createButtonBar());
	}

	private Node createButtonBar() {
		HBox box = new HBox();
		box.setSpacing(10);
		box.getStyleClass().add("button-container");
		box.setAlignment(Pos.CENTER_LEFT);
		DecompileManager manager = RecafUI.getController().getServices().getDecompileManager();
		ComboBox<String> decompilerCombo = new ComboBox<>();
		for (Decompiler decompiler : manager.getRegisteredImpls()) {
			decompilerCombo.getItems().add(decompiler.getName());
		}
		decompilerCombo.getSelectionModel().selectedItemProperty().addListener((observable, old, current) -> {
			decompiler = manager.get(current);
			onUpdate(lastClass);
		});
		Label decompilersLabel = new Label("Decompiler: ");
		box.getChildren().add(decompilersLabel);
		box.getChildren().add(decompilerCombo);
		// TODO: Add button to configure current decompiler
		//   (pull from tool map, each decompiler has separate config page)
		// Select preferred decompiler, or whatever is first if the preferred option is not available
		decompilerCombo.getSelectionModel().select(Configs.decompiler().decompiler);
		if (decompilerCombo.getSelectionModel().isEmpty()) {
			decompilerCombo.getSelectionModel().select(0);
		}
		return box;
	}

	@Override
	public boolean supportsMemberSelection() {
		return javaArea.supportsMemberSelection();
	}

	@Override
	public boolean isMemberSelectionReady() {
		return javaArea.isMemberSelectionReady();
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		javaArea.selectMember(memberInfo);
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		lastClass = newValue;
		javaArea.onUpdate(newValue);
		if (newValue instanceof ClassInfo) {
			if (ignoreNextDecompile) {
				ignoreNextDecompile = false;
				return;
			}
			if (decompiler == null) {
				javaArea.setText("// No decompiler available!");
			}
			// Cancel old thread
			if (threadPool.hasActiveThreads()) {
				threadPool.clear();
			}
			// Create new threaded decompile
			Future<?> decompileFuture = threadPool.submit(() -> {
				Workspace workspace = RecafUI.getController().getWorkspace();
				ClassInfo classInfo = ((ClassInfo) newValue);
				String code = decompiler.decompile(workspace, classInfo).getValue();

				Threads.runFx(() -> {
					javaArea.setText(code);
				});
			});
			int timeout = Configs.decompiler().decompileTimeout;
			if (!Threads.timeout(timeout, decompileFuture)) {
				threadPool.clear();
				String name = newValue.getName();
				javaArea.setText("// Decompile thread for '" + name + "' exceeded timeout of " + timeout + "ms.\n" +
						"// Some suggestions:\n" +
						"//  - Increase the timeout in the config menu\n" +
						"//  - Try a different decompiler\n" +
						"//  - Switch display modes\n");
			}
		}
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return lastClass;
	}

	@Override
	public void cleanup() {
		javaArea.cleanup();
		threadPool.clear();
		threadPool.shutdownNow();
	}

	@Override
	public SaveResult save() {
		// The save operation updates the primary resource. Due to the listener setup anything that gets modified
		// is updated (which includes this pane). If we are the one who invoked the change, we want to ignore it.
		ignoreNextDecompile = true;
		SaveResult result = javaArea.save();
		// If the result was not a success the next resource update call is not from our request here.
		// So we do want to acknowledge the next decompile.
		if (result != SaveResult.SUCCESS) {
			ignoreNextDecompile = false;
		}
		return result;
	}

	@Override
	public boolean supportsEditing() {
		return javaArea.supportsEditing();
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}
}
