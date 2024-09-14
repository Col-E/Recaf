package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import regexodus.Matcher;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.RegexUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * A simple completer where completion candidate phrases are just words found in the current document.
 *
 * @author Matt Coley
 */
public class ExistingWordTabCompleter implements TabCompleter<String> {
	private final Set<String> words = ConcurrentHashMap.newKeySet();
	private final CompletionPopup<String> completionPopup = new StringCompletionPopup(8);
	private CodeArea area;
	private String lineContext;

	@Override
	public boolean requestCompletion(@Nonnull KeyEvent event) {
		// Recompute line context to ensure its up-to-date.
		String localContext = recomputeLineContext();

		// Skip if no text context or empty text context.
		if (localContext.isBlank())
			return false;

		// Complete if the completion popup is showing.
		return completionPopup.isShowing() && completeFromContext(localContext, completionPopup::doComplete);
	}

	@Nonnull
	@Override
	public List<String> computeCurrentCompletions() {
		List<String> items = new ArrayList<>();
		String localContext = lineContext;
		String group = null;
		Matcher matcher = RegexUtil.getMatcher("\\w+", localContext);
		while (matcher.find())
			group = matcher.group();
		if (group != null)
			for (String word : words)
				if (word.startsWith(group) && !word.equals(group))
					items.add(word);
		return items;
	}

	@Override
	public void onFineTextUpdate(@Nonnull PlainTextChange change) {
		recomputeLineContext();
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
		area = editor.getCodeArea();
		completionPopup.install(area, this);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		completionPopup.uninstall();
		area = null;
	}

	@Nonnull
	private String recomputeLineContext() {
		String line = area.getParagraph(area.getCurrentParagraph()).getText();
		return lineContext = computeCompletionContext(line, area.getCaretColumn());
	}

	private boolean completeFromCurrentContext(@Nonnull Predicate<String> completionHandler) {
		return completeFromContext(lineContext, completionHandler);
	}

	private static boolean completeFromContext(@Nullable String context, @Nonnull Predicate<String> completionHandler) {
		if (context == null)
			return false;
		String group = null;
		Matcher matcher = RegexUtil.getMatcher("\\w+", context);
		while (matcher.find())
			group = matcher.group();
		return group != null && completionHandler.test(group);
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

	private class StringCompletionPopup extends CompletionPopup<String> {
		private StringCompletionPopup(int maxItemsToShow) {
			super(STANDARD_CELL_SIZE, maxItemsToShow, t -> t);
		}

		@Override
		public void completeCurrentSelection() {
			completeFromCurrentContext(this::doComplete);
		}
	}
}
