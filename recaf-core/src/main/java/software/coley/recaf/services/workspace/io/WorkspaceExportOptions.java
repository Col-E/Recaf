package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.file.DexHeader;
import me.darknet.dex.file.DexMapBuilder;
import me.darknet.dex.file.HiddenApiData;
import me.darknet.dex.io.Output;
import me.darknet.dex.tree.DexFile;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ApkFileInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.JarFileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.ZipFileInfo;
import software.coley.recaf.info.properties.builtin.PathOriginalNameProperty;
import software.coley.recaf.info.properties.builtin.PathPrefixProperty;
import software.coley.recaf.info.properties.builtin.PathSuffixProperty;
import software.coley.recaf.info.properties.builtin.ZipAccessTimeProperty;
import software.coley.recaf.info.properties.builtin.ZipCommentProperty;
import software.coley.recaf.info.properties.builtin.ZipCompressionProperty;
import software.coley.recaf.info.properties.builtin.ZipCreationTimeProperty;
import software.coley.recaf.info.properties.builtin.ZipModificationTimeProperty;
import software.coley.recaf.info.properties.builtin.ZipPrefixDataProperty;
import software.coley.recaf.util.android.ApkArchiveUtil;
import software.coley.recaf.util.android.ApkSigningUtil;
import software.coley.recaf.util.android.DexIOUtil;
import software.coley.recaf.util.io.ZipCreationUtils;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.AndroidDexMetadata;
import software.coley.recaf.workspace.model.bundle.AndroidDexMetadata.DexEntryProperties;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.DeflaterOutputStream;

import static software.coley.lljzip.format.compression.ZipCompressions.DEFLATED;
import static software.coley.lljzip.format.compression.ZipCompressions.STORED;

/**
 * Options for configuring / preparing a {@link WorkspaceExporter}.
 *
 * @author Matt Coley
 */
public class WorkspaceExportOptions {
	private static final Logger logger = Logging.get(WorkspaceExportOptions.class);

	private final WorkspaceCompressType compressType;
	private final WorkspaceOutputType outputType;
	private final WorkspaceExportConsumer consumer;
	private boolean bundleSupporting;
	private boolean createZipDirEntries;
	private ApkSigningOptions apkSigningOptions;

	/**
	 * @param outputType
	 * 		Type of output for contents.
	 * @param consumer
	 * 		Consumer to write to.
	 */
	public WorkspaceExportOptions(@Nonnull WorkspaceOutputType outputType, @Nonnull WorkspaceExportConsumer consumer) {
		this(WorkspaceCompressType.MATCH_ORIGINAL, outputType, consumer);
	}

