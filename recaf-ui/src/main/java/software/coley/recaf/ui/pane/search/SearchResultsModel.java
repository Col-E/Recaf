package software.coley.recaf.ui.pane.search;

import jakarta.annotation.Nonnull;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.search.result.Result;
import software.coley.recaf.services.search.result.Results;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Model for a completed search result set.
 *
 * @author Matt Coley
 */
public final class SearchResultsModel {
	private final ObservableList<Result<?>> results = FXCollections.observableArrayList();
	private final ObservableList<Result<?>> readOnlyResults = FXCollections.unmodifiableObservableList(results);
	private final ReadOnlyIntegerWrapper matchCount = new ReadOnlyIntegerWrapper();
	private final ReadOnlyIntegerWrapper pathCount = new ReadOnlyIntegerWrapper();

	/**
	 * Replace the current result snapshot.
	 *
	 * @param results
	 * 		New result collection.
	 */
	public void setResults(@Nonnull Results results) {
		setResults((Collection<? extends Result<?>>) results);
	}

	/**
	 * Replace the current result snapshot.
	 *
	 * @param results
	 		New result collection.
	 */
	public void setResults(@Nonnull Collection<? extends Result<?>> results) {
		this.results.setAll(results);

		List<? extends PathNode<?>> paths = this.results.stream()
				.map(Result::getPath)
				.toList();
		matchCount.set(this.results.size());
		pathCount.set(new HashSet<>(paths).size());
	}

	/**
	 * @return Observable read-only result snapshot.
	 */
	@Nonnull
	public ObservableList<Result<?>> getResults() {
		return readOnlyResults;
	}

	/**
	 * @return Number of accepted result objects.
	 */
	public int getMatchCount() {
		return matchCount.get();
	}

	/**
	 * @return Observable match count.
	 */
	@Nonnull
	public ReadOnlyIntegerProperty matchCountProperty() {
		return matchCount.getReadOnlyProperty();
	}

	/**
	 * @return Number of distinct paths containing accepted results.
	 */
	public int getPathCount() {
		return pathCount.get();
	}

	/**
	 * @return Observable distinct-path count.
	 */
	@Nonnull
	public ReadOnlyIntegerProperty pathCountProperty() {
		return pathCount.getReadOnlyProperty();
	}

	/**
	 * Format every result in its current sorted order.
	 *
	 * @param formatter
	 * 		Formatter for individual results.
	 *
	 * @return One formatted result per line.
	 */
	@Nonnull
	public String formatResults(@Nonnull Function<Result<?>, String> formatter) {
		return results.stream()
				.map(formatter)
				.collect(Collectors.joining("\n"));
	}
}
