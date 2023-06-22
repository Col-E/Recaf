package software.coley.recaf.workspace.io;

import software.coley.recaf.info.*;
import software.coley.recaf.info.properties.builtin.*;
import software.coley.recaf.util.Unchecked;
import software.coley.recaf.util.ZipCreationUtils;
import software.coley.recaf.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.DeflaterOutputStream;

import static software.coley.llzip.format.compression.ZipCompressions.DEFLATED;
import static software.coley.llzip.format.compression.ZipCompressions.STORED;

/**
 * Options for configuring / preparing a {@link WorkspaceExporter} when calling
 * {@link WorkspaceManager#createExporter(WorkspaceExportOptions)}.
 *
 * @author Matt Coley
 */
public class WorkspaceExportOptions {
	private final CompressType compressType;
	private final OutputType outputType;
	private final Path path;
	private boolean bundleSupporting;
	private boolean createZipDirEntries;

	/**
	 * @param outputType
	 * 		Type of output for contents.
	 * @param path
	 * 		Path to write to.
	 */
	public WorkspaceExportOptions(OutputType outputType, Path path) {
		this(CompressType.MATCH_ORIGINAL, outputType, path);
	}

	/**
	 * @param compressType
	 * 		Compression option for contents exported.
	 * @param outputType
	 * 		Type of output for contents.
	 * @param path
	 * 		Path to write to.
	 */
	public WorkspaceExportOptions(CompressType compressType, OutputType outputType, Path path) {
		this.compressType = compressType;
		this.outputType = outputType;
		this.path = path;
	}

	/**
	 * @param bundleSupporting
	 *        {@code true} to bundle all {@link Workspace#getSupportingResources()} into the output.
	 */
	public void setBundleSupporting(boolean bundleSupporting) {
		this.bundleSupporting = bundleSupporting;
	}

	/**
	 * @param createZipDirEntries
	 *        {@code true} to create directory entries in the output ZIP.
	 * 		Does nothing when output type is a directory.
	 */
	public void setCreateZipDirEntries(boolean createZipDirEntries) {
		this.createZipDirEntries = createZipDirEntries;
	}

	/**
	 * @return New exporter from current options.
	 */
	public WorkspaceExporter create() {
		return new WorkspaceExporterImpl();
	}

	/**
	 * Compression option for ZIP/JAR outputs.
	 */
	public enum CompressType {
		/**
		 * Match the original compression of a {@link Info} item by checking {@link ZipCompressionProperty}.
		 * When unknown, defaults to enabling compression.
		 */
		MATCH_ORIGINAL,
		/**
		 * Compress items only when if it will yield more compact data.
		 * Some smaller files do not compress well due to the overhead cost of the compression.
		 */
		SMART,
		/**
		 * Compress all items in the output.
		 */
		ALWAYS,
		/**
		 * Do not compress any items in the output.
		 */
		NEVER,
	}

	/**
	 * Output option between single files and directories.
	 */
	public enum OutputType {
		/**
		 * Output to a single file. The type of which is determined by the primary resource's
		 * {@link WorkspaceFileResource#getFileInfo()} if available. Otherwise, defaults to ZIP/JAR.
		 */
		FILE,
		/**
		 * Output to a directory.
		 */
		DIRECTORY
	}

	/**
	 * Basic implementation of {@link WorkspaceExporter} that pulls from the options defined here.
	 */
	private class WorkspaceExporterImpl implements WorkspaceExporter {
		private final Map<String, byte[]> contents = new TreeMap<>();
		private final Map<String, Integer> compression = new HashMap<>();
		private final Map<String, String> comments = new HashMap<>();
		private final Map<String, Long> modifyTimes = new HashMap<>();
		private final Map<String, Long> createTimes = new HashMap<>();
		private final Map<String, Long> accessTimes = new HashMap<>();

		@Override
		public void export(Workspace workspace) throws IOException {
			populate(workspace);
			switch (outputType) {
				case FILE:
					ZipCreationUtils.ZipBuilder zipBuilder = ZipCreationUtils.builder();
					if (createZipDirEntries)
						zipBuilder = zipBuilder.createDirectories();

					// Final copy for lambda, write all contents to ZIP buffer
					ZipCreationUtils.ZipBuilder finalZipBuilder = zipBuilder;
					contents.forEach((name, content) -> {
						// Cannot mirror exact compression type, so we'll just do binary "is this compressed or nah?"
						boolean compress = compression.getOrDefault(name, STORED) > STORED;

						// Other properties
						String comment = comments.getOrDefault(name, null);
						long modifyTime = modifyTimes.getOrDefault(name, -1L);
						long createTime = createTimes.getOrDefault(name, -1L);
						long accessTime = accessTimes.getOrDefault(name, -1L);

						// Adding the entry
						finalZipBuilder.add(name, content, compress, comment, createTime, modifyTime, accessTime);
					});

					// Write buffer to path
					Files.write(path, zipBuilder.bytes());
					break;
				case DIRECTORY:
					for (Map.Entry<String, byte[]> entry : contents.entrySet()) {
						// Write everything relative to the path
						String relativePath = entry.getKey();
						byte[] content = entry.getValue();
						Path destination = path.resolve(relativePath);
						Path parent = destination.getParent();
						if (Files.isDirectory(path)) {
							Files.createDirectories(parent);
						}
						Files.write(destination, content);
					}
					break;
			}
		}

