package software.coley.recaf.ui.pane.editing.binary.hex;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.ui.control.AbstractSearchBar;
import software.coley.recaf.ui.control.VirtualizedScrollPaneWrapper;
import software.coley.recaf.ui.pane.editing.binary.hex.ops.HexAccess;
import software.coley.recaf.ui.pane.editing.binary.hex.ops.HexNavigation;
import software.coley.recaf.ui.pane.editing.binary.hex.ops.HexOperations;
import software.coley.recaf.ui.pane.editing.binary.hex.cell.HexRow;
import software.coley.recaf.ui.pane.editing.binary.hex.cell.HexRowHeader;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.IntRange;
import software.coley.recaf.util.NodeEvents;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Hex editor control.
 *
 * @author Matt Coley
 */
@Dependent
public class HexEditor extends BorderPane {
	private static final Logger logger = Logging.get(HexEditor.class);
	private final HexConfig config;
	private final ObservableList<Integer> rows = FXCollections.observableArrayList();
	private final IntegerProperty rowCount = new SimpleIntegerProperty(0);
	private final VirtualFlow<Integer, HexRow> flow;
	private final VBox topContent = new VBox();
	private final HexOperations ops = newHexOperations();
	private final Node rowHeader;
	private HexSearchBar searchBar;
	private Consumer<byte[]> commitAction;
	private byte[] data;
	private byte[] dataDefaultState;
	private HexSelection selection = new HexSelection(0, 0);
	private boolean hexColumnActive = true;
	private int dragOrigin = -1;
	private int dragAnchor;
	private boolean shiftDragging;
	private long revision;
	private ExecutorService executor;

	@Inject
	public HexEditor(@Nonnull HexConfig config) {
		this.config = config;
		getStyleClass().add("hex-view");
		flow = VirtualFlow.createVertical(rows, row -> new HexRow(config, rowCount, ops, row));
		flow.setFocusTraversable(true);
		VirtualizedScrollPane<VirtualFlow<Integer, HexRow>> scroll = new VirtualizedScrollPaneWrapper<>(flow);

		config.getRowLength().addChangeListener((ob, old, cur) -> refreshRowDisplay());
		config.getRowSplitInterval().addChangeListener((ob, old, cur) -> refreshRowDisplay());
		config.getShowAddress().addChangeListener((ob, old, cur) -> refreshRowDisplay());
		config.getShowAscii().addChangeListener((ob, old, cur) -> refreshRowDisplay());

		registerInputListeners();

		rowHeader = new HexRowHeader(config, rowCount, ops).getNode();
		topContent.getChildren().add(rowHeader);
		setTop(topContent);
		setCenter(scroll);
	}

	private void registerInputListeners() {
		NodeEvents.addKeyPressHandler(flow, ops.keyListener());
		NodeEvents.addMousePressHandler(flow, e -> {
			if (!e.isPrimaryButtonDown())
				return;

			int offset = pickOffsetAtPosition(e.getX(), e.getY());
			dragOrigin = offset;
			if (offset >= 0) {
				HexNavigation navigation = ops.navigation();
				HexSelection current = navigation.selection();
				shiftDragging = e.isShiftDown();
				dragAnchor = shiftDragging ? current.anchor() : offset;
				if (shiftDragging) {
					int caret = offset < dragAnchor ? offset : Math.min(data.length, offset + 1);
					navigation.selectRange(dragAnchor, caret);
				} else if (current.range().empty() && current.caret() == offset) {
					ops.engageCurrent();
				} else {
					navigation.select(offset);
				}
			}

			// Do not propagate the event up the scene graph.
			e.consume();
		});
		NodeEvents.addMouseDraggedHandler(flow, e -> {
			if (dragOrigin < 0 || !e.isPrimaryButtonDown())
				return;
			int offset = pickOffsetAtPosition(e.getX(), e.getY());
			if (offset < 0)
				return;

			if (shiftDragging) {
				int caret = offset < dragAnchor ? offset : Math.min(data.length, offset + 1);
				ops.navigation().selectRange(dragAnchor, caret);
			} else if (offset < dragOrigin) {
				ops.navigation().selectRange(dragOrigin + 1, offset);
			} else {
				ops.navigation().selectRange(dragOrigin, Math.min(data.length, offset + 1));
			}
			e.consume();
		});
		NodeEvents.addMouseReleaseHandler(flow, e -> {
			if (e.getButton() == MouseButton.PRIMARY)
				dragOrigin = -1;

			// If we've not gotten focus grab it and consume the event so that it does not propagate up the scene graph.
			if (!flow.isFocusWithin()) {
				e.consume();
				flow.requestFocus();
			}
		});
	}

