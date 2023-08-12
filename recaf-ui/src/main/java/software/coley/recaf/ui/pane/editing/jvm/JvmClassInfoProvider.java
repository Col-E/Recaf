package software.coley.recaf.ui.pane.editing.jvm;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.pane.editing.AbstractClassInfoProvider;
import software.coley.recaf.ui.pane.editing.ToolsContainerComponent;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.util.Lang;

import java.util.Objects;

/**
 * Overlay component for {@link Editor} that allows quick display of JVM class information.
 *
 * @author Matt Coley
 */
public class JvmClassInfoProvider extends AbstractClassInfoProvider<JvmClassInfo> {
	/**
	 * @param toolsContainer
	 * 		Container to house tool buttons for display in the {@link Editor}.
	 * @param classProvider
	 * 		The provider of the latest class info.
	 */
	public JvmClassInfoProvider(@Nonnull ToolsContainerComponent toolsContainer, @Nonnull ClassNavigable classProvider) {
		super(toolsContainer, classProvider);
	}

	@Nonnull
	@Override
	protected Node createInfoContent(@Nonnull JvmClassInfo info) {
		GridPane content = new GridPane();
		ColumnConstraints col1 = new ColumnConstraints();
		ColumnConstraints col2 = new ColumnConstraints();
		col2.setFillWidth(true);
		col2.setHgrow(Priority.ALWAYS);
		col2.setHalignment(HPos.RIGHT);
		content.getColumnConstraints().addAll(col1, col2);
		content.setHgap(10);
		content.setVgap(5);

		Label titleLabel = new BoundLabel(Lang.getBinding("java.info"));
		titleLabel.getStyleClass().addAll(Styles.TEXT_UNDERLINED, Styles.TITLE_4);

		Label versionLabel = new BoundLabel(Lang.getBinding("java.info.version"));
		Label sourceLabel = new BoundLabel(Lang.getBinding("java.info.sourcefile"));
		versionLabel.getStyleClass().addAll(Styles.TEXT_BOLD);
		sourceLabel.getStyleClass().addAll(Styles.TEXT_BOLD);

		Label versionValueLabel = new Label(String.valueOf(info.getVersion() - JavaVersion.VERSION_OFFSET));
		Label sourceValueLabel = new Label(Objects.requireNonNullElse(info.getSourceFileName(), ""));

		int row = 0;
		content.add(titleLabel, 0, row++, 2, 1);

		content.add(versionLabel, 0, row);
		content.add(versionValueLabel, 1, row++);

		content.add(sourceLabel, 0, row);
		content.add(sourceValueLabel, 1, row++);

		return content;
	}
}
