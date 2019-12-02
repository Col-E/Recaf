package me.coley.recaf.ui.controls.text;

import com.github.javaparser.ast.Node;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import javafx.geometry.Point2D;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.javadoc.*;
import me.coley.recaf.parse.source.SourceCode;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.model.TwoDimensional;

import java.util.Optional;

import static me.coley.recaf.util.SourceUtil.*;

/**
 * On-hover JavaDoc handler.
 *
 * @author Matt
 */
public class JavaDocHandling {
	private final JavaPane pane;
	private Point2D last;

	/**
	 * @param pane
	 * 		Pane to handle JavaDoc on.
	 * @param controller
	 * 		Controller to pull docs from.
	 * @param code
	 * 		Analyzed code.
	 */
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
			if (resolved instanceof ResolvedReferenceType) {
				ResolvedReferenceType type = (ResolvedReferenceType) resolved;
				handleClassType(controller, toInternal(type));
			} else if (resolved instanceof ResolvedReferenceTypeDeclaration) {
				ResolvedReferenceTypeDeclaration type = (ResolvedReferenceTypeDeclaration) resolved;
				handleClassType(controller, toInternal(type));
			} else if (resolved instanceof ResolvedConstructorDeclaration) {
				ResolvedConstructorDeclaration type = (ResolvedConstructorDeclaration) resolved;
				handleClassType(controller, toInternal(type.declaringType()));
			} else if (resolved instanceof ResolvedMethodDeclaration) {
				ResolvedMethodDeclaration type = (ResolvedMethodDeclaration) resolved;
				handleMethodType(controller, type);
			} else if (resolved instanceof ResolvedFieldDeclaration) {
				ResolvedFieldDeclaration type = (ResolvedFieldDeclaration) resolved;
				handleFieldType(controller, type);
			}
		});
	}

	private void handleClassType(GuiController controller, String type) {
		Javadocs docs = controller.getWorkspace().getClassDocs(type);
		if(docs == null)
			return;
		JavaDocWindow.ofClass(docs).show(pane, last.getX(), last.getY());
	}

	private void handleFieldType(GuiController controller, ResolvedFieldDeclaration type) {
		Javadocs docs = controller.getWorkspace().getClassDocs(getFieldOwner(type));
		if (docs == null)
			return;
		Optional<DocField> optField = docs.getFields().stream()
				.filter(f ->
						f.getType().equals(simplify(type.getType().describe())) &&
						f.getName().equals(type.getName()))
				.findFirst();
		optField.ifPresent(field -> JavaDocWindow.ofField(field).show(pane, last.getX(), last.getY()));
	}

	private void handleMethodType(GuiController controller, ResolvedMethodDeclaration type) {
		Javadocs docs = controller.getWorkspace().getClassDocs(getMethodOwner(type));
		if (docs == null)
			return;
		Optional<DocMethod> optMethod = docs.getMethods().stream()
				.filter(f ->
						f.getReturnType().equals(simplify(type.getReturnType().describe())) &&
						f.getName().equals(type.getName()))
				.findFirst();
		optMethod.ifPresent(method -> JavaDocWindow.ofMethod(method).show(pane, last.getX(), last.getY()));
	}

	private String simplify(String quantified) {
		return quantified.contains(".") ?
				quantified.substring(quantified.lastIndexOf(".") + 1) : quantified;
	}
}