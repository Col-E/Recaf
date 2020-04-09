package me.coley.recaf.ui.controls.text;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.source.SourceCode;
import me.coley.recaf.ui.ContextBuilder;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.ui.controls.text.selection.*;
import me.coley.recaf.util.Log;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;

import java.util.Optional;

import static me.coley.recaf.util.JavaParserUtil.*;
import static me.coley.recaf.ui.ContextBuilder.menu;

/**
 * Context menu handler for {@link JavaPane}.
 *
 * @author Matt
 */
public class JavaContextHandling extends ContextHandling {
	private SymbolResolver solver;
	private SourceCode code;

	/**
	 * @param controller
	 * 		Controller to pull info from.
	 * @param codeArea
	 * 		Text editor events originate from.
	 */
	public JavaContextHandling(GuiController controller, CodeArea codeArea) {
		super(controller, codeArea);
		// Fetch the solver so we can call it manually (see below for why)
		Optional<SymbolResolver> optSolver = controller.getWorkspace().getSourceParseConfig().getSymbolResolver();
		if (!optSolver.isPresent())
			throw new IllegalStateException("");
		this.solver = optSolver.get();
		// Set context selection action
		onContextRequest(selection -> {
			if (selection instanceof ClassSelection) {
				handleClassType((ClassSelection) selection);
			} else if (selection instanceof MemberSelection){
				handleMemberType((MemberSelection) selection);
			}
		});
	}

	@Override
	protected Object getSelection(TwoDimensional.Position pos) {
		// Get declaration at point
		Node node = getSelectedNode(code, pos);
		if(node == null)
			return null;
		// Resolve node to some declaration type and display context menu
		Object selection = checkForDeclaredSelection(solver, node);
		if (selection != null)
			return selection;
		selection = checkReferencedSelection(node);
		return selection;
	}

	private static Object checkForDeclaredSelection(SymbolResolver solver, Node node) {
		try {
			if(node instanceof TypeDeclaration) {
				ResolvedReferenceTypeDeclaration dec = ((TypeDeclaration) node).resolve();
				String name = toInternal(dec);
				return new ClassSelection(name, true);
			} else if(node instanceof FieldDeclaration || (node instanceof VariableDeclarator &&
					node.getParentNode().get() instanceof FieldDeclaration)) {
				// Check if we need to fetch the parent instead
				if(node instanceof VariableDeclarator)
					node = node.getParentNode().get();
				ResolvedFieldDeclaration dec = ((FieldDeclaration) node).resolve();
				String owner = getOwner(dec);
				String name = dec.getName();
				String desc = getDescriptor(dec.getType());
				return new MemberSelection(owner, name, desc, true);
			} else if(node instanceof MethodDeclaration) {
				ResolvedMethodDeclaration dec = ((MethodDeclaration) node).resolve();
				String owner = getOwner(dec);
				String name = dec.getName();
				String desc = getDescriptor(dec);
				return new MemberSelection(owner, name, desc, true);
			} else if(node instanceof ConstructorDeclaration) {
				ResolvedConstructorDeclaration dec = ((ConstructorDeclaration) node).resolve();
				String owner = toInternal(dec.declaringType());
				String name = "<init>";
				String desc = getDescriptor(dec);
				return new MemberSelection(owner, name, desc, true);
			} else if(node instanceof InitializerDeclaration) {
				InitializerDeclaration dec = (InitializerDeclaration) node;
				if(!dec.getParentNode().isPresent())
					return null; // sanity check, but it should ALWAYS be present and a type declaration
				String owner = toInternal(((TypeDeclaration) dec.getParentNode().get()).resolve());
				String name = "<clinit>";
				String desc = "()V";
				return new MemberSelection(owner, name, desc, true);
			} else if(node instanceof NameExpr) {
				// Ok so this one is a bit tricky. There are different cases we want to handle.
				NameExpr nameExpr = (NameExpr) node;
				// This "type" is used as a fallback. This is for cases like:
				//  - MyType.func()
				//  - MyType.constant
				// Where we want to resolve the type "MyType" and NOT the whole declared member.
				// This works because in these cases "MyType" is not a reference or declaration, only a name.
				ResolvedType type = solver.calculateType(nameExpr);
				String internal = toInternal(type);
				// Check if we want to resolve the member, and not the selected value's type.
				// - Check by seeing if the resolved member name is the selected name.
				try {
					ResolvedValueDeclaration dec = nameExpr.resolve();
					if (nameExpr.getName().asString().equals(dec.getName())) {
						String owner = dec.isField() ? getOwner(dec.asField()) : getOwner(dec.asMethod());
						String name = dec.getName();
						String desc = getDescriptor(dec.getType());
						return new MemberSelection(owner, name, desc, false);
					}
				} catch(Exception ex) {
					// Failed, but its ok. We'll just return the type of this name.
					// - Method arguments names will have their type resolved
				}
				return new ClassSelection(internal, false);
			}
		} catch(UnsolvedSymbolException ex) {
			Log.error("Failed to resolve: " + ex.toString());
		}
		return null;
	}

