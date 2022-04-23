package me.coley.recaf.ui.pane;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.decompile.DecompileManager;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;

import java.util.function.Supplier;

/**
 * Decompiler UI for {@link ClassInfo}.
 *
 * @author Matt Coley
 * @see DexDecompilePane Android alternative.
 */
public class DecompilePane extends CommonDecompilePane {
	private Decompiler decompiler;

	/**
	 * Create and set up the panel.
	 */
	public DecompilePane() {
		super();
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

	@Override
	protected boolean checkDecompilePreconditions() {
		if (decompiler == null) {
			FxThreadUtil.run(() -> javaArea.setText("// No decompiler available!", false));
			return false;
		}
		return true;
	}

	@Override
	protected Supplier<String> supplyDecompile() {
		return () -> {
			Workspace workspace = RecafUI.getController().getWorkspace();
			ClassInfo classInfo = ((ClassInfo) getCurrentClassInfo());
			return decompiler.decompile(workspace, classInfo).getValue();
		};
	}
}
