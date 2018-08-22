package me.coley.recaf.ui.component;

import java.util.List;

import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.Editors;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.TypeAnnotationNode;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;

// TODO: Support adding to the "values" list in TypeAnnotationNode.
public class AnnotationTypeListEditor<T extends List<TypeAnnotationNode>> extends StagedCustomEditor<T> {
	private final ObservableList<TypeAnnotationNode> annotations;

	@SuppressWarnings("unchecked")
	public AnnotationTypeListEditor(Item item) {
		super(item);
		T list = (T) item.getValue();
		if (list == null) {
			annotations = FXCollections.observableArrayList();
		} else {
			annotations = FXCollections.observableArrayList(list);
		}
	}

	@Override
	public Node getEditor() {
		return new ActionButton(Lang.get("misc.edit"), () -> open(this));
	}

	private void open(AnnotationTypeListEditor<T> annotationListEditor) {
		if (staged()) {
			return;
		}
		BorderPane content = new BorderPane();
		ListView<TypeAnnotationNode> view = new ListView<>(annotations);
		annotations.addListener(new ListChangeListener<TypeAnnotationNode>() {
			@SuppressWarnings("unchecked")
			@Override
			public void onChanged(Change<? extends TypeAnnotationNode> c) {
				setValue((T) annotations);
			}
		});
		view.setCellFactory(param -> new ListCell<TypeAnnotationNode>() {
			@Override
			public void updateItem(TypeAnnotationNode item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					// Reset 'hidden' items
					setGraphic(null);
					setText(null);
				} else {
					setGraphic(FormatFactory.annotation(item));
				}
			}
		});
		view.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.DELETE)) {
					delete(view);
				}
			}
		});
		view.setItems(annotations);
		view.setEditable(true);
		// textfield / button to add new value
		BorderPane menuPane = new BorderPane();
		TextField annoDesc = new TextField();
		TextField annoType = new TextField();
		ComboBox<RefType> comboRef = new ComboBox<>(JavaFX.observableList(RefType.values()));
		comboRef.setTooltip(new Tooltip(Lang.get("ui.bean.class.annotations.ref.tooltip")));
		annoDesc.setTooltip(new Tooltip(Lang.get("ui.bean.class.annotations.desc.tooltip")));
		annoType.setTooltip(new Tooltip(Lang.get("ui.bean.class.annotations.type.tooltip")));

		annoDesc.setOnAction((e) -> add(annoDesc, annoType, comboRef, view));
		annoType.setOnAction((e) -> add(annoDesc, annoType, comboRef, view));

		Button addAnno = new ActionButton(Lang.get("misc.add"), () -> add(annoDesc, annoType, comboRef, view));
		menuPane.setCenter(new SplitPane(annoDesc, annoType, comboRef));
		menuPane.setRight(addAnno);
		content.setCenter(view);
		content.setBottom(menuPane);
		setStage("ui.bean.class.annotations.title", content, 400, 500);
	}

	/**
	 * Add member in TextField to ListView.
	 * 
	 * @param textDesc
	 * @param view
	 */
	private void add(TextField textDesc, TextField textType, ComboBox<RefType> comboRef, ListView<TypeAnnotationNode> view) {
		TypeAnnotationNode node = construct(textDesc, textType, comboRef);
		if (node != null) {
			view.itemsProperty().get().add(node);
			textDesc.textProperty().setValue("");
		}
	}

	/**
	 * Remove selected items from ListView.
	 * 
	 * @param view
	 */
	private void delete(ListView<TypeAnnotationNode> view) {
		MultipleSelectionModel<TypeAnnotationNode> selection = view.selectionModelProperty().getValue();
		for (int index : selection.getSelectedIndices()) {
			view.getItems().remove(index);
		}
	}

	private TypeAnnotationNode construct(TextField textDesc, TextField textType, ComboBox<RefType> comboRef) {
		if (comboRef.getValue() == null) {
			Logging.error(Lang.get("misc.invalidtype.typeref"), true);
			return null;
		}
		String desc = textDesc.textProperty().get();
		String type = textType.textProperty().get();
		if (desc == null || desc.isEmpty() || !TypeUtil.isStandard(desc)) {
			Logging.error(Lang.get("misc.invalidtype.standard"), true);
			return null;
		}
		try {
			if (type == null || type.isEmpty()) {
				Logging.error(Lang.get("misc.invalidtype.typepath"), true);
				return null;
			}
			TypePath typePath = TypePath.fromString(type);
			return new TypeAnnotationNode(comboRef.getValue().value, typePath, desc);
		} catch(IllegalArgumentException e) {
			Logging.error(Lang.get("misc.invalidtype.typepath"), true);
		} catch (Exception e) {
			Logging.error(e, true);
		}
		return null;
	}

	public enum RefType {
		//@formatter:off
		CLASS_TYPE_PARAMETER(TypeReference.CLASS_TYPE_PARAMETER),
		METHOD_TYPE_PARAMETER(TypeReference.METHOD_TYPE_PARAMETER),
		CLASS_EXTENDS(TypeReference.CLASS_EXTENDS),
		CLASS_TYPE_PARAMETER_BOUND(TypeReference.CLASS_TYPE_PARAMETER_BOUND),
		METHOD_TYPE_PARAMETER_BOUND(TypeReference.METHOD_TYPE_PARAMETER_BOUND),
		FIELD(TypeReference.FIELD),
		METHOD_RETURN(TypeReference.METHOD_RETURN),
		METHOD_RECEIVER(TypeReference.METHOD_RECEIVER),
		METHOD_FORMAL_PARAMETER(TypeReference.METHOD_FORMAL_PARAMETER),
		THROWS(TypeReference.THROWS),
		LOCAL_VARIABLE(TypeReference.LOCAL_VARIABLE),
		RESOURCE_VARIABLE(TypeReference.RESOURCE_VARIABLE),
		EXCEPTION_PARAMETER(TypeReference.EXCEPTION_PARAMETER),
		INSTANCEOF(TypeReference.INSTANCEOF),
		NEW(TypeReference.NEW),
		CONSTRUCTOR_REFERENCE(TypeReference.CONSTRUCTOR_REFERENCE),
		METHOD_REFERENCE(TypeReference.METHOD_REFERENCE),
		CAST(TypeReference.CAST),
		CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT(TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT),
		METHOD_INVOCATION_TYPE_ARGUMENT(TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT),
		CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT(TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT),
		METHOD_REFERENCE_TYPE_ARGUMENT(TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT);
		//@formatter:on

		private final int value;

		RefType(int value) {
			this.value = value;
		}
	}
}
