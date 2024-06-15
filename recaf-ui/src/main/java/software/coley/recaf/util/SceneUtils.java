package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import software.coley.recaf.ui.docking.DockingRegion;
import software.coley.recaf.ui.docking.DockingTab;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Scene utilities.
 *
 * @author Matt Coley
 */
public class SceneUtils {
	private SceneUtils() {}

	/**
	 * @param node
	 * 		Node within a target {@link Scene} to bring to front/focus.
	 */
	public static void focus(@Nullable Node node) {
		while (node != null) {
			// Get the parent of the node, skip the intermediate 'content area' from tab-pane default skin.
			Parent parent = node.getParent();
			if (parent != null && parent.getStyleClass().contains("tab-content-area"))
				parent = parent.getParent();

			// If the tab content is the node, select it and return.
			if (parent instanceof DockingRegion tabParent) {
				Scene scene = parent.getScene();
				for (DockingTab tab : tabParent.getDockTabs())
					if (tab.getContent() == node) {
						tab.select();
						SceneUtils.focus(scene);
						return;
					}
			}

			// Next parent.
			node = parent;
		}
	}

	/**
	 * @param scene
	 * 		Scene to bring to front/focus.
	 */
	public static void focus(@Nullable Scene scene) {
		if (scene == null)
			return;

		Window window = scene.getWindow();
		if (window.isFocused()) return;

		if (window instanceof Stage stage) {
			// If minified, unminify it.
			stage.setIconified(false);
			stage.show();

			// The method 'stage.toFront()' does not work as you'd expect so this hack is how we
			// force the window to the front.
			stage.setAlwaysOnTop(true);
			stage.setAlwaysOnTop(false);
		}
		window.requestFocus();
	}

	/**
	 * @param node
	 * 		Node to search hierarchy of.
	 * @param parentType
	 * 		Parent type to check for.
	 * @param <T>
	 * 		Parent type.
	 *
	 * @return Matching parent node in hierarchy, or {@code null} if nothing matched.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public static <T extends Node> T getParentOfType(@Nonnull Node node, @Nonnull Class<T> parentType) {
		Parent parent = node.getParent();
		while (parent != null) {
			if (parent.getClass().isAssignableFrom(parentType)) return (T) parent;
			parent = parent.getParent();
		}
		return null;
	}

	/**
	 * @param node
	 * 		Node to search hierarchy of.
	 * @param parentType
	 * 		Parent type to check for.
	 * @param <T>
	 * 		Parent type.
	 *
	 * @return Future containing the matching parent node in hierarchy, or {@code null} if nothing matched.
	 */
	@Nonnull
	public static <T extends Node> CompletableFuture<T> getParentOfTypeLater(@Nonnull Node node, @Nonnull Class<T> parentType) {
		return whenAddedToSceneMap(node, (n) -> getParentOfType(n, parentType));
	}

	/**
	 * @param node
	 * 		Node to initiate query with.
	 * @param function
	 * 		Function taking in the node and yielding some value. To be run when the node has a {@link Scene} associated with it.
	 * @param <T>
	 * 		Function return type.
	 * @param <N>
	 * 		Node type.
	 *
	 * @return Future of function lookup.
	 */
	@Nonnull
	public static <T, N extends Node> CompletableFuture<T> whenAddedToSceneMap(@Nonnull N node, @Nonnull Function<N, T> function) {
		// If added to the UI, immediately look up value.
		if (node.getScene() != null) return CompletableFuture.completedFuture(function.apply(node));

		// When there is no scene it is not added to the UI yet.
		// We want to wait for it to be added before calling the function.
		CompletableFuture<T> future = new CompletableFuture<>();
		node.sceneProperty().addListener(new ChangeListener<>() {
			@Override
			public void changed(ObservableValue<? extends Scene> observable, Scene prior, Scene current) {
				node.sceneProperty().removeListener(this);
				future.complete(function.apply(node));
			}
		});
		return future;
	}

	/**
	 * @param node
	 * 		Node to initiate query with.
	 * @param consumer
	 * 		Consumer taking in the node. To be run when the node has a {@link Scene} associated with it.
	 * @param <N>
	 * 		Node type.
	 */
	public static <N extends Node> void whenAddedToSceneConsume(@Nonnull N node, @Nonnull Consumer<N> consumer) {
		// If added to the UI, immediately call the consumer.
		if (node.getScene() != null) {
			consumer.accept(node);
			return;
		}

		// When there is no scene it is not added to the UI yet.
		// We want to wait for it to be added before calling the consumer.
		node.sceneProperty().addListener(new ChangeListener<>() {
			@Override
			public void changed(ObservableValue<? extends Scene> observable, Scene prior, Scene current) {
				node.sceneProperty().removeListener(this);
				consumer.accept(node);
			}
		});
	}
}
