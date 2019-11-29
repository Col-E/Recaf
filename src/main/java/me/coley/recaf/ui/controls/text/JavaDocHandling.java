package me.coley.recaf.ui.controls.text;

import com.github.javaparser.ast.Node;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.javadoc.*;
import me.coley.recaf.parse.source.SourceCode;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.model.TwoDimensional;

import java.util.Optional;
import java.util.stream.Collectors;

import static me.coley.recaf.util.SourceUtil.*;
import static me.coley.recaf.util.StringUtil.*;


public class JavaDocHandling {
	private final JavaPane pane;
	private Point2D last;

	public JavaDocHandling(JavaPane pane, GuiController controller, SourceCode code) {
		this.pane = pane;
		pane.codeArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, e -> {
			last = e.getScreenPosition();
			// Get node from event position
			int charPos = e.getCharacterIndex();
			TwoDimensional.Position pos =
					pane.codeArea.offsetToPosition(charPos, TwoDimensional.Bias.Backward);
			Node node = code.getNodeAt(pos.getMajor() + 1, pos.getMinor());
			if(!(node instanceof Resolvable))
				return;
			// Resolve node to some declaration type
			Resolvable<?> r = (Resolvable<?>) node;
			Object resolved = r.resolve();
			if(resolved instanceof ResolvedTypeDeclaration) {
				ResolvedTypeDeclaration type = (ResolvedTypeDeclaration) resolved;
				Javadocs docs = controller.getWorkspace().getClassDocs(toInternal(type));
				if(docs == null)
					return;
				showClass(docs);
			} else if(resolved instanceof ResolvedConstructorDeclaration) {
				ResolvedConstructorDeclaration type = (ResolvedConstructorDeclaration) resolved;
				Javadocs docs = controller.getWorkspace().getClassDocs(toInternal(type.declaringType()));
				if(docs == null)
					return;
				showClass(docs);
			} else if(resolved instanceof ResolvedReferenceType) {
				ResolvedReferenceType type = (ResolvedReferenceType) resolved;
				Javadocs docs = controller.getWorkspace().getClassDocs(toInternal(type));
				if(docs == null)
					return;
				showClass(docs);
			} else if(resolved instanceof ResolvedFieldDeclaration) {
				ResolvedFieldDeclaration type = (ResolvedFieldDeclaration) resolved;
				Javadocs docs = controller.getWorkspace().getClassDocs(getFieldOwner(type));
				if (docs == null)
					return;
				Optional<DocField> optField = docs.getFields().stream()
						.filter(f ->
								f.getType().equals(simplify(type.getType().describe())) &&
										f.getName().equals(type.getName()))
						.findFirst();
				optField.ifPresent(docField -> showField(docs, docField));

			} else if(resolved instanceof ResolvedMethodDeclaration) {
				ResolvedMethodDeclaration type = (ResolvedMethodDeclaration) resolved;
				Javadocs docs = controller.getWorkspace().getClassDocs(getMethodOwner(type));
				if (docs == null)
					return;
				Optional<DocMethod> optMethod = docs.getMethods().stream()
						.filter(f ->
								f.getReturnType().equals(simplify(type.getReturnType().describe())) &&
										f.getName().equals(type.getName()))
						.findFirst();
				optMethod.ifPresent(docMethod -> showMethod(docs, docMethod));
			} else {
				System.out.println(resolved.getClass().getSimpleName());
			}
		});
	}

	private String simplify(String quantified) {
		return quantified.contains(".") ?
				quantified.substring(quantified.lastIndexOf(".") + 1) : quantified;
	}

	private void showClass(Javadocs docs) {
		// Build doc window
		VBox flow = new VBox();
		ScrollPane root = new ScrollPane(flow);
		// Class
		Label lblName = new Label(docs.getInternalName());
		Label lblNameDesc = new Label(docs.getDescription());
		lblNameDesc.setWrapText(true);
		lblName.getStyleClass().add("h1");
		lblNameDesc.getStyleClass().add("faint");
		flow.getChildren().addAll(lblName, lblNameDesc);
		// Inheritance
		int indent = 1;
		flow.getChildren().add(new Label("Inheritance"));
		for (String name : docs.getInheritance()) {
			Label lblInherited = new Label(indent(indent, "  ") + name);
			lblInherited.getStyleClass().add("faint");
			flow.getChildren().add(lblInherited);
			indent++;
		}
		// Subclasses
		// - one, two, three
		if (!docs.getSubclasses().isEmpty()) {
			flow.getChildren().add(new Label("Subclasses"));
			VBox subs = new VBox();
			for(String name : docs.getSubclasses()) {
				Label lblSub = new Label(indent(1, "  ") + name);
				lblSub.getStyleClass().add("faint");
				subs.getChildren().add(lblSub);
			}
			flow.getChildren().add(subs);
		}
		// Fields
		// - Type, name:
		// - Desc
		if (!docs.getFields().isEmpty()) {
			flow.getChildren().add(new Label("Fields"));
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
			flow.getChildren().add(subs);
		}
		// Methods
		// - Return Type, name(args):
		// - Desc
		if (!docs.getMethods().isEmpty()) {
			flow.getChildren().add(new Label("Methods"));
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
			flow.getChildren().add(subs);
		}
		// Show popup
		show(root, lblName);
	}

	private void showField(Javadocs docs, DocField field) {
		// Build doc window
		VBox flow = new VBox();
		ScrollPane root = new ScrollPane(flow);
		// Field
		Label lblDefinition = new Label(field.getType() + " " + field.getName());
		Label lblDesc = new Label(field.getDescription());
		lblDefinition.getStyleClass().add("h1");
		lblDesc.getStyleClass().add("faint");
		lblDesc.setWrapText(true);
		flow.getChildren().addAll(lblDefinition, lblDesc);
		// Show popup
		show(root, lblDefinition);
	}

	private void showMethod(Javadocs docs, DocMethod method) {
		// Build doc window
		VBox flow = new VBox();
		ScrollPane root = new ScrollPane(flow);
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
		flow.getChildren().addAll(lblDefinition, lblDesc);
		// Show popup
		show(root, lblDefinition);
	}

	private void show(ScrollPane root, Control handle) {
		root.getStyleClass().add("scroll-antiblur-hack");
		root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		root.setFitToWidth(true);
		Popup pop = new Popup();
		double[] xOffset = {0};
		double[] yOffset = {0};
		handle.setOnMousePressed(event -> {
			xOffset[0] = event.getSceneX();
			yOffset[0] = event.getSceneY();
		});
		handle.setOnMouseDragged(event -> {
			pop.setX(event.getScreenX() - xOffset[0]);
			pop.setY(event.getScreenY() - yOffset[0]);
		});
		pop.getContent().setAll(root);
		pop.setAutoHide(true);
		root.addEventHandler(MouseEvent.MOUSE_EXITED, e -> pop.hide());
		pop.show(pane, last.getX(), last.getY());
	}
}
