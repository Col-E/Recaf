package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;

/**
 * Basic T value to graphic mapper.
 *
 * @param <T>
 * 		Completion value type.
 *
 * @author Matt Coley
 */
public interface CompletionValueGraphicMapper<T> {
	/**
	 * @param value
	 * 		Value to represent.
	 *
	 * @return Visual accompanying graphic of the value.
	 */
	@Nullable
	Node toGraphic(@Nonnull T value);
}
