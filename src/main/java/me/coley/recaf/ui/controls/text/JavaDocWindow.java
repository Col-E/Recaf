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
		Label lblInheritance = new Label("Inheritance");
		lblInheritance.getStyleClass().addAll("bold", "underlined");
		box.getChildren().add(lblInheritance);
		for (String name : docs.getInheritance()) {
			Label lblInherited = new Label(indent(indent, "  ") + name);
			lblInherited.getStyleClass().add("faint");
			box.getChildren().add(lblInherited);
			indent++;
		}
		// Subclasses
		// - one, two, three
		if (!docs.getSubclasses().isEmpty()) {
			Label lblSubclasses = new Label("Subclasses");
			lblSubclasses.getStyleClass().addAll("bold", "underlined");
			box.getChildren().add(lblSubclasses);
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
			Label lblFields = new Label("Fields");
			lblFields.getStyleClass().addAll("bold", "underlined");
			box.getChildren().add(lblFields);
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
			Label lblMethods = new Label("Methods");
			lblMethods.getStyleClass().addAll("bold", "underlined");
			box.getChildren().add(lblMethods);
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
		lblDefinition.getStyleClass().add("h1");
		if (field.getDescription() != null && !field.getDescription().isEmpty()) {
			Label lblDesc = new Label(field.getDescription());
			lblDesc.getStyleClass().add("faint");
			lblDesc.setWrapText(true);
			box.getChildren().addAll(lblDesc);
		}
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
		lblDefinition.getStyleClass().add("h1");
		// Description
		if (method.getDescription() != null && !method.getDescription().isEmpty()) {
			Label lblDesc = new Label(method.getDescription());
			lblDesc.getStyleClass().add("faint");
			lblDesc.setWrapText(true);
			box.getChildren().addAll(lblDesc);
		}
		// Parameters
		if (!method.getParameters().isEmpty()) {
			VBox subs = new VBox();
			Label lblParameters = new Label("Parameters");
			lblParameters.getStyleClass().addAll("bold", "underlined");
			box.getChildren().add(lblParameters);
			for(DocParameter parameter : method.getParameters()) {
				Label lblParamDef = new Label(indent(2, "  ") + parameter.getName());
				Label lblParamDesc = new Label(indent(3, "  ") + parameter.getDescription());
				lblParamDesc.getStyleClass().add("faint");
				lblParamDesc.setWrapText(true);
				subs.getChildren().add(lblParamDef);
				if (!parameter.getDescription().trim().isEmpty())
					subs.getChildren().add(lblParamDesc);
			}
			box.getChildren().add(subs);
		}
		// Return type
		Label lblReturn = new Label("Return");
		lblReturn.getStyleClass().addAll("bold", "underlined");
		Label lblReturnDesc = new Label(indent(2, "  ") + method.getReturnDescription());
		lblReturnDesc.getStyleClass().add("faint");
		box.getChildren().add(lblReturn);
		box.getChildren().add(lblReturnDesc);
		return new JavaDocWindow(root, lblDefinition);
	}
}
