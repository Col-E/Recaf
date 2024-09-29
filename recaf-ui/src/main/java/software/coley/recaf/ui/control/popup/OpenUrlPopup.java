package software.coley.recaf.ui.control.popup;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.io.ResourceImporter;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import static software.coley.recaf.util.Lang.get;


/**
 * Popup for opening a workspace from a URL.
 *
 * @author Matt Coley
 */
@Dependent
public class OpenUrlPopup extends RecafStage {
	private static final Logger logger = Logging.get(OpenUrlPopup.class);
	private final TextField input = new TextField();

	@Inject
	public OpenUrlPopup(@Nonnull WorkspaceManager workspaceManager, @Nonnull ResourceImporter resourceImporter) {
		BooleanProperty isInvalidUrl = new SimpleBooleanProperty(true);
		isInvalidUrl.bind(input.textProperty().map(text -> {
			try {
				// noinspection ResultOfMethodCallIgnored
				new URI(text).toURL();
				return false;
			} catch (Throwable t) {
				return true;
			}
		}));
		ObservableValue<String> downloadText = isInvalidUrl.flatMap(invalid ->
				invalid ? Lang.getBinding("misc.download.invalid-url") : Lang.getBinding("misc.load"));
		Button load = new ActionButton(CarbonIcons.CLOUD_DOWNLOAD, downloadText, () -> {
			input.setDisable(true);
			CompletableFuture.supplyAsync(() -> {
				String url = input.getText();
				try {
					WorkspaceResource resource = resourceImporter.importResource(URI.create(url));
					workspaceManager.setCurrent(new BasicWorkspace(resource));
					return true;
				} catch (Exception ex) {
					logger.error("Could not read from URL '{}'", url, ex);
					return false;
				}
			}).thenAcceptAsync((result) -> {
				input.setDisable(false);
				if (result) {
					close();
				} else {
					Animations.animateFailure(input, 1000);
				}
			}, FxThreadUtil.executor());
		});
		load.disableProperty().bind(input.disabledProperty().or(isInvalidUrl));
		input.setPromptText("https://example.com/application.jar");
		input.setOnAction(e -> load.getOnAction().handle(e));


		GridPane layout = new GridPane(8, 8);
		ColumnConstraints e = new ColumnConstraints(0, -1, Double.MAX_VALUE);
		e.setHgrow(Priority.ALWAYS);
		layout.getColumnConstraints().add(e);
		layout.add(input, 0, 0);
		layout.add(load, 0, 1);
		layout.setPadding(new Insets(10));
		layout.setAlignment(Pos.TOP_CENTER);

		input.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		load.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		layout.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		setMinWidth(300);
		setMinHeight(90);
		setTitle(get("menu.file.openurl"));
		setScene(new RecafScene(layout, 300, 90));
	}

	/**
	 * Focuses the text input.
	 */
	public void requestInputFocus() {
		toFront();
		requestFocus();
		input.requestFocus();
	}
}
