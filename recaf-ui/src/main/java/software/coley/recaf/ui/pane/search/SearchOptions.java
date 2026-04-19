package software.coley.recaf.ui.pane.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;

import java.util.Arrays;
import java.util.List;

/**
 * Search options that constrain what the search query visits.
 *
 * @author Matt Coley
 */
public class SearchOptions {
	private final BooleanProperty searchClasses = new SimpleBooleanProperty(true);
	private final BooleanProperty searchFiles = new SimpleBooleanProperty(true);
	private final StringProperty includedPackages = new SimpleStringProperty("");
	private final StringProperty excludedPackages = new SimpleStringProperty("");
	private final StringProperty includedDirectories = new SimpleStringProperty("");
	private final StringProperty excludedDirectories = new SimpleStringProperty("");

	/**
	 * @return Property controlling class visitation.
	 */
	@Nonnull
	public BooleanProperty searchClassesProperty() {
		return searchClasses;
	}

	/**
	 * @return Property controlling file visitation.
	 */
	@Nonnull
	public BooleanProperty searchFilesProperty() {
		return searchFiles;
	}

	/**
	 * @return Comma-separated list of package prefixes to include.
	 */
	@Nonnull
	public StringProperty includedPackagesProperty() {
		return includedPackages;
	}

	/**
	 * @return Comma-separated list of package prefixes to exclude.
	 */
	@Nonnull
	public StringProperty excludedPackagesProperty() {
		return excludedPackages;
	}

	/**
	 * @return Comma-separated list of directory prefixes to include.
	 */
	@Nonnull
	public StringProperty includedDirectoriesProperty() {
		return includedDirectories;
	}

	/**
	 * @return Comma-separated list of directory prefixes to exclude.
	 */
	@Nonnull
	public StringProperty excludedDirectoriesProperty() {
		return excludedDirectories;
	}

	/**
	 * @return Snapshot of current options.
	 */
	@Nonnull
	public Snapshot snapshot() {
		return new Snapshot(
				searchClasses.get(),
				searchFiles.get(),
				parsePrefixes(includedPackages.get()),
				parsePrefixes(excludedPackages.get()),
				parsePrefixes(includedDirectories.get()),
				parsePrefixes(excludedDirectories.get())
		);
	}

	@Nonnull
	private static List<String> parsePrefixes(@Nullable String text) {
		if (text == null || text.isBlank())
			return List.of();
		return Arrays.stream(text.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
	}

	/**
	 * Immutable view of search options for a single search run.
	 *
	 * @param searchClasses
	 * 		Flag to search classes.
	 * @param searchFiles
	 * 		Flag to search files.
	 * @param includedPackages
	 * 		Package prefixes to include.
	 * @param excludedPackages
	 * 		Package prefixes to exclude.
	 * @param includedDirectories
	 * 		Directory prefixes to include.
	 * @param excludedDirectories
	 * 		Directory prefixes to exclude.
	 */
	public record Snapshot(boolean searchClasses,
	                       boolean searchFiles,
	                       @Nonnull List<String> includedPackages,
	                       @Nonnull List<String> excludedPackages,
	                       @Nonnull List<String> includedDirectories,
	                       @Nonnull List<String> excludedDirectories) {
		/**
		 * @param cls
		 * 		Class to check.
		 *
		 * @return {@code true} when the class should be visited.
		 */
		public boolean shouldVisitClass(@Nonnull ClassInfo cls) {
			if (!searchClasses)
				return false;
			return shouldVisit(cls.getPackageName(), includedPackages, excludedPackages);
		}

		/**
		 * @param file
		 * 		File to check.
		 *
		 * @return {@code true} when the file should be visited.
		 */
		public boolean shouldVisitFile(@Nonnull FileInfo file) {
			if (!searchFiles)
				return false;
			return shouldVisit(file.getDirectoryName(), includedDirectories, excludedDirectories);
		}

		private static boolean shouldVisit(@Nullable String path, @Nonnull List<String> includes, @Nonnull List<String> excludes) {
			String safePath = path == null ? "" : path;
			boolean included = includes.isEmpty() || matchesAny(safePath, includes);
			return included && !matchesAny(safePath, excludes);
		}

		private static boolean matchesAny(@Nonnull String path, @Nonnull List<String> prefixes) {
			for (String prefix : prefixes)
				if (path.startsWith(prefix))
					return true;
			return false;
		}
	}
}
