package me.coley.recaf.assemble.suggestions.type;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import me.coley.recaf.code.AccessibleInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.ui.util.Icons;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.BitSet;

/**
 * Suggestion impl for {@link MemberInfo} values.
 *
 * @author Matt Coley
 */
public class MemberInfoSuggestion extends StringMatchSuggestion {
	@Nonnull
	private final MemberInfo info;

	/**
	 * @param input
	 * 		Input typed when auto-completing
	 * @param info
	 * 		Associated item info of the suggestion.
	 * @param matchedChars
	 * 		Chars which match the search.
	 */
	public MemberInfoSuggestion(String input, @Nonnull MemberInfo info, @Nullable BitSet matchedChars) {
		super(input, info.getName() + " " + info.getDescriptor(), matchedChars);
		this.info = info;
	}

	/**
	 * May denote a class, field, or method depending on the context of the suggestion result.
	 *
	 * @return Associated item info of the suggestion.
	 */
	@Nonnull
	public MemberInfo getInfo() {
		return info;
	}

	@Nullable
	@Override
	public Node getGraphic() {
		HBox graphics = new HBox(Icons.getInfoIcon(info));
		graphics.getChildren().add(Icons.getVisibilityIcon(info.getAccess()));
		return graphics;
	}
}
