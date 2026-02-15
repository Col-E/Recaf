package software.coley.recaf.ui.pane.editing;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.observables.ObservableObject;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.ObservableComboBox;
import software.coley.recaf.ui.control.ObservableSpinner;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.pane.editing.jvm.DecompilerPaneConfig;
import software.coley.recaf.util.Lang;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Overlay component for {@link Editor} that allows quick configuration of properties of a {@link AbstractDecompilePane}.
 *
 * @author Matt Coley
 */
public abstract class AbstractDecompilerPaneConfigurator extends Button {
	private final DecompilerPaneConfig config;
	private final ObservableObject<JvmDecompiler> decompiler;
	private final DecompilerManager decompilerManager;
	private Popover popover;

	/**
	 * @param toolsContainer
	 * 		Container to house tool buttons for display in the {@link Editor}.
	 * @param config
	 * 		Containing {@link AbstractDecompilePane} config singleton.
	 * @param decompiler
	 * 		Local decompiler implementation.
	 * @param decompilerManager
	 * 		Manager to pull available {@link JvmDecompiler} instances from.
	 */
	public AbstractDecompilerPaneConfigurator(@Nonnull ToolsContainerComponent toolsContainer,
											  @Nonnull DecompilerPaneConfig config,
											  @Nonnull ObservableObject<JvmDecompiler> decompiler,
											  @Nonnull DecompilerManager decompilerManager) {
		this.config = config;
		this.decompiler = decompiler;
		this.decompilerManager = decompilerManager;
		setGraphic(new FontIconView(CarbonIcons.SETTINGS));
		getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT);
		setOnAction(this::showConfiguratorPopover);
		toolsContainer.add(this);
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
		ObjectProperty<Node> decompileMatchedState = new SimpleObjectProperty<>();
		decompileMatchedState.set(new FontIconView(CarbonIcons.LOCKED));
		decompiler.addChangeListener((ob, old, cur) -> {
			boolean matched = Objects.equals(decompilerManager.getServiceConfig().getPreferredJvmDecompiler().getValue(), cur.getName());
			decompileMatchedState.set(matched ? new FontIconView(CarbonIcons.LOCKED) : new FontIconView(CarbonIcons.UNLOCKED, Color.RED));
		});
		decompilerManager.getServiceConfig().getPreferredJvmDecompiler().addChangeListener((ob, old, cur) -> {
			boolean matched = Objects.equals(decompiler.getValue().getName(), cur);
			decompileMatchedState.set(matched ? new FontIconView(CarbonIcons.LOCKED) : new FontIconView(CarbonIcons.UNLOCKED, Color.RED));
		});
		Label decompileTitle = new BoundLabel(Lang.getBinding("service.decompile"));
		decompileTitle.getStyleClass().addAll(Styles.TEXT_UNDERLINED, Styles.TITLE_4);
		Label labelDecompiler = new BoundLabel(Lang.getBinding("java.decompiler"));
		Button lockDecompiler = new ActionButton(decompileMatchedState, () -> {
			decompilerManager.getServiceConfig().getPreferredJvmDecompiler().setValue(decompiler.getValue().getName());
		});
		lockDecompiler.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.SMALL);
		HBox boxDecompilerLabel = new HBox(labelDecompiler, new Spacer(), lockDecompiler);
		boxDecompilerLabel.setAlignment(Pos.CENTER_LEFT);
		Label labelTimeout = new BoundLabel(Lang.getBinding("service.ui.decompile-pane-config.timeout-seconds"));
		Spinner<Integer> spinTimeout = ObservableSpinner.intSpinner(config.getTimeoutSeconds(), 1, Integer.MAX_VALUE);
		spinTimeout.getStyleClass().add(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL);
		content.add(decompileTitle, 0, 0, 2, 1);
		content.add(boxDecompilerLabel, 0, 1);
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
