package software.coley.recaf.ui.pane.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.search.CancellableSearchFeedback;
import software.coley.recaf.services.search.SearchService;
import software.coley.recaf.services.search.query.Query;
import software.coley.recaf.services.search.result.Result;
import software.coley.recaf.services.search.result.Results;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.PathNodeTree;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.ui.control.tree.WorkspaceTreeNode;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.threading.Batch;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Common base capabilities for search panels.
 *
 * @author Matt Coley
 */
public abstract class AbstractSearchPane extends BorderPane implements Navigable {
	private final WorkspaceManager workspaceManager;
	private final SearchService searchService;
	private final CellConfigurationService configurationService;
	private final Actions actions;
	private final WorkspacePathNode workspacePath;
	protected final PathNodeTree liveResultsTree;
	protected final BooleanProperty liveResults = new SimpleBooleanProperty(true);
	private CancellableSearchFeedback lastSearchFeedback;

	/**
	 * Create the base outline of a search panel capabilities.
	 *
	 * @param workspaceManager
	 * 		Manager to pull current workspace from.
	 * @param searchService
	 * 		Search service to initiate searches with.
	 * @param configurationService
	 * 		Cell configuration service to stylize the output tree model.
	 * @param actions
	 * 		Action service to assist stylizing the output tree model.
	 */
	public AbstractSearchPane(@Nonnull WorkspaceManager workspaceManager,
	                          @Nonnull SearchService searchService,
	                          @Nonnull CellConfigurationService configurationService,
	                          @Nonnull Actions actions) {
		this.workspaceManager = workspaceManager;
		this.searchService = searchService;
		this.configurationService = configurationService;
		this.actions = actions;

		liveResultsTree = newTree();

		workspacePath = PathNodes.workspacePath(Objects.requireNonNull(workspaceManager.getCurrent(),
				"Cannot open search if no workspace is open"));
	}

	@Override
	public boolean isTrackable() {
		// We want this type to be navigable to benefit from automatic close support.
		return false;
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return workspacePath;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		cancelLastSearch();
		liveResultsTree.setRoot(null);
		getChildren().clear();
		setDisable(true);
	}

	/**
	 * Set up the UI with the given inputs.
	 *
	 * @param input
	 * 		Node to handle user input.
	 */
	protected void setInputs(@Nonnull Node input) {
		// TODO: It would be nice to have additional 'advanced' options
		//  - Which could implement the search-feedback 'doVisitX' methods to allow skipping over unwanted content

		setTop(input);
		setCenter(liveResultsTree);

		liveResults.addListener((ob, old, cur) -> {
			if (cur) {
				setCenter(liveResultsTree);
			} else {
				setCenter(null);
			}
		});
	}

	/**
	 * @return New path-node tree.
	 */
	@Nonnull
	protected PathNodeTree newTree() {
		PathNodeTree tree = new PathNodeTree(configurationService, actions);
		tree.setOnMousePressed(e -> {
			if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
				var item = tree.getSelectionModel().getSelectedItem();
				if (item != null && item.isLeaf()) {
					try {
						actions.gotoDeclaration(item.getValue());
					} catch (IncompletePathException ignored) {
						// ignored
					}
				}
			}
		});
		return tree;
	}

	/**
	 * @return The built query from current search inputs,
	 * or {@code null} if the inputs were invalid for any reason.
	 */
	@Nullable
	protected abstract Query buildQuery();

	/**
	 * Initiates the search with current search inputs. Updates the output display.
	 */
	protected final void search() {
		// Skip if the panel has been disabled (occurs when closing it).
		// Sometimes the delay between searching and the user closing will initiate a search after closing.
		if (isDisabled()) return;

		// Must have a current workspace to search in.
		if (!workspaceManager.hasCurrentWorkspace())
			return;

		// Create a new root.
		Workspace workspace = workspaceManager.getCurrent();
		PathNodeTree tree = liveResults.get() ? liveResultsTree : newTree();
		WorkspaceTreeNode root = new WorkspaceTreeNode(PathNodes.workspacePath(workspace));
		root.setExpanded(true);
		tree.setRoot(root);

		// Cancel last search before we start a new one.
		cancelLastSearch();

		// Skip if the query couldn't be built (invalid inputs most likely)
		Query query = buildQuery();
		if (query == null)
			return;

		// Run new search.
		CancellableSearchFeedback feedback;
		if (liveResults.get()) {
			feedback = new LiveOnlySearchFeedback(result -> {
				WorkspaceTreeNode node = WorkspaceTreeNode.getOrInsertIntoTree(root, result.getPath(), false);
				TreeItems.expandParents(node);
			});
			CompletableFuture.runAsync(() -> searchService.search(workspace, query, feedback));
		} else {
			feedback = new CancellableSearchFeedback();
			CompletableFuture.supplyAsync(() -> searchService.search(workspace, query, feedback))
					.thenAccept(this::handleSearchResults);
		}
		lastSearchFeedback = feedback;
	}

	/**
	 * Called when a search completes that is not {@link #liveResults live}.
	 *
	 * @param results
	 * 		Results of a non-live search.
	 */
	protected void handleSearchResults(@Nonnull Results results) {
		// TODO: Handle displaying the results for non-live search
		//  - put display in center, should be a PathNodeTree model, ideally dockable so user can move it around
		//  - maybe have the toggle for "[x] live" be an overlay like the "(i)" in decompile UI
	}

	/**
	 * Stops the prior search.
	 */
	private void cancelLastSearch() {
		if (lastSearchFeedback != null) {
			lastSearchFeedback.cancel();
			lastSearchFeedback = null;
		}
	}

	/**
	 * Feedback that incrementally updates the search results tree.
	 * <br>
	 * Disables the collection of results into a single wrapper at the end of a search.
	 * Since this is for live-only feedback, we won't use the resulting collection anyways, so we don't need to do
	 * the extra work.
	 */
	private static class LiveOnlySearchFeedback extends CancellableSearchFeedback {
		private final Batch batch = FxThreadUtil.batch();
		private final AnimationTimer batchTimer = new AnimationTimer() {
			private static final long BATCH_INTERVAL_MS = 1000 / 20;
			private long last;

			@Override
			public void handle(long now) {
				if (now - last > BATCH_INTERVAL_MS) {
					publishResults();
					last = now;
				}
			}
		};
		private final Consumer<Result<?>> resultConsumer;

		private LiveOnlySearchFeedback(@Nonnull Consumer<Result<?>> resultConsumer) {
			this.resultConsumer = resultConsumer;
			batchTimer.start();
		}

		@Override
		public boolean doAcceptResult(@Nonnull Result<?> result) {
			batch.add(() -> resultConsumer.accept(result));
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return false;
		}

		@Override
		public void onCompletion() {
			batchTimer.stop();
			publishResults();
		}

		private void publishResults() {
			batch.execute();
		}
	}
}
