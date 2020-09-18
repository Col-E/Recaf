package me.coley.recaf.ui.controls.text;

import com.github.javaparser.resolution.SymbolResolver;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.source.SourceCode;
import me.coley.recaf.ui.ContextBuilder;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.ui.controls.text.selection.*;
import me.coley.recaf.util.JavaParserUtil;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;

import java.util.Optional;

import static me.coley.recaf.ui.ContextBuilder.menu;

/**
 * Context menu handler for {@link JavaEditorPane}.
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
		return JavaParserUtil.getSelection(code, solver, pos);
	}

	@Override
	protected Object getCurrentSelection() {
		// Get selection at current position.
		TwoDimensional.Position pos = codeArea.offsetToPosition(codeArea.getCaretPosition(),
				TwoDimensional.Bias.Backward);
		return JavaParserUtil.getSelection(code, solver, pos);
	}

	/**
	 * @param code
	 * 		Analyzed code.
	 */
	public void setCode(SourceCode code) {
		this.code = code;
	}

	/**
	 * @return
	 *      Analyzed code.
	 */
	public SourceCode getCode() {
		return code;
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