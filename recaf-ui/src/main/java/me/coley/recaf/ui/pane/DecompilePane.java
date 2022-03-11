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
import me.coley.recaf.ui.behavior.*;
import me.coley.recaf.ui.control.ErrorDisplay;
import me.coley.recaf.ui.control.SearchBar;
import me.coley.recaf.ui.control.code.ProblemIndicatorInitializer;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.util.ClearableThreadPool;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
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
		StackPane.setAlignment(errorDisplay, Configs.editor().errorIndicatorPos);
		StackPane.setMargin(errorDisplay, new Insets(16, 25, 25, 53));
		stack.getChildren().add(node);
		stack.getChildren().add(errorDisplay);
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
				FxThreadUtil.run(() -> javaArea.setText("// No decompiler available!"));
				return;
			}
			// Cancel old thread
			if (threadPool.hasActiveThreads()) {
				threadPool.clear();
			}
			ScrollSnapshot scrollSnapshot = makeScrollSnapshot();
			FxThreadUtil.run(() -> javaArea.setText("// Decompiling " + newValue.getName()));
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
						StringWriter writer = new StringWriter();
						t.printStackTrace(new PrintWriter(writer));
						String name = newValue.getName();
						String text = "// Decompiler for " + name + " has crashed.\n" +
								"// Cause:\n\n" + writer;
						javaArea.setText(text, false);
					}
				} else {
					javaArea.setText(code, false);
					FxThreadUtil.delayedRun(100, scrollSnapshot::restore);
				}
			};
			decompileFuture.whenCompleteAsync(onComplete, FxThreadUtil.executor())
					.exceptionally(t -> {
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
