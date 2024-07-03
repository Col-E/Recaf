package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import me.darknet.assembler.compile.analysis.Value;
import software.coley.recaf.util.EscapeUtil;

import java.util.Objects;

/**
 * Cell for rendering {@link Value}.
 *
 * @param <S>
 * 		Table-view generic type.
 *
 * @author Matt Coley
 */
public class ValueTableCell<S> extends TableCell<S, ValueTableCell.ValueWrapper> {
	private static final String CHANGED = "analysis-value-changed";

	@Override
	protected void updateItem(ValueWrapper wrapper, boolean empty) {
		super.updateItem(wrapper, empty);
		if (empty || wrapper == null) {
			setText(null);
			setGraphic(null);
			getStyleClass().remove(CHANGED);
		} else {
			configureValue(wrapper);
		}
	}

	private void configureValue(@Nonnull ValueWrapper wrapper) {
		Value value = wrapper.value;
		String valueRep = value.valueAsString();
		if (valueRep != null)
			valueRep = EscapeUtil.escapeAll(valueRep);
		setText(valueRep);

		// If a prior frame/value exists, highlight changed items
		ObservableList<String> styleClass = getStyleClass();
		if (valueRep != null) {
			Value priorValue = wrapper.priorValue;
			if (priorValue != null) {
				String priorValueRep = priorValue.valueAsString();
				if (!Objects.equals(valueRep, priorValueRep) && !styleClass.contains(CHANGED)) {
					styleClass.add(CHANGED);
					return;
				}
			}
		}
		styleClass.remove(CHANGED);
	}

	/**
	 * @param value
	 * 		Variable value.
	 * @param priorValue
	 * 		Prior state in previous frame, if known.
	 */
	public record ValueWrapper(@Nonnull Value value, @Nullable Value priorValue) {}
}
