package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.match.NumberPredicateProvider;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.pane.search.NumberSearchPane;
import software.coley.recaf.ui.pane.search.StringSearchPane;
import software.coley.recaf.util.Lang;

/**
 * Basic context menu factory for standalone constant values.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicConstantContextMenuProviderFactory extends AbstractContextMenuProviderFactory implements ConstantContextMenuProviderFactory {
	@Inject
	public BasicConstantContextMenuProviderFactory(@Nonnull TextProviderService textService,
	                                               @Nonnull IconProviderService iconService,
	                                               @Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getConstantContextMenuProvider(@Nonnull Object value) {
		if (value instanceof String stringValue) {
			return () -> {
				ContextMenu menu = new ContextMenu();
				menu.getItems().add(new ActionMenuItem(Lang.getBinding("menu.search.string.value"),
						new FontIconView(CarbonIcons.QUOTES), () -> {
					StringSearchPane pane = actions.openNewStringSearch();
					pane.stringPredicateIdProperty().setValue(StringPredicateProvider.KEY_EQUALS);
					pane.setSearchValue(stringValue);
				}));
				return menu;
			};
		} else if (value instanceof Number numberValue) {
			return () -> {
				ContextMenu menu = new ContextMenu();
				menu.getItems().add(new ActionMenuItem(Lang.getBinding("menu.search.number.value"),
						new FontIconView(CarbonIcons.NUMBER_0), () -> {
					NumberSearchPane pane = actions.openNewNumberSearch();
					pane.numericPredicateIdProperty().setValue(NumberPredicateProvider.KEY_EQUAL);
					pane.setSearchValue(numberValue);
				}));
				return menu;
			};
		} else return emptyProvider();
	}
}
