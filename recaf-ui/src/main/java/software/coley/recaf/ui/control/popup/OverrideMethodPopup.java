package software.coley.recaf.ui.control.popup;

import atlantafx.base.controls.Spacer;
import jakarta.annotation.Nonnull;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.BasicMethodMember;
import software.coley.recaf.info.member.LocalVariable;
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
import software.coley.recaf.util.NameTypePair;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.kordamp.ikonli.carbonicons.CarbonIcons.CHECKMARK;
import static org.kordamp.ikonli.carbonicons.CarbonIcons.CLOSE;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_NATIVE;

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

		// Keyboard accept/cancel
		BooleanBinding disable = Bindings.createBooleanBinding(() -> {
			TreeItem<PathNode<?>> selectedItem = tree.getSelectionModel().getSelectedItem();
			return selectedItem == null || !(selectedItem.getValue() instanceof ClassMemberPathNode);
		}, tree.getSelectionModel().selectedItemProperty());
		BiConsumer<ClassInfo, MethodMember> overrideConsumer = (owner, method) -> memberConsumer.accept(owner, createOverrideMethod(targetClass, method));
		tree.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER && !disable.get()) {
				accept(overrideConsumer);
			} else if (e.getCode() == KeyCode.ESCAPE) {
				hide();
			}
		});

		// Setup tree contents
		InheritanceVertex vertex = inheritanceGraph.getVertex(targetClass.getName());
		if (vertex != null) {
			Map<InheritanceVertex, ClassPathNode> parentPaths = new LinkedHashMap<>();
			vertex.allParents().forEach(parent -> {
				ClassPathNode parentPath = workspace.findClass(parent.getName());
				if (parentPath != null)
					parentPaths.put(parent, parentPath);
			});

			Map<InheritanceVertex, List<MethodMember>> overrides = select(targetClass, parentPaths.keySet());
			overrides.forEach((parent, methods) -> {
				TreeItem<PathNode<?>> parentItem = new TreeItem<>(parentPaths.get(parent));
				rootItem.getChildren().add(parentItem);
				for (MethodMember method : methods)
					parentItem.getChildren().add(new TreeItem<>(parentPaths.get(parent).child(method)));
				parentItem.setExpanded(true);
			});
		}

		Button acceptButton = new ActionButton(new FontIconView(CHECKMARK, Color.LAWNGREEN), () -> accept(overrideConsumer));
		Button cancelButton = new ActionButton(new FontIconView(CLOSE, Color.RED), this::hide);
		acceptButton.disableProperty().bind(disable);
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

	/**
	 * @param targetClass
	 * 		Class receiving the overrides.
	 * @param parents
	 * 		Parent vertices, ordered from nearest to farthest.
	 *
	 * @return Eligible inherited methods grouped by their declaring parent.
	 * For duplicate signatures, only the nearest declaration is included.
	 */
	@Nonnull
	private static Map<InheritanceVertex, List<MethodMember>> select(@Nonnull ClassInfo targetClass,
	                                                                 @Nonnull Collection<InheritanceVertex> parents) {
		Set<NameTypePair> seen = new HashSet<>();
		for (MethodMember method : targetClass.getMethods())
			seen.add(NameTypePair.of(method));

		Map<InheritanceVertex, List<MethodMember>> selected = new LinkedHashMap<>();
		for (InheritanceVertex parent : parents) {
			List<MethodMember> methods = new ArrayList<>();
			ClassInfo parentClass = parent.getValue();
			for (MethodMember method : parentClass.getMethods()) {
				if (!isInheritedBy(targetClass, parentClass, method))
					continue;
				if (!seen.add(NameTypePair.of(method)))
					continue;
				if (method.hasStaticModifier() || method.hasFinalModifier())
					continue;
				methods.add(method);
			}
			if (!methods.isEmpty())
				selected.put(parent, methods);
		}
		return selected;
	}

	/**
	 * @param targetClass
	 * 		Class receiving the override.
	 * @param parentClass
	 * 		Class declaring the inherited method.
	 * @param method
	 * 		Inherited method to check.
	 *
	 * @return {@code true} when the method is inherited by the target class.
	 */
	private static boolean isInheritedBy(@Nonnull ClassInfo targetClass, @Nonnull ClassInfo parentClass,
	                                     @Nonnull MethodMember method) {
		String name = method.getName();
		if (name.equals("<init>") || name.equals("<clinit>") || method.hasPrivateModifier())
			return false;
		if (parentClass.hasInterfaceModifier() && method.hasStaticModifier())
			return false;
		return !method.hasPackagePrivateModifier() ||
				Objects.equals(targetClass.getPackageName(), parentClass.getPackageName());
	}

	/**
	 * @param targetClass
	 * 		Class that will receive the override.
	 * @param inheritedMethod
	 * 		Method to override.
	 *
	 * @return Concrete method member for the target class.
	 */
	@Nonnull
	private static MethodMember createOverrideMethod(@Nonnull ClassInfo targetClass, @Nonnull MethodMember inheritedMethod) {
		int access = inheritedMethod.getAccess() & ~(ACC_ABSTRACT | ACC_NATIVE);
		BasicMethodMember method = new BasicMethodMember(inheritedMethod.getName(), inheritedMethod.getDescriptor(),
				inheritedMethod.getSignature(), access, inheritedMethod.getThrownTypes());
		for (LocalVariable localVariable : inheritedMethod.getLocalVariables())
			method.addLocalVariable(localVariable);
		method.setDeclaringClass(targetClass);
		return method;
	}
}