	/**
	 * @return Hex operations API for this editor.
	 */
	@Nonnull
	public HexOperations getOperations() {
		return ops;
	}

	/**
	 * Open the byte search bar.
	 */
	public void openFindDialog() {
		finishPendingInput();
		if (searchBar == null)
			searchBar = new HexSearchBar(this);
		searchBar.show();
	}

	/**
	 * Set or remove the optional search bar from the editor header.
	 * @param node Search bar node to set, or {@code null} to remove the search bar.
	 */
	public void setSearchBarNode(@Nullable AbstractSearchBar node) {
		if (node == null)
			topContent.getChildren().setAll(rowHeader);
		else
			topContent.getChildren().setAll(node, rowHeader);
	}

	/**
	 * Return keyboard focus to the byte grid.
	 */
	public void requestByteFocus() {
		flow.requestFocus();
	}

	/**
	 * Refresh visible cells after search-result state changes.
	 */
	public void refreshHexDisplay() {
		refreshRowDisplay();
	}

	/**
	 * Refresh search results after data changes.
	 */
	private void refreshSearchResults() {
		if (searchBar != null && searchBar.isVisible())
			searchBar.refreshResults();
	}

	/**
	 * If there is any pending input in the editor, finish it before doing other operations.
	 */
	private void finishPendingInput() {
		if (data == null || data.length == 0)
			return;
		int offset = Math.min(ops.navigation().selection().caret(), data.length - 1);
		bringOffsetIntoView(offset);
		flow.getCellIfVisible(offsetToRowIndex(offset)).ifPresent(row -> row.finishPendingInput(offset));
	}

	/**
	 * @return Current revision of the data.
	 */
	public long getRevision() {
		return revision;
	}

	/**
	 * @return Executor service for running background tasks.
	 */
	@Nonnull
	public ExecutorService getExecutor() {
		if (executor == null)
			executor = ThreadPoolFactory.newSingleThreadExecutor("hex-pool");
		return executor;
	}

	/**
	 * Dispose of the editor and any resources it holds.
	 */
	public void dispose() {
		if (searchBar != null)
			searchBar.dispose();
		if (executor != null)
			executor.shutdownNow();
	}

	/**
	 * The view does not know anything about where the data originates, thus
	 * it is the caller's responsibility to delegate 'commit' to update the
	 * original data.
	 * <p>
	 * The specified action will be passed the current state of the data as
	 * seen in the hex-view when the user requests a save.
	 *
	 * @param dataCommit
	 * 		Action to handle committing changes of the content.
	 */
	public void setCommitAction(@Nullable Consumer<byte[]> dataCommit) {
		this.commitAction = dataCommit;
	}

	/**
	 * Called to 'commit' the changes.
	 * <p>
	 * Delegates to the user provided action via {@link #setCommitAction(Consumer)}.
	 */
	public boolean commit() {
		if (data == null) {
			logger.warn("Tried to commit hex-view contents without assocated data.");
			return false;
		}
		if (commitAction == null) {
			logger.warn("Tried to commit hex-view contents without commit action specified.");
			return false;
		}
		try {
			commitAction.accept(data);
			return true;
		} catch (Exception ex) {
			logger.error("Failed to commit hex-view contents", ex);
			return false;
		}
	}

	/**
	 * @return {@code true} when data has been assigned to the editor.
	 */
	public boolean hasData() {
		return data != null;
	}

	/**
	 * Used to assign the initial state of the data to display.
	 * For making modifications to the current data, use {@link #updateData(byte[])} instead.
	 *
	 * @param data
	 * 		Data to assign to the hex-view.
	 */
	public void setInitialData(@Nullable byte[] data) {
		if (data == null)
			data = new byte[0];
		else
			data = Arrays.copyOf(data, data.length);
		setData0(data);

		// Scroll to the top
		FxThreadUtil.run(() -> flow.show(0));
	}

	/**
	 * Used to update the existing data of the hex-view.
	 *
	 * @param data
	 * 		Updated data model.
	 */
	public void updateData(@Nonnull byte[] data) {
		if (dataDefaultState == null) {
			logger.warn("Tried to update hex-view data before setting the initial data state");
			return;
		}
		this.data = data;
		selection = clampSelection(selection);
		revision++;

		refreshRowModel();
		refreshRowDisplay();
		refreshSearchResults();
	}

