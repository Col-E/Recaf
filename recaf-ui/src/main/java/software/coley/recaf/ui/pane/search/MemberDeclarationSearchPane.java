package software.coley.recaf.ui.pane.search;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.SearchService;
import software.coley.recaf.services.search.match.StringPredicate;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.services.search.query.DeclarationQuery;
import software.coley.recaf.services.search.query.Query;
import software.coley.recaf.services.search.result.MemberDeclaration;
import software.coley.recaf.services.search.result.MemberDeclarationResult;
import software.coley.recaf.services.search.result.Result;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.BoundToggleIcon;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;

import java.util.function.Predicate;

/**
 * Member declaration search pane.
 *
 * @author Matt Coley
 * @see MemberReferenceSearchPane
 */
@Dependent
public class MemberDeclarationSearchPane extends AbstractMemberSearchPane {
	private final BooleanProperty includeFields = new SimpleBooleanProperty(true);
	private final BooleanProperty includeMethods = new SimpleBooleanProperty(true);
	private final BooleanProperty includeStatic = new SimpleBooleanProperty(true);
	private final BooleanProperty includeNonStatic = new SimpleBooleanProperty(true);
	private final BooleanProperty includeFinal = new SimpleBooleanProperty(true);
	private final BooleanProperty includeNonFinal = new SimpleBooleanProperty(true);

	@Inject
	public MemberDeclarationSearchPane(@Nonnull WorkspaceManager workspaceManager,
	                                   @Nonnull SearchService searchService,
	                                   @Nonnull CellConfigurationService configurationService,
	                                   @Nonnull Actions actions,
	                                   @Nonnull StringPredicateProvider stringPredicateProvider) {
		super(workspaceManager, searchService, configurationService, actions, stringPredicateProvider);
		setupFilterListeners();
	}

	@Nonnull
	@Override
	protected Query newQuery(@Nullable StringPredicate ownerPredicate,
	                         @Nullable StringPredicate namePredicate,
	                         @Nullable StringPredicate descPredicate) {
		return new DeclarationQuery(ownerPredicate, namePredicate, descPredicate);
	}

	@Override
	protected int addCustomSearchOptions(@Nonnull GridPane content, int row) {
		content.add(createFilterStrip(), 0, row, 2, 1);
		return row + 1;
	}

	@Override
	protected Predicate<Result<?>> createResultFilter() {
		boolean fields = includeFields.get();
		boolean methods = includeMethods.get();
		boolean statics = includeStatic.get();
		boolean nonStatics = includeNonStatic.get();
		boolean finals = includeFinal.get();
		boolean nonFinals = includeNonFinal.get();
		return result -> {
			if (!(result instanceof MemberDeclarationResult declarationResult))
				return true;

			MemberDeclaration declaration = declarationResult.getValue();
			if (declaration.isFieldDeclaration() ? !fields : !methods)
				return false;
			if (declaration.hasStaticModifier() ? !statics : !nonStatics)
				return false;
			return declaration.hasFinalModifier() ? finals : nonFinals;
		};
	}

	private void setupFilterListeners() {
		includeFields.addListener((ob, old, cur) -> search());
		includeMethods.addListener((ob, old, cur) -> search());
		includeStatic.addListener((ob, old, cur) -> search());
		includeNonStatic.addListener((ob, old, cur) -> search());
		includeFinal.addListener((ob, old, cur) -> search());
		includeNonFinal.addListener((ob, old, cur) -> search());
	}

	@Nonnull
	private HBox createFilterStrip() {
		HBox box = new HBox(6,
				new BoundLabel(Lang.getBinding("dialog.search.options.modifiers")),
				createFilterToggle(Icons.FIELD, "dialog.search.options.fields", includeFields),
				createFilterToggle(Icons.METHOD, "dialog.search.options.methods", includeMethods),
				createFilterToggle(Icons.ACCESS_STATIC_FULL, "dialog.search.options.static", includeStatic),
				createFilterToggle(createNegatedModifierGraphic(Icons.ACCESS_STATIC_FULL), "dialog.search.options.not-static", includeNonStatic),
				createFilterToggle(Icons.ACCESS_FINAL_FULL, "dialog.search.options.final", includeFinal),
				createFilterToggle(createNegatedModifierGraphic(Icons.ACCESS_FINAL_FULL), "dialog.search.options.not-final", includeNonFinal));
		box.setAlignment(Pos.CENTER_LEFT);
		return box;
	}

	@Nonnull
	private static BoundToggleIcon createFilterToggle(@Nonnull String iconPath,
	                                                  @Nonnull String tooltipKey,
	                                                  @Nonnull BooleanProperty property) {
		return createFilterToggle(Icons.getIconView(iconPath), tooltipKey, property);
	}

	@Nonnull
	private static BoundToggleIcon createFilterToggle(@Nonnull Node graphic,
	                                                  @Nonnull String tooltipKey,
	                                                  @Nonnull BooleanProperty property) {
		BoundToggleIcon button = new BoundToggleIcon(graphic, property).withTooltip(tooltipKey);
		button.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT, Styles.SMALL);
		button.setFocusTraversable(false);
		return button;
	}

	@Nonnull
	private static Node createNegatedModifierGraphic(@Nonnull String iconPath) {
		StackPane graphic = new StackPane(Icons.getIconView(iconPath));
		FontIconView badge = new FontIconView(CarbonIcons.FLASH_FILLED, 10, Color.RED);
		StackPane.setAlignment(badge, Pos.TOP_RIGHT);
		StackPane.setMargin(badge, new Insets(-4));
		graphic.getChildren().add(badge);
		return graphic;
	}
}
