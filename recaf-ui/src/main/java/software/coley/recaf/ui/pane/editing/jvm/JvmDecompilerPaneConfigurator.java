package software.coley.recaf.ui.pane.editing.jvm;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableObject;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.ObservableCheckBox;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.pane.editing.AbstractDecompilerPaneConfigurator;
import software.coley.recaf.ui.pane.editing.ToolsContainerComponent;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.ToStringConverter;

/**
 * Overlay component for {@link Editor} that allows quick configuration of properties of a {@link JvmDecompilerPane}.
 *
 * @author Matt Coley
 */
public class JvmDecompilerPaneConfigurator extends AbstractDecompilerPaneConfigurator {
	private final ObservableInteger javacTarget;
	private final ObservableInteger javacDownsampleTarget;
	private final ObservableBoolean javacDebug;

	/**
	 * @param toolsContainer
	 * 		Container to house tool buttons for display in the {@link Editor}.
	 * @param config
	 * 		Containing {@link JvmDecompilerPane} config singleton.
	 * @param decompiler
	 * 		Local decompiler implementation.
	 * @param javacTarget
	 * 		Local target version for {@code javac}.
	 * @param javacDownsampleTarget
	 * 		Local target version to downsample to for {@code javac}.
	 * @param javacDebug
	 * 		Local debug flag for {@code javac}.
	 * @param decompilerManager
	 * 		Manager to pull available {@link JvmDecompiler} instances from.
	 */
	public JvmDecompilerPaneConfigurator(@Nonnull ToolsContainerComponent toolsContainer,
										 @Nonnull DecompilerPaneConfig config,
										 @Nonnull ObservableObject<JvmDecompiler> decompiler,
										 @Nonnull ObservableInteger javacTarget,
										 @Nonnull ObservableInteger javacDownsampleTarget,
										 @Nonnull ObservableBoolean javacDebug,
										 @Nonnull DecompilerManager decompilerManager) {
		super(toolsContainer, config, decompiler, decompilerManager);
		this.javacTarget = javacTarget;
		this.javacDownsampleTarget = javacDownsampleTarget;
		this.javacDebug = javacDebug;
	}

	@Nonnull
	@Override
	protected GridPane createGrid() {
		GridPane content = super.createGrid();

		// Compilation config
		Label compileTitle = new BoundLabel(Lang.getBinding("service.compile"));
		compileTitle.getStyleClass().addAll(Styles.TEXT_UNDERLINED, Styles.TITLE_4);
		Label labelTargetVersion = new BoundLabel(Lang.getBinding("java.targetversion"));
		Label labelTargetDownsampleVersion = new BoundLabel(Lang.getBinding("java.targetdownsampleversion"));
		Label labelDebug = new BoundLabel(Lang.getBinding("java.targetdebug"));

		int row = content.getRowCount() + 1;
		content.add(compileTitle, 0, row++, 2, 1);

		content.add(labelTargetVersion, 0, row);
		content.add(fix(new JavacVersionComboBox()), 1, row++);

		content.add(labelTargetDownsampleVersion, 0, row);
		content.add(fix(new JavacDownsampleVersionComboBox()), 1, row++);

		content.add(labelDebug, 0, row);
		content.add(fix(new ObservableCheckBox(javacDebug, Lang.getBinding("misc.enabled"))), 1, row++);

		return content;
	}

	private class JavacVersionComboBox extends ComboBox<Integer> {
		private JavacVersionComboBox() {
			// TODO: Changing the value in this box causes the UI thread to 'sometimes' freeze the UI
			//  - Nothing in the stacktrace from Recaf, so probably some weird event-loop
			//  - No idea why this would trigger that though
			//  - Does not occur if the valueProperty listener is commented out

			int max = JavaVersion.get();
			for (int i = JavacCompiler.getMinTargetVersion(); i <= max; i++)
				getItems().add(i);

			// Edge case for 'automatic'
			getItems().add(-1);
			setValue(-1);
			setConverter(ToStringConverter.from(version -> {
				int v = version;
				if (v < 0)
					return Lang.get("java.targetversion.auto");
				return String.valueOf(v);
			}));

			// Hack to prevent odd resize-based deadlock: #798
			int w = 200;
			setMaxWidth(w);
			setPrefWidth(w);

			// Update property.
			valueProperty().addListener((ob, old, cur) -> javacTarget.setValue(cur));
		}
	}

	private class JavacDownsampleVersionComboBox extends ComboBox<Integer> {
		private JavacDownsampleVersionComboBox() {
			int max = JavaVersion.get();
			for (int i = JavacCompiler.MIN_DOWNSAMPLE_VER; i <= max; i++)
				getItems().add(i);

			// Edge case for 'disabled'
			getItems().add(-1);
			setValue(-1);
			setConverter(ToStringConverter.from(version -> {
				int v = version;
				if (v < 0)
					return Lang.get("java.targetdownsampleversion.disabled");
				return String.valueOf(v);
			}));

			// Hack to prevent odd resize-based deadlock: #798
			int w = 200;
			setMaxWidth(w);
			setPrefWidth(w);

			// Update property.
			valueProperty().addListener((ob, old, cur) -> javacDownsampleTarget.setValue(cur));
		}
	}
}
