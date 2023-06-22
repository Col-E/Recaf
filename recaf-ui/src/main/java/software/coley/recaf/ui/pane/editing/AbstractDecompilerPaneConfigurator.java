package software.coley.recaf.ui.pane.editing;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.observables.ObservableObject;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.ui.control.*;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;
import software.coley.recaf.ui.control.richtext.ScrollbarPaddingUtil;
import software.coley.recaf.ui.pane.editing.jvm.DecompilerPaneConfig;
import software.coley.recaf.ui.pane.editing.jvm.JvmDecompilerPane;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.util.Lang;

import java.util.ArrayList;

/**
 * Overlay component for {@link Editor} that allows quick configuration of properties of a {@link AbstractDecompilePane}.
 *
 * @author Matt Coley
 */
public abstract class AbstractDecompilerPaneConfigurator extends Button implements EditorComponent {
	private final ChangeListener<Boolean> handleScrollbarVisibility = (ob, old, cur) -> ScrollbarPaddingUtil.handleScrollbarVisibility(this, cur);
	private final DecompilerPaneConfig config;
	private final ObservableObject<JvmDecompiler> decompiler;
	private final DecompilerManager decompilerManager;
	private Popover popover;

	/**
	 * @param config
	 * 		Containing {@link AbstractDecompilePane} config singleton.
	 * @param decompiler
	 * 		Local decompiler implementation.
	 * @param decompilerManager
	 * 		Manager to pull available {@link JvmDecompiler} instances from.
	 */
	public AbstractDecompilerPaneConfigurator(@Nonnull DecompilerPaneConfig config,
											  @Nonnull ObservableObject<JvmDecompiler> decompiler,
											  @Nonnull DecompilerManager decompilerManager) {
		this.config = config;
		this.decompiler = decompiler;
		this.decompilerManager = decompilerManager;
		setGraphic(new FontIconView(CarbonIcons.SETTINGS));
		getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT);
		setOnAction(this::showConfiguratorPopover);

		// Initial layout
		StackPane.setAlignment(this, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(this, new Insets(7));
	}

	@Override
	public void install(@Nonnull Editor editor) {
		editor.getPrimaryStack().getChildren().add(this);
		editor.getVerticalScrollbar().visibleProperty().addListener(handleScrollbarVisibility);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		editor.getPrimaryStack().getChildren().remove(this);
		editor.getVerticalScrollbar().visibleProperty().removeListener(handleScrollbarVisibility);
	}

	private void showConfiguratorPopover(ActionEvent e) {
		if (popover == null) {
			GridPane content = createGrid();

			// Wrap in popover
			popover = new Popover(content);
			popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
		}
		popover.show(this);
	}

	@Nonnull
	protected GridPane createGrid() {
		GridPane content = new GridPane();
		ColumnConstraints col1 = new ColumnConstraints();
		ColumnConstraints col2 = new ColumnConstraints();
		col2.setFillWidth(true);
		col2.setHgrow(Priority.ALWAYS);
		col2.setHalignment(HPos.RIGHT);
		content.getColumnConstraints().addAll(col1, col2);
		content.setHgap(10);
		content.setVgap(5);

		// Decompile config
		Label decompileTitle = new BoundLabel(Lang.getBinding("service.decompile"));
		decompileTitle.getStyleClass().addAll(Styles.TEXT_UNDERLINED, Styles.TITLE_4);
		Label labelDecompiler = new BoundLabel(Lang.getBinding("java.decompiler"));
		Label labelTimeout = new BoundLabel(Lang.getBinding("service.ui.decompile-pane-config.timeout-seconds"));
		Spinner<Integer> spinTimeout = ObservableSpinner.intSpinner(config.getTimeoutSeconds(), 1, Integer.MAX_VALUE);
		spinTimeout.getStyleClass().add(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL);
		content.add(decompileTitle, 0, 0, 2, 1);
		content.add(labelDecompiler, 0, 1);
		content.add(fix(new ObservableComboBox<>(decompiler, new ArrayList<>(decompilerManager.getJvmDecompilers()))), 1, 1);
		content.add(labelTimeout, 0, 2);
		content.add(fix(spinTimeout), 1, 2);

		return content;
	}

	protected static Control fix(Control control) {
		control.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(control, true);
		return control;
	}
}
