package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.pane.SystemInformationPane;
import software.coley.recaf.util.Lang;

/**
 * Window wrapper for {@link SystemInformationPane}.
 *
 * @author Matt Coley
 * @see SystemInformationPane
 */
@Dependent
public class SystemInformationWindow extends AbstractIdentifiableStage {
	@Inject
	public SystemInformationWindow(@Nonnull SystemInformationPane infoPane) {
		super(WindowManager.WIN_INFO);

		// Layout
		titleProperty().bind(Lang.getBinding("menu.help.sysinfo"));
		setMinWidth(450);
		setMinHeight(300);
		ScrollPane scroll = new ScrollPane(infoPane);
		scroll.setFitToWidth(true);
		setScene(new RecafScene(new BorderPane(scroll), 500, 710));
	}
}
