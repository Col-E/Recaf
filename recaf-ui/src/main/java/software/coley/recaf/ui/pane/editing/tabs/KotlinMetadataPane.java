package software.coley.recaf.ui.pane.editing.tabs;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.ui.config.MemberDisplayFormatConfig;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.kotlin.model.KtClass;
import software.coley.recaf.util.kotlin.model.KtConstructor;
import software.coley.recaf.util.kotlin.model.KtElement;
import software.coley.recaf.util.kotlin.model.KtFunction;
import software.coley.recaf.util.kotlin.model.KtParameter;
import software.coley.recaf.util.kotlin.model.KtProperty;
import software.coley.recaf.util.kotlin.model.KtType;
import software.coley.recaf.util.kotlin.model.KtVariable;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Displays fields, methods and other subcomponents of a {@link ClassInfo}.
 *
 * @author Matt Coley
 */
@Dependent
public class KotlinMetadataPane extends BorderPane implements Navigable {
	private final TreeView<KtElement> tree = new TreeView<>();
	private boolean navigationLock;
	private ClassPathNode path;

	@Inject
	public KotlinMetadataPane(@Nonnull MemberDisplayFormatConfig memberFormatConfig,
	                          @Nonnull TextFormatConfig formatConfig,
	                          @Nonnull Actions actions) {
		tree.setShowRoot(true);
		tree.getStyleClass().addAll(Tweaks.EDGE_TO_EDGE, Styles.DENSE);
		tree.setCellFactory(param -> new TreeCell<>() {
			@Override
			protected void updateItem(KtElement element, boolean empty) {
				super.updateItem(element, empty);
				if (empty || element == null) {
					setText(null);
					setGraphic(null);
				} else {
					switch (element) {
						case KtClass ktClass -> {
							setText(formatConfig.filter(ktClass.getName()));
							setGraphic(Icons.getIconView(Icons.CLASS));
						}
						case KtFunction ktFunction -> {
							String name = Objects.requireNonNullElse(ktFunction.getName(), "?");
							String descriptor = KtType.toDescriptor(ktFunction);
							setText(memberFormatConfig.getMethodDisplay(name, descriptor));
							setGraphic(Icons.getIconView(Icons.METHOD));
						}
						case KtVariable ktVariable -> {
							String name = Objects.requireNonNullElse(ktVariable.getName(), "?");
							String descriptor = KtType.toDescriptor(ktVariable);
							setText(memberFormatConfig.getFieldDisplay(name, descriptor));
							if (ktVariable instanceof KtParameter) {
								setGraphic(Icons.getIconView(Icons.PRIMITIVE));
							} else if (ktVariable instanceof KtProperty) {
								setGraphic(Icons.getIconView(Icons.FIELD));
							} else {
								setGraphic(Icons.getIconView(Icons.FIELD));
							}
						}
					}
				}
			}
		});
		Label label = new BoundLabel(Lang.getBinding("kotlinmetadata.orderwarning"));
		label.getStyleClass().add(Styles.TEXT_SUBTLE);
		label.setPadding(new Insets(8));
		label.setWrapText(true);
		BorderPane labelWrapper = new BorderPane(label);
		labelWrapper.getStyleClass().add("workspace-filter-pane"); // Style used for top-border separator
		setCenter(tree);
		setBottom(labelWrapper);
	}

	/**
	 * @param metadata Metadata contents to display.
	 */
	public void setMetadata(@Nullable KtClass metadata) {
		if (metadata == null) {
			tree.setRoot(null);
			return;
		}
		TreeItem<KtElement> root = new TreeItem<>(metadata);
		for (KtProperty property : metadata.getProperties()) {
			TreeItem<KtElement> propertyTree = new TreeItem<>(property);
			root.getChildren().add(propertyTree);
		}
		for (KtConstructor constructor : metadata.getConstructors()) {
			TreeItem<KtElement> constructorTree = new TreeItem<>(constructor);
			for (KtVariable parameter : constructor.getParameters()) {
				TreeItem<KtElement> parameterTree = new TreeItem<>(parameter);
				constructorTree.getChildren().add(parameterTree);
			}
			root.getChildren().add(constructorTree);
		}
		for (KtFunction function : metadata.getFunctions()) {
			TreeItem<KtElement> constructorTree = new TreeItem<>(function);
			for (KtVariable parameter : function.getParameters()) {
				TreeItem<KtElement> parameterTree = new TreeItem<>(parameter);
				constructorTree.getChildren().add(parameterTree);
			}
			root.getChildren().add(constructorTree);
		}
		root.setExpanded(true);
		tree.setRoot(root);
	}

	@Nullable
	@Override
	public PathNode<?> getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		setDisable(true);
		tree.setRoot(null);
	}
}