	/**
	 * @param compressType
	 * 		Compression option for contents exported.
	 * @param outputType
	 * 		Type of output for contents.
	 * @param consumer
	 * 		Consumer to write to.
	 */
	public WorkspaceExportOptions(@Nonnull WorkspaceCompressType compressType, @Nonnull WorkspaceOutputType outputType,
	                              @Nonnull WorkspaceExportConsumer consumer) {
		this.compressType = compressType;
		this.outputType = outputType;
		this.consumer = consumer;
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
	 * @param options
	 * 		Signing configuration for an APK file output, or {@code null} for unsigned output.
	 */
	public void setApkSigningOptions(@Nullable ApkSigningOptions options) {
		this.apkSigningOptions = options;
	}

	/**
	 * @return New exporter from current options.
	 */
	@Nonnull
	public WorkspaceExporter create() {
		return new WorkspaceExporterImpl();
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
		private byte[] prefix;

		@Override
		public void export(@Nonnull Workspace workspace) throws IOException {
			WorkspaceResource primary = workspace.getPrimaryResource();

			// Validate APK signing options.
			if (apkSigningOptions != null) {
				// Signed APK's can only be exported to a single signed APK file.
				if (outputType != WorkspaceOutputType.FILE)
					throw new IOException("APK signing options can only be used for APK file outputs");

				// The primary resource needs to be originally read from an APK file.
				// There are some properties we assign only to APK file resources on import
				// which we use again here.
				if (!(primary instanceof WorkspaceFileResource fileResource)
						|| !(fileResource.getFileInfo() instanceof ApkFileInfo))
					throw new IOException("APK signing options can only be used for APK file outputs");
			}

			populate(workspace);
			switch (outputType) {
				case FILE:
					// For APK outputs, we need to remove any existing v1 signature artifacts from the contents.
					// They are artifacts of the original signing process, and we're overwriting them with our own.
					boolean apkOutput = primary instanceof WorkspaceFileResource primaryFileResource
							&& primaryFileResource.getFileInfo() instanceof ApkFileInfo;
					if (apkOutput)
						removeV1SignatureArtifacts();

					// Test if we're supposed to just write the file as-is instead of bundling it in an archive.
					//  - Must only have one thing to write
					//  - Workspace input must be a single non-archive file
					if (contents.size() == 1 &&
							primary instanceof WorkspaceFileResource primaryFileResource &&
							!(primaryFileResource.getFileInfo() instanceof ZipFileInfo)) {
						byte[] data = contents.values().iterator().next();
						if (prefix != null)
							consumer.write(prefix);
						consumer.write(data);
						consumer.commit();
						return;
					}

					// Otherwise, lets make an archive.
					ZipCreationUtils.ZipBuilder zipBuilder = ZipCreationUtils.builder();
					if (createZipDirEntries)
						zipBuilder = zipBuilder.createDirectories();

					// Final copy for lambda, write all contents to ZIP buffer.
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

					// Align APK contents before signing.
					// Android expects APK files to be aligned to 4-byte boundaries.
					byte[] archiveBytes = zipBuilder.bytes();
					boolean hasPrefixData = prefix != null && prefix.length > 0;
					if (apkOutput) {
						// Prefix data is not compatible with APK signing.
						// Its usually data like a shebang line, or executable stub. These are for running jars.
						if (hasPrefixData && apkSigningOptions != null)
							throw new IOException("Cannot sign APK output containing prefix data");
						if (hasPrefixData)
							logger.warn("Skipping APK alignment for APK output with prefix data");
						else
							archiveBytes = ApkArchiveUtil.align(archiveBytes);
					}
					if (apkSigningOptions != null)
						archiveBytes = ApkSigningUtil.signAndVerify(archiveBytes, apkSigningOptions);

					// Write pre-existing prefix data before the archive, then write the archive itself.
					if (prefix != null)
						consumer.write(prefix);

					// The rest of the owl.
					consumer.write(archiveBytes);
					consumer.commit();
					break;
				case DIRECTORY:
					for (Map.Entry<String, byte[]> entry : contents.entrySet()) {
						// Write everything relative to the path
						String relativePath = entry.getKey();
						byte[] content = entry.getValue();
						consumer.writeRelative(relativePath, content);
					}
					consumer.commit();
					break;
			}
		}

		/**
		 * @param workspace
		 * 		Workspace to pull data from.
		 */
		private void populate(@Nonnull Workspace workspace) {
			// If shading libs, they go first so the primary content will be the authoritative copy for
			// any duplicate paths held by both resources.
			if (bundleSupporting) {
				for (WorkspaceResource supportingResource : workspace.getSupportingResources()) {
					mapInto(contents, supportingResource);
				}
			}
			WorkspaceResource primary = workspace.getPrimaryResource();
			mapInto(contents, primary);

			// If the resource had prefix data, get it here so that we can write it back later.
			if (primary instanceof WorkspaceFileResource resource)
				prefix = ZipPrefixDataProperty.get(resource.getFileInfo());
		}

		/**
		 * Takes the contents of the given resource and puts them into the map.
		 *
		 * @param map
		 * 		Map to collect values into.
		 * @param resource
		 * 		Resource to pull values from.
		 */
		private void mapInto(@Nonnull Map<String, byte[]> map, @Nonnull WorkspaceResource resource) {
			// Place classes into map.
			resource.jvmClassBundleStream().forEach(bundle -> {
				for (JvmClassInfo classInfo : bundle) {
					String key;
					String originalName = PathOriginalNameProperty.get(classInfo);
					if (originalName == null) {
						String pathPrefix = PathPrefixProperty.get(classInfo);
						String pathSuffix = Objects.requireNonNullElse(PathSuffixProperty.get(classInfo), ".class");
						key = classInfo.getName() + pathSuffix;
						if (pathPrefix != null)
							key = pathPrefix + key;
					} else {
						key = originalName;
					}
					map.put(key, classInfo.getBytecode());
					updateProperties(key, classInfo);
				}
			});

			// Place versioned files into map.
			for (Map.Entry<Integer, VersionedJvmClassBundle> entry : resource.getVersionedJvmClassBundles().entrySet()) {
				String versionPath = JarFileInfo.MULTI_RELEASE_PREFIX + entry.getKey() + "/";
				for (Map.Entry<String, JvmClassInfo> classEntry : entry.getValue().entrySet()) {
					String key = versionPath + classEntry.getKey() + ".class";
					JvmClassInfo value = classEntry.getValue();
					map.put(key, value.getBytecode());
					updateProperties(key, value);
				}
			}

			// Rebuild Android DEX files and place into map.
			for (Map.Entry<String, AndroidClassBundle> entry : resource.getAndroidClassBundles().entrySet()) {
				String dexName = entry.getKey();
				AndroidClassBundle bundle = entry.getValue();
				AndroidDexMetadata metadata = bundle.getDexMetadata();

				// Collect all classes from the bundle, and convert them back into DEX definitions.
				// We match the original class order to keep the hidden-API data valid, since it
				// relies on the order of class definitions to determine offsets and flags.
				Map<String, AndroidClassInfo> currentClasses = new TreeMap<>(bundle);
				List<ClassDefinition> classDefinitions = new ArrayList<>(currentClasses.size());
				for (String className : metadata.classOrder()) {
					AndroidClassInfo classInfo = currentClasses.remove(className);
					if (classInfo != null)
						classDefinitions.add(classInfo.getBackingDefinition());
				}
				for (AndroidClassInfo classInfo : currentClasses.values())
					classDefinitions.add(classInfo.getBackingDefinition());

				// Build the DEX file, and check if the hidden-API data is still valid.
				HiddenApiData hiddenApiData = metadata.hiddenApiData();
				DexFile dexFile = new DexFile(bundle.getVersion(), classDefinitions, bundle.getLinkData(), hiddenApiData);
				DexHeader header = DexFile.CODEC.unmap(dexFile, new DexMapBuilder());
				if (hiddenApiData != null && !DexIOUtil.getEncodedClassLayout(header).equals(metadata.hiddenApiLayout())) {
					// If the hidden-API layout has changed, we can't guarantee that the offsets and flags in the hidden-API data are still valid.
					bundle.discardHiddenApiData();
					logger.warn("Dropping hidden-API metadata for DEX entry '{}' because its encoded class/member layout changed", dexName);
					dexFile = new DexFile(bundle.getVersion(), classDefinitions, bundle.getLinkData(), null);
					header = DexFile.CODEC.unmap(dexFile, new DexMapBuilder());
				}

				// Write the DEX file to a byte array, and place it into the map.
				Output output = Output.wrap();
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					DexHeader.CODEC.write(header, output);
					output.pipe(baos);
					byte[] dexBytes = baos.toByteArray();
					map.put(dexName, dexBytes);
					int mode = switch (compressType) {
						case ALWAYS -> DEFLATED;
						case NEVER -> STORED;
						case SMART -> getCompressionType(dexBytes);
						case MATCH_ORIGINAL -> metadata.entryProperties().compressionMethod() != null
								&& metadata.entryProperties().compressionMethod() == STORED ? STORED : DEFLATED;
					};
					compression.put(dexName, mode);
					updateDexProperties(dexName, metadata.entryProperties());
				} catch (IOException ex) {
					throw new IllegalStateException("Failed to write DEX file for export: " + dexName, ex);
				}
			}

			// Place files into map.
			for (FileInfo fileInfo : resource.getFileBundle()) {
				map.put(fileInfo.getName(), fileInfo.getRawContent());
				updateProperties(fileInfo.getName(), fileInfo);
			}

			// Recreate embedded resources as ZIP files with the original file paths.
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
		private void updateProperties(@Nonnull String name, @Nonnull Info info) {
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
		 * @param name
		 * 		Map key.
		 * @param properties
		 * 		Properties to pull values from.
		 */
		private void updateDexProperties(@Nonnull String name, @Nonnull DexEntryProperties properties) {
			Long createTime = properties.creationTimeMillis();
			if (createTime != null)
				createTimes.put(name, createTime);

			Long modifyTime = properties.modificationTimeMillis();
			if (modifyTime != null)
				modifyTimes.put(name, modifyTime);

			Long accessTime = properties.accessTimeMillis();
			if (accessTime != null)
				accessTimes.put(name, accessTime);

			String comment = properties.comment();
			if (comment != null)
				comments.put(name, comment);
		}

		/**
		 * @param info
		 * 		Info to get compression for.
		 *
		 * @return Compression type for into value.
		 */
		private int getCompression(@Nonnull Info info) {
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
					return getCompressionType(content);
				case MATCH_ORIGINAL:
				default:
					return ZipCompressionProperty.getOr(info, DEFLATED);

			}
		}

		/**
		 * @param content
		 * 		Content to test for compression.
		 *
		 * @return {@link ZipCompressions#DEFLATED} if the content can be compressed, otherwise {@link ZipCompressions#STORED}.
		 */
		private int getCompressionType(@Nonnull byte[] content) {
			InputStream in = new ByteArrayInputStream(content);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try (DeflaterOutputStream deflate = new DeflaterOutputStream(out)) {
				byte[] buffer = new byte[2048];
				int len;
				while ((len = in.read(buffer)) > 0)
					deflate.write(buffer, 0, len);
				deflate.finish();
				if (out.size() < content.length)
					return DEFLATED;
			} catch (IOException ignored) {
				// Cannot compress
			}
			return STORED;
		}

		/**
		 * Removes any v1 signature artifacts from {@link #contents} for APK outputs.
		 */
		private void removeV1SignatureArtifacts() {
			Iterator<String> iterator = contents.keySet().iterator();
			while (iterator.hasNext()) {
				String name = iterator.next();
				if (isV1SignatureArtifact(name)) {
					iterator.remove();
					compression.remove(name);
					comments.remove(name);
					modifyTimes.remove(name);
					createTimes.remove(name);
					accessTimes.remove(name);
				}
			}
		}

		/**
		 * @param name
		 * 		Name of the file to check.
		 *
		 * @return {@code true} if the file is a v1 signature artifact.
		 */
		private static boolean isV1SignatureArtifact(@Nonnull String name) {
			// Android's MANIFEST.MF file isn't useful like a regular JAR's MANIFEST.MF file.
			// Unlike a JAR which can use them to specify things like the main class, Android
			// just uses them for specifying the v1 signature of contents.
			if (name.equalsIgnoreCase("META-INF/MANIFEST.MF"))
				return true;

			// Any file deeper than the META-INF/ directory is not a v1 signature artifact.
			//  - META-INF/<file>.SF                -> Signature file
			//  - META-INF/something/else/<file>.SF -> Not a signature file
			int prefixLength = "META-INF/".length();
			if (name.length() <= prefixLength
					|| !name.regionMatches(true, 0, "META-INF/", 0, prefixLength)
					|| name.indexOf('/', prefixLength) >= 0)
				return false;

			// Standard jar digital signature files can be used in V1.
			if (name.regionMatches(true, prefixLength, "SIG-", 0, 4))
				return true;

			// Find extension-based signature files in the META-INF/ directory.
			int extensionStart = name.lastIndexOf('.');
			if (extensionStart <= prefixLength)
				return false;
			return switch (name.length() - extensionStart) {
				case 3 -> name.regionMatches(true, extensionStart, ".SF", 0, 3) ||
						name.regionMatches(true, extensionStart, ".EC", 0, 3);
				case 4 -> name.regionMatches(true, extensionStart, ".RSA", 0, 4) ||
						name.regionMatches(true, extensionStart, ".DSA", 0, 4);
				default -> false;
			};
		}
	}
}
