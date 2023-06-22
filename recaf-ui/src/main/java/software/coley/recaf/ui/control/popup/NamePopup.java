package software.coley.recaf.ui.control.popup;

import jakarta.annotation.Nonnull;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.bundle.ClassBundle;

import java.awt.*;
import java.util.function.Consumer;

import static atlantafx.base.theme.Styles.*;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CHECKMARK;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CLOSE;

/**
 * Generic name input popup.
 *
 * @author Matt Coley
 */
public class NamePopup extends RecafStage {
	private final BooleanProperty classNameConflict = new SimpleBooleanProperty(false);
	private final Label output = new Label();
	private final TextField nameInput = new TextField();
	private final Button accept;

	/**
	 * @param nameConsumer
	 * 		Consumer to handle accepted inputs.
	 */
	public NamePopup(@Nonnull Consumer<String> nameConsumer) {
		// Handle user accepting input
		nameInput.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				accept(nameConsumer);
			} else if (e.getCode() == KeyCode.ESCAPE) {
				hide();
			}
		});
		accept = new ActionButton(new FontIconView(CHECKMARK, Color.LAWNGREEN), () -> accept(nameConsumer));
		Button cancel = new ActionButton(new FontIconView(CLOSE, Color.RED), this::hide);
		accept.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, SUCCESS);
		cancel.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, DANGER);

		// Layout
		HBox buttons = new HBox(accept, cancel);
		buttons.setSpacing(10);
		buttons.setPadding(new Insets(10, 0, 10, 0));
		buttons.setAlignment(Pos.CENTER_RIGHT);
		VBox layout = new VBox(nameInput, buttons, new Group(output));
		layout.setAlignment(Pos.TOP_CENTER);
		layout.setPadding(new Insets(10));
		setScene(new RecafScene(layout, 400, 150));
	}

	/**
	 * @param nameConsumer
	 * 		Action to run on accept.
	 */
	private void accept(@Nonnull Consumer<String> nameConsumer) {
		// Do nothing if conflict detected
		if (classNameConflict.get()) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		nameConsumer.accept(nameInput.getText());
		hide();
	}

	/**
	 * @param bundle
	 * 		Target bundle the class will reside in.
	 * 		Used to check for name overlap.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forClassCopy(@Nonnull ClassBundle<?> bundle) {
		titleProperty().bind(Lang.getBinding("dialog.title.copy-class"));
		output.textProperty().bind(Lang.getBinding("dialog.header.rename-class-error"));

		// Bind conflict property
		classNameConflict.bind(nameInput.textProperty().map(bundle::containsKey));
		accept.disableProperty().bind(classNameConflict);
		output.visibleProperty().bind(classNameConflict);
		return this;
	}

	/**
	 * @param bundle
	 * 		Target bundle the class will reside in.
	 * 		Used to check for name overlap.
	 *
	 * @return Self.
	 */
	@Nonnull
	public NamePopup forClassRename(@Nonnull ClassBundle<?> bundle) {
		titleProperty().bind(Lang.getBinding("dialog.title.rename-class"));
		output.textProperty().bind(Lang.getBinding("dialog.header.rename-class-error"));

		// Bind conflict property
		classNameConflict.bind(nameInput.textProperty().map(bundle::containsKey));
		accept.disableProperty().bind(classNameConflict);
		output.visibleProperty().bind(classNameConflict);
		return this;
	}

	@Nonnull
	public NamePopup withInitialClassName(@Nonnull String name) {
		// TODO: Need to handle escaping names with newlines and such
		//  - and handle un-escaping when calling the consumer<string>
		nameInput.setText(name);

		// Need to get focus before selecting range.
		// When the input gets focus, it resets the selection.
		// But if we focus it, then set the selection we're good.
		nameInput.requestFocus();

		// Select the last portion of the name.
		nameInput.selectRange(name.lastIndexOf('/') + 1, name.length());
		return this;
	}
}
