package me.coley.recaf.ui.controls.text;

import com.github.javaparser.resolution.SymbolResolver;
import javafx.geometry.Point2D;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.javadoc.*;
import me.coley.recaf.parse.source.SourceCode;
import me.coley.recaf.ui.controls.text.selection.ClassSelection;
import me.coley.recaf.ui.controls.text.selection.MemberSelection;
import me.coley.recaf.util.JavaParserUtil;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.fxmisc.richtext.model.TwoDimensional;
import org.objectweb.asm.Type;

import java.util.Optional;

/**
 * On-hover JavaDoc handler.
 *
 * @author Matt
 */
public class JavaDocHandling {
	private final JavaEditorPane pane;
	private final SymbolResolver solver;
	private final SourceCode code;
	private Point2D last;

	/**
	 * @param pane
	 * 		Pane to handle JavaDoc on.
	 * @param controller
	 * 		Controller to pull docs from.
	 * @param code
	 * 		Analyzed code.
	 */
	public JavaDocHandling(JavaEditorPane pane, GuiController controller, SourceCode code) {
		this.pane = pane;
		// Fetch the solver so we can call it manually (see below for why)
		Optional<SymbolResolver> optSolver = controller.getWorkspace().getSourceParseConfig().getSymbolResolver();
		if (!optSolver.isPresent())
			throw new IllegalStateException("");
		this.solver = optSolver.get();
		this.code = code;
		// Set mouse-over event
		pane.codeArea.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN, e -> {
			last = e.getScreenPosition();
			// Get node from event position
			int charPos = e.getCharacterIndex();
			TwoDimensional.Position pos = pane.codeArea.offsetToPosition(charPos,
					TwoDimensional.Bias.Backward);
			// So the problem with "getSelection" is that internally it will resolve to the ACTUAL
			// proper descriptor... but in the JavaDocs, if we have generics, we can have a return type "T"
			// but in the descriptor it really is "java/lang/Object".
			Object selection = getSelection(pos);
			if(selection instanceof ClassSelection) {
				handleClassType(controller, (ClassSelection) selection);
			} else if(selection instanceof MemberSelection) {
				MemberSelection member = (MemberSelection) selection;
				if (member.method())
					handleMethodType(controller, member);
				else
					handleFieldType(controller, member);
			}
		});
	}

	protected Object getSelection(TwoDimensional.Position pos) {
		return JavaParserUtil.getSelection(code, solver, pos);
	}

	private void handleClassType(GuiController controller, ClassSelection selection) {
		Javadocs docs = controller.getWorkspace().getClassDocs(selection.name);
		if(docs == null)
			return;
		JavaDocWindow.ofClass(docs).show(pane, last.getX(), last.getY());
	}

	private void handleFieldType(GuiController controller, MemberSelection selection) {
		Javadocs docs = controller.getWorkspace().getClassDocs(selection.owner);
		if (docs == null)
			return;
		String type = simplify(Type.getType(selection.desc).getClassName());
		Optional<DocField> optField = docs.getFields().stream()
				.filter(f ->
						f.getType().equals(type) &&
						f.getName().equals(selection.name))
				.findFirst();
		optField.ifPresent(field -> JavaDocWindow.ofField(field).show(pane, last.getX(), last.getY()));
	}

	private void handleMethodType(GuiController controller, MemberSelection selection) {
		Javadocs docs = controller.getWorkspace().getClassDocs(selection.owner);
		if (docs == null)
			return;
		String retType = simplify(Type.getType(selection.desc).getReturnType().getClassName());
		Optional<DocMethod> optMethod = docs.getMethods().stream()
				.filter(f ->
						f.getReturnType().equals(retType) &&
						f.getName().equals(selection.name))
				.findFirst();
		optMethod.ifPresent(method -> JavaDocWindow.ofMethod(method).show(pane, last.getX(), last.getY()));
	}

	private String simplify(String qualified) {
		return qualified.contains(".") ?
				qualified.substring(qualified.lastIndexOf(".") + 1) : qualified;
	}
}