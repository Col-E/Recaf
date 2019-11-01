package me.coley.recaf.ui.controls;

import java.util.LinkedHashSet;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 * Extra calls for TableView which would have been nice to see in JavaFx TableView
 *
 * @author <a href="https://stackoverflow.com/users/4712734/duncg">DuncG</a>
 */
public class TableViewExtra<T> {
	private final LinkedHashSet<TableRow<T>> rows = new LinkedHashSet<>();
	private final TableView<T> table;
	private int firstIndex;
	private int lastIndex;

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

	/**
	 * Changes the current view to ensure that one of the passed index positions
	 * is visible on screen. The view is not changed if any of the passed index positions is
	 * already visible.
	 * The table scroll position is moved so that the closest index to the current position is
	 * visible.
	 *
	 * @param indices
	 * 		Assumed to be in ascending order.
	 */
	public void scrollToIndex(int... indices) {
		int first = getFirstVisibleIndex();
		int last = getLastVisibleIndex();
		int where = first;
		boolean changeScrollPos = true;
		// No point moving current scroll position if one of the index items is visible already:
		if(first >= 0 && last >= first)
			for(int idx : indices) {
				if(first <= idx && idx <= last) {
					changeScrollPos = false;
					break;
				}
			}
		if(indices.length > 0 && changeScrollPos) {
			where = indices[0];
			if(first >= 0) {
				int x = closestTo(indices, first);
				int abs = Math.abs(x - first);
				if(abs < Math.abs(where - first))
					where = x;
			}
			if(last >= 0) {
				int x = closestTo(indices, last);
				int abs = Math.abs(x - last);
				if(abs < Math.abs(where - last))
					where = x;
			}
			table.scrollTo(where);
		}
	}

	private static int closestTo(int[] indices, int value) {
		int x = indices[0];
		int diff = Math.abs(value - x);
		int newDiff = diff;
		for(int v : indices) {
			newDiff = Math.abs(value - v);
			if(newDiff < diff) {
				x = v;
				diff = newDiff;
			}
		}
		return x;
	}

	private void recomputeVisibleIndexes() {
		firstIndex = -1;
		lastIndex = -1;
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
			if(!hidden) {
				if(firstIndex < 0 || r.getIndex() < firstIndex)
					firstIndex = r.getIndex();
				if(lastIndex < 0 || r.getIndex() > lastIndex)
					lastIndex = r.getIndex();
			}
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

	/**
	 * Find the last row in the table which is visible on the display
	 *
	 * @return {@code -1} if none visible or the index of the last visible row (wholly or fully)
	 */
	public int getLastVisibleIndex() {
		recomputeVisibleIndexes();
		return lastIndex;
	}
}