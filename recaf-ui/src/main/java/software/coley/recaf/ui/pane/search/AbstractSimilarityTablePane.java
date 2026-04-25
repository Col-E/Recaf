package software.coley.recaf.ui.pane.search;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.services.search.CancellableSearchFeedback;
import software.coley.recaf.services.search.SimilaritySearchService;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundIntSpinner;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Common base for similarity result tables.
 *
 * @param <P>
 * 		Reference path type.
 *
 * @author Matt Coley
 */
public abstract class AbstractSimilarityTablePane<P extends PathNode<?>> extends BorderPane implements UpdatableNavigable {
	protected static final int DEFAULT_THRESHOLD = 95;

	protected P referencePath;
	protected final SimilaritySearchService searchService;
	protected final CellConfigurationService configurationService;
	protected final Actions actions;
	protected final SearchOptions searchOptions = new SearchOptions();
	protected final IntegerProperty threshold = new SimpleIntegerProperty(DEFAULT_THRESHOLD);
	protected final StringProperty includedPackages = new SimpleStringProperty("");
	protected final StringProperty excludedPackages = new SimpleStringProperty("");
	private final StringProperty referenceText = new SimpleStringProperty("");
	private Popover advancedOptionsPopover;
	private ActionButton advancedOptionsButton;
	private CancellableSearchFeedback lastSearchFeedback;
	private Control focusTarget;
	protected int searchGeneration;

	protected AbstractSimilarityTablePane(@Nonnull SimilaritySearchService searchService,
	                                      @Nonnull CellConfigurationService configurationService,
	                                      @Nonnull Actions actions) {
		this.searchService = searchService;
		this.configurationService = configurationService;
		this.actions = actions;

		searchOptions.searchFilesProperty().set(false);
		searchOptions.includedPackagesProperty().bind(includedPackages);
		searchOptions.excludedPackagesProperty().bind(excludedPackages);
	}

	@Override
	public boolean isTrackable() {
		return false;
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		if (referencePath == null)
			throw new IllegalStateException("Similarity pane path has not been initialized");
		return referencePath;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		searchGeneration++;
		cancelLastSearch();
		getChildren().clear();
		setDisable(true);
	}

	@Override
	public void requestFocus() {
		if (focusTarget != null)
			focusTarget.requestFocus();
	}

	protected final void initialize(@Nonnull Node resultView,
	                                @Nonnull Control focusTarget,
	                                @Nonnull ObservableValue<?>... refreshTriggers) {
		this.focusTarget = focusTarget;
		setTop(createInputs());
		setCenter(resultView);
		setupAutoRefresh(refreshTriggers);
		FxThreadUtil.run(this::refresh);
	}

	@Nonnull
	protected final Node createConfiguredPathDisplay(@Nonnull PathNode<?> path) {
		Label label = new Label(configurationService.textOf(path));
		label.setGraphic(configurationService.graphicOf(path));
		label.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(label, Priority.ALWAYS);
		configurationService.configureStyle(label, path);
		label.setContextMenu(configurationService.contextMenuOf(SearchContextSource.SEARCH_INSTANCE, path));

		HBox container = new HBox(6, label);
		container.setAlignment(Pos.CENTER_LEFT);
		return container;
	}

	@Nonnull
	protected final TableSearchFeedback createTableSearchFeedback() {
		return new TableSearchFeedback(searchOptions.snapshot());
	}

	protected final void replaceSearchFeedback(@Nonnull CancellableSearchFeedback feedback) {
		cancelLastSearch();
		lastSearchFeedback = feedback;
	}

	protected final void clearSearchFeedback(@Nonnull CancellableSearchFeedback feedback) {
		if (lastSearchFeedback == feedback)
			lastSearchFeedback = null;
	}

	@Nonnull
	protected final ProgressIndicator searchingPlaceholder() {
		return new ProgressIndicator();
	}

