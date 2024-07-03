package software.coley.recaf.ui.control.popup;

import jakarta.annotation.Nonnull;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Generic list item selection handling popup.
 *
 * @author Matt Coley
 */
@SuppressWarnings("unchecked")
public class ItemListSelectionPopup<T> extends SelectionPopup<T> {
	private final ListView<T> list = new ListView<>();

	/**
	 * @param items
	 * 		Items to show.
	 * @param consumer
	 * 		Consumer to run when user accepts selected items.
	 */
	public ItemListSelectionPopup(@Nonnull Collection<T> items, @Nonnull Consumer<List<T>> consumer) {
		setup(consumer);

		// Handle user accepting input
		list.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				accept(consumer);
			} else if (e.getCode() == KeyCode.ESCAPE) {
				hide();
			}
		});
		list.setOnMousePressed(e -> {
			if (e.getClickCount() >= 2) {
				accept(consumer);
			}
		});
		list.setItems(FXCollections.observableArrayList(items));
		list.setCellFactory(param -> new ListCell<>() {
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

		// Initial selection if there is only one item.
		// Allows the user to jump straight to accept/cancel buttons.
		if (items.size() == 1) list.getSelectionModel().selectFirst();
	}

	@Nonnull
	@Override
	protected Node getSelectionComponent() {
		return list;
	}

	@Nonnull
	@Override
	protected List<T> adaptCurrentSelection() {
		return list.getSelectionModel().getSelectedItems();
	}

	@Nonnull
	@Override
	protected ObservableValue<Boolean> isNullSelection() {
		return list.getSelectionModel().selectedItemProperty().isNull();
	}

	@Nonnull
	@Override
	public ItemListSelectionPopup<T> withMultipleSelection() {
		list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		return this;
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
	public static ItemListSelectionPopup<FieldMember> forFields(@Nonnull ClassInfo cls,
																@Nonnull Consumer<List<FieldMember>> fieldConsumer) {
		return new ItemListSelectionPopup<>(cls.getFields(), fieldConsumer);
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
	public static ItemListSelectionPopup<MethodMember> forMethods(@Nonnull ClassInfo cls,
																  @Nonnull Consumer<List<MethodMember>> methodConsumer) {
		return new ItemListSelectionPopup<>(cls.getMethods(), methodConsumer);
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
	public static ItemListSelectionPopup<AnnotationInfo> forAnnotationRemoval(@Nonnull Annotated annotated,
																			  @Nonnull Consumer<List<AnnotationInfo>> annotationConsumer) {
		return new ItemListSelectionPopup<>(annotated.getAnnotations(), annotationConsumer);
	}
}
