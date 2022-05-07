package me.coley.recaf.ui.pane;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.decompile.DecompileManager;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.ui.behavior.*;
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.control.ErrorDisplay;
import me.coley.recaf.ui.control.SearchBar;
import me.coley.recaf.ui.control.code.ProblemIndicatorInitializer;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.ClearableThreadPool;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

/**
 * Decompiler wrapper of {@link JavaArea}.
 *
 * @author Matt Coley
 */
public class DecompilePane extends BorderPane implements ClassRepresentation, Cleanable, Scrollable {
	private static final Logger log = Logging.get(DecompilePane.class);
	private final ClearableThreadPool threadPool = new ClearableThreadPool(1, true, "Decompile");
	private final VBox overlay = new VBox();
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
		// Wrap content, create error display
		StackPane node = new StackPane(new VirtualizedScrollPane<>(javaArea));
		Node errorDisplay = new ErrorDisplay(javaArea, tracking);
		// Overlay for 'pls wait for decompile' message
		overlay.setAlignment(Pos.CENTER);
		overlay.getChildren().addAll(new ProgressBar(-1.0), new BoundLabel(Lang.getBinding("java.decompiling")));
		overlay.getChildren().forEach(n -> n.opacityProperty().bind(overlay.opacityProperty()));
		overlay.getStyleClass().add("progress-overlay");
		// Layout
		StackPane.setAlignment(errorDisplay, Configs.editor().errorIndicatorPos);
		StackPane.setMargin(errorDisplay, new Insets(16, 25, 25, 53));
		StackPane stack = new StackPane();
		stack.getChildren().addAll(node, errorDisplay, overlay);
		setCenter(stack);
		// Search support
		SearchBar.install(this, javaArea);
		// Bottom controls for quick config changes
		Node buttonBar = createButtonBar();
		setBottom(buttonBar);
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
			if (lastClass != null)
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

	/**
	 * Fades in the <i>"decompile is in progress bla bla bla"</i> overlay.
	 */
	private void showOverlay() {
		// Makethe element click-opaque
		overlay.setMouseTransparent(false);
		FadeTransition ft = new FadeTransition(Duration.millis(1000), overlay);
		ft.setFromValue(overlay.getOpacity());
		ft.setToValue(0.9);
		ft.play();
	}

	/**
	 * Fades out the <i>"decompile is in progress bla bla bla"</i> overlay.
	 */
	private void hideOverlay() {
		// Make the element click-through
		overlay.setMouseTransparent(true);
		FadeTransition ft = new FadeTransition(Duration.millis(1000), overlay);
		ft.setFromValue(overlay.getOpacity());
		ft.setToValue(0.0);
		ft.play();
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
		boolean isInitialDecompile = lastClass == null;
		lastClass = newValue;
		javaArea.onUpdate(newValue);
		if (newValue instanceof ClassInfo) {
			if (ignoreNextDecompile) {
				ignoreNextDecompile = false;
				return;
			}
			if (decompiler == null) {
				FxThreadUtil.run(() -> javaArea.setText("// No decompiler available!", false));
				return;
			}
			// Cancel old thread
			if (threadPool.hasActiveThreads()) {
				threadPool.clear();
			}
			// Only snapshot if the code is being re-decompiled.
			// We don't need to do scroll-state snapshotting for the initial decompile.
			ScrollSnapshot scrollSnapshot = isInitialDecompile ? null : makeScrollSnapshot();
			showOverlay();
			long timeout = Long.MAX_VALUE;
			if (Configs.decompiler().enableDecompilerTimeout) {
				timeout = Configs.decompiler().decompileTimeout + 500;
			}
			log.debug("Queueing decompilation for {} with timeout {}ms", newValue.getName(), timeout);
			// Create new threaded decompile
			CompletableFuture<String> decompileFuture = CompletableFuture.supplyAsync(() -> {
				Workspace workspace = RecafUI.getController().getWorkspace();
				ClassInfo classInfo = ((ClassInfo) newValue);
				return decompiler.decompile(workspace, classInfo).getValue();
			}, threadPool).orTimeout(timeout, TimeUnit.MILLISECONDS);
			long finalTimeout = timeout;
			BiConsumer<String, Throwable> onComplete = (code, t) -> {
				hideOverlay();
				log.debug("Finished decompilation of {}", newValue.getName(), t);
				if (t != null) {
					if (t instanceof TimeoutException) {
						threadPool.clear();
						String name = newValue.getName();
						String text = "// Decompile thread for '" + name + "' exceeded timeout of " + finalTimeout + "ms.\n" +
								"// Some suggestions:\n" +
								"//  - Increase the timeout in the config menu\n" +
								"//  - Try a different decompiler\n" +
								"//  - Switch view modes\n";
						javaArea.setText(text, false);
					} else {
						String message = StringUtil.traceToString(t);
						String name = newValue.getName();
						String text = "// Decompiler for " + name + " has crashed.\n" +
								"// Cause:\n\n" + message;
						javaArea.setText(text, false);
					}
				} else {
					// Decompile success
					javaArea.setText(code, false);
					if (scrollSnapshot != null)
						FxThreadUtil.delayedRun(100, scrollSnapshot::restore);
				}
			};
			decompileFuture.whenCompleteAsync(onComplete, FxThreadUtil.executor())
					.exceptionally(t -> {
						hideOverlay();
						log.error("Uncaught error while updating decompiler output for {}", newValue.getName(), t);
						return null;
					});
		}
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return lastClass;
	}

	@Override
	public void cleanup() {
		javaArea.cleanup();
		threadPool.clearAndShutdown();
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

	@Override
	public ScrollSnapshot makeScrollSnapshot() {
		return javaArea.makeScrollSnapshot();
	}
}
