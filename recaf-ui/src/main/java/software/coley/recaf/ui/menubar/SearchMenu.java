package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.FontIconView;

import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.action;

/**
 * Search menu component for {@link MainMenu}.
 *
 * @author Matt Coley
 */
@Dependent
public class SearchMenu extends WorkspaceAwareMenu {

	@Inject
	public SearchMenu(@Nonnull WorkspaceManager workspaceManager,
					  @Nonnull Actions actions) {
		super(workspaceManager);

		textProperty().bind(getBinding("menu.search"));
		setGraphic(new FontIconView(CarbonIcons.SEARCH));

		getItems().add(action("menu.search.string", CarbonIcons.QUOTES, actions::openNewStringSearch));
		getItems().add(action("menu.search.number", CarbonIcons.NUMBER_0, actions::openNewNumberSearch));
		getItems().add(action("menu.search.class.type-references", CarbonIcons.CODE_REFERENCE, actions::openNewClassReferenceSearch));
		getItems().add(action("menu.search.class.member-references", CarbonIcons.CODE_REFERENCE, actions::openNewMemberReferenceSearch));
		getItems().add(action("menu.search.class.member-declarations", CarbonIcons.CODE, actions::openNewMemberDeclarationSearch));
		getItems().add(action("menu.search.class.instruction", CarbonIcons.CODE, actions::openNewInstructionSearch));

		disableProperty().bind(hasWorkspace.not());
	}
}
