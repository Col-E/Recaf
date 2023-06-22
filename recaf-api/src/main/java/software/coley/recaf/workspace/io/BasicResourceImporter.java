package software.coley.recaf.workspace.io;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.llzip.format.model.CentralDirectoryFileHeader;
import software.coley.llzip.format.model.ZipArchive;
import software.coley.llzip.util.ByteData;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.*;
import software.coley.recaf.info.builder.FileInfoBuilder;
import software.coley.recaf.info.properties.builtin.*;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.*;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.ByteSources;
import software.coley.recaf.util.io.LocalFileHeaderSource;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicFileBundle;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

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
	private WorkspaceResource handleSingle(WorkspaceResourceBuilder builder,
										   String pathName, ByteSource source) throws IOException {
		// Read input as raw info in order to determine file-type.
		Info readInfo = infoImporter.readInfo(pathName.substring(pathName.lastIndexOf('/') + 1), source);

		// Associate input path with the read value.
		InputFilePathProperty.set(readInfo, Paths.get(pathName));

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
			return builder.withFileInfo(fileInfo)
					.withJvmClassBundle(bundle)
					.build();
		}

		// Must be some non-class type of file.
		FileInfo readInfoAsFile = readInfo.asFile();
		builder = builder.withFileInfo(readInfoAsFile);

		// Check for general ZIP container format (ZIP/JAR/WAR/APK/JMod)
		if (readInfoAsFile.isZipFile()) {
			ZipFileInfo readInfoAsZip = readInfoAsFile.asZipFile();
			return handleZip(builder, readInfoAsZip, source);
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

	private WorkspaceFileResource handleZip(WorkspaceResourceBuilder builder, ZipFileInfo zipInfo, ByteSource source) throws IOException {
		logger.info("Reading input from ZIP container '{}'", zipInfo.getName());
		builder.withFileInfo(zipInfo);
		BasicJvmClassBundle classes = new BasicJvmClassBundle();
		BasicFileBundle files = new BasicFileBundle();
		Map<String, AndroidClassBundle> androidClassBundles = new HashMap<>();
		NavigableMap<Integer, JvmClassBundle> versionedJvmClassBundles = new TreeMap<>();
		Map<String, WorkspaceFileResource> embeddedResources = new HashMap<>();

		// Read ZIP entries
		ZipArchive archive = config.getZipStrategy().getValue().mapping().apply(source.readAll());
		archive.getLocalFiles().forEach(header -> {
			LocalFileHeaderSource headerSource = new LocalFileHeaderSource(header);
			String entryName = header.getFileNameAsString();

			// Skip directories. There is no such thing as a 'directory' entry in ZIP files.
			// The only thing we can say is that if it ends with a '/' and has no data associated with it,
			// then it is probably a directory.
			if (entryName.endsWith("/") && Unchecked.getOr(headerSource::isEmpty, false))
				return;

			// Skip the following cases:
			//  - zero-length directories
			//  - path traversal attempts
			if (entryName.contains("//") || entryName.contains("../"))
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
			CentralDirectoryFileHeader centralHeader = header.getLinkedDirectoryFileHeader();
			ZipCompressionProperty.set(info, header.getCompressionMethod());
			if (centralHeader.getFileCommentLength() > 0)
				ZipCommentProperty.set(info, centralHeader.getFileCommentAsString());
			int extraLen = header.getExtraFieldLength();
			if (extraLen > 0 && extraLen < 0xFFFF) {
				// Reimplementation of 'java.util.zip.ZipEntry#setExtra0(...)'
				ByteData extra = header.getExtraField();
				int off = 0;
				int len = (int) extra.length();
				while (off + 4 < len) {
					int tag = extra.getShort(off);
					int size = extra.getShort(off + 2);
					off += 4;
					if (off + size > len)
						break;
					if (tag == /* EXTID_NTFS */ 0xA) {
						if (size < 32) // reserved  4 bytes + tag 2 bytes + size 2 bytes
							break;   // m[a|c]time 24 bytes
						int pos = off + 4;
						if (extra.getShort(pos) != 0x0001 || extra.getShort(pos + 2) != 24)
							break;
						long wtime;
						wtime = extra.getInt(pos + 4) | ((long) extra.getInt(pos + 8) << 32);
						if (wtime != Long.MIN_VALUE) {
							ZipModificationTimeProperty.set(info, ZipCreationUtils.winTimeToFileTime(wtime).toMillis());
						}
						wtime = extra.getInt(pos + 12) | ((long) extra.getInt(pos + 16) << 32);
						if (wtime != Long.MIN_VALUE) {
							ZipAccessTimeProperty.set(info, ZipCreationUtils.winTimeToFileTime(wtime).toMillis());
						}
						wtime = extra.getInt(pos + 20) | ((long) extra.getInt(pos + 8) << 24);
						if (wtime != Long.MIN_VALUE) {
							ZipCreationTimeProperty.set(info, ZipCreationUtils.winTimeToFileTime(wtime).toMillis());
						}
					} else if (tag == /* EXTID_EXTT */ 0x5455) {
						int flag = extra.get(off);
						int localOff = 1;
						// The CEN-header extra field contains the modification
						// time only, or no timestamp at all. 'sz' is used to
						// flag its presence or absence. But if mtime is present
						// in LOC it must be present in CEN as well.
						if ((flag & 0x1) != 0 && (localOff + 4) <= size) {
							// get32S(extra, off + localOff)
							ZipModificationTimeProperty.set(info, ZipCreationUtils.unixTimeToFileTime(extra.getInt(off + localOff)).toMillis());
							localOff += 4;
						}
						if ((flag & 0x2) != 0 && (localOff + 4) <= size) {
							ZipAccessTimeProperty.set(info, ZipCreationUtils.unixTimeToFileTime(extra.getInt(off + localOff)).toMillis());
							localOff += 4;
						}
						if ((flag & 0x4) != 0 && (localOff + 4) <= size) {
							ZipCreationTimeProperty.set(info, ZipCreationUtils.unixTimeToFileTime(extra.getInt(off + localOff)).toMillis());
							localOff += 4;
						}
					}
					off += size;
				}
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
		NavigableMap<Integer, JvmClassBundle> versionedJvmClassBundles = new TreeMap<>();
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
						 NavigableMap<Integer, JvmClassBundle> versionedJvmClassBundles,
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
							.computeIfAbsent(version, v -> new BasicJvmClassBundle());

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
					WorkspaceResourceBuilder embeddedResourceBuilder = new WorkspaceResourceBuilder()
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
			String intersection = StringUtil.getIntersection(commonPrefix, className);
			cmpExistingName = intersection + cmpExistingName.substring(commonPrefix.length());
			cmpCurrentName = intersection + cmpCurrentName.substring(commonPrefix.length());
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

		// The file-info name should be an absolute path for any non-uri driven import.
		// We have to use a path because unless we implement our own module reader, the internal API
		// only provides reader access via a path item.
		Path pathToModuleFile = Paths.get(moduleInfo.asFile().getName());
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

	@Override
	public WorkspaceResource importResource(ByteSource source) throws IOException {
		return handleSingle(new WorkspaceResourceBuilder(), "unknown.dat", source);
	}

	@Override
	public WorkspaceResource importResource(Path path) throws IOException {
		// Load name/data from path, parse into resource.
		String absolutePath = StringUtil.pathToAbsoluteString(path);
		if (Files.isDirectory(path)) {
			return handleDirectory(new WorkspaceResourceBuilder(), path);
		} else {
			ByteSource byteSource = ByteSources.forPath(path);
			return handleSingle(new WorkspaceResourceBuilder(), absolutePath, byteSource);
		}
	}

	@Override
	public WorkspaceResource importResource(URL url) throws IOException {
		// Extract name from URL
		String path = url.getFile();
		if (path.isEmpty())
			path = url.toString();
		if (path.charAt(0) == '/')
			path = path.substring(1);

		// Load content, parse into resource.
		byte[] bytes = IOUtil.toByteArray(url.openStream());
		ByteSource byteSource = ByteSources.wrap(bytes);
		return handleSingle(new WorkspaceResourceBuilder(), path, byteSource);
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
}