	protected final void showAdvancedOptionsPopover() {
		if (advancedOptionsPopover == null) {
			advancedOptionsPopover = new Popover(createAdvancedOptionsContent());
			advancedOptionsPopover.setArrowLocation(Popover.ArrowLocation.TOP_RIGHT);
		}
		advancedOptionsPopover.show(advancedOptionsButton);
	}

	@Nonnull
	protected GridPane createBaseAdvancedOptionsContent() {
		GridPane content = new GridPane();
		ColumnConstraints labelColumn = new ColumnConstraints();
		ColumnConstraints controlColumn = new ColumnConstraints();
		controlColumn.setFillWidth(true);
		controlColumn.setHgrow(Priority.ALWAYS);
		controlColumn.setHalignment(HPos.RIGHT);
		content.getColumnConstraints().addAll(labelColumn, controlColumn);
		content.setHgap(10);
		content.setVgap(5);
		return content;
	}

	@Nonnull
	protected final Node createInputs() {
		BoundIntSpinner thresholdSpinner = new BoundIntSpinner(threshold, 0, 100);
		thresholdSpinner.setPrefWidth(90);

		TextField referenceField = new TextField();
		referenceField.textProperty().bind(referenceText);
		referenceField.setEditable(false);
		referenceField.setFocusTraversable(false);
		referenceField.setMaxWidth(Double.MAX_VALUE);

		ActionButton search = new ActionButton(CarbonIcons.SEARCH, this::refresh);
		search.withTooltip("menu.search");
		search.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT);

		advancedOptionsButton = new ActionButton(CarbonIcons.SETTINGS, this::showAdvancedOptionsPopover);
		advancedOptionsButton.withTooltip(advancedOptionsTooltipKey());
		advancedOptionsButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);

		Region spacer = new Region();
		HBox input = new HBox(10,
				new BoundLabel(Lang.getBinding(referenceLabelKey())), referenceField,
				new BoundLabel(Lang.getBinding(thresholdLabelKey())), thresholdSpinner,
				spacer, advancedOptionsButton, search);
		input.setAlignment(Pos.CENTER_LEFT);
		input.setPadding(new Insets(10));
		HBox.setHgrow(referenceField, Priority.ALWAYS);
		HBox.setHgrow(spacer, Priority.ALWAYS);
		return input;
	}

	private void setupAutoRefresh(@Nonnull ObservableValue<?>... refreshTriggers) {
		EventStream<?> stream = EventStreams.changesOf(threshold).map(unused -> new Object())
				.or(EventStreams.changesOf(includedPackages).map(unused -> new Object()))
				.or(EventStreams.changesOf(excludedPackages).map(unused -> new Object()));
		for (ObservableValue<?> refreshTrigger : refreshTriggers)
			stream = stream.or(EventStreams.changesOf(refreshTrigger).map(unused -> new Object()));
		stream.reduceSuccessions(Collections::singletonList, (list, ignored) -> list, Duration.ofMillis(250))
				.addObserver(unused -> refresh());
	}

	private void cancelLastSearch() {
		if (lastSearchFeedback != null) {
			lastSearchFeedback.cancel();
			lastSearchFeedback = null;
		}
	}

	@Nonnull
	protected abstract String referenceText();

	protected final void setReferencePath(@Nonnull P referencePath) {
		this.referencePath = Objects.requireNonNull(referencePath);
		referenceText.set(referenceText());
	}

	@Nonnull
	protected abstract String referenceLabelKey();

	@Nonnull
	protected abstract String thresholdLabelKey();

	@Nonnull
	protected abstract String advancedOptionsTooltipKey();

	@Nonnull
	protected abstract Node createAdvancedOptionsContent();

	protected abstract void refresh();

	protected static class TableSearchFeedback extends CancellableSearchFeedback {
		private final SearchOptions.Snapshot optionsSnapshot;

		private TableSearchFeedback(@Nonnull SearchOptions.Snapshot optionsSnapshot) {
			this.optionsSnapshot = Objects.requireNonNull(optionsSnapshot);
		}

		@Override
		public boolean doVisitClass(@Nonnull ClassInfo cls) {
			return optionsSnapshot.shouldVisitClass(cls);
		}
	}
}
