package software.coley.recaf.services.search.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.search.FileSearchVisitor;
import software.coley.recaf.util.TextMatchMode;

import java.util.function.BiConsumer;

/**
 * String search implementation.
 *
 * @author Matt Coley
 */
public class StringQuery extends AbstractValueQuery {
	private final TextMatchMode matchMode;
	private final String target;

	/**
	 * @param matchMode
	 * 		Text matching mode.
	 * @param target
	 * 		Text to match against.
	 */
	public StringQuery(@Nonnull TextMatchMode matchMode, @Nonnull String target) {
		this.matchMode = matchMode;
		this.target = target;
	}

	@Override
	protected boolean isMatch(Object value) {
		if (value instanceof String text)
			return matchMode.match(target, text);
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
		public void visit(@Nonnull BiConsumer<PathNode<?>, Object> resultSink,
						  @Nonnull FilePathNode filePath,
						  @Nonnull FileInfo fileInfo) {
			if (delegate != null) delegate.visit(resultSink, filePath, fileInfo);

			// Search text files text content on a line by line basis
			if (fileInfo.isTextFile()) {
				String text = fileInfo.asTextFile().getText();

				// Split by single newline (including goofy carriage returns)
				String[] lines = text.split("\\r?\\n\\r?");
				for (int i = 0; i < lines.length; i++) {
					String lineText = lines[i];
					if (isMatch(lineText))
						resultSink.accept(filePath.child(i + 1), lineText);
				}
			}
		}
	}
}
