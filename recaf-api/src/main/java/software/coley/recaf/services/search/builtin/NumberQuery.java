package software.coley.recaf.services.search.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import regexodus.Matcher;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.search.FileSearchVisitor;
import software.coley.recaf.util.NumberMatchMode;
import software.coley.recaf.util.NumberUtil;

import java.util.function.BiConsumer;

import static software.coley.recaf.util.RegexUtil.getMatcher;

/**
 * Number search implementation.
 *
 * @author Matt Coley
 */
public class NumberQuery extends AbstractValueQuery {
	private final NumberMatchMode matchMode;
	private final Number target;

	/**
	 * @param matchMode
	 * 		Number matching mode.
	 * @param target
	 * 		Number to match against.
	 */
	public NumberQuery(@Nonnull NumberMatchMode matchMode, @Nonnull Number target) {
		this.matchMode = matchMode;
		this.target = target;
	}

	@Override
	protected boolean isMatch(Object value) {
		if (value instanceof Number number)
			return matchMode.match(target, number);
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

					// Extract numbers (decimal, hex) from line, check if match
					Matcher matcher = getMatcher("(?:\\b|-)(?:\\d+(?:.\\d+[DdFf]?)?|0[xX][0-9a-fA-F]+)\\b", lineText);
					while (matcher.find()) {
						String group = matcher.group(0);
						try {
							Number value = NumberUtil.parse(group);
							if (isMatch(value))
								resultSink.accept(filePath.child(i + 1), value);
						} catch (NumberFormatException ignored) {
							// Invalid match
						}
					}

				}
			}
		}
	}
}
