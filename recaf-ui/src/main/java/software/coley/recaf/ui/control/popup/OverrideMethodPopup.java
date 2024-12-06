package software.coley.recaf.ui.control.popup;

import atlantafx.base.controls.Spacer;
import jakarta.annotation.Nonnull;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.PathNodeTree;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

import java.util.function.BiConsumer;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.CHECKMARK;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CLOSE;

/**
 * Popup to show a selection of method to override from parent classes.
 *
 * @author Matt Coley
 */
public class OverrideMethodPopup extends RecafStage {
	private final PathNodeTree tree;

	public OverrideMethodPopup(@Nonnull Actions actions, @Nonnull CellConfigurationService configurationService,
	                           @Nonnull InheritanceGraph inheritanceGraph, @Nonnull Workspace workspace,
	                           @Nonnull ClassInfo targetClass, @Nonnull BiConsumer<ClassInfo, MethodMember> memberConsumer) {
		WorkspacePathNode rootPath = PathNodes.workspacePath(workspace);
		TreeItem<PathNode<?>> rootItem = new TreeItem<>(rootPath);
		tree = new PathNodeTree(configurationService, actions);
		tree.setShowRoot(false);
		tree.setRoot(rootItem);
		tree.getStyleClass().add("border-muted");

		// Setup tree contents
		InheritanceVertex vertex = inheritanceGraph.getVertex(targetClass.getName());
		if (vertex != null) {
			vertex.allParents().forEach(parent -> {
				ClassPathNode parentPath = workspace.findClass(parent.getName());
				if (parentPath == null)
					return;
				TreeItem<PathNode<?>> parentItem = new TreeItem<>(parentPath);
				rootItem.getChildren().add(parentItem);
				for (MethodMember method : parent.getValue().getMethods()) {
					if (method.hasPrivateModifier() || method.hasFinalModifier() || method.hasNativeModifier())
						continue;
					parentItem.getChildren().add(new TreeItem<>(parentPath.child(method)));
				}
				parentItem.setExpanded(true);
			});
		}

		Button acceptButton = new ActionButton(new FontIconView(CHECKMARK, Color.LAWNGREEN), () -> accept(memberConsumer));
		Button cancelButton = new ActionButton(new FontIconView(CLOSE, Color.RED), this::hide);
		acceptButton.disableProperty().bind(tree.getSelectionModel().selectedItemProperty().map(i -> !(i.getValue() instanceof ClassMemberPathNode)));
		HBox buttons = new HBox(acceptButton, new Spacer(), cancelButton);
		VBox layout = new VBox(tree, buttons);
		layout.setSpacing(10);
		layout.setAlignment(Pos.TOP_CENTER);
		layout.setPadding(new Insets(10));
		setMinWidth(500);
		setMinHeight(230);
		setMaxHeight(460);
		titleProperty().bind(Lang.getBinding("dialog.title.override-method"));
		setScene(new RecafScene(layout, 500, 350));
	}

	private void accept(@Nonnull BiConsumer<ClassInfo, MethodMember> memberConsumer) {
		TreeItem<PathNode<?>> selectedItem = tree.getSelectionModel().getSelectedItem();
		if (selectedItem != null && selectedItem.getValue() instanceof ClassMemberPathNode memberPath) {
			ClassInfo owner = memberPath.getValueOfType(ClassInfo.class);
			MethodMember method = (MethodMember) memberPath.getValue();
			if (owner != null)
				memberConsumer.accept(owner, method);
		}
		hide();
	}
}