	private static Object checkReferencedSelection(Node node) {
		if (node instanceof Resolvable<?>) {
			Resolvable<?> r = (Resolvable<?>) node;
			Object resolved = null;
			try {
				resolved = r.resolve();
			} catch(UnsolvedSymbolException ex) {
				return null;
			}
			if(node instanceof ReferenceType) {
				ResolvedType dec = ((ReferenceType) node).resolve();
				String name = toInternal(dec);
				return new ClassSelection(name, false);
			} else if(resolved instanceof ResolvedReferenceType) {
				ResolvedReferenceType type = (ResolvedReferenceType) resolved;
				return new ClassSelection(toInternal(type), false);
			} else if (resolved instanceof ResolvedReferenceTypeDeclaration) {
				ResolvedReferenceTypeDeclaration type = (ResolvedReferenceTypeDeclaration) resolved;
				return new ClassSelection(toInternal(type), false);
			} else if (resolved instanceof ResolvedConstructorDeclaration) {
				ResolvedConstructorDeclaration type = (ResolvedConstructorDeclaration) resolved;
				return new ClassSelection(toInternal(type.declaringType()), false);
			} else if (resolved instanceof ResolvedFieldDeclaration) {
				ResolvedFieldDeclaration type = (ResolvedFieldDeclaration) resolved;
				String owner = getOwner(type);
				String name = type.getName();
				String desc = getDescriptor(type);
				return new MemberSelection(owner, name, desc, false);
			} else if (resolved instanceof ResolvedMethodDeclaration) {
				ResolvedMethodDeclaration type = (ResolvedMethodDeclaration) resolved;
				String owner = getOwner(type);
				String name = type.getName();
				String desc = getDescriptor(type);
				return new MemberSelection(owner, name, desc, false);
			}
		}
		return null;
	}

	/**
	 * @param code
	 * 		Analyzed code.
	 */
	public void setCode(SourceCode code) {
		this.code = code;
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
		// Abort if no analyzed code to parse
		if (code == null)
			return null;
		// Get node at row/column
		Node node = code.getVerboseNodeAt(pos.getMajor() + 1, pos.getMinor());
		// Go up a level until node type is supported
		while(node != null) {
			if(node instanceof Resolvable || node instanceof InitializerDeclaration)
				break;
			Optional<Node> parent = node.getParentNode();
			if(!parent.isPresent())
				break;
			node = parent.get();
		}
		return node;
	}

	private void handleClassType(ClassSelection selection) {
		codeArea.setContextMenu(menu().controller(controller)
				.view(getViewport())
				.declaration(selection.dec)
				.ofClass(selection.name));
	}

	private void handleMemberType(MemberSelection selection) {
		ContextBuilder cb = menu().controller(controller)
				.view(getViewport())
				.declaration(selection.dec);
		if (selection.method())
			codeArea.setContextMenu(cb.ofMethod(selection.owner, selection.name, selection.desc));
		else
			codeArea.setContextMenu(cb.ofField(selection.owner, selection.name, selection.desc));
	}

	private ClassViewport getViewport() {
		return (ClassViewport) codeArea.getParent().getParent().getParent().getParent().getParent();
	}
}