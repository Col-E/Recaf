package software.coley.recaf.ui.pane.editing.jvm;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableObject;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.ObservableCheckBox;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.pane.editing.AbstractDecompilerPaneConfigurator;
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
	private final ObservableBoolean javacDebug;

	/**
	 * @param config
	 * 		Containing {@link JvmDecompilerPane} config singleton.
	 * @param decompiler
	 * 		Local decompiler implementation.
	 * @param javacTarget
	 * 		Local target version for {@code javac}.
	 * @param javacDebug
	 * 		Local debug flag for {@code javac}.
	 * @param decompilerManager
	 * 		Manager to pull available {@link JvmDecompiler} instances from.
	 */
	public JvmDecompilerPaneConfigurator(@Nonnull DecompilerPaneConfig config,
										 @Nonnull ObservableObject<JvmDecompiler> decompiler,
										 @Nonnull ObservableInteger javacTarget,
										 @Nonnull ObservableBoolean javacDebug,
										 @Nonnull DecompilerManager decompilerManager) {
		super(config, decompiler, decompilerManager);
		this.javacTarget = javacTarget;
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
		Label labelDebug = new BoundLabel(Lang.getBinding("java.targetdebug"));
		content.add(compileTitle, 0, 3, 2, 1);
		content.add(labelTargetVersion, 0, 4);
		content.add(fix(new JavacVersionComboBox()), 1, 4);
		content.add(labelDebug, 0, 5);
		content.add(fix(new ObservableCheckBox(javacDebug, Lang.getBinding("misc.enabled"))), 1, 5);

		return content;
	}

	private class JavacVersionComboBox extends ComboBox<Integer> {
		private JavacVersionComboBox() {
			int max = JavaVersion.get();
			for (int i = 7; i <= max; i++)
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

			// Update property.
			valueProperty().addListener((ob, old, cur) -> javacTarget.setValue(cur));
		}
	}
}
