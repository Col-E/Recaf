package software.coley.recaf.ui.control.popup;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static atlantafx.base.theme.Styles.*;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CHECKMARK;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CLOSE;

/**
 * Common item selection popup handling.
 *
 * @author Matt Coley
 */
@SuppressWarnings("unchecked")
public abstract class SelectionPopup<T> extends RecafStage {
	protected Function<T, String> textMapper;
	protected Function<T, Node> graphicMapper;

	/**
	 * @param consumer
	 * 		Action to run on selected items.
	 */
	protected void setup(@Nonnull Consumer<List<T>> consumer) {
		Button accept = new ActionButton(new FontIconView(CHECKMARK, Color.LAWNGREEN), () -> accept(consumer));
		accept.disableProperty().bind(isNullSelection());
		Button cancel = new ActionButton(new FontIconView(CLOSE, Color.RED), this::hide);
		accept.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, SUCCESS);
		cancel.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, DANGER);

		// Layout
		HBox buttons = new HBox(accept, cancel);
		buttons.setSpacing(10);
		buttons.setPadding(new Insets(10, 0, 10, 0));
		buttons.setAlignment(Pos.CENTER_RIGHT);
		VBox layout = new VBox(getSelectionComponent(), buttons);
		layout.setAlignment(Pos.TOP_CENTER);
		layout.setPadding(new Insets(10));
		setScene(new RecafScene(layout, 400, 300));
	}

	/**
	 * @param consumer
	 * 		Action to run on accept.
	 */
	protected void accept(@Nonnull Consumer<List<T>> consumer) {
		accept(adaptCurrentSelection(), consumer);
	}

	/**
	 * @param selectedItems
	 * 		Selected items to accept selection of.
	 * @param consumer
	 * 		Action to run on accept.
	 */
	private void accept(@Nonnull List<T> selectedItems, @Nonnull Consumer<List<T>> consumer) {
		consumer.accept(selectedItems);
		hide();
	}

	@Nonnull
	protected abstract Node getSelectionComponent();

	@Nonnull
	protected abstract List<T> adaptCurrentSelection();

	@Nonnull
	protected abstract ObservableValue<Boolean> isNullSelection();


	/**
	 * Enables multiple selection.
	 *
	 * @return Self.
	 */
	@Nonnull
	public abstract <P extends SelectionPopup<T>> P withMultipleSelection();

	/**
	 * @param binding
	 * 		Title binding.
	 *
	 * @return Self.
	 */
	@Nonnull
	public <P extends SelectionPopup<T>> P withTitle(@Nonnull StringBinding binding) {
		titleProperty().bind(binding);
		return (P) this;
	}

	/**
	 * @param textMapper
	 * 		List view item text mapper.
	 *
	 * @return Self.
	 */
	@Nonnull
	public <P extends SelectionPopup<T>> P withTextMapping(@Nonnull Function<T, String> textMapper) {
		this.textMapper = textMapper;
		return (P) this;
	}

	/**
	 * @param graphicMapper
	 * 		List view item graphic mapper.
	 *
	 * @return Self.
	 */
	@Nonnull
	public <P extends SelectionPopup<T>> P withGraphicMapping(@Nonnull Function<T, Node> graphicMapper) {
		this.graphicMapper = graphicMapper;
		return (P) this;
	}
}
