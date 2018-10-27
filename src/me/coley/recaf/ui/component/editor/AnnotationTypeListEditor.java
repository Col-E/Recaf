package me.coley.recaf.ui.component.editor;

import java.util.List;
import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.TypeAnnotationNode;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.ui.component.constructor.TypeAnnotationNodeConstructor;

/**
 * Editor for annotation-type lists.
 * 
 * @author Matt
 */
// TODO: Support adding to the "values" list in TypeAnnotationNode.
public class AnnotationTypeListEditor extends
		AbstractListEditor<TypeAnnotationNode, TypeAnnotationNodeConstructor, List<TypeAnnotationNode>> {

	public AnnotationTypeListEditor(Item item) {
		super(item, "ui.bean.class.annotations.title", 400, 500);
	}

	@Override
	protected TypeAnnotationNodeConstructor create(ListView<TypeAnnotationNode> view) {
		return new TypeAnnotationNodeConstructor();
	}

	@Override
	protected TypeAnnotationNode getValue(TypeAnnotationNodeConstructor control) {
		return control.get();
	}

	@Override
	protected void reset(TypeAnnotationNodeConstructor control) {
		control.reset();
	}

	@Override
	protected void setupView(ListView<TypeAnnotationNode> view) {
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
	}
}
