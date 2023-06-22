package software.coley.recaf.ui.wizard;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.pane.PathPromptPane;
import software.coley.recaf.util.Lang;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Wizard page to select multiple paths via {@link PathPromptPane}.
 *
 * @author Matt Coley
 * @see SinglePathWizardPage
 */
public class MultiPathWizardPage extends Wizard.WizardPage {
	private final ObservableList<PathPromptPane> panes = FXCollections.observableArrayList();
	private final RecentFilesConfig recentFilesConfig;

	/**
	 * @param title
	 * 		Title binding.
	 * @param recentFilesConfig
	 * 		Config to pull recent locations from, to populate initial directories of file/directory choosers.
	 */
	public MultiPathWizardPage(@Nonnull StringBinding title, @Nonnull RecentFilesConfig recentFilesConfig) {
		super(title);
		this.recentFilesConfig = recentFilesConfig;
	}

	@Override
	protected Node createDisplay() {
		setCanProgress(true);

		// Display multiple rows of path prompt panes
		VBox pathItems = new VBox();
		pathItems.setFillWidth(true);
		pathItems.setSpacing(15);
		ListChangeListener<PathPromptPane> pathPanesListener = c -> {
			pathItems.getChildren().clear();

			// Add note that there is nothing
			if (panes.isEmpty()) {
				HBox empty = new HBox(new BoundLabel(Lang.getBinding("dialog.file.nothing")));
				empty.setAlignment(Pos.CENTER);
				pathItems.getChildren().add(empty);
			}

			// Populate panes
			for (PathPromptPane pane : panes) {
				ActionButton remove = new ActionButton("", () -> panes.remove(pane));
				remove.setGraphic(new FontIconView(CarbonIcons.TRASH_CAN));
				HBox wrapper = new HBox(remove, pane);
				wrapper.setSpacing(15);
				wrapper.setPadding(new Insets(0, 15, 0, 15));
				HBox.setHgrow(pane, Priority.ALWAYS);
				pathItems.getChildren().add(wrapper);
			}
		};
		pathPanesListener.onChanged(null); // Dummy value to initiate initial state
		panes.addListener(pathPanesListener);
		ScrollPane itemsScrollPane = new ScrollPane(pathItems);
		itemsScrollPane.setMinHeight(47);
		itemsScrollPane.setFitToWidth(true);

		// Controls to add rows
		ActionButton add = new ActionButton("", () -> panes.add(new PathPromptPane(recentFilesConfig)));
		add.setGraphic(new FontIconView(CarbonIcons.ADD_ALT));
		HBox controls = new HBox();
		controls.setSpacing(15);
		controls.getChildren().add(add);

		// Layout
		VBox layout = new VBox(itemsScrollPane, new Separator(), controls);
		layout.setSpacing(15);
		layout.setAlignment(Pos.TOP_LEFT);
		layout.setFillWidth(true);
		return layout;
	}

	/**
	 * @return List of paths.
	 */
	@Nonnull
	public List<Path> getPaths() {
		return panes.stream().map(p -> p.pathProperty().get())
				.filter(Objects::nonNull)
				.toList();
	}
}
