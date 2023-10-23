package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import regexodus.Matcher;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.RegexUtil;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic tab completer implementation. Completion candidate phrases are just words found in the current document.
 *
 * @author Matt Coley
 */
public class BasicTabCompleter implements TabCompleter {
	private final Set<String> words = ConcurrentHashMap.newKeySet();
	private CodeArea area;
	private String lineContext;

	@Override
	public boolean requestCompletion(@Nonnull KeyEvent event) {
		String localContext = lineContext;

		// Skip if no text context or empty text context.
		if (localContext == null || localContext.isBlank())
			return false;

		// TODO: Create a UI that shows the available options and synchronize 'current selection'
		//  based on arrow keys. The current selection will be filled here instead.
		//  - This temporary solution just matches the first available word, no UI
		String group = null;
		Matcher matcher = RegexUtil.getMatcher("\\w+", localContext);
		while (matcher.find())
			group = matcher.group();
		if (group != null)
			for (String word : words)
				if (word.startsWith(group))
					return complete(group, word);

		return false;
	}

	@Override
	public void onFineTextUpdate(@Nonnull PlainTextChange changes) {
		String line = area.getParagraph(area.getCurrentParagraph()).getText();
		lineContext = computeCompletionContext(line, area.getCaretColumn());
	}

	@Override
	public void onRoughTextUpdate(@Nonnull List<PlainTextChange> changes) {
		for (PlainTextChange change : changes) {
			String inserted = change.getInserted();
			if (inserted != null && !inserted.isBlank()) {
				String[] changedWords = inserted.split("\\W+");
				Collections.addAll(words, changedWords);
			}
		}
	}

	@Override
	public void install(@Nonnull Editor editor) {
		this.area = editor.getCodeArea();
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		area = null;
	}

	private boolean complete(@Nonnull String currentWordPart, @Nonnull String fullWord) {
		String remainingWordText = fullWord.substring(currentWordPart.length());
		area.insertText(area.getCaretPosition(), remainingWordText);
		return true;
	}

	@Nonnull
	private static String computeCompletionContext(@Nonnull String line, int column) {
		// Nothing to complete from an empty line.
		if (column == 0) return "";

		// Valid cases:
		//  foo.[TAB]
		//  foo.a[TAB]
		//
		// Invalid cases:
		//  fo[TAB]o
		//  [TAB]

		String preColumn = line.substring(0, column);
		char preColumnLastChar = preColumn.charAt(preColumn.length() - 1);

		// Check for '.' used as context before the tab was pressed.
		if (preColumnLastChar == '.') return preColumn.trim();

		// Ensure the content in front of the caret is empty.
		String postColumn = line.substring(column);
		if (postColumn.isBlank()) return preColumn.trim();

		// Not a valid case.
		return "";
	}
}
