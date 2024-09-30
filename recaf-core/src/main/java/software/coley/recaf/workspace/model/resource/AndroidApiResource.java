package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import software.coley.lljzip.ZipIO;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.properties.BasicPropertyContainer;
import software.coley.recaf.util.io.LocalFileHeaderSource;
import software.coley.recaf.workspace.model.bundle.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Implementation of a workspace resource sourced from the Android API.
 * This is a special case of resource which is automatically added to all workspaces with android content.
 * Listeners and such are not implemented and are ignored by design.
 *
 * @author Matt Coley
 */
public class AndroidApiResource extends BasicPropertyContainer implements WorkspaceResource {
	private static final Logger logger = Logging.get(AndroidApiResource.class);
	private static AndroidApiResource instance;
	private final FileBundle files = new BasicFileBundle();
	private final JvmClassBundle bundle;

	private AndroidApiResource(@Nonnull JvmClassBundle bundle) {
		this.bundle = bundle;
	}

	/**
	 * @return Shared instance of the Android API resource.
	 */
	@Nonnull
	public static AndroidApiResource getInstance() {
		if (instance == null) {
			logger.info("Initializing Android API support resource...");
			try {
				// We provide our own archive containing Android API classes.
				JvmClassBundle bundle = new BasicJvmClassBundle();
				byte[] jar = AndroidApiResource.class.getResourceAsStream("/android/api-outline-30.jar").readAllBytes();
				ZipArchive archive = ZipIO.readJvm(jar);
				for (LocalFileHeader fileEntry : archive.getLocalFiles()) {
					String name = fileEntry.getFileNameAsString();
					if (name.endsWith(".class")) {
						byte[] bytecode = new LocalFileHeaderSource(fileEntry).readAll();
						JvmClassInfo info = new JvmClassInfoBuilder(bytecode).build();
						bundle.put(info);
					}
				}
				instance = new AndroidApiResource(bundle);
			} catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}
		return instance;
	}

	@Override
	public void close() {
		// no-op
	}

	@Nonnull
	@Override
	public JvmClassBundle getJvmClassBundle() {
		return bundle;
	}

	@Nonnull
	@Override
	public NavigableMap<Integer, VersionedJvmClassBundle> getVersionedJvmClassBundles() {
		return Collections.emptyNavigableMap();
	}

	@Nonnull
	@Override
	public Map<String, AndroidClassBundle> getAndroidClassBundles() {
		return Collections.emptyMap();
	}

	@Nonnull
	@Override
	public FileBundle getFileBundle() {
		return files;
	}

	@Nonnull
	@Override
	public Map<String, WorkspaceFileResource> getEmbeddedResources() {
		return Collections.emptyMap();
	}

	@Nullable
	@Override
	public WorkspaceResource getContainingResource() {
		return null;
	}

	@Override
	public void setContainingResource(WorkspaceResource resource) {
		// no-op
	}

	@Override
	public void addResourceJvmClassListener(ResourceJvmClassListener listener) {
		// no-op
	}

	@Override
	public void removeResourceJvmClassListener(ResourceJvmClassListener listener) {
		// no-op
	}

	@Override
	public void addResourceAndroidClassListener(ResourceAndroidClassListener listener) {
		// no-op
	}

	@Override
	public void removeResourceAndroidClassListener(ResourceAndroidClassListener listener) {
		// no-op
	}

	@Override
	public void addResourceFileListener(ResourceFileListener listener) {
		// no-op
	}

	@Override
	public void removeResourceFileListener(ResourceFileListener listener) {
		// no-op
	}
}
