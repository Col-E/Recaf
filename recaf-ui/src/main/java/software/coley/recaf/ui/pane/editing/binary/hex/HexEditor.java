package software.coley.recaf.ui.pane.editing.binary.hex;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
			int currentOffset = ops.navigation().selectionOffset();
			for (HexRow cell : flow.visibleCells()) {
				Bounds bounds = cell.getNode().getBoundsInParent();
				if (bounds.contains(e.getX(), e.getY())) {
					int offset = cell.pickOffsetAtPosition(e.getX(), e.getY());
					if (offset >= 0) {
						if (currentOffset == offset) {
							ops.engageCurrent();
						} else {
							ops.navigation().select(offset);
						}
					}
				}
			}

			// Do not propagate the event up the scene graph.
			e.consume();
		});
		NodeEvents.addMouseReleaseHandler(flow, e -> {
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
	 * Refresh visible byte cells after search state changes.
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
		int offset = ops.navigation().selectionOffset();
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
		revision++;

		refreshRowDisplay();
		refreshSearchResults();
	}

	/**
	 * Reset the data content to what it was when initially calling {@link #setInitialData(byte[])}.
	 */
	public void resetData() {
		this.data = Arrays.copyOf(dataDefaultState, dataDefaultState.length);
		revision++;
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

	@Nonnull
	private HexOperations newHexOperations() {
		IntegerProperty focusedOffset = new SimpleIntegerProperty(0);
		BooleanProperty isHexColumActive = new SimpleBooleanProperty(true);
		HexAccess currentRead = () -> data;
		HexAccess originalRead = () -> dataDefaultState;
		HexNavigation navigation = new HexNavigation() {
			@Override
			public int selectionOffset() {
				return focusedOffset.get();
			}

			@Override
			public boolean isHexColumnSelected() {
				return isHexColumActive.get();
			}

			@Override
			public void switchColumns() {
				if (config.getShowAscii().getValue()) {
					// If we show both hex/ascii columns we want to toggle between the two and refresh the display.
					isHexColumActive.set(!isHexColumActive.get());
					refreshRowDisplay(focusedOffset.get());
				} else {
					// If we only show the hex column there is nothing to toggle between, and no reason to refresh.
					isHexColumActive.set(true);
				}
			}

			@Override
			public void select(int offset) {
				int max = currentRead.length();
				int clampedOffset = Math.clamp(offset, 0, max - 1);

				focusedOffset.setValue(clampedOffset);

				flow.visibleCells().forEach(c -> c.updateSelection(clampedOffset));

				bringOffsetIntoView(offset);
			}

			@Override
			public void selectNext() {
				select(focusedOffset.get() + 1);
			}

			@Override
			public void selectPrevious() {
				select(focusedOffset.get() - 1);
			}

			@Override
			public void selectDown() {
				select(focusedOffset.get() + config.getRowLength().getValue());
			}

			@Override
			public void selectUp() {
				select(focusedOffset.get() - config.getRowLength().getValue());
			}

			@Override
			public void selectDown(int rows) {
				select(focusedOffset.get() + config.getRowLength().getValue() * rows);
			}

			@Override
			public void selectUp(int rows) {
				select(focusedOffset.get() - config.getRowLength().getValue() * rows);
			}
		};
		return new HexOperations() {
			private final EventHandler<KeyEvent> listener = e -> {
				// If the focus is not in the editor, bring it in.
				if (!isFocusWithin()) flow.requestFocus();

				// Do not propagate the key-event up the scene graph.
				// Things like arrow keys and tabs if handled by parents can
				// unintentionally bring focus away from the editor component.
				e.consume();

				// Handle special keys.
				KeyCode code = e.getCode();
				HexNavigation nav = navigation();
				switch (code) {
					case ENTER -> engageCurrent();
					case ESCAPE -> cancelCurrent();
					case TAB -> nav.switchColumns();
					case PAGE_UP -> nav.selectUp(Math.max(1, flow.visibleCells().size() - 1));
					case PAGE_DOWN -> nav.selectDown(Math.max(1, flow.visibleCells().size() - 1));
					case RIGHT, KP_RIGHT -> nav.selectNext();
					case LEFT, KP_LEFT -> nav.selectPrevious();
					case UP, KP_UP -> nav.selectUp();
					case DOWN, KP_DOWN -> nav.selectDown();
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

				// Engage can initiate editing in a cell, or commit work-in-progress edits.
				// When WIP edits are committed, the text editor is swapped out of the scene
				// which can cause a loss of focus. We'll catch that and re-focus here.
				if (!flow.isFocusWithin()) flow.requestFocus();
			}

			@Override
			public void cancelCurrent() {
				engage((row, offset) -> row.engage(offset, false));

				// Same reasoning as with engage-current above.
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
			public boolean isActiveSearchMatch(int offset) {
				return searchBar != null && searchBar.isActiveMatch(offset);
			}

			private void engage(CellConsumer consumer) {
				int currentOffset = focusedOffset.get();

				bringOffsetIntoView(currentOffset);
				int rowLength = config.getRowLength().getValue();

				Optional<HexRow> cellIfVisible = flow.getCellIfVisible(offsetToRowIndex(currentOffset));
				if (cellIfVisible.isPresent()) {
					HexRow row = cellIfVisible.get();
					consumer.accept(row, currentOffset);
				}
			}

			interface CellConsumer {
				void accept(@Nonnull HexRow cell, int offset);
			}
		};
	}
}