	/**
	 * Reset the data content to what it was when initially calling {@link #setInitialData(byte[])}.
	 */
	public void resetData() {
		this.data = Arrays.copyOf(dataDefaultState, dataDefaultState.length);
		selection = clampSelection(selection);
		revision++;
		refreshRowModel();
		refreshRowDisplay();
		refreshSearchResults();
	}

	/**
	 * @param data
	 * 		Data to set.
	 */
	private void setData0(@Nonnull byte[] data) {
		this.data = data;
		this.dataDefaultState = Arrays.copyOf(data, data.length);
		selection = new HexSelection(0, 0);
		dragOrigin = -1;
		revision++;

		// Refresh model/display.
		refreshRowModel();
		refreshRowDisplay();
		refreshSearchResults();
	}

	/**
	 * Called when {@link #data} is updated and requires recomputing how many rows need to be displayed.
	 */
	private void refreshRowModel() {
		// Update observable list model to generate the numbers of rows we want to show.
		double rowLength = config.getRowLength().getValue().doubleValue();
		int rowCount = (int) Math.max(1, Math.ceil(data.length / rowLength));
		if (rowCount != this.rows.size()) {
			List<Integer> zeroToRowMax = IntStream.range(0, rowCount)
					.boxed().collect(Collectors.toList());
			this.rows.setAll(zeroToRowMax);
			this.rowCount.setValue(rowCount);
		}
	}

	/**
	 * Called when the data is replaced.
	 */
	protected void refreshRowDisplay() {
		flow.visibleCells().forEach(HexRow::redraw);
	}

	/**
	 * Called when the data is replaced.
	 *
	 * @param offset
	 * 		Data offset.
	 */
	protected void refreshRowDisplay(int offset) {
		// Map the offset to the row, then refresh the row UI if it is visible
		flow.getCellIfVisible(offsetToRowIndex(offset)).ifPresent(HexRow::redraw);
	}

	/**
	 * Brings the data at the given offset into view.
	 *
	 * @param offset
	 * 		Data offset.
	 */
	private void bringOffsetIntoView(int offset) {
		int rowLength = config.getRowLength().getValue();
		int offsetRow = offset / rowLength;
		int firstVisRow = flow.getFirstVisibleIndex();

		// Checking for one below the current low ensures if we show the current row as the 'first'
		// then there will be an additional line above it as a sort of buffer.
		if (offsetRow - 1 < firstVisRow) {
			flow.showAsFirst(Math.max(0, offsetRow));
		} else {
			// Similarly having a +2 check has the same effect but as a buffer below.
			// Something with virtualization makes an equivalent offset but reversed not work here, so +2 it is.
			int lastVisRow = flow.getLastVisibleIndex();
			if (offsetRow + 2 >= lastVisRow) {
				flow.showAsLast(Math.min(rowCount.intValue(), offsetRow + 2));
			}
		}
	}

	/**
	 * @param offset
	 * 		Data offset.
	 *
	 * @return Row index that contains the offset.
	 */
	private int offsetToRowIndex(int offset) {
		int rowLength = config.getRowLength().getValue();
		return (offset / rowLength);
	}

	/**
	 * @param x
	 * 		X coordinate in the flow.
	 * @param y
	 * 		Y coordinate in the flow.
	 *
	 * @return Offset at the given position, or {@code -1} if no offset is at that position.
	 */
	private int pickOffsetAtPosition(double x, double y) {
		for (HexRow row : flow.visibleCells()) {
			Bounds bounds = row.getNode().getBoundsInParent();
			if (bounds.contains(x, y))
				return row.pickOffsetAtPosition(x, y);
		}
		return -1;
	}

	/**
	 * Select a single byte at the given offset. Clamped to {@code [0, data length]}.
	 *
	 * @param offset
	 * 		Byte offset to select.
	 */
	private void select(int offset) {
		finishPendingInput();
		int length = data == null ? 0 : data.length;
		int clamped = length == 0 ? 0 : Math.clamp(offset, 0, length - 1);
		setSelection(new HexSelection(clamped, clamped));
		bringOffsetIntoView(clamped);
	}

	/**
	 * Select a range of bytes. Both boundaries are clamped to {@code [0, data length]}.
	 *
	 * @param anchor
	 * 		Fixed boundary.
	 * @param caret
	 * 		Moving boundary.
	 */
	private void selectRange(int anchor, int caret) {
		finishPendingInput();
		setSelection(new HexSelection(clampBoundary(anchor), clampBoundary(caret)));
		bringOffsetIntoView(selection.caret());
	}

