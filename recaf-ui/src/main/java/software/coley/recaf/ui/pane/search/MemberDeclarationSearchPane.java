package software.coley.recaf.ui.pane.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.SearchService;
import software.coley.recaf.services.search.match.StringPredicate;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.services.search.query.DeclarationQuery;
import software.coley.recaf.services.search.query.Query;
import software.coley.recaf.services.workspace.WorkspaceManager;

/**
 * Member declaration search pane.
 *
 * @author Matt Coley
 * @see MemberReferenceSearchPane
 */
@Dependent
public class MemberDeclarationSearchPane extends AbstractMemberSearchPane {
	@Inject
	public MemberDeclarationSearchPane(@Nonnull WorkspaceManager workspaceManager,
	                                   @Nonnull SearchService searchService,
	                                   @Nonnull CellConfigurationService configurationService,
	                                   @Nonnull Actions actions,
	                                   @Nonnull StringPredicateProvider stringPredicateProvider) {
		super(workspaceManager, searchService, configurationService, actions, stringPredicateProvider);
	}

	@Nonnull
	@Override
	protected Query newQuery(@Nullable StringPredicate ownerPredicate,
	                         @Nullable StringPredicate namePredicate,
	                         @Nullable StringPredicate descPredicate) {
		return new DeclarationQuery(ownerPredicate, namePredicate, descPredicate);
	}
}
