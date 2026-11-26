package software.coley.recaf.services.info.summary;

import atlantafx.base.controls.Popover;
import jakarta.annotation.Nonnull;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;

import java.util.List;

/**
 * Utils for {@link AntiReversalResultPresenter} implementations.
 *
 * @author Matt Coley
 */
public class PresenterUtils {
	private static final Logger logger = Logging.get(PresenterUtils.class);

	private PresenterUtils() {}

	/**
	 * Combines two summary nodes with the standard spacing and alignment.
	 *
	 * @param left
	 * 		Left node.
	 * @param right
	 * 		Right node.
	 *
	 * @return Combined summary row.
	 */
	@Nonnull
	public static Node box(@Nonnull Node left, @Nonnull Node right) {
		HBox box = new HBox(left, right);
		box.setSpacing(10);
		box.setAlignment(Pos.CENTER_LEFT);
		return box;
	}

	/**
	 * Shows a popover listing classes.
	 *
	 * @param owner
	 * 		Hyperlink that owns the popover.
	 * @param classes
	 * 		Classes to display.
	 * @param cellConfigurationService
	 * 		Service for configuring class cells.
	 * @param actions
	 * 		Navigation actions.
	 */
	public static void showClassListPopover(@Nonnull Hyperlink owner,
	                                        @Nonnull List<ClassPathNode> classes,
	                                        @Nonnull CellConfigurationService cellConfigurationService,
	                                        @Nonnull Actions actions) {
		Popover popover = new Popover(createClassesList(classes, cellConfigurationService, actions));
		popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
		popover.setAutoHide(true);
		popover.show(owner);
	}

	@Nonnull
	private static Node createClassesList(@Nonnull List<ClassPathNode> classes,
	                                      @Nonnull CellConfigurationService cellConfigurationService,
	                                      @Nonnull Actions actions) {
		ListView<ClassPathNode> list = new ListView<>(FXCollections.observableArrayList(classes));
		list.setCellFactory(param -> new ListCell<>() {
			@Override
			protected void updateItem(ClassPathNode item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					setOnMouseClicked(null);
				} else {
					setText(cellConfigurationService.textOf(item));
					setGraphic(cellConfigurationService.graphicOf(item));
					setOnMouseClicked(_ -> {
						try {
							actions.gotoDeclaration(item);
						} catch (IncompletePathException ex) {
							// A complete class path should always be available here.
							logger.warn("Cannot goto location, path incomplete", ex);
						}
					});
				}
			}
		});

		// Limit the height of the list to 8 items, otherwise it can get very large.
		//  - Each cell is roughly 30px tall (at least on my system + default scaling).
		int height = Math.min(classes.size(), 8) * 30 + 2;
		list.setFocusTraversable(false);
		list.setPrefWidth(400);
		list.setPrefHeight(height);
		return list;
	}
}