	/**
	 * Move the caret to a byte boundary, optionally extending from the current anchor.
	 *
	 * @param offset
	 * 		Target boundary.
	 * @param extend
	 *        {@code true} to preserve the current anchor.
	 */
	private void moveTo(int offset, boolean extend) {
		finishPendingInput();
		int clamped = clampBoundary(offset);
		setSelection(extend ? new HexSelection(selection.anchor(), clamped) : new HexSelection(clamped, clamped));
		bringOffsetIntoView(clamped);
	}

	/**
	 * @param offset
	 * 		Byte offset to clamp.
	 *
	 * @return Clamped offset to {@code [0, data length]}
	 */
	private int clampBoundary(long offset) {
		int length = data == null ? 0 : data.length;
		return (int) Math.max(0L, Math.min(length, offset));
	}

	/**
	 * @param selection
	 * 		Selection to clamp.
	 *
	 * @return Clamped selection.
	 */
	@Nonnull
	private HexSelection clampSelection(@Nonnull HexSelection selection) {
		return new HexSelection(clampBoundary(selection.anchor()), clampBoundary(selection.caret()));
	}

	/**
	 * @param selection
	 * 		New selection to set.
	 */
	private void setSelection(@Nonnull HexSelection selection) {
		if (this.selection.equals(selection))
			return;

		this.selection = selection;
		refreshRowDisplay();
	}

	/**
	 * Delete the current selection, or if there is no selection delete a single byte in the given direction.
	 *
	 * @param backward
	 *        {@code true} to delete the byte before the caret,
	 *        {@code false} to delete the byte after the caret.
	 */
	private void deleteSelection(boolean backward) {
		finishPendingInput();
		if (data == null)
			return;

		IntRange range = selection.range();
		int start;
		int end;
		if (!range.empty()) {
			start = range.start();
			end = range.end();
		} else if (backward) {
			if (selection.caret() == 0)
				return;
			start = selection.caret() - 1;
			end = selection.caret();
		} else {
			if (selection.caret() >= data.length)
				return;
			start = selection.caret();
			end = selection.caret() + 1;
		}
		replaceRange(start, end, new byte[0]);
	}

	/**
	 * Replace a range of bytes with the given replacement.
	 *
	 * @param start
	 * 		Start of the range to replace (inclusive).
	 * @param end
	 * 		End of the range to replace (exclusive).
	 * @param replacement
	 * 		New bytes to insert in place of the range.
	 */
	private void replaceRange(int start, int end, @Nonnull byte[] replacement) {
		if (data == null || start < 0 || end < start || end > data.length)
			throw new IndexOutOfBoundsException("Invalid byte range: [" + start + ", " + end + ")");

		long resultLength = (long) data.length - (end - start) + replacement.length;
		if (resultLength > Integer.MAX_VALUE)
			throw new IllegalArgumentException("Edited data is too large");
		if (start == end && replacement.length == 0)
			return;

		byte[] updated = new byte[(int) resultLength];
		System.arraycopy(data, 0, updated, 0, start);
		System.arraycopy(replacement, 0, updated, start, replacement.length);
		System.arraycopy(data, end, updated, start + replacement.length, data.length - end);
		data = updated;
		selection = new HexSelection(start, start);
		revision++;
		refreshRowModel();
		refreshRowDisplay();
		refreshSearchResults();
	}

