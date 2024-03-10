package software.coley.recaf.services.search.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.services.search.FileSearchVisitor;
import software.coley.recaf.services.search.ResultSink;
import software.coley.recaf.services.search.match.StringPredicate;

/**
 * String search implementation.
 *
 * @author Matt Coley
 */
public class StringQuery extends AbstractValueQuery {
	private final StringPredicate predicate;

	/**
	 * @param predicate
	 * 		String matching predicate.
	 */
	public StringQuery(@Nonnull StringPredicate predicate) {
		this.predicate = predicate;
	}

	@Override
	protected boolean isMatch(Object value) {
		if (value instanceof String text)
			return predicate.match(text);
		return false;
	}

	@Nonnull
	@Override
	public FileSearchVisitor visitor(@Nullable FileSearchVisitor delegate) {
		return new FileVisitor(delegate);
	}

	/**
	 * Points {@link #visitor(FileSearchVisitor)} to file content.
	 */
	private class FileVisitor implements FileSearchVisitor {
		private final FileSearchVisitor delegate;

		private FileVisitor(FileSearchVisitor delegate) {
			this.delegate = delegate;
		}

		@Override
		public void visit(@Nonnull ResultSink resultSink,
						  @Nonnull FilePathNode filePath,
						  @Nonnull FileInfo fileInfo) {
			if (delegate != null) delegate.visit(resultSink, filePath, fileInfo);

			// Search text files text content on a line by line basis
			if (fileInfo.isTextFile()) {
				String[] lines = fileInfo.asTextFile().getTextLines();

				// Split by single newline (including goofy carriage returns)
				for (int i = 0; i < lines.length; i++) {
					String lineText = lines[i];
					if (isMatch(lineText))
						resultSink.accept(filePath.child(i + 1), lineText);
				}
			}
		}
	}
}
