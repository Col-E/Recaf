package me.coley.recaf.ui.panel;

import com.fxgraph.cells.AbstractCell;
import com.fxgraph.edges.DoubleCorneredEdge;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.Model;
import com.fxgraph.layout.AbegoTreeLayout;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.graph.InheritanceVertex;
import me.coley.recaf.ui.context.ContextBuilder;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.Threads;
import org.abego.treelayout.Configuration;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A panel that displays the class hierarchy of any given class along with some general information about each class.
 *
 * @author Matt Coley
 */
public class ClassHierarchyPanel extends BorderPane {
	/**
	 * Create the panel from the initial root class.
	 *
	 * @param initialClass
	 * 		Initial class to use to search for the class hierarchy.
	 */
	public ClassHierarchyPanel(ClassInfo initialClass) {
		Graph graph = new Graph();
		graph.getNodeGestures().setDragButton(MouseButton.NONE);
		graph.getViewportGestures().setPanButton(MouseButton.PRIMARY);
		Model model = graph.getModel();
		graph.beginUpdate();
		Map<String, ClassCell> cellMap = new HashMap<>();
		InheritanceVertex root = RecafUI.getController().getServices()
				.getInheritanceGraph().getVertex(initialClass.getName());
		// Create cells
		int maxWidth = 100;
		int maxHeight = 100;
		for (InheritanceVertex classVertex : root.getFamily()) {
			ClassCell cell = new ClassCell(classVertex);
			cellMap.put(classVertex.getName(), cell);
			model.addCell(cell);
			if (cell.getGraphic().getWidth() > maxWidth) {
				maxWidth = (int) cell.getGraphic().getWidth();
			}
			if (cell.getGraphic().getHeight() > maxHeight) {
				maxHeight = (int) cell.getGraphic().getHeight();
			}
		}
		// Add edges
		for (InheritanceVertex classVertex : root.getFamily()) {
			ClassCell cell = cellMap.get(classVertex.getName());
			if (cell == null)
				continue;
			for (InheritanceVertex parentVertex : classVertex.getParents()) {
				ClassCell parentCell = cellMap.get(parentVertex.getName());
				if (parentCell == null)
					continue;
				DoubleCorneredEdge edge = new DoubleCorneredEdge(cell, parentCell, true, Orientation.VERTICAL);
				model.addEdge(edge);
			}
		}
		graph.endUpdate();
		setCenter(graph.getCanvas());
		// Resize later so graph elements can be created and their sizes can be used for the layout.
		int finalMaxHeight = maxHeight;
		int finalMaxWidth = maxWidth;
		Threads.runFxDelayed(25, () -> {
			ClassCell rootCell = cellMap.get(root.getName());
			graph.layout(new AbegoTreeLayout(finalMaxHeight, finalMaxWidth, Configuration.Location.Top));
			graph.getCanvas().setPivot(
					rootCell.getGraphic().getLayoutX(),
					rootCell.getGraphic().getLayoutY()
			);
		});
	}

	/**
	 * Graph cell representing a class.
	 */
	static class ClassCell extends AbstractCell {
		private final InheritanceVertex vertex;
		private ContextMenu menu;
		private Region graphic;

		/**
		 * @param vertex
		 * 		Inheritance node to represent.
		 */
		public ClassCell(InheritanceVertex vertex) {
			this.vertex = vertex;
		}

		@Override
		public Region getGraphic(Graph graph) {
			return getGraphic();
		}

		/**
		 * @return A graphic node that represents the class by displaying its members.
		 */
		public Region getGraphic() {
			if (graphic == null) {
				ClassInfo info = RecafUI.getController().getWorkspace().getResources().getClass(vertex.getName());
				int row = 0;
				int fieldRows = 0;
				int methodRows = 0;
				GridPane grid = new GridPane();
				grid.setVgap(5);
				grid.addRow(row++, createTitle());
				grid.addRow(row++, new Separator());
				grid.getStyleClass().add("hierarchy-node");
				GridPane fieldsGrid = new GridPane();
				fieldsGrid.setHgap(15);
				GridPane methodsGrid = new GridPane();
				methodsGrid.setHgap(15);
				for (FieldInfo field : info.getFields()) {
					fieldsGrid.addRow(fieldRows++, createField(field));
				}
				grid.addRow(row++, fieldsGrid);
				grid.addRow(row++, new Separator());
				for (MethodInfo method : info.getMethods()) {
					methodsGrid.addRow(methodRows++, createMethod(method));
				}
				grid.addRow(row++, methodsGrid);
				if (AccessFlag.isInterface(vertex.getValue().getAccess())) {
					grid.getStyleClass().add("hierarchy-interface");
				} else if (AccessFlag.isEnum(vertex.getValue().getAccess())) {
					grid.getStyleClass().add("hierarchy-enum");
				} else {
					grid.getStyleClass().add("hierarchy-class");
				}
				grid.setOnContextMenuRequested(e -> {
							if (menu == null)
								menu = ContextBuilder.forClass(info).build();
							else
								menu.hide();
							menu.show(grid, e.getScreenX(), e.getScreenY());
						}
				);
				graphic = grid;
			}
			return graphic;
		}


		/**
		 * @return Node to represent a class.
		 */
		private Node createTitle() {
			String shortName = vertex.getName().substring(vertex.getName().lastIndexOf("/") + 1);
			Label label = new Label(shortName);
			label.getStyleClass().addAll("h2", "b");
			label.setGraphic(new HBox(
					Icons.getClassIcon(vertex.getValue()),
					Icons.getVisibilityIcon(vertex.getValue().getAccess())
			));
			return label;
		}

		/**
		 * @param field
		 * 		Field information.
		 *
		 * @return Array of nodes to represent a field as a row in a {@link GridPane}.
		 */
		private Node[] createField(FieldInfo field) {
			String typeString = Type.getType(field.getDescriptor()).getClassName();
			Label name = new Label(field.getName());
			Label type = new Label(typeString.substring(typeString.lastIndexOf('.') + 1));
			HBox icons = new HBox(Icons.getFieldIcon(field), Icons.getVisibilityIcon(field.getAccess()));
			name.setAlignment(Pos.CENTER_LEFT);
			icons.setAlignment(Pos.CENTER_LEFT);
			type.setAlignment(Pos.CENTER_RIGHT);
			return new Node[]{icons, type, name};
		}

		/**
		 * @param method
		 * 		Method information.
		 *
		 * @return Array of nodes to represent a method as a row in a {@link GridPane}.
		 */
		private Node[] createMethod(MethodInfo method) {
			Type mtype = Type.getMethodType(method.getDescriptor());
			String retType = mtype.getReturnType().getClassName();
			Label name = new Label(method.getName());
			String typeString = retType.substring(retType.lastIndexOf('.') + 1) +
					" (" + Arrays.stream(mtype.getArgumentTypes())
					.map(t -> t.getClassName().substring(t.getClassName().lastIndexOf('.') + 1))
					.collect(Collectors.joining(", ")) +
					")";
			Label type = new Label(typeString);
			HBox icons = new HBox(Icons.getMethodIcon(method), Icons.getVisibilityIcon(method.getAccess()));
			name.setAlignment(Pos.CENTER_LEFT);
			icons.setAlignment(Pos.CENTER_LEFT);
			type.setAlignment(Pos.CENTER_RIGHT);
			return new Node[]{icons, name, type};
		}
	}
}
