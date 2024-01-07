package software.coley.recaf.ui.pane.editing;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;

/**
 * Overlay component for {@link Editor} that allows quick display of class information.
 *
 * @author Matt Coley
 */
public abstract class AbstractClassInfoProvider<T extends ClassInfo> extends Button {
	private final ClassNavigable classProvider;

	/**
	 * @param toolsContainer
	 * 		Container to house tool buttons for display in the {@link Editor}.
	 * @param classProvider
	 * 		The provider of the latest class info.
	 */
	public AbstractClassInfoProvider(@Nonnull ToolsContainerComponent toolsContainer, @Nonnull ClassNavigable classProvider) {
		this.classProvider = classProvider;
		setGraphic(new FontIconView(CarbonIcons.INFORMATION));
		getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT);
		setOnAction(this::showClassInfoPopover);
		toolsContainer.add(this);
	}

	@SuppressWarnings("unchecked")
	private void showClassInfoPopover(@Nonnull ActionEvent e) {
		ClassPathNode path = classProvider.getClassPath();
		ClassInfo info = path.getValue();
		Popover popover = new Popover(createInfoContent((T) info));
		popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
		popover.show(this);
	}

	@Nonnull
	protected abstract Node createInfoContent(@Nonnull T info);
}
