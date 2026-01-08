package software.coley.recaf.ui.control.richtext.inheritance;

import jakarta.annotation.Nonnull;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.LineContainer;
import software.coley.recaf.ui.control.richtext.linegraphics.LineGraphicFactory;
import software.coley.recaf.util.SVG;

import java.util.List;
import java.util.Objects;

/**
 * Graphic factory that adds overlays to line graphics indicating the inheritance status of declared methods on the line.
 *
 * @author Matt Coley
 * @see InheritanceTracking
 */
public class InheritanceGutterGraphicFactory extends AbstractLineGraphicFactory {
	private static final Logger logger = Logging.get(InheritanceGutterGraphicFactory.class);
	private final CellConfigurationService configurationService;
	private final Actions actions;
	private Editor editor;

	/**
	 * New graphic factory.
	 *
	 * @param configurationService
	 * 		Cell configuration service for fetching method graphics and such.
	 * @param actions
	 * 		Actions for navigation when clicking on inheritance items.
	 */
	public InheritanceGutterGraphicFactory(@Nonnull CellConfigurationService configurationService,
	                                       @Nonnull Actions actions) {
		super(LineGraphicFactory.P_LINE_INHERITANCES);

		this.configurationService = configurationService;
		this.actions = actions;
	}

	@Override
	public void install(@Nonnull Editor editor) {
		this.editor = editor;
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		this.editor = null;
	}

	@Override
	public void apply(@Nonnull LineContainer container, int paragraph) {
		InheritanceTracking inheritanceTracking = (InheritanceTracking) editor.getComponent(InheritanceTracking.COMPONENT_KEY);

		// Always null if no bracket tracking is registered for the editor.
		if (inheritanceTracking == null)
			return;

		// Add problem graphic overlay to lines with problems.
		int line = paragraph + 1;

		// Skip if there are no inheritance items at all.
		int directions = (int) inheritanceTracking.getAllItems().stream().map(Inheritance::getClass).distinct().count();
		if (directions == 0)
			return;

		// Separate by type.
		List<Inheritance> items = inheritanceTracking.getItemsOnLine(line);
		List<Inheritance.Parent> parents = items.stream()
				.filter(Inheritance.Parent.class::isInstance)
				.map(Inheritance.Parent.class::cast).toList();
		List<Inheritance.Child> children = items.stream()
				.filter(Inheritance.Child.class::isInstance)
				.map(Inheritance.Child.class::cast).toList();

		// Wrapper box that will always be a given size based on how many directions of inheritance there are.
		// This ensures lines with no inheritance still have the same gutter width as lines that do.
		HBox box = new HBox();
		box.setAlignment(Pos.CENTER_LEFT);
		box.setSpacing(2);
		box.setPadding(new Insets(0, 0, 0, 4));
		box.setMinWidth(6 + directions * 16);

		// Add inheritance icons.
		if (!parents.isEmpty()) {
			boolean isInterface = Objects.requireNonNull(parents.getFirst().path().getParent()).getValue().hasInterfaceModifier();
			Node node = SVG.ofIconFile(isInterface ? SVG.METHOD_IMPLEMENTING : SVG.METHOD_OVERRIDING);
			node.setCursor(Cursor.HAND);
			node.setOnMousePressed(e -> {
				ContextMenu menu = new ContextMenu();
				menu.getItems().add(new MenuItem("Parents:"));
				menu.setAutoHide(true);
				for (Inheritance.Parent parent : parents) {
					ClassMemberPathNode parentPath = parent.path();
					menu.getItems().add(new ActionMenuItem(configurationService.textOf(Objects.requireNonNull(parentPath.getParent())), configurationService.graphicOf(parentPath), () -> {
						try {
							actions.gotoDeclaration(parentPath);
						} catch (IncompletePathException ex) {
							logger.warn("Failed to navigate to parent method: {}", parentPath, ex);
						}
					}));
				}
				menu.show(box, e.getScreenX(), e.getScreenY());
			});
			box.getChildren().add(node);
		}
		if (!children.isEmpty()) {
			boolean isInterface = Objects.requireNonNull(children.getFirst().path().getParent()).getValue().hasInterfaceModifier();
			Node node = SVG.ofIconFile(isInterface ? SVG.METHOD_IMPLEMENTED : SVG.METHOD_OVERRIDDEN);
			node.setCursor(Cursor.HAND);
			node.setOnMousePressed(e -> {
				ContextMenu menu = new ContextMenu();
				menu.getItems().add(new MenuItem("Children:"));
				menu.setAutoHide(true);
				for (Inheritance.Child child : children) {
					ClassMemberPathNode childPath = child.path();
					menu.getItems().add(new ActionMenuItem(configurationService.textOf(Objects.requireNonNull(childPath.getParent())), configurationService.graphicOf(childPath), () -> {
						try {
							actions.gotoDeclaration(childPath);
						} catch (IncompletePathException ex) {
							logger.warn("Failed to navigate to child method: {}", childPath, ex);
						}
					}));
				}
				menu.show(box, e.getScreenX(), e.getScreenY());
			});
			box.getChildren().add(node);
		}

		container.addHorizontal(box);
	}
}
