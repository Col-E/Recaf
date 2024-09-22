package software.coley.recaf.services.info.association;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.Service;
import software.coley.recaf.ui.LanguageStylesheets;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;
import software.coley.recaf.ui.control.richtext.syntax.RegexRule;
import software.coley.recaf.ui.control.richtext.syntax.RegexSyntaxHighlighter;

import static software.coley.recaf.util.StringUtil.*;

/**
 * Provides mapping of {@link FileInfo#getName() file extensions} to {@link RegexRule language patterns}
 * for use by {@link RegexSyntaxHighlighter}.
 * <p>
 * Users can change which highlighter is used for different file extensions by updating the
 * {@link FileTypeSyntaxAssociationServiceConfig#getExtensionsToLangKeys() extensions map config}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class FileTypeSyntaxAssociationService implements Service {
	public static final String SERVICE_ID = "file-type-syntax-association";
	private final FileTypeSyntaxAssociationServiceConfig config;

	@Inject
	public FileTypeSyntaxAssociationService(@Nonnull FileTypeSyntaxAssociationServiceConfig config) {
		this.config = config;
	}

	/**
	 * Updates the syntax highlighter and stylesheet of the given editor to match the file contents of the given info object.
	 * <br>
	 * {@link JvmClassInfo} will use {@code java} syntax highlighting. {@link FileInfo} will use the file extension in their names
	 * to determine which syntax to use.
	 *
	 * @param info
	 * 		Info to target.
	 * @param editor
	 * 		Editor to update.
	 */
	public void configureEditorSyntax(@Nonnull Info info, @Nonnull Editor editor) {
		String fileExtension;
		if (info.isClass()) fileExtension = "java";
		else if (info instanceof BinaryXmlFileInfo) fileExtension = "xml";
		else fileExtension = getAfter(info.getName(), ".");
		configureEditorSyntax(fileExtension, editor);
	}

	/**
	 * Updates the syntax highlighter and stylesheet of the given editor to match the file contents of the given file type.
	 *
	 * @param fileExtension
	 * 		File extension to target.
	 * @param editor
	 * 		Editor to update.
	 */
	public void configureEditorSyntax(@Nonnull String fileExtension, @Nonnull Editor editor) {
		String lowerExtension = fileExtension.toLowerCase();
		String languageKey = config.getExtensionsToLangKeys().getOrDefault(lowerExtension, lowerExtension);

		String sheet = LanguageStylesheets.getLanguageStylesheet(languageKey);
		RegexRule language = RegexLanguages.getLanguage(languageKey);

		ObservableList<String> stylesheets = editor.getStylesheets();
		if (sheet != null && !stylesheets.contains(sheet))
			stylesheets.add(sheet);
		if (language != null)
			editor.setSyntaxHighlighter(new RegexSyntaxHighlighter(language));
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public FileTypeSyntaxAssociationServiceConfig getServiceConfig() {
		return config;
	}
}
