package software.coley.recaf.ui.control.popup;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.workspace.model.bundle.ClassBundle;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static atlantafx.base.theme.Styles.*;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CHECKMARK;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CLOSE;

/**
 * Generic item selection handling popup.
 *
 * @author Matt Coley
 */
public class ItemSelectionPopup<T> extends RecafStage {
	private final ListView<T> members = new ListView<>();
	private Function<T, String> textMapper;
	private Function<T, Node> graphicMapper;

	/**
	 * @param items
	 * 		Items to show.
	 * @param consumer
	 * 		Consumer to run when user accepts selected items.
	 */
	public ItemSelectionPopup(@Nonnull Collection<T> items, @Nonnull Consumer<List<T>> consumer) {
		// Handle user accepting input
		members.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				accept(consumer);
			} else if (e.getCode() == KeyCode.ESCAPE) {
				hide();
			}
		});
		members.setItems(FXCollections.observableArrayList(items));
		members.setCellFactory(param -> new ListCell<>() {
			@Override
			protected void updateItem(T item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
				} else {
					if (textMapper != null) setText(textMapper.apply(item));
					if (graphicMapper != null) setGraphic(graphicMapper.apply(item));
				}
			}
		});
		Button accept = new ActionButton(new FontIconView(CHECKMARK, Color.LAWNGREEN), () -> accept(consumer));
		accept.disableProperty().bind(members.getSelectionModel().selectedItemProperty().isNull());
		Button cancel = new ActionButton(new FontIconView(CLOSE, Color.RED), this::hide);
		accept.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, SUCCESS);
		cancel.getStyleClass().addAll(BUTTON_ICON, BUTTON_OUTLINED, DANGER);

		// Layout
		HBox buttons = new HBox(accept, cancel);
		buttons.setSpacing(10);
		buttons.setPadding(new Insets(10, 0, 10, 0));
		buttons.setAlignment(Pos.CENTER_RIGHT);
		VBox layout = new VBox(members, buttons);
		layout.setAlignment(Pos.TOP_CENTER);
		layout.setPadding(new Insets(10));
		setScene(new RecafScene(layout, 400, 300));
	}

	/**
	 * @param consumer
	 * 		Action to run on accept.
	 */
	private void accept(@Nonnull Consumer<List<T>> consumer) {
		List<T> selectedItems = members.getSelectionModel().getSelectedItems();
		consumer.accept(selectedItems);
		hide();
	}

	/**
	 * Enables multiple selection.
	 *
	 * @return Self.
	 */
	@Nonnull
	public ItemSelectionPopup<T> withMultipleSelection() {
		members.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		return this;
	}

	/**
	 * @param binding
	 * 		Title binding.
	 *
	 * @return Self.
	 */
	@Nonnull
	public ItemSelectionPopup<T> withTitle(@Nonnull StringBinding binding) {
		titleProperty().bind(binding);
		return this;
	}

	/**
	 * @param textMapper
	 * 		List view item text mapper.
	 *
	 * @return Self.
	 */
	@Nonnull
	public ItemSelectionPopup<T> withTextMapping(@Nonnull Function<T, String> textMapper) {
		this.textMapper = textMapper;
		return this;
	}

	/**
	 * @param graphicMapper
	 * 		List view item graphic mapper.
	 *
	 * @return Self.
	 */
	@Nonnull
	public ItemSelectionPopup<T> withGraphicMapping(@Nonnull Function<T, Node> graphicMapper) {
		this.graphicMapper = graphicMapper;
		return this;
	}

	/**
	 * @param bundle
	 * 		Target bundle to pull packages from.
	 * @param packageConsumer
	 * 		Action to run on accepted packages.
	 *
	 * @return
	 */
	@Nonnull
	public static ItemSelectionPopup<String> forPackageNames(@Nonnull ClassBundle<?> bundle,
															 @Nonnull Consumer<List<String>> packageConsumer) {
		Set<String> packages = new HashSet<>();
		packages.add(""); // Empty package
		for (String className : bundle.keySet()) {
			int slash = className.lastIndexOf('/');
			if (slash > 0) packages.add(className.substring(0, slash));
		}
		return new ItemSelectionPopup<>(packages, packageConsumer);
	}

	/**
	 * @param cls
	 * 		Class to pull fields from.
	 * @param fieldConsumer
	 * 		Action to run on accepted fields.
	 *
	 * @return Field selection popup.
	 */
	@Nonnull
	public static ItemSelectionPopup<FieldMember> forFields(@Nonnull ClassInfo cls,
															@Nonnull Consumer<List<FieldMember>> fieldConsumer) {
		return new ItemSelectionPopup<>(cls.getFields(), fieldConsumer);
	}

	/**
	 * @param cls
	 * 		Class to pull methods from.
	 * @param methodConsumer
	 * 		Action to run on accepted methods.
	 *
	 * @return Method selection popup.
	 */
	@Nonnull
	public static ItemSelectionPopup<MethodMember> forMethods(@Nonnull ClassInfo cls,
															  @Nonnull Consumer<List<MethodMember>> methodConsumer) {
		return new ItemSelectionPopup<>(cls.getMethods(), methodConsumer);
	}

	/**
	 * @param annotated
	 * 		Annotated item to pull annotations from.
	 * @param annotationConsumer
	 * 		Action to run on accepted annotations.
	 *
	 * @return Annotation selection popup.
	 */
	@Nonnull
	public static ItemSelectionPopup<AnnotationInfo> forAnnotationRemoval(@Nonnull Annotated annotated,
																		  @Nonnull Consumer<List<AnnotationInfo>> annotationConsumer) {
		return new ItemSelectionPopup<>(annotated.getAnnotations(), annotationConsumer);
	}
}
