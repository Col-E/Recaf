package software.coley.recaf.services.cell.icon;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.info.*;
import software.coley.recaf.services.cell.icon.IconProvider;
import software.coley.recaf.services.cell.icon.PackageIconProviderFactory;
import software.coley.recaf.services.cell.icon.ResourceIconProviderFactory;
import software.coley.recaf.services.phantom.GeneratedPhantomWorkspaceResource;
import software.coley.recaf.util.ByteHeaderUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link PackageIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicResourceIconProviderFactory implements ResourceIconProviderFactory {
	private static final IconProvider PROVIDER_DIR = Icons.createProvider(Icons.FOLDER);
	private static final IconProvider PROVIDER_ANDROID = Icons.createProvider(Icons.ANDROID);
	private static final IconProvider PROVIDER_TEXT = Icons.createProvider(Icons.FILE_TEXT);
	private static final IconProvider PROVIDER_ZIP = Icons.createProvider(Icons.FILE_ZIP);
	private static final IconProvider PROVIDER_JAR = Icons.createProvider(Icons.FILE_JAR);
	private static final IconProvider PROVIDER_CLASS = Icons.createProvider(Icons.FILE_CLASS);
	private static final IconProvider PROVIDER_IMAGE = Icons.createProvider(Icons.FILE_IMAGE);
	private static final IconProvider PROVIDER_AUDIO = Icons.createProvider(Icons.FILE_AUDIO);
	private static final IconProvider PROVIDER_PROGRAM = Icons.createProvider(Icons.FILE_PROGRAM);
	private static final IconProvider PROVIDER_PHANTOM = Icons.createProvider(Icons.PHANTOM);

	@Nonnull
	@Override
	public IconProvider getResourceIconProvider(@Nonnull Workspace workspace,
												@Nonnull WorkspaceResource resource) {
		if (resource instanceof WorkspaceDirectoryResource)
			return PROVIDER_DIR;
		if (resource instanceof WorkspaceFileResource fileResource) {
			FileInfo file = fileResource.getFileInfo();
			if (file instanceof ApkFileInfo || file instanceof DexFileInfo || file instanceof ArscFileInfo)
				return PROVIDER_ANDROID;
			if (file instanceof JarFileInfo || file instanceof WarFileInfo || file instanceof JModFileInfo)
				return PROVIDER_JAR;
			if (file instanceof TextFileInfo)
				return PROVIDER_TEXT;
			if (ByteHeaderUtil.match(file.getRawContent(), ByteHeaderUtil.CLASS))
				return PROVIDER_CLASS;
			if (ByteHeaderUtil.matchAny(file.getRawContent(), ByteHeaderUtil.PROGRAM_HEADERS))
				return PROVIDER_PROGRAM;
			if (ByteHeaderUtil.matchAny(file.getRawContent(), ByteHeaderUtil.IMAGE_HEADERS))
				return PROVIDER_IMAGE;
			if (ByteHeaderUtil.matchAny(file.getRawContent(), ByteHeaderUtil.AUDIO_HEADERS))
				return PROVIDER_AUDIO;
		}
		if (resource instanceof GeneratedPhantomWorkspaceResource)
			return PROVIDER_PHANTOM;
		return PROVIDER_ZIP;
	}
}
