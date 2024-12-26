package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.ExtraFieldTime;
import software.coley.lljzip.util.MemorySegmentUtil;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.DexFileInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.JarFileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.ModulesFileInfo;
import software.coley.recaf.info.ZipFileInfo;
import software.coley.recaf.info.builder.FileInfoBuilder;
import software.coley.recaf.info.builder.ZipFileInfoBuilder;
import software.coley.recaf.info.properties.builtin.*;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.IOUtil;
import software.coley.recaf.util.ModulesIOUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.android.DexIOUtil;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.ByteSources;
import software.coley.recaf.util.io.LocalFileHeaderSource;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicFileBundle;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicVersionedJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResourceBuilder;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Basic implementation of the resource importer.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicResourceImporter implements ResourceImporter, Service {
	private static final int MAX_ZIP_DEPTH = 3;
	private static final Logger logger = Logging.get(BasicResourceImporter.class);
	private final InfoImporter infoImporter;
	private final ResourceImporterConfig config;

	@Inject
	public BasicResourceImporter(@Nonnull InfoImporter infoImporter,
	                             @Nonnull ResourceImporterConfig config) {
		this.infoImporter = infoImporter;
		this.config = config;
	}

	/**
	 * General read handling for any single-file resource kind. Delegates to others when needed.
	 *
	 * @param builder
	 * 		Builder to work with.
	 * @param pathName
	 * 		Name of input file / content.
	 * @param source
	 * 		Access to content / data.
	 *
	 * @return Read resource.
	 */
	private WorkspaceResource handleSingle(@Nonnull WorkspaceFileResourceBuilder builder,
	                                       @Nonnull String pathName, @Nonnull ByteSource source) throws IOException {
		// Read input as raw info in order to determine file-type.
		PathAndName pathAndName = PathAndName.fromString(pathName);
		String name = pathAndName.name;
		Path localPath = pathAndName.path;
		Info readInfo = infoImporter.readInfo(name, source);

		// Check if it is a single class.
		if (readInfo.isClass()) {
			// If it is a class, we know it MUST be a single JVM class since Android classes do not exist
			// in single file form. They only come bundled in DEX files.
			JvmClassInfo readAsJvmClass = readInfo.asClass().asJvmClass();
			BasicJvmClassBundle bundle = new BasicJvmClassBundle();
			bundle.initialPut(readAsJvmClass);

			// To satisfy our file-info requirement for the file resource we can create a wrapper file-info
			// using the JVM class's bytecode.
			FileInfo fileInfo = new FileInfoBuilder<>()
					.withName(readAsJvmClass.getName() + ".class")
					.withRawContent(readAsJvmClass.getBytecode())
					.build();
			if (localPath != null)
				InputFilePathProperty.set(fileInfo, localPath); // Associate input path with the read value.
			return builder.withFileInfo(fileInfo)
					.withJvmClassBundle(bundle)
					.build();
		}

		// Associate input path with the read value.
		if (localPath != null) InputFilePathProperty.set(readInfo, localPath);

		// Must be some non-class type of file.
		FileInfo readInfoAsFile = readInfo.asFile();
		builder = builder.withFileInfo(readInfoAsFile);

		// Check for general ZIP container format (ZIP/JAR/WAR/APK/JMod)
		if (readInfoAsFile.isZipFile()) {
			ZipFileInfo readInfoAsZip = readInfoAsFile.asZipFile();
			return handleZip(builder, readInfoAsZip, source);
		} else if (ZipMarkerProperty.get(readInfoAsFile)) {
			// In some cases the file may have been matched as something else (like an executable)
			// but also count as a ZIP container. Applications that bundle Java applications into native exe files
			// tend to do this.
			try {
				return handleZip(builder, new ZipFileInfoBuilder(readInfoAsFile.toFileBuilder()).build(), source);
			} catch (Throwable t) {
				// Some files will just so happen to have a ZIP marker in their bytes but not represent an actual ZIP.
				// This is fine because by this point we have an info-type to fall back on.
				logger.debug("Saw ZIP marker in file {} but could not parse as ZIP.", name);
			}
		}

		// Check for DEX file format.
		if (readInfoAsFile instanceof DexFileInfo) {
			String dexName = readInfoAsFile.getName();
			AndroidClassBundle dexBundle = DexIOUtil.read(readInfoAsFile.getRawContent());
			return builder.withAndroidClassBundles(Map.of(dexName, dexBundle))
					.build();
		}

		// Must be some edge case type: Modules, or an unknown file type
		if (readInfoAsFile instanceof ModulesFileInfo) {
			return handleModules(builder, (ModulesFileInfo) readInfoAsFile);
		}

		// Unknown file type
		BasicFileBundle bundle = new BasicFileBundle();
		bundle.initialPut(readInfoAsFile);
		return builder
				.withFileBundle(bundle)
				.build();
	}

	private WorkspaceFileResource handleZip(WorkspaceFileResourceBuilder builder, ZipFileInfo zipInfo, ByteSource source) throws IOException {
		logger.info("Reading input from ZIP container '{}'", zipInfo.getName());
		builder.withFileInfo(zipInfo);
		BasicJvmClassBundle classes = new BasicJvmClassBundle();
		BasicFileBundle files = new BasicFileBundle();
		Map<String, AndroidClassBundle> androidClassBundles = new HashMap<>();
		NavigableMap<Integer, VersionedJvmClassBundle> versionedJvmClassBundles = new TreeMap<>();
		Map<String, WorkspaceFileResource> embeddedResources = new HashMap<>();

		// Read ZIP
		boolean isAndroid = zipInfo.getName().toLowerCase().endsWith(".apk");
		ZipArchive archive = config.mapping().apply(source.readAll());

		// Sanity check, if there's data at the head of the file AND its otherwise empty its probably junk.
		MemorySegment prefixData = archive.getPrefixData();
		if (prefixData != null && archive.getEnd() != null && archive.getParts().size() == 1) {
			// We'll throw as the caller should catch this case and handle it based on their needs.
			throw new IOException("Content matched ZIP header but had no file entries");
		}

		// Record prefix data to attribute held by the zip file info.
		if (prefixData != null) {
			ZipPrefixDataProperty.set(zipInfo, MemorySegmentUtil.toByteArray(prefixData));
		}

		// Build model from the contained files in the ZIP
		archive.getLocalFiles().forEach(header -> {
			LocalFileHeaderSource headerSource = new LocalFileHeaderSource(header, isAndroid);
			String entryName = header.getFileNameAsString();

			// Skip directories. There is no such thing as a 'directory' entry in ZIP files.
			// The only thing we can say is that if it ends with a '/' and has no data associated with it,
			// then it is probably a directory.
			if (entryName.endsWith("/") && Unchecked.getOr(headerSource::isEmpty, false))
				return;

			// Read the value of the entry to figure out how to handle adding it to the resource builder.
			Info info;
			try {
				info = infoImporter.readInfo(entryName, headerSource);
			} catch (IOException ex) {
				logger.error("IO error reading ZIP entry '{}' - skipping", entryName, ex);
				return;
			}

			// Record common entry attributes
			ZipCompressionProperty.set(info, header.getCompressionMethod());
			ExtraFieldTime.TimeWrapper extraTimes = ExtraFieldTime.read(header);
			CentralDirectoryFileHeader centralHeader = header.getLinkedDirectoryFileHeader();
			if (centralHeader != null) {
				if (centralHeader.getFileCommentLength() > 0)
					ZipCommentProperty.set(info, centralHeader.getFileCommentAsString());
				if (extraTimes == null)
					extraTimes = ExtraFieldTime.read(centralHeader);
			}
			if (extraTimes != null) {
				ZipCreationTimeProperty.set(info, extraTimes.getCreationMs());
				ZipModificationTimeProperty.set(info, extraTimes.getModifyMs());
				ZipAccessTimeProperty.set(info, extraTimes.getAccessMs());
			}

			// Skipping ZIP bombs
			if (info.isFile() && info.asFile().isZipFile()) {
				ZipFileInfo zipFile = info.asFile().asZipFile();
				if (Arrays.equals(zipFile.getRawContent(), zipInfo.getRawContent())) {
					logger.warn("Skip self-extracting ZIP bomb: {}", entryName);
					return;
				} else if (Arrays.stream(Thread.currentThread().getStackTrace())
						.filter(trace -> trace.getMethodName().equals("handleZip"))
						.count() > MAX_ZIP_DEPTH) {
					logger.warn("Skip extracting embedded ZIP after {} levels: {}", MAX_ZIP_DEPTH, entryName);
					return;
				}
			}

			// Add the info to the appropriate bundle
			addInfo(classes, files, androidClassBundles, versionedJvmClassBundles, embeddedResources,
					headerSource, entryName, info);
		});
		return builder
				.withJvmClassBundle(classes)
				.withAndroidClassBundles(androidClassBundles)
				.withVersionedJvmClassBundles(versionedJvmClassBundles)
				.withFileBundle(files)
				.withEmbeddedResources(embeddedResources)
				.withFileInfo(zipInfo)
				.build();
	}

	private WorkspaceDirectoryResource handleDirectory(WorkspaceResourceBuilder builder, Path directoryPath) throws IOException {
		logger.info("Reading input from directory '{}'", directoryPath);
		BasicJvmClassBundle classes = new BasicJvmClassBundle();
		BasicFileBundle files = new BasicFileBundle();
		Map<String, AndroidClassBundle> androidClassBundles = new HashMap<>();
		NavigableMap<Integer, VersionedJvmClassBundle> versionedJvmClassBundles = new TreeMap<>();
		Map<String, WorkspaceFileResource> embeddedResources = new HashMap<>();

		// Walk the directory
		Files.walkFileTree(directoryPath, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				try {
					// Read info from file
					ByteSource source = ByteSources.forPath(file);
					String fileName = directoryPath.relativize(file).toString();
					if (File.separator.equals("\\"))
						fileName = fileName.replace('\\', '/');
					Info info = infoImporter.readInfo(fileName, source);

					// Add the info to the appropriate bundle
					addInfo(classes, files, androidClassBundles, versionedJvmClassBundles, embeddedResources,
							source, fileName, info);
				} catch (IOException ex) {
					logger.error("IO error reading ZIP entry '{}' - skipping", file, ex);
				}

				return FileVisitResult.CONTINUE;
			}
		});
		return builder
				.withJvmClassBundle(classes)
				.withAndroidClassBundles(androidClassBundles)
				.withVersionedJvmClassBundles(versionedJvmClassBundles)
				.withFileBundle(files)
				.withEmbeddedResources(embeddedResources)
				.withDirectoryPath(directoryPath)
				.build();
	}

	private void addInfo(BasicJvmClassBundle classes,
	                     BasicFileBundle files,
	                     Map<String, AndroidClassBundle> androidClassBundles,
	                     NavigableMap<Integer, VersionedJvmClassBundle> versionedJvmClassBundles,
	                     Map<String, WorkspaceFileResource> embeddedResources,
	                     ByteSource infoSource,
	                     String pathName,
	                     Info info) {
		if (info.isClass()) {
			// Must be a JVM class since Android classes do not exist in single-file form.
			JvmClassInfo classInfo = info.asClass().asJvmClass();
			String className = classInfo.getName();

			// JVM edge case allows trailing '/' for class entries in JARs.
			// We're going to normalize that away.
			if (pathName.endsWith(".class/")) {
				pathName = pathName.replace(".class/", ".class");
			}

			// Record the class name, including path suffix/prefix.
			// If the name is totally different, record the original path name.
			int index = pathName.indexOf(className);
			if (index >= 0) {
				// Class name is within the entry name.
				// Record the prefix before the class name, and suffix after it (extension).
				if (index > 0) {
					String prefix = pathName.substring(0, index);
					PathPrefixProperty.set(classInfo, prefix);
				}
				int suffixIndex = index + className.length();
				if (suffixIndex < pathName.length()) {
					String suffix = pathName.substring(suffixIndex);
					PathSuffixProperty.set(classInfo, suffix);
				}
			} else {
				// Class name doesn't match entry name.
				PathOriginalNameProperty.set(classInfo, pathName);
			}

			// First we must handle edge cases. Up first, we'll look at multi-release jar prefixes.
			if (pathName.startsWith(JarFileInfo.MULTI_RELEASE_PREFIX) &&
					!className.startsWith(JarFileInfo.MULTI_RELEASE_PREFIX)) {
				String versionName = "<null>";
				try {
					// Extract version from '<prefix>/version/<class-name>' pattern
					int startOffset = JarFileInfo.MULTI_RELEASE_PREFIX.length();
					int slashIndex = pathName.indexOf('/', startOffset);
					if (slashIndex < 0)
						throw new NumberFormatException("Version name is null");
					versionName = pathName.substring(startOffset, slashIndex);

					// Only add if the names match
					int classStart = slashIndex + 1;
					int classEnd = pathName.length() - ".class".length();
					if (classEnd > classStart) {
						String classPath = pathName.substring(classStart, classEnd);
						if (!classPath.equals(className))
							throw new IllegalArgumentException("Class in multi-release directory" +
									" does not match it's declared class name: " + classPath);
					} else {
						throw new IllegalArgumentException("Class in multi-release directory " +
								"does not end in '.class'");
					}

					// Put it into the correct versioned class bundle.
					int version = Integer.parseInt(versionName);
					BasicJvmClassBundle bundle = (BasicJvmClassBundle) versionedJvmClassBundles
							.computeIfAbsent(version, BasicVersionedJvmClassBundle::new);

					// Handle duplicate classes
					JvmClassInfo existingClass = bundle.get(className);
					if (existingClass != null) {
						deduplicateClass(existingClass, classInfo, bundle, files);
					} else {
						VersionedClassProperty.set(classInfo, version);
						bundle.initialPut(classInfo);
					}
				} catch (NumberFormatException ex) {
					// Version is invalid, record it as a file instead.
					logger.warn("Class entry seemed to be for multi-release jar, " +
							"but version is non-numeric value: " + versionName);

					// Override the prior value.
					// The JVM always selects the last option if there are duplicates.
					files.initialPut(new FileInfoBuilder<>()
							.withName(pathName)
							.withRawContent(classInfo.getBytecode())
							.build());
				} catch (IllegalArgumentException ex) {
					// Class name doesn't match what is declared locally in the versioned folder.
					logger.warn("Class entry seemed to be for multi-release jar, " +
							"but the name doesn't align with the declared type: " + pathName);

					// Override the prior value.
					// The JVM always selects the last option if there are duplicates.
					files.initialPut(new FileInfoBuilder<>()
							.withName(pathName)
							.withRawContent(classInfo.getBytecode())
							.build());
				}
				return;
			}

			// Handle duplicate classes
			JvmClassInfo existingClass = classes.get(className);
			if (existingClass != null) {
				deduplicateClass(existingClass, classInfo, classes, files);
			} else {
				classes.initialPut(classInfo);
			}
		} else if (info.isFile()) {
			FileInfo fileInfo = info.asFile();

			// Check for special file cases (Currently just DEX)
			if (fileInfo instanceof DexFileInfo) {
				try {
					AndroidClassBundle dexBundle = DexIOUtil.read(infoSource);
					androidClassBundles.put(pathName, dexBundle);
					return;
				} catch (IOException ex) {
					logger.error("Failed to read embedded DEX '{}'", pathName, ex);
				}
			}

			// Check for container file cases (Any ZIP type, JAR/WAR/etc)
			if (fileInfo.isZipFile()) {
				try {
					WorkspaceFileResourceBuilder embeddedResourceBuilder = new WorkspaceFileResourceBuilder()
							.withFileInfo(fileInfo);
					WorkspaceFileResource embeddedResource = handleZip(embeddedResourceBuilder,
							fileInfo.asZipFile(), infoSource);
					embeddedResources.put(pathName, embeddedResource);
				} catch (IOException ex) {
					logger.error("Failed to read embedded ZIP '{}'", pathName, ex);
				}
				return;
			}

			// Check for other edge case types containing embedded content.
			if (fileInfo instanceof ModulesFileInfo) {
				try {
					WorkspaceResourceBuilder embeddedResourceBuilder = new WorkspaceResourceBuilder()
							.withFileInfo(fileInfo);
					WorkspaceFileResource embeddedResource =
							(WorkspaceFileResource) handleModules(embeddedResourceBuilder, (ModulesFileInfo) fileInfo);
					embeddedResources.put(pathName, embeddedResource);
				} catch (IOException ex) {
					logger.error("Failed to read embedded ZIP '{}'", pathName, ex);
				}
				return;
			}

			// Warn if there are duplicate file entries.
			// Same cases for why this may occur are described above when handling classes.
			// The JVM will always use the last item for duplicate entries anyways.
			if (files.containsKey(pathName)) {
				logger.warn("Multiple duplicate entries for file '{}', dropping older entry", pathName);
			}

			// Store in bundle.
			files.initialPut(fileInfo);
		} else {
			throw new IllegalStateException("Unknown info type: " + info);
		}
	}

	/**
	 * Should <i>ONLY</i> be called if there is an existing duplicate/conflict in the given JVM class bundle.
	 *
	 * @param existingClass
	 * 		Prior class entry in the class bundle.
	 * @param currentClass
	 * 		New entry to de-duplicate.
	 * @param classes
	 * 		Target class bundle.
	 * @param files
	 * 		Target file bundle for fallback item placement.
	 */
	private void deduplicateClass(JvmClassInfo existingClass, JvmClassInfo currentClass,
	                              BasicJvmClassBundle classes, BasicFileBundle files) {
		String className = currentClass.getName();
		String existingPrefix = PathPrefixProperty.get(existingClass);
		String existingSuffix = PathSuffixProperty.get(existingClass);
		String existingOriginal = PathOriginalNameProperty.get(existingClass);
		String currentPrefix = PathPrefixProperty.get(currentClass);
		String currentSuffix = PathSuffixProperty.get(currentClass);
		String currentOriginal = PathOriginalNameProperty.get(currentClass);

		// The target names to use should we want to store the items as files
		String existingName = existingOriginal != null ? existingOriginal :
				(existingPrefix != null ? existingPrefix : "") + className +
						(existingSuffix != null ? existingSuffix : "");
		String currentName = currentOriginal != null ? currentOriginal :
				(currentPrefix != null ? currentPrefix : "") + className +
						(currentSuffix != null ? currentSuffix : "");

		// Check for literal duplicate ZIP entries.
		if (existingName.equals(currentName)) {
			// The new name is an exact match, but occurs later in the file.
			// Since the JVM prefers the last entry of a set of duplicates we will drop the prior value.
			logger.warn("Dropping prior class duplicate, matched exact file path: {}", className);
			classes.initialPut(currentClass);
			return;
		}

		// Ok, so the path names aren't the same.
		// We'll want to normalize the paths and compare them. Whichever is best fit to be the JVM class will be kept
		// in the classes bundle. The worse fit goes to the files bundle. If we aren't sure then the newest entry
		// lands in the JVM bundle.

		// Normalize prefix/suffix
		if (Objects.equals(existingPrefix, currentPrefix)) {
			existingPrefix = null;
			currentPrefix = null;
		}
		if (Objects.equals(existingSuffix, currentSuffix)) {
			existingSuffix = null;
			currentSuffix = null;
		}

		// Names to use for comparison purposes
		String cmpExistingName = existingOriginal != null ? existingOriginal :
				(existingPrefix != null ? existingPrefix : "") + className +
						(existingSuffix != null ? existingSuffix : "");
		String cmpCurrentName = currentOriginal != null ? currentOriginal :
				(currentPrefix != null ? currentPrefix : "") + className +
						(currentSuffix != null ? currentSuffix : "");

		// Try and get class names via the file paths and determine which is the best fit to the real class name.
		String commonPrefix = StringUtil.getCommonPrefix(cmpExistingName, cmpCurrentName);
		if (commonPrefix.startsWith(JarFileInfo.MULTI_RELEASE_PREFIX)) {
			// Class names start at the '<prefix>/<version>/'
			int i = commonPrefix.indexOf('/', JarFileInfo.MULTI_RELEASE_PREFIX.length()) + 1;
			cmpExistingName = cmpExistingName.substring(i);
			cmpCurrentName = cmpCurrentName.substring(i);
		} else if (!commonPrefix.isEmpty()) {
			// Class names should start at the common prefix minus the intersection of the class name
			cmpExistingName = commonPrefix + cmpExistingName.substring(commonPrefix.length());
			cmpCurrentName = commonPrefix + cmpCurrentName.substring(commonPrefix.length());
		}

		// Best fit checking
		if (cmpExistingName.equals(className + ".class")) {
			// The existing class entry name IS the class name. Thus, the other (current) one does not match.
			// We will add the current one as a file instead, and keep the prior as a class.
			logger.warn("Duplicate class '{}' found. The prior entry better aligns to class name so the new one " +
					"will be tracked as a file instead: {}", className, currentName);
			files.initialPut(new FileInfoBuilder<>()
					.withName(currentName)
					.withRawContent(currentClass.getBytecode())
					.build());
		} else if (cmpCurrentName.equals(className + ".class")) {
			// The current class entry name IS the class name. Thus, the other (prior) one does not match.
			// We will add the prior one as a file, and record this new one as a class
			logger.warn("Duplicate class '{}' found. The new entry better aligns to class name so the prior one " +
					"will be tracked as a file instead: {}", className, existingName);
			VersionedClassProperty.remove(existingClass);
			files.initialPut(new FileInfoBuilder<>()
					.withName(existingName)
					.withRawContent(existingClass.getBytecode())
					.build());
			classes.initialPut(currentClass);
		} else {
			// Neither of them really follow the class name accurately. We'll just record the last one as the JVM class
			// because that more accurately follows JVM behavior.
			logger.warn("Duplicate class '{}' found. Neither entry match their class names," +
					" tracking the newer item as the JVM class and retargeting the old item as a file: {}", className, existingName);
			VersionedClassProperty.remove(existingClass);
			files.initialPut(new FileInfoBuilder<>()
					.withName(existingName)
					.withRawContent(existingClass.getBytecode())
					.build());
			classes.initialPut(currentClass);
		}
	}

	private WorkspaceResource handleModules(WorkspaceResourceBuilder builder, ModulesFileInfo moduleInfo) throws IOException {
		BasicJvmClassBundle classes = new BasicJvmClassBundle();
		BasicFileBundle files = new BasicFileBundle();

		// The file-info should have the absolute path set as a property.
		// We have to use a path because unless we implement our own module reader, the internal API
		// only provides reader access via a path item.
		Path pathToModuleFile = InputFilePathProperty.get(moduleInfo);
		ModulesIOUtil.stream(pathToModuleFile)
				.forEach(entry -> {
					// Follows the pattern: /<module-name>/<file-name>
					//  - entry extracts these values
					ModulesIOUtil.Entry moduleEntry = entry.getElement();
					ByteSource moduleFileSource = entry.getByteSource();
					Info info;
					try {
						info = infoImporter.readInfo(moduleEntry.getFileName(), moduleFileSource);
					} catch (IOException ex) {
						logger.error("IO error reading modules entry '{}' - skipping", moduleEntry.getOriginalPath());
						return;
					}
					// Add to appropriate bundle.
					// Modules file only has two expected kinds of content, classes and generic files.
					if (info.isClass()) {
						// Modules file only contains JVM classes
						classes.initialPut(info.asClass().asJvmClass());
					} else {
						// Anything else should be a general file
						files.initialPut(info.asFile());
					}

					// Record the original prefix '/<module-name>/' for the input
					PathPrefixProperty.set(info, "/" + moduleEntry.getModuleName() + "/");
				});

		return builder
				.withJvmClassBundle(classes)
				.withFileBundle(files)
				.build();
	}

	@Nonnull
	@Override
	public WorkspaceResource importResource(@Nonnull ByteSource source) throws IOException {
		return handleSingle(new WorkspaceFileResourceBuilder(), "unknown.dat", source);
	}

	@Nonnull
	@Override
	public WorkspaceResource importResource(@Nonnull Path path) throws IOException {
		// Load name/data from path, parse into resource.
		String absolutePath = StringUtil.pathToAbsoluteString(path);
		if (Files.isDirectory(path)) {
			return handleDirectory(new WorkspaceFileResourceBuilder(), path);
		} else {
			ByteSource byteSource = ByteSources.forPath(path);
			return handleSingle(new WorkspaceFileResourceBuilder(), absolutePath, byteSource);
		}
	}

	@Nonnull
	@Override
	public WorkspaceResource importResource(@Nonnull URL url) throws IOException {
		// Extract name from URL
		String path;
		if (url.getProtocol().equals("file")) {
			path = url.getFile();
			if (path.isEmpty())
				path = url.toString();
			if (path.charAt(0) == '/')
				path = path.substring(1);
		} else {
			path = url.toString();
		}

		// Load content, parse into resource.
		byte[] bytes = IOUtil.toByteArray(url.openStream());
		ByteSource byteSource = ByteSources.wrap(bytes);
		return handleSingle(new WorkspaceFileResourceBuilder(), path, byteSource);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public ResourceImporterConfig getServiceConfig() {
		return config;
	}

	private record PathAndName(@Nullable Path path, @Nonnull String name) {
		@Nonnull
		private static PathAndName fromString(@Nonnull String pathName) {
			if (pathName.contains("://")) {
				// Absolute URI paths
				if (pathName.startsWith("file://")) {
					return fromUriString(pathName);
				} else {
					// Probably something like "https://foo.com/bar.zip"
					// Try normalizing a simple name out of it if possible.
					while (pathName.endsWith("/")) pathName = pathName.substring(0, pathName.length() - 1);
					String name = pathName.substring(pathName.lastIndexOf('/') + 1);
					if (!name.matches("\\w+")) name = "remote";
					return new PathAndName(null, name);
				}
			} else if (pathName.startsWith("file:./")) {
				// Relative URI paths
				return fromUriString(pathName);
			} else {
				// Probably local file paths
				Path localPath;
				try {
					// Try and resolve a file path to the give path-name.
					// In some cases the input name is a remote resource not covered by the block above,
					// so we don't really care if it fails. That just means it is a remote resource of some kind.
					localPath = Paths.get(pathName);
				} catch (Throwable t) {
					localPath = null;
				}
				return new PathAndName(localPath, pathName.substring(pathName.lastIndexOf('/') + 1));
			}
		}

		@Nonnull
		private static PathAndName fromUriString(@Nonnull String pathName) {
			String name;
			Path localPath;
			name = pathName.substring(pathName.lastIndexOf('/') + 1);
			try {
				// Try and resolve a file path to the give path-name.
				// It should be an absolute path.
				localPath = Paths.get(URI.create(pathName));
			} catch (Throwable t) {
				localPath = null;
			}
			return new PathAndName(localPath, name);
		}
	}
}
