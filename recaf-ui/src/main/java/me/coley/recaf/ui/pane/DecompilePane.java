package me.coley.recaf.ui.pane;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.util.Threads;
import me.coley.recaf.workspace.Workspace;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Decompiler wrapper of {@link JavaArea}.
 *
 * @author Matt Coley
 */
public class DecompilePane extends BorderPane implements ClassRepresentation, Cleanable {
	private final ExecutorService decompileThreadService = Executors.newSingleThreadExecutor();
	private final JavaArea javaArea;
	private Decompiler decompiler;
	private CommonClassInfo lastClass;
	private Future<?> decompileFuture;

	/**
	 * Create and set up the panel.
	 */
	public DecompilePane() {
		ProblemTracking tracking = new ProblemTracking();
		this.javaArea = new JavaArea(tracking);
		Node node = new VirtualizedScrollPane<>(javaArea);
		setCenter(node);
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
		decompilerCombo.getSelectionModel().select(Configs.editor().decompiler);
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
		javaArea.onUpdate(newValue);
		if (newValue instanceof ClassInfo) {
			if (decompiler == null) {
				javaArea.setText("// No decompiler available!");
			}
			// Cancel old thread
			if (decompileFuture != null) {
				decompileFuture.cancel(true);
			}
			// Create new threaded decompile
			decompileFuture = decompileThreadService.submit(() -> {
				Workspace workspace = RecafUI.getController().getWorkspace();
				String code = decompiler.decompile(workspace, (ClassInfo) newValue).getValue();
				Threads.runFx(() -> {
					javaArea.setText(code);
				});
			});
		} else {
			// This should not happen
		}
		lastClass = newValue;
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return lastClass;
	}

	@Override
	public void cleanup() {
		javaArea.cleanup();
		decompileThreadService.shutdownNow();
		if (decompileFuture != null)
			decompileFuture.cancel(true);
	}

	@Override
	public SaveResult save() {
		return javaArea.save();
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
