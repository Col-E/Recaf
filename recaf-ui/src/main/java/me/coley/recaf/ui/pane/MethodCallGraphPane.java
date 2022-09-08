package me.coley.recaf.ui.pane;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.graph.call.CallGraphRegistry;
import me.coley.recaf.graph.call.CallGraphVertex;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.behavior.Updatable;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MethodCallGraphPane extends BorderPane implements Updatable<CommonClassInfo> {
	public static final int MAX_TREE_DEPTH = 20;
	private final Workspace workspace;
	private CommonClassInfo classInfo;
	private final ObjectProperty<MethodInfo> currentMethod = new SimpleObjectProperty<>();
	private final CallGraphTreeView graphTreeView = new CallGraphTreeView();
	private final CallGraphMode mode;

	public enum CallGraphMode {
		CALLS(CallGraphVertex::getCalls),
		CALLERS(CallGraphVertex::getCallers);

		private final Function<CallGraphVertex, Collection<CallGraphVertex>> childrenGetter;

		CallGraphMode(Function<CallGraphVertex, Collection<CallGraphVertex>> getCallers) {
			childrenGetter = getCallers;
		}
	}

	public MethodCallGraphPane(Workspace workspace, CallGraphMode mode) {
		this.mode = mode;
		this.workspace = workspace;
		currentMethod.addListener((ChangeListener<? super MethodInfo>) this::onUpdateMethod);
		graphTreeView.onUpdate(classInfo);
		setCenter(graphTreeView);
	}

	private void onUpdateMethod(
			ObservableValue<? extends MethodInfo> observable,
			MethodInfo oldValue,
			MethodInfo newValue
	) {
		graphTreeView.onUpdate(classInfo);
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		classInfo = newValue;

	}

	private class CallGraphTreeView extends TreeView<MethodInfo> implements Updatable<CommonClassInfo> {

		public CallGraphTreeView() {
			getStyleClass().add("transparent-tree");
			setCellFactory(param -> new CallGraphCell());
		}

		@Override
		public void onUpdate(CommonClassInfo newValue) {
			CallGraphRegistry callGraph = RecafUI.getController().getServices().getCallGraphRegistry();
			final MethodInfo methodInfo = currentMethod.get();
			if (methodInfo == null) setRoot(null);
			else ThreadUtil.run(() -> {
				CallGraphItem root = buildCallGraph(methodInfo, callGraph, mode.childrenGetter);
				root.setExpanded(true);
				FxThreadUtil.run(() -> setRoot(root));
			});
		}

		private CallGraphItem buildCallGraph(MethodInfo rootMethod, CallGraphRegistry callGraph, Function<CallGraphVertex, Collection<CallGraphVertex>> childrenGetter) {
			ArrayDeque<MethodInfo> visitedMethods = new ArrayDeque<>();
			ArrayDeque<List<CallGraphItem>> workingStack = new ArrayDeque<>();
			CallGraphItem root;
			workingStack.push(new ArrayList<>(Set.of(root = new CallGraphItem(rootMethod, false))));
			int depth = 0;
			while (!workingStack.isEmpty()) {
				List<CallGraphItem> todo = workingStack.peek();
				if (!todo.isEmpty()) {
					final CallGraphItem item = todo.remove(todo.size() - 1);
					if (item.recursive) continue;
					visitedMethods.push(item.getValue());
					depth++;
					final CallGraphVertex vertex = callGraph.getVertex(item.getValue());
					if (vertex != null) {
						final List<CallGraphItem> newTodo = childrenGetter.apply(vertex)
								.stream().map(CallGraphVertex::getMethodInfo)
								.map(c -> new CallGraphItem(c, visitedMethods.contains(c)))
								.filter(i -> {
									if (i.getValue() == null) return false;
									item.getChildren().add(i);
									return !i.recursive;
								}).collect(Collectors.toList());
						if (!newTodo.isEmpty() && depth < MAX_TREE_DEPTH) {
							workingStack.push(newTodo);
						} else visitedMethods.pop();
					}
					continue;
				}
				workingStack.pop();
				if(!visitedMethods.isEmpty()) visitedMethods.pop();
				depth--;
			}
			return root;
		}
	}


	/**
	 * Item of a class in the hierarchy.
	 */
	static class CallGraphItem extends TreeItem<MethodInfo> {
		boolean recursive;

		private CallGraphItem(MethodInfo info, boolean recursive) {
			super(info);
			this.recursive = recursive;
		}

		private CallGraphItem(MethodInfo info) {
			this(info, false);
		}
	}

	/**
	 * Cell of a class in the hierarchy.
	 */
	class CallGraphCell extends TreeCell<MethodInfo> {
		private EventHandler<MouseEvent> onClickFilter;

		private CallGraphCell() {
			getStyleClass().add("transparent-cell");
			getStyleClass().add("monospace");
		}

		@Override
		protected void updateItem(MethodInfo item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
				setOnMouseClicked(null);
				if (onClickFilter != null)
					removeEventFilter(MouseEvent.MOUSE_PRESSED, onClickFilter);
			} else {
				onClickFilter = null;
				Text classText = new Text(TextDisplayUtil.escapeShortenPath(item.getOwner()));
				classText.setFill(Color.CADETBLUE);
				Text methodText = new Text(item.getName());
				if (AccessFlag.isStatic(item.getAccess())) methodText.setFill(Color.GREEN);
				else methodText.setFill(Color.YELLOW);
				HBox box = new HBox(Icons.getMethodIcon(item), new TextFlow(classText, new Label("#"), methodText, new Label(item.getDescriptor())));
				box.setSpacing(5);
				if (getTreeItem() instanceof CallGraphItem && ((CallGraphItem) getTreeItem()).recursive)
					box.getChildren().add(Icons.getIconView(Icons.REFERENCE));
				setGraphic(box);
				//				setText(TextDisplayUtil.escapeShortenPath(item.getOwner()) + "#" + item.getName());
				ClassInfo classInfo = workspace.getResources().getClass(item.getOwner());
				if (classInfo != null) {
					setContextMenu(ContextBuilder.forMethod(classInfo, item).setDeclaration(false).build());
					// Override the double click behavior to open the class. Doesn't work using the "setOn..." methods.
					onClickFilter = (MouseEvent e) -> {
						if (e.getClickCount() >= 2 && e.getButton().equals(MouseButton.PRIMARY)) {
							e.consume();
							CommonUX.openMember(classInfo, item);
						}
					};
					addEventFilter(MouseEvent.MOUSE_PRESSED, onClickFilter);
				}
			}
		}
	}

	public MethodInfo getCurrentMethod() {
		return currentMethod.get();
	}

	public ObjectProperty<MethodInfo> currentMethodProperty() {
		return currentMethod;
	}
}
