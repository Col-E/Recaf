package me.coley.recaf.ui.controls.text;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import javafx.scene.input.MouseButton;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.source.SourceCode;
import me.coley.recaf.ui.ContextMenus;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.model.TwoDimensional;

import java.util.Optional;

import static me.coley.recaf.util.JavaParserUtil.*;

/**
 * Context menu handler for JavaPane's.
 *
 * @author Matt
 */
public class JavaContextHandling {
	private final JavaPane pane;

	/**
	 * @param pane
	 * 		Pane to handle context menus on.
	 * @param controller
	 * 		Controller to pull info from.
	 * @param code
	 * 		Analyzed code.
	 */
	public JavaContextHandling(JavaPane pane, GuiController controller, SourceCode code) {
		this.pane = pane;
		pane.codeArea.setOnMousePressed(e -> {
			// Only accept right-click presses
			if (e.getButton() != MouseButton.SECONDARY)
				return;
			// Mouse to location
			CharacterHit hit = pane.codeArea.hit(e.getX(), e.getY());
			int charPos = hit.getInsertionIndex();
			pane.codeArea.getCaretSelectionBind().displaceCaret(charPos);
			TwoDimensional.Position pos = pane.codeArea.offsetToPosition(charPos,
					TwoDimensional.Bias.Backward);
			// Get declaration at point
			Node node = getSelectedNode(code, pos);
			if(node == null) {
				pane.codeArea.setContextMenu(null);
				return;
			}
			// Resolve node to some declaration type and display context menu
			if(node instanceof ClassOrInterfaceDeclaration) {
				ResolvedReferenceTypeDeclaration dec =
						((ClassOrInterfaceDeclaration) node).resolve();
				String name = toInternal(dec);
				handleClassType(controller, pane, name, true);
			} else if(node instanceof FieldDeclaration) {
				ResolvedFieldDeclaration dec = ((FieldDeclaration) node).resolve();
				String owner = getOwner(dec);
				String name = dec.getName();
				String desc = getDescriptor(dec.getType());
				handleFieldType(controller, pane, owner, name, desc, true);
			} else if(node instanceof MethodDeclaration) {
				ResolvedMethodDeclaration dec = ((MethodDeclaration) node).resolve();
				String owner = getOwner(dec);
				String name = dec.getName();
				String desc = getDescriptor(dec);
				handleMethodType(controller, pane, owner, name, desc, true);
			} else if (node instanceof Resolvable<?>) {
				Resolvable<?> r = (Resolvable<?>) node;
				Object resolved = null;
				try {
					resolved = r.resolve();
				} catch(Exception ex) {
					return;
				}
				if (resolved instanceof ResolvedReferenceType) {
					ResolvedReferenceType type = (ResolvedReferenceType) resolved;
					handleClassType(controller, pane, type.getQualifiedName().replace('.', '/'), false);
				} else if (resolved instanceof ResolvedReferenceTypeDeclaration) {
					ResolvedReferenceTypeDeclaration type = (ResolvedReferenceTypeDeclaration) resolved;
					handleClassType(controller, pane, type.getQualifiedName().replace('.', '/'), false);
				} else if (resolved instanceof ResolvedConstructorDeclaration) {
					ResolvedConstructorDeclaration type = (ResolvedConstructorDeclaration) resolved;
					handleClassType(controller, pane, type.declaringType().getQualifiedName().replace('.', '/'), false);
				} else if (resolved instanceof ResolvedFieldDeclaration) {
					ResolvedFieldDeclaration type = (ResolvedFieldDeclaration) resolved;
					String owner = getOwner(type);
					String name = type.getName();
					String desc = getDescriptor(type);
					handleFieldType(controller, pane, owner, name, desc, false);
				} else if (resolved instanceof ResolvedMethodDeclaration) {
					ResolvedMethodDeclaration type = (ResolvedMethodDeclaration) resolved;
					String owner = getOwner(type);
					String name = type.getName();
					String desc = getDescriptor(type);
					handleMethodType(controller, pane, owner, name, desc, false);
				}
			}
		});
	}

	/**
	 * @param code
	 * 		Code wrapper.
	 * @param pos
	 * 		Position of caret.
	 *
	 * @return Node of supported type at position.
	 */
	private static Node getSelectedNode(SourceCode code, TwoDimensional.Position pos) {
		Node node = code.getNodeAt(pos.getMajor() + 1, pos.getMinor());
		// Go up a level until node type is supported
		while(true) {
			if(node instanceof MethodDeclaration ||
					node instanceof FieldDeclaration ||
					node instanceof ClassOrInterfaceDeclaration ||
					node instanceof Expression) {
				break;
			}
			Optional<Node> parent = node.getParentNode();
			if(!parent.isPresent())
				break;
			node = parent.get();
		}
		return node;
	}

	private void handleClassType(GuiController controller, JavaPane pane, String name, boolean declaration) {
		pane.codeArea.setContextMenu(ContextMenus.ofClass(controller, pane, name, declaration));
	}

	private void handleFieldType(GuiController controller, JavaPane pane, String owner, String name, String desc,
								 boolean declaration) {
		pane.codeArea.setContextMenu(ContextMenus.ofField(controller, pane, owner, name, desc, declaration));
	}

	private void handleMethodType(GuiController controller, JavaPane pane, String owner, String name, String desc,
								  boolean declaration) {
		pane.codeArea.setContextMenu(ContextMenus.ofMethod(controller, pane, owner, name, desc, declaration));
	}
}