package me.coley.recaf.ui.control.code.java;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.parse.JavaParserHelper;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.ui.util.ScrollUtils;
import me.coley.recaf.util.logging.Logging;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.TwoDimensional;
import org.slf4j.Logger;

import java.util.Optional;
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
	private static final Logger logger = Logging.get(JavaArea.class);
	private final ExecutorService parseThreadService = Executors.newSingleThreadExecutor();
	private CompilationUnit lastAST;
	private boolean isLastAstCurrent;
	private Future<?> parseFuture;
	private ContextMenu menu;

	/**
	 * @param problemTracking
	 * 		Optional problem tracking implementation to enable line problem indicators.
	 */
	public JavaArea(ProblemTracking problemTracking) {
		super(Languages.JAVA, problemTracking);
		setOnContextMenuRequested(this::onMenuRequested);
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

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	private void onMenuRequested(ContextMenuEvent e) {
		// Close old menu
		if (menu != null) {
			menu.hide();
			menu = null;
		}
		// Check if there is parsable AST info
		if (lastAST == null) {
			// TODO: More visually noticeable warning to user that the AST failed to be parsed
			//  - Offer to switch class representation?
			logger.warn("Could not request context menu since the code is not parsable!");
			return;
		}
		// Convert the event position to line/column
		CharacterHit hit = hit(e.getX(), e.getY());
		Position hitPos = offsetToPosition(hit.getInsertionIndex(),
				TwoDimensional.Bias.Backward);
		int line = hitPos.getMajor() + 1; // Position is 0 indexed
		int column = hitPos.getMinor();
		// Sync caret
		moveTo(hit.getInsertionIndex());
		// Check if there is info about the selected item
		JavaParserHelper helper = RecafUI.getController().getServices().getJavaParserHelper();
		Optional<ItemInfo> infoAtPosition = helper.at(lastAST, line, column);
		if (infoAtPosition.isPresent()) {
			if (infoAtPosition.get() instanceof ClassInfo) {
				ClassInfo info = (ClassInfo) infoAtPosition.get();
				menu = ContextBuilder.forClass(info).build();
			} else if (infoAtPosition.get() instanceof DexClassInfo) {
				DexClassInfo info = (DexClassInfo) infoAtPosition.get();
				menu = ContextBuilder.forDexClass(info).build();
			} else if (infoAtPosition.get() instanceof FieldInfo) {
				FieldInfo info = (FieldInfo) infoAtPosition.get();
				CommonClassInfo owner = RecafUI.getController().getWorkspace()
						.getResources().getClass(info.getOwner());
				if (owner != null)
					menu = ContextBuilder.forField(owner, info).build();
			} else if (infoAtPosition.get() instanceof MethodInfo) {
				MethodInfo info = (MethodInfo) infoAtPosition.get();
				CommonClassInfo owner = RecafUI.getController().getWorkspace()
						.getResources().getClass(info.getOwner());
				if (owner != null)
					menu = ContextBuilder.forMethod(owner, info).build();
			}
		}
		// Show if present
		if (menu != null) {
			menu.setAutoHide(true);
			menu.setHideOnEscape(true);
			menu.show(getScene().getWindow(), e.getScreenX(), e.getScreenY());
			menu.requestFocus();
		} else {
			logger.warn("No class or member at selected position [line {}, column {}]", line, column);
		}
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
				ScrollUtils.forceScroll(virtualParent, estimatedScrollY);
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
}
