package me.coley.recaf.ui.controls;

import java.util.LinkedHashSet;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 * Extra calls for TableView which would have been nice to see in JavaFx TableView
 *
 * @param <T>
 * 		Table content type.
 *
 * @author <a href="https://stackoverflow.com/users/4712734/duncg">DuncG</a>
 */
public class TableViewExtra<T> {
	private final LinkedHashSet<TableRow<T>> rows = new LinkedHashSet<>();
	private final TableView<T> table;
	private int firstIndex;

	/**
	 * @param tableView
	 * 		Table to wrap.
	 */
	public TableViewExtra(TableView<T> tableView) {
		this.table = tableView;
		// Callback to monitor row creation and to identify visible screen rows
		final Callback<TableView<T>, TableRow<T>> rf = tableView.getRowFactory();
		final Callback<TableView<T>, TableRow<T>> modifiedRowFactory = param -> {
			TableRow<T> r = rf != null ? rf.call(param) : new TableRow<>();
			// Save row, this implementation relies on JaxaFX re-using TableRow efficiently
			rows.add(r);
			return r;
		};
		tableView.setRowFactory(modifiedRowFactory);
	}

	private void recomputeVisibleIndexes() {
		firstIndex = -1;
		// Work out which of the rows are visible
		double tblViewHeight = table.getHeight();
		double headerHeight =
                table.lookup(".column-header-background").getBoundsInLocal().getHeight();
		double viewPortHeight = tblViewHeight - headerHeight;
		for(TableRow<T> r : rows) {
			if(!r.isVisible())
				continue;
			double minY = r.getBoundsInParent().getMinY();
			double maxY = r.getBoundsInParent().getMaxY();
			boolean hidden = (maxY < 0) || (minY > viewPortHeight);
			if(hidden)
				continue;
			if(firstIndex < 0 || r.getIndex() < firstIndex)
				firstIndex = r.getIndex();
		}
	}

	/**
	 * Find the first row in the table which is visible on the display
	 *
	 * @return {@code -1} if none visible or the index of the first visible row (wholly or fully)
	 */
	public int getFirstVisibleIndex() {
		recomputeVisibleIndexes();
		return firstIndex;
	}
}