package me.coley.recaf.ui.control.code.java;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.parse.JavaParserHelper;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.util.Threads;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.richtext.model.PlainTextChange;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static me.coley.recaf.parse.JavaParserResolving.resolvedValueToInfo;

/**
 * Syntax area implementation with a focus on Java specific behavior.
 *
 * @author Matt Coley
 */
public class JavaArea extends SyntaxArea implements ClassRepresentation {
	private final ExecutorService parseThreadService = Executors.newSingleThreadExecutor();
	private CompilationUnit lastAST;
	private boolean isLastAstCurrent;
	private Future<?> parseFuture;

	/**
	 * @param problemTracking
	 * 		Optional problem tracking implementation to enable line problem indicators.
	 */
	public JavaArea(ProblemTracking problemTracking) {
		super(Languages.JAVA, problemTracking);
	}

	@Override
	protected void onTextChanged(PlainTextChange change) {
		super.onTextChanged(change);
		// Queue up new parse task, killing prior task if present
		if (parseFuture != null) {
			parseFuture.cancel(true);
		}
		parseFuture = parseThreadService.submit(this::updateParse);
	}

	@Override
	public boolean supportsMemberSelection() {
		return lastAST != null;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		WorkspaceTypeSolver solver = RecafUI.getController().getServices().getTypeSolver();
		if (memberInfo.isField()) {
			lastAST.findFirst(FieldDeclaration.class, dec -> {
				MemberInfo declaredInfo = (MemberInfo) resolvedValueToInfo(solver, dec.resolve());
				return memberInfo.equals(declaredInfo);
			}).flatMap(NodeWithRange::getBegin).ifPresent(this::selectPosition);
		} else if (memberInfo.getName().equals("<init>")) {
			lastAST.findFirst(ConstructorDeclaration.class, dec -> {
				MemberInfo declaredInfo = (MemberInfo) resolvedValueToInfo(solver, dec.resolve());
				return memberInfo.equals(declaredInfo);
			}).flatMap(NodeWithRange::getBegin).ifPresent(this::selectPosition);
		} else {
			lastAST.findFirst(MethodDeclaration.class, dec -> {
				MemberInfo declaredInfo = (MemberInfo) resolvedValueToInfo(solver, dec.resolve());
				return memberInfo.equals(declaredInfo);
			}).flatMap(NodeWithRange::getBegin).ifPresent(this::selectPosition);
		}
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		// No-op
	}

	@Override
	public void cleanup() {
		super.cleanup();
		parseThreadService.shutdownNow();
		if (parseFuture != null)
			parseFuture.cancel(true);
	}

	/**
	 * @param text
	 * 		Text to set.
	 */
	public void setText(String text) {
		setText(text, true);
	}

	private void setText(String text, boolean keepPosition) {
		if (keepPosition) {
			// Record prior caret position
			int caret = getCaretPosition();
			// Record prior scroll position
			double estimatedScrollY = 0;
			if (getParent() instanceof Virtualized) {
				Virtualized virtualParent = (Virtualized) getParent();
				estimatedScrollY = virtualParent.getEstimatedScrollY();
			}
			// Update the text
			clear();
			appendText(text);
			// Set to prior caret position
			if (caret >= 0 && caret < text.length()) {
				moveTo(caret);
			}
			// Set to prior scroll position
			if (estimatedScrollY >= 0 && getParent() instanceof Virtualized) {
				Virtualized virtualParent = (Virtualized) getParent();
				double targetY = estimatedScrollY;
				Threads.runFxDelayed(50, () -> virtualParent.scrollYToPixel(targetY));
			}
		} else {
			clear();
			appendText(text);
		}
	}

	/**
	 * Update the {@link #lastAST latest AST} and {@link #isLastAstCurrent AST is-up-to-date flag}.
	 */
	private void updateParse() {
		JavaParserHelper helper = RecafUI.getController().getServices().getJavaParserHelper();
		ParseResult<CompilationUnit> result = helper.parseClass(getText());
		if (result.getResult().isPresent()) {
			lastAST = result.getResult().get();
			isLastAstCurrent = true;
		} else {
			// We don't nullify the AST but do note that it is not up-to-date.
			isLastAstCurrent = false;
		}
	}

	/**
	 * Select the position of an AST element and {@link #centerParagraph(int) center it on the screen}.
	 *
	 * @param pos
	 * 		Position to select.
	 */
	private void selectPosition(com.github.javaparser.Position pos) {
		int currentLine = getCurrentParagraph();
		int targetLine = pos.line - 1;
		moveTo(targetLine, pos.column);
		requestFocus();
		selectWord();
		if (currentLine != targetLine) {
			centerParagraph(targetLine);
		}
	}

	/**
	 * @param paragraph
	 * 		Paragraph to center in the viewport.
	 */
	private void centerParagraph(int paragraph) {
		// Normally a full bounds will show the paragraph at the top of the viewport.
		// If we offset the position by half the height upwards, it centers it.
		Bounds bounds = new BoundingBox(0, -getHeight() / 2, getWidth(), getHeight());
		showParagraphRegion(paragraph, bounds);
	}
}