	/**
	 * @return New operations API for this editor.
	 */
	@Nonnull
	private HexOperations newHexOperations() {
		HexAccess currentRead = () -> data;
		HexAccess originalRead = () -> dataDefaultState;
		HexNavigation navigation = new HexNavigation() {
			@Nonnull
			@Override
			public HexSelection selection() {
				return selection;
			}

			@Override
			public boolean isHexColumnSelected() {
				return hexColumnActive;
			}

			@Override
			public void switchColumns() {
				if (config.getShowAscii().getValue()) {
					hexColumnActive = !hexColumnActive;
					refreshRowDisplay();
				} else {
					hexColumnActive = true;
				}
			}

			@Override
			public void select(int offset) {
				HexEditor.this.select(offset);
			}

			@Override
			public void selectRange(int anchor, int caret) {
				HexEditor.this.selectRange(anchor, caret);
			}

			@Override
			public void moveTo(int offset, boolean extend) {
				HexEditor.this.moveTo(offset, extend);
			}
		};
		return new HexOperations() {
			private final EventHandler<KeyEvent> listener = e -> {
				KeyCode code = e.getCode();
				// Leave deletion keys to the active TextField while editing a single byte.
				if ((code == KeyCode.BACK_SPACE || code == KeyCode.DELETE)
						&& e.getTarget() instanceof javafx.scene.control.TextField)
					return;

				// If the focus is not in the editor, bring it in.
				if (!isFocusWithin()) flow.requestFocus();

				// Do not propagate the key-event up the scene graph.
				e.consume();

				HexNavigation nav = navigation();
				HexSelection current = nav.selection();
				int caret = current.caret();
				boolean extend = e.isShiftDown();
				int rowLength = config.getRowLength().getValue();
				switch (code) {
					case ENTER -> engageCurrent();
					case ESCAPE -> cancelCurrent();
					case TAB -> nav.switchColumns();
					case BACK_SPACE -> deleteSelection(true);
					case DELETE -> deleteSelection(false);
					case PAGE_UP -> nav.moveTo(clampBoundary((long) caret - (long) rowLength * Math.max(1, flow.visibleCells().size() - 1)), extend);
					case PAGE_DOWN -> nav.moveTo(clampBoundary((long) caret + (long) rowLength * Math.max(1, flow.visibleCells().size() - 1)), extend);
					case RIGHT, KP_RIGHT -> {
						if (!extend && !current.range().empty())
							nav.select(current.range().end());
						else
							nav.moveTo(clampBoundary((long) caret + 1), extend);
					}
					case LEFT, KP_LEFT -> {
						if (!extend && !current.range().empty())
							nav.select(current.range().start());
						else
							nav.moveTo(clampBoundary((long) caret - 1), extend);
					}
					case UP, KP_UP -> nav.moveTo(clampBoundary((long) caret - rowLength), extend);
					case DOWN, KP_DOWN -> nav.moveTo(clampBoundary((long) caret + rowLength), extend);
					case HOME -> nav.moveTo(caret - caret % rowLength, extend);
					case END -> nav.moveTo(clampBoundary((long) (caret - caret % rowLength) + rowLength), extend);
				}
			};

			@Nonnull
			@Override
			public HexAccess currentAccess() {
				return currentRead;
			}

			@Nonnull
			@Override
			public HexAccess originalAccess() {
				return originalRead;
			}

			@Nonnull
			@Override
			public HexNavigation navigation() {
				return navigation;
			}

			@Override
			public void refreshDisplay(int offset, boolean asciiOrigin) {
				revision++;
				refreshRowDisplay(offset);
				refreshSearchResults();
			}

			@Override
			public void engageCurrent() {
				engage((row, offset) -> row.engage(offset, true));
				if (!flow.isFocusWithin()) flow.requestFocus();
			}

			@Override
			public void cancelCurrent() {
				engage((row, offset) -> row.engage(offset, false));
				if (!flow.isFocusWithin()) flow.requestFocus();
			}

			@Override
			public void sendKeyToCurrentEngaged(@Nonnull KeyCode code) {
				engage((row, offset) -> row.sendKeyToCurrentEngaged(offset, code));
			}

			@Nonnull
			@Override
			public EventHandler<KeyEvent> keyListener() {
				return listener;
			}

			@Override
			public boolean isSearchMatch(int offset) {
				return searchBar != null && searchBar.isMatch(offset);
			}

			@Override
			public boolean isSearchMatchStart(int offset) {
				return searchBar != null && searchBar.isMatchStart(offset);
			}

			@Override
			public boolean isSearchMatchEnd(int offset) {
				return searchBar != null && searchBar.isMatchEnd(offset);
			}

			@Override
			public boolean isSearchMatchTop(int offset, int rowStart, int rowLength) {
				return searchBar != null && searchBar.isMatchTop(offset, rowStart, rowLength);
			}

			@Override
			public boolean isSearchMatchBottom(int offset, int rowStart, int rowLength) {
				return searchBar != null && searchBar.isMatchBottom(offset, rowStart, rowLength);
			}

			private void engage(CellConsumer consumer) {
				int currentOffset = selection.caret();
				if (data == null || currentOffset >= data.length)
					return;

				bringOffsetIntoView(currentOffset);
				Optional<HexRow> cellIfVisible = flow.getCellIfVisible(offsetToRowIndex(currentOffset));
				cellIfVisible.ifPresent(row -> consumer.accept(row, currentOffset));
			}

			interface CellConsumer {
				void accept(@Nonnull HexRow cell, int offset);
			}
		};
	}
}
