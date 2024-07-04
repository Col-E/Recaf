package software.coley.recaf.ui.control.graph;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.collections.Lists;
import software.coley.collections.Unchecked;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.callgraph.CallGraph;
import software.coley.recaf.services.callgraph.MethodVertex;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tree display of method calls.
 *
 * @author Amejonah
 */
public class MethodCallGraphPane extends BorderPane implements ClassNavigable, UpdatableNavigable {
	public static final int MAX_TREE_DEPTH = 20;
	private final ObjectProperty<MethodMember> currentMethod = new SimpleObjectProperty<>();
	private final CallGraphTreeView graphTreeView = new CallGraphTreeView();
	private final CellConfigurationService configurationService;
	private final TextFormatConfig format;
	private final CallGraph callGraph;
	private final CallGraphMode mode;
	private final Workspace workspace;
	private final Actions actions;
	private ClassPathNode path;

	public MethodCallGraphPane(@Nonnull Workspace workspace, @Nonnull CallGraph callGraph, @Nonnull CellConfigurationService configurationService,
	                           @Nonnull TextFormatConfig format, @Nonnull Actions actions, @Nonnull CallGraphMode mode,
	                           @Nullable ObjectProperty<MethodMember> methodInfoObservable) {
		this.configurationService = configurationService;
		this.workspace = workspace;
		this.callGraph = callGraph;
		this.actions = actions;
		this.format = format;
		this.mode = mode;

		currentMethod.addListener((ob, old, cur) -> graphTreeView.onUpdate());
		graphTreeView.onUpdate();

		setCenter(graphTreeView);

		if (methodInfoObservable != null) currentMethod.bindBidirectional(methodInfoObservable);
	}

	@Nullable
	@Override
	public PathNode<?> getPath() {
		return getClassPath();
	}

