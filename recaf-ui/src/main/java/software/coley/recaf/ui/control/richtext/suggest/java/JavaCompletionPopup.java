package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.fxmisc.richtext.CodeArea;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.ui.control.richtext.suggest.CompletionPopup;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.util.Icons;

import java.util.function.Supplier;

/**
 * Java-specific completion popup handling insertion text and caret backtracking.
 *
 * @author Matt Coley
 */
public final class JavaCompletionPopup extends CompletionPopup<JavaCompletion> {
	private final JavaLexicalContextParser lexicalContextParser;
	private final Supplier<JavaLexicalContext> contextSupplier;
	private final JavaCompletionRanker completionRanker;
	private CodeArea area;

	public JavaCompletionPopup(@Nonnull TabCompletionConfig config,
	                           @Nonnull CellConfigurationService configurationService,
	                           @Nonnull JavaLexicalContextParser lexicalContextParser,
	                           @Nonnull Supplier<JavaLexicalContext> contextSupplier,
	                           @Nonnull JavaCompletionRanker completionRanker) {
		super(config, STANDARD_CELL_SIZE, JavaCompletion::displayText, completion -> {
			PathNode<?> path = completion.path();
			if (path != null)
				return configurationService.graphicOf(path);
			return switch (completion.kind()) {
				case KEYWORD, LOCAL -> Icons.getIconView(Icons.INTERNAL);
				case FIELD -> Icons.getIconView(Icons.FIELD);
				case METHOD -> Icons.getIconView(Icons.METHOD);
				case TYPE ->
						completion.annotationOnly() ? Icons.getIconView(Icons.ANNOTATION) : Icons.getIconView(Icons.CLASS);
				case PACKAGE -> Icons.getIconView(Icons.FOLDER_PACKAGE);
			};
		});
		this.lexicalContextParser = lexicalContextParser;
		this.contextSupplier = contextSupplier;
		this.completionRanker = completionRanker;
	}

	@Override
	public void setArea(@Nullable CodeArea area) {
		super.setArea(area);
		this.area = area;
	}

	@Override
	public void completeCurrentSelection() {
		doComplete(contextSupplier.get().partialText());
	}

	@Override
	public boolean doComplete(@Nonnull String partialText) {
		JavaCompletion selected = getSelected();
		hide();
		if (selected == null)
			return false;
		CodeArea localArea = area;
		if (localArea == null)
			return false;
		String suffix = lexicalContextParser.completionSuffix(partialText, selected.fullInsertionText());
		if (suffix == null)
			return false;
		localArea.insertText(localArea.getCaretPosition(), suffix);
		completionRanker.recordSelection(selected);
		if (selected.caretBacktrack() > 0)
			localArea.moveTo(localArea.getCaretPosition() - selected.caretBacktrack());
		if (localArea.getCaretBounds().isEmpty())
			localArea.showParagraphAtCenter(localArea.getCurrentParagraph());
		return true;
	}
}
