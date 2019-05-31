package me.coley.recaf.ui.component.editor;

import java.util.List;

import org.controlsfx.control.PropertySheet.Item;
import org.objectweb.asm.tree.AnnotationNode;

import javafx.scene.control.*;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.ui.FormatFactory;
import me.coley.recaf.util.Lang;

/**
 * Editor for annotation lists.
 * 
 * @author Matt
 */
// TODO: Support adding to the "values" list in AnnotationNode.
// TODO: Support for Kotlin Metadata annotation
public class AnnotationListEditor extends AbstractListEditor<AnnotationNode, TextField, List<AnnotationNode>> {

	public AnnotationListEditor(Item item) {
		super(item, "ui.bean.class.annotations.title", 400, 500);
	}

	@Override
	protected TextField create(ListView<AnnotationNode> view) {
		TextField annoDesc = new TextField();
		annoDesc.setTooltip(new Tooltip(Lang.get("ui.bean.class.annotations.desc.tooltip")));
		annoDesc.setOnAction((e) -> add(annoDesc, view));
		return annoDesc;
	}

	@Override
	protected AnnotationNode getValue(TextField control) {
		String desc = control.textProperty().get();
		if (desc == null || desc.isEmpty() || !TypeUtil.isStandard(desc)) {
			Logging.error(Lang.get("misc.invalidtype.standard"), true);
			return null;
		}
		return new AnnotationNode(desc);
	}

	@Override
	protected void reset(TextField control) {
		control.textProperty().setValue("");
	}

	@Override
	protected void setupView(ListView<AnnotationNode> view) {
		view.setCellFactory(param -> new ListCell<AnnotationNode>() {
			@Override
			public void updateItem(AnnotationNode item, boolean empty) {
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