		/**
		 * @param workspace
		 * 		Workspace to pull data from.
		 */
		private void populate(Workspace workspace) {
			if (bundleSupporting) {
				for (WorkspaceResource supportingResource : workspace.getSupportingResources()) {
					mapInto(contents, supportingResource);
				}
			}
			mapInto(contents, workspace.getPrimaryResource());
		}

		/**
		 * Takes the contents of the given resource and puts them into the map.
		 *
		 * @param map
		 * 		Map to collect values into.
		 * @param resource
		 * 		Resource to pull values from.
		 */
		private void mapInto(Map<String, byte[]> map, WorkspaceResource resource) {
			// Place classes into map
			for (JvmClassInfo classInfo : resource.getJvmClassBundle()) {
				String key = classInfo.getName() + ".class";
				map.put(key, classInfo.getBytecode());
				updateProperties(key, classInfo);
			}

			// Place versioned files into map
			for (Map.Entry<Integer, JvmClassBundle> entry : resource.getVersionedJvmClassBundles().entrySet()) {
				String versionPath = JarFileInfo.MULTI_RELEASE_PREFIX + entry.getKey() + "/";
				for (Map.Entry<String, JvmClassInfo> classEntry : entry.getValue().entrySet()) {
					String key = versionPath + classEntry.getKey() + ".class";
					JvmClassInfo value = classEntry.getValue();
					map.put(key, value.getBytecode());
					updateProperties(key, value);
				}
			}

			// Rebuild Android DEX files and place into map
			for (Map.Entry<String, AndroidClassBundle> entry : resource.getAndroidClassBundles().entrySet()) {
				// TODO: Need to write back DEX files
			}

			// Place files into map
			for (FileInfo fileInfo : resource.getFileBundle()) {
				map.put(fileInfo.getName(), fileInfo.getRawContent());
				updateProperties(fileInfo.getName(), fileInfo);
			}

			// Recreate embedded resources as ZIP files with the original file paths
			for (Map.Entry<String, WorkspaceFileResource> entry : resource.getEmbeddedResources().entrySet()) {
				String embeddedFilePath = entry.getKey();
				WorkspaceFileResource embeddedResource = entry.getValue();
				Map<String, byte[]> embeddedMap = new TreeMap<>();
				mapInto(embeddedMap, embeddedResource);
				byte[] embeddedBytes = Unchecked.get(() -> ZipCreationUtils.createZip(embeddedMap));
				map.put(embeddedFilePath, embeddedBytes);
				FileInfo embeddedFile = embeddedResource.getFileInfo();
				updateProperties(embeddedFilePath, embeddedFile);
			}
		}

		/**
		 * @param name
		 * 		Map key.
		 * @param info
		 * 		Info to pull properties from.
		 */
		private void updateProperties(String name, Info info) {
			compression.put(name, getCompression(info));

			Long createTime = ZipCreationTimeProperty.get(info);
			if (createTime != null)
				createTimes.put(name, createTime);

			Long modifyTime = ZipModificationTimeProperty.get(info);
			if (modifyTime != null)
				modifyTimes.put(name, modifyTime);

			Long accessTime = ZipAccessTimeProperty.get(info);
			if (accessTime != null)
				accessTimes.put(name, accessTime);

			String comment = ZipCommentProperty.get(info);
			if (comment != null)
				comments.put(name, comment);
		}

		/**
		 * @param info
		 * 		Info to get compression for.
		 *
		 * @return Compression type for into value.
		 */
		private int getCompression(Info info) {
			switch (compressType) {
				case ALWAYS:
					return DEFLATED;
				case NEVER:
					return STORED;
				case SMART:
					// Get content from info
					byte[] content = null;
					if (info.isFile())
						content = info.asFile().getRawContent();
					else if (info.isClass()) {
						ClassInfo classInfo = info.asClass();
						if (classInfo.isJvmClass())
							content = classInfo.asJvmClass().getBytecode();
					}

					// Validate
					if (content == null)
						throw new IllegalStateException("Unhandled info type, cannot get byte[]: " + info.getClass().getName());

					// Check if deflate would be more optimal.
					InputStream in = new ByteArrayInputStream(content);
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					try (DeflaterOutputStream deflate = new DeflaterOutputStream(out)) {
						byte[] buffer = new byte[2048];
						int len;
						while ((len = in.read(buffer)) > 0) {
							deflate.write(buffer, 0, len);
						}
						deflate.finish();
						int inputSize = content.length;
						int compressedSize = out.size();
						if (compressedSize < inputSize)
							return DEFLATED;
					} catch (IOException ignored) {
						// Cannot compress
					}
					return STORED;
				case MATCH_ORIGINAL:
				default:
					return ZipCompressionProperty.getOr(info, DEFLATED);

			}
		}
	}
}