	@Nonnull
	@Override
	public ClassPathNode getClassPath() {
		return path;
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof ClassPathNode classPath)
			this.path = classPath;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		graphTreeView.setRoot(null);
	}

	@Override
	public void requestFocus(@Nonnull ClassMember member) {
		// no-op
	}

	public enum CallGraphMode {
		CALLS(MethodVertex::getCalls),
		CALLERS(MethodVertex::getCallers);

		private final Function<MethodVertex, Collection<MethodVertex>> childrenGetter;

		CallGraphMode(@Nonnull Function<MethodVertex, Collection<MethodVertex>> getCallers) {
			childrenGetter = getCallers;
		}
	}

	/**
	 * Item of a class in the hierarchy.
	 */
	private static class CallGraphItem extends TreeItem<MethodMember> implements Comparable<CallGraphItem> {
		private static final Comparator<String> comparator = CaseInsensitiveSimpleNaturalComparator.getInstance();
		boolean recursive;

		private CallGraphItem(@Nonnull MethodMember method, boolean recursive) {
			super(method);
			this.recursive = recursive;
		}

		@Nullable
		private ClassInfo getDeclaringClass() {
			return getValue().getDeclaringClass();
		}

		@Override
		public int compareTo(CallGraphItem o) {
			// We want the tree display to have items in sorted order by
			//    package > class > method-name > method-args
			int cmp = 0;
			MethodMember method = getValue();
			MethodMember otherMethod = o.getValue();
			ClassInfo declaringClass = getDeclaringClass();
			ClassInfo otherDeclaringClass = o.getDeclaringClass();
			if (declaringClass != null)
				cmp = comparator.compare(declaringClass.getName(), otherDeclaringClass.getName());
			if (cmp == 0)
				cmp = comparator.compare(method.getName(), otherMethod.getName());
			if (cmp == 0)
				cmp = comparator.compare(method.getDescriptor(), otherMethod.getDescriptor());
			return cmp;
		}
	}

	/**
	 * Cell of a class in the hierarchy.
	 */
	class CallGraphCell extends TreeCell<MethodMember> {
		private EventHandler<MouseEvent> onClickFilter;

		private CallGraphCell() {
			getStyleClass().addAll("code-area", "transparent-cell");
		}

		@Override
		protected void updateItem(MethodMember method, boolean empty) {
			super.updateItem(method, empty);
			if (empty || method == null) {
				setText(null);
				setGraphic(null);
				setOnMouseClicked(null);
				setContextMenu(null);
				if (onClickFilter != null)
					removeEventFilter(MouseEvent.MOUSE_PRESSED, onClickFilter);
				setOpacity(1);
			} else {
				onClickFilter = null;

				ClassInfo declaringClass = method.getDeclaringClass();
				if (declaringClass == null)
					return;

				ClassPathNode ownerPath = workspace.findClass(declaringClass.getName());
				if (ownerPath == null)
					return;

				ClassMemberPathNode methodPath = ownerPath.child(method);

				String methodOwnerName = declaringClass.getName();
				Text classText = new Text(format.filter(methodOwnerName, false, true, true));
				classText.setFill(Color.CADETBLUE);

				Text methodText = new Text(method.getName());
				if (method.hasStaticModifier()) methodText.setFill(Color.LIGHTGREEN);
				else methodText.setFill(Color.YELLOW);

				// Layout
				TextFlow textFlow = new TextFlow(classText, new Label("#"), methodText, new Label(method.getDescriptor()));
				HBox box = new HBox(configurationService.graphicOf(methodPath), textFlow);
				box.setSpacing(5);
				if (getTreeItem() instanceof CallGraphItem i && i.recursive) {
					box.getChildren().add(new FontIconView(CarbonIcons.CODE_REFERENCE));
					box.setOpacity(0.4);
				}
				setGraphic(box);

				// Context menu support
				ContextMenu contextMenu = configurationService.contextMenuOf(ContextSource.REFERENCE, methodPath);
				MenuItem focusItem = new MenuItem();
				focusItem.setGraphic(new FontIconView(CarbonIcons.CI_3D_CURSOR_ALT));
				focusItem.textProperty().bind(Lang.getBinding("menu.view.methodcallgraph.focus"));
				focusItem.setOnAction(e -> currentMethod.set(method));
				contextMenu.getItems().add(1, focusItem);
				setContextMenu(contextMenu);

				// Override the double click behavior to open the class. Doesn't work using the "setOn..." methods.
				onClickFilter = e -> {
					if (e.getButton().equals(MouseButton.PRIMARY) && e.getClickCount() >= 2) {
						e.consume();
						try {
							actions.gotoDeclaration(ownerPath).requestFocus(method);
						} catch (IncompletePathException ex) {
							// TODO: Log error
						}
					}
				};
				addEventFilter(MouseEvent.MOUSE_PRESSED, onClickFilter);
			}
		}

		public MethodMember getCurrentMethod() {
			return currentMethod.get();
		}

		public ObjectProperty<MethodMember> currentMethodProperty() {
			return currentMethod;
		}
	}

	private class CallGraphTreeView extends TreeView<MethodMember> {
		public CallGraphTreeView() {
			getStyleClass().add("transparent-tree");
			setCellFactory(param -> new CallGraphCell());
		}

		public void onUpdate() {
			final MethodMember methodInfo = currentMethod.get();
			if (methodInfo == null) {
				setRoot(null);
			} else {
				CompletableFuture.supplyAsync(() -> {
					while (!callGraph.isReady().getValue()) Unchecked.run(() -> Thread.sleep(100));
					return buildCallGraph(methodInfo, mode.childrenGetter);
				}).thenAcceptAsync(root -> {
					root.setExpanded(true);
					setRoot(root);
				}, FxThreadUtil.executor());
			}
		}

		@Nonnull
		private CallGraphItem buildCallGraph(@Nonnull MethodMember rootMethod, @Nonnull Function<MethodVertex, Collection<MethodVertex>> childrenGetter) {
			ArrayDeque<MethodMember> visitedMethods = new ArrayDeque<>();
			ArrayDeque<List<CallGraphItem>> workingStack = new ArrayDeque<>();
			CallGraphItem root = new CallGraphItem(rootMethod, false);
			workingStack.push(new ArrayList<>(Set.of(root)));
			int depth = 0;
			while (!workingStack.isEmpty()) {
				List<CallGraphItem> todo = workingStack.peek();
				if (!todo.isEmpty()) {
					final CallGraphItem item = todo.removeLast();
					if (item.recursive)
						continue;
					visitedMethods.push(item.getValue());
					depth++;
					final MethodVertex vertex = callGraph.getVertex(item.getValue());
					if (vertex != null) {
						final List<CallGraphItem> newTodo = childrenGetter.apply(vertex).stream()
								.filter(c -> c.getResolvedMethod() != null)
								.map(c -> {
									MethodMember cm = c.getResolvedMethod();
									return new CallGraphItem(cm, visitedMethods.contains(cm));
								})
								.filter(i -> {
									if (i.getValue() == null) return false;
									int insert = Lists.sortedInsertIndex(Unchecked.cast(item.getChildren()), i);
									item.getChildren().add(insert, i);
									return !i.recursive;
								}).collect(Collectors.toList());
						if (!newTodo.isEmpty() && depth < MAX_TREE_DEPTH) {
							workingStack.push(newTodo);
						} else visitedMethods.pop();
					}
					continue;
				}
				workingStack.pop();
				if (!visitedMethods.isEmpty()) visitedMethods.pop();
				depth--;
			}
			return root;
		}
	}
}
