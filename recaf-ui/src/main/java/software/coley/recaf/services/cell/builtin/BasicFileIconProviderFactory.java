package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.services.cell.FileIconProviderFactory;
import software.coley.recaf.services.cell.IconProvider;
import software.coley.recaf.util.ByteHeaderUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Set;

/**
 * Basic implementation for {@link FileIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicFileIconProviderFactory implements FileIconProviderFactory {
	private static final Set<String> CODE_EXTENSIONS = Set.of("java", "groovy", "kt", "css", "xml", "html", "json");
	private static final IconProvider TEXT = Icons.createProvider(Icons.FILE_TEXT);
	private static final IconProvider TEXT_CODE = Icons.createProvider(Icons.FILE_CODE);
	private static final IconProvider AUDIO = Icons.createProvider(Icons.FILE_AUDIO);
	private static final IconProvider IMAGE = Icons.createProvider(Icons.FILE_IMAGE);
	private static final IconProvider EXECUTABLE = Icons.createProvider(Icons.FILE_PROGRAM);
	private static final IconProvider ZIP = Icons.createProvider(Icons.FILE_ZIP);
	private static final IconProvider JAR = Icons.createProvider(Icons.FILE_JAR);
	private static final IconProvider ANDROID = Icons.createProvider(Icons.ANDROID);
	private static final IconProvider UNKNOWN = Icons.createProvider(Icons.FILE_BINARY);

	@Nonnull
	@Override
	public IconProvider getFileInfoIconProvider(@Nonnull Workspace workspace,
												@Nonnull WorkspaceResource resource,
												@Nonnull FileBundle bundle,
												@Nonnull FileInfo info) {
		// Built-in info match
		if (info.isTextFile()) {
			String name = info.getName();
			String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
			return (CODE_EXTENSIONS.contains(ext)) ? TEXT_CODE : TEXT;
		} else if (info.isZipFile()) {
			String name = info.getName();
			String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
			if (ext.equals("jar") || ext.equals("jmod") || ext.equals("war"))
				return JAR;
			if (ext.equals("apk"))
				return ANDROID;
			return ZIP;
		}

		// Content match
		byte[] content = info.getRawContent();
		if (ByteHeaderUtil.matchAny(content, ByteHeaderUtil.IMAGE_HEADERS))
			return IMAGE;
		if (ByteHeaderUtil.match(content, ByteHeaderUtil.CLASS))
			return JAR;
		if (ByteHeaderUtil.matchAny(content, ByteHeaderUtil.PROGRAM_HEADERS))
			return EXECUTABLE;
		if (ByteHeaderUtil.matchAny(content, ByteHeaderUtil.AUDIO_HEADERS))
			return AUDIO;
		if (ByteHeaderUtil.match(content, ByteHeaderUtil.DEX))
			return ANDROID;

		// No obvious association
		return UNKNOWN;
	}
}
