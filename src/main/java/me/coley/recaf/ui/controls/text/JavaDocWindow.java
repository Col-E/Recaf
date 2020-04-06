package me.coley.recaf.ui.controls.text;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import me.coley.recaf.parse.javadoc.*;
import me.coley.recaf.ui.controls.popup.DragPopup;

import java.util.stream.Collectors;

import static me.coley.recaf.util.StringUtil.indent;

/**
 * Draggable JavaDoc window.
 *
 * @author Matt
 */
public class JavaDocWindow extends DragPopup {
	private JavaDocWindow(ScrollPane content, Control handle) {
		super(content, handle);
	}

	/**
	 * @param docs
	 * 		Docs to build window from.
	 *
	 * @return Class JavaDocs window.
	 */
	public static JavaDocWindow ofClass(Javadocs docs) {
		// Build doc window
		VBox box = new VBox();
		ScrollPane root = new ScrollPane(box);
		// Class
		Label lblName = new Label(docs.getInternalName());
		Label lblNameDesc = new Label(docs.getDescription());
		lblNameDesc.setWrapText(true);
		lblName.getStyleClass().add("h1");
		lblNameDesc.getStyleClass().add("faint");
		box.getChildren().addAll(lblNameDesc);
		// Inheritance
		int indent = 1;
		box.getChildren().add(new Label("Inheritance"));
		for (String name : docs.getInheritance()) {
			Label lblInherited = new Label(indent(indent, "  ") + name);
			lblInherited.getStyleClass().add("faint");
			box.getChildren().add(lblInherited);
			indent++;
		}
		// Subclasses
		// - one, two, three
		if (!docs.getSubclasses().isEmpty()) {
			box.getChildren().add(new Label("Subclasses"));
			VBox subs = new VBox();
			for(String name : docs.getSubclasses()) {
				Label lblSub = new Label(indent(1, "  ") + name);
				lblSub.getStyleClass().add("faint");
				subs.getChildren().add(lblSub);
			}
			box.getChildren().add(subs);
		}
		// Fields
		// - Type, name:
		// - Desc
		if (!docs.getFields().isEmpty()) {
			box.getChildren().add(new Label("Fields"));
			VBox subs = new VBox();
			for(DocField field : docs.getFields()) {
				Label lblDef = new Label(indent(2, "  ") + field.getType() + " " + field.getName());
				Label lblDesc = new Label(indent(3, "  ") + field.getDescription());
				lblDesc.getStyleClass().add("faint");
				lblDesc.setWrapText(true);
				subs.getChildren().addAll(lblDef);
				if (!field.getDescription().trim().isEmpty())
					subs.getChildren().addAll(lblDesc);
			}
			box.getChildren().add(subs);
		}
		// Methods
		// - Return Type, name(args):
		// - Desc
		if (!docs.getMethods().isEmpty()) {
			box.getChildren().add(new Label("Methods"));
			VBox subs = new VBox();
			for(DocMethod method : docs.getMethods()) {
				String def = method.getReturnType() + " " + method.getName() + "(";
				def += method.getParameters().stream()
						.map(DocParameter::getName)
						.collect(Collectors.joining(", "));
				def += ")";
				Label lblDef = new Label(indent(2, "  ") + def);
				Label lblDesc = new Label(indent(3, "  ") + method.getDescription());
				lblDesc.getStyleClass().add("faint");
				lblDesc.setWrapText(true);
				subs.getChildren().add(lblDef);
				if (!method.getDescription().trim().isEmpty())
					subs.getChildren().add(lblDesc);
			}
			box.getChildren().add(subs);
		}
		return new JavaDocWindow(root, lblName);
	}

	/**
	 * @param field
	 * 		Docs to build window from.
	 *
	 * @return Field JavaDocs window.
	 */
	public static JavaDocWindow ofField(DocField field) {
		// Build doc window
		VBox box = new VBox();
		ScrollPane root = new ScrollPane(box);
		// Field
		Label lblDefinition = new Label(field.getType() + " " + field.getName());
		Label lblDesc = new Label(field.getDescription());
		lblDefinition.getStyleClass().add("h1");
		lblDesc.getStyleClass().add("faint");
		lblDesc.setWrapText(true);
		box.getChildren().addAll(lblDesc);
		return new JavaDocWindow(root, lblDefinition);

	}

	/**
	 * @param method
	 * 		Docs to build window from.
	 *
	 * @return Method JavaDocs window.
	 */
	public static JavaDocWindow ofMethod(DocMethod method) {
		// Build doc window
		VBox box = new VBox();
		ScrollPane root = new ScrollPane(box);
		// Method
		String def = method.getReturnType() + " " + method.getName() + "(";
		def += method.getParameters().stream()
				.map(DocParameter::getName)
				.collect(Collectors.joining(", "));
		def += ")";
		Label lblDefinition = new Label(def);
		Label lblDesc = new Label(method.getDescription());
		lblDefinition.getStyleClass().add("h1");
		lblDesc.getStyleClass().add("faint");
		lblDesc.setWrapText(true);
		box.getChildren().addAll(lblDesc);
		return new JavaDocWindow(root, lblDefinition);
	}
}
