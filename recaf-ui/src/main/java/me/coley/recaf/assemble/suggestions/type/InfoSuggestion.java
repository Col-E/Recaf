package me.coley.recaf.assemble.suggestions.type;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import me.coley.recaf.code.AccessibleInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.ui.util.Icons;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.BitSet;

/**
 * Suggestion impl for generic {@link ItemInfo} values.
 *
 * @author Amejonah
 */
public class InfoSuggestion extends StringMatchSuggestion {
	@Nonnull
	private final ItemInfo info;

	/**
	 * @param input
	 * 		Input typed when auto-completing
	 * @param info
	 * 		Associated item info of the suggestion.
	 * @param matchedChars
	 * 		Chars which match the search.
	 */
	public InfoSuggestion(String input, @Nonnull ItemInfo info, @Nullable BitSet matchedChars) {
		super(input, info.getName(), matchedChars);
		this.info = info;
	}

	/**
	 * May denote a class, field, or method depending on the context of the suggestion result.
	 *
	 * @return Associated item info of the suggestion.
	 */
	@Nonnull
	public ItemInfo getInfo() {
		return info;
	}

	@Nullable
	@Override
	public Node getGraphic() {
		// Populate with icon if possible
		HBox graphics = new HBox(Icons.getInfoIcon(info));
		if (info instanceof AccessibleInfo)
			graphics.getChildren().add(Icons.getVisibilityIcon(((AccessibleInfo) info).getAccess()));
		return graphics;
	}
}
