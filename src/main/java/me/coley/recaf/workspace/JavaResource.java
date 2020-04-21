package me.coley.recaf.workspace;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import me.coley.recaf.parse.javadoc.DocumentationParseException;
import me.coley.recaf.parse.javadoc.Javadocs;
import me.coley.recaf.parse.source.SourceCode;
import me.coley.recaf.parse.source.SourceCodeException;
import me.coley.recaf.util.struct.ListeningMap;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.*;

import static me.coley.recaf.util.Log.*;

/**
 * An importable unit.
 *
 * @author Matt
 */
public abstract class JavaResource {
	private final ResourceKind kind;
	private List<String> skippedPrefixes = Collections.emptyList();
	private ListeningMap<String, byte[]> cachedClasses;
	private ListeningMap<String, byte[]> cachedFiles;
	private final Map<String, History> classHistory = new HashMap<>();
	private final Map<String, History> fileHistory = new HashMap<>();
	private final Set<String> dirtyClasses = new HashSet<>();
	private final Set<String> dirtyFiles = new HashSet<>();
	private final Map<String, SourceCode> classSource = new HashMap<>();
	private final Map<String, Javadocs> classDocs = new HashMap<>();
	private File classSourceFile;
	private File classDocsFile;
	private boolean isPrimary;

	/**
	 * Constructs a java resource.
	 *
	 * @param kind
	 * 		The kind of resource implementation.
	 */
	public JavaResource(ResourceKind kind) {
		this.kind = kind;
	}

	/**
	 * @return Type of referenced resource.
	 */
	public ResourceKind getKind() {
		return kind;
	}

	/**
	 * @return Prefixes to skip.
	 */
	public List<String> getSkippedPrefixes() {
		return skippedPrefixes;
	}

	/**
	 * @param name
	 * 		File name.
	 *
	 * @return {@code true} if the name if prefixed by a blacklisted item.
	 */
	protected boolean shouldSkip(String name) {
		for(String prefix : getSkippedPrefixes())
			if(name.startsWith(prefix))
				return true;
		return false;
	}

	/**
	 * @param skippedPrefixes
	 * 		Prefixes to skip.
	 */
	public void setSkippedPrefixes(List<String> skippedPrefixes) {
		this.skippedPrefixes = skippedPrefixes;
	}

	/**
	 * @return Set of classes that have been modified since initially loading.
	 */
	public Set<String> getDirtyClasses() {
		return dirtyClasses;
	}

	/**
	 * @return Set of files that have been modified since initially loading.
	 */
	public Set<String> getDirtyFiles() {
		return dirtyFiles;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return History for class. {@code null} if no save-states for the class exist.
	 */
	public History getClassHistory(String name) {
		return classHistory.get(name);
	}

	/**
	 * @return Map of all class histories.
	 */
	public Map<String, History> getClassHistory() {
		return classHistory;
	}

	/**
	 * @param name
	 * 		File name.
	 *
	 * @return History for file. {@code null} if no save-states for the file exist.
	 */
	public History getFileHistory(String name) {
		return fileHistory.get(name);
	}

	/**
	 * @return Map of all file histories.
	 */
	public Map<String, History> getFileHistory() {
		return fileHistory;
	}

	/**
	 * @return Map of class names to sources.
	 */
	public Map<String, SourceCode> getClassSources() {
		return classSource;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Source code wrapper for class.
	 */
	public SourceCode getClassSource(String name) {
		return classSource.get(name);
	}

	/**
	 * @return Map of class names to javadocs.
	 */
	public Map<String, Javadocs> getClassDocs() {
		return classDocs;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Javadocs wrapper for class.
	 */
	public Javadocs getClassDocs(String name) {
		return classDocs.get(name);
	}

	/**
	 * Create a save-state for the class.
	 *
	 * @param name
	 * 		Class name.
	 *
	 * @return {@code true} if the save-state was created successfully.
	 */
	public boolean createClassSave(String name) {
		byte[] value = cachedClasses.get(name);
		if(value == null)
			return false;
		History history = classHistory.computeIfAbsent(name, key -> new History(cachedClasses, key));
		history.push(value);
		return true;
	}

	private void addClassSave(String name, byte[] value) {
		History history = classHistory.computeIfAbsent(name, key -> new History(cachedClasses, key));
		history.push(value);
	}

	/**
	 * Create a save-state for the file.
	 *
	 * @param name
	 * 		File name.
	 *
	 * @return {@code true} if the save-state was created successfully.
	 */
	public boolean createFileSave(String name) {
		byte[] value = cachedFiles.get(name);
		if(value == null)
			return false;
		History history = fileHistory.computeIfAbsent(name, key -> new History(cachedFiles, key));
		history.push(value);
		return true;
	}

	private void addFileSave(String name, byte[] value) {
		History history = fileHistory.computeIfAbsent(name, key -> new History(cachedFiles, key));
		history.push(value);
	}

	/**
	 * @return Map of class names to their bytecode.
	 */
	public ListeningMap<String, byte[]> getClasses() {
		if(cachedClasses == null) {
			try {
				cachedClasses = new ListeningMap<>(new ConcurrentHashMap<>(loadClasses()));
				cachedClasses.getPutListeners().add((name, code) -> dirtyClasses.add(name));
				cachedClasses.getRemoveListeners().add(dirtyClasses::remove);
				// Create initial save state
				for (Map.Entry<String, byte[]> e : cachedClasses.entrySet()) {
					addClassSave(e.getKey(), e.getValue());
				}
				// Add listener to create initial save states for newly made classes
				cachedClasses.getPutListeners().add((name, code) -> {
					if (!cachedClasses.containsKey(name)) {
						addClassSave(name, code);
					}
				});
			} catch(IOException ex) {
				error(ex, "Failed to load classes from resource \"{}\"", toString());
			}
		}
		return cachedClasses;
	}

	/**
	 * @return Map of file names to their raw data.
	 */
	public ListeningMap<String, byte[]> getFiles() {
		if(cachedFiles == null) {
			try {
				cachedFiles = new ListeningMap<>(new ConcurrentHashMap<>(loadFiles()));
				cachedFiles.getPutListeners().add((name, code) -> dirtyFiles.add(name));
				cachedFiles.getRemoveListeners().add(dirtyFiles::remove);
				// Create initial save state
				for (Map.Entry<String, byte[]> e : cachedFiles.entrySet()) {
					addFileSave(e.getKey(), e.getValue());
				}
				// Add listener to create initial save states for newly made files
				cachedFiles.getPutListeners().add((name, code) -> {
					if (!cachedFiles.containsKey(name)) {
						addFileSave(name, code);
					}
				});
			} catch(IOException ex) {
				error(ex, "Failed to load files from resource \"{}\"", toString());
			}
		}
		return cachedFiles;
	}

	/**
	 * Refresh this resource.
	 */
	public void invalidate() {
		// TODO: Store old listeners (not the defaults, the user-added ones) to copy over to new maps
		cachedFiles.getPutListeners().clear();
		cachedFiles.getRemoveListeners().clear();
		cachedFiles = null;
		cachedClasses.getPutListeners().clear();
		cachedClasses.getRemoveListeners().clear();
		cachedClasses = null;
		classDocs.clear();
		classSource.clear();
		classHistory.clear();
	}

	/**
	 * @return Map of class names to their bytecode.
	 *
	 * @throws IOException
	 * 		When the resource could not be fetched or parsed.
	 */
	protected abstract Map<String, byte[]> loadClasses() throws IOException;

	/**
	 * @return Map of file names to their raw data.
	 *
	 * @throws IOException
	 * 		When the resource could not be fetched or parsed.
	 */
	protected abstract Map<String, byte[]> loadFiles() throws IOException;

	/**
	 * @param file
	 * 		File containing source code.
	 *
	 * @return Map of class names to their source code wrappers.
	 *
	 * @throws IOException
	 * 		When the file could not be fetched or parsed.
	 */
	protected Map<String, SourceCode> loadSources(File file) throws IOException {
		Map<String, SourceCode> map = new HashMap<>();
		// Will throw IO exception if the file couldn't be opened as an archive
		try (ZipFile zip = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String name = entry.getName();
				if (!name.endsWith(".java"))
					continue;
				String src = IOUtils.toString(zip.getInputStream(entry), StandardCharsets.UTF_8);
				try {
					SourceCode code = new SourceCode(this, src);
					code.analyze();
					map.put(code.getInternalName(), code);
				} catch(SourceCodeException ex) {
					error(ex, "Failed to parse source: {} in {}", name, file);
				}
			}
		}
		return map;
	}

	/**
	 * @param file
	 * 		File containing documentation.
	 *
	 * @return Map of class names to their documentation.
	 *
	 * @throws IOException
	 * 		When the file could not be fetched or parsed.
	 */
	protected Map<String, Javadocs> loadDocs(File file) throws IOException {
		Map<String, Javadocs> map = new HashMap<>();
		// Will throw IO exception if the file couldn't be opened as an archive
		try (ZipFile zip = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String name = entry.getName();
				if (!name.endsWith(".html"))
					continue;
				if (name.contains("-") || name.contains("index"))
					continue;
				String src = IOUtils.toString(zip.getInputStream(entry), StandardCharsets.UTF_8);
				try {
					Javadocs docs = new Javadocs(name, src);
					docs.parse();
					map.put(docs.getInternalName(), docs);
				} catch(DocumentationParseException ex) {
					error(ex, "Failed to parse docs: {} in {}", name, file);
				}
			}
		}
		return map;
	}

	/**
	 * Loads the source code from the given file.
	 *
	 * @param file
	 * 		File containing source code.
	 *
	 * @return {@code true} if sources have been discovered. {@code false} if no sources were
	 * found.
	 *
	 * @throws IOException
	 * 		When the file could not be fetched or parsed.
	 */
	public boolean setClassSources(File file) throws  IOException {
		this.classSourceFile = file;
		this.classSource.clear();
		this.classSource.putAll(loadSources(file));
		return !classSource.isEmpty();
	}

	/**
	 * Loads the documentation from the given file.
	 *
	 * @param file
	 * 		File containing documentation.
	 *
	 * @return {@code true} if docs have been discovered. {@code false} if no docs were
	 * found.
	 *
	 * @throws IOException
	 * 		When the file could not be fetched or parsed.
	 */
	public boolean setClassDocs(File file) throws  IOException {
		this.classDocsFile = file;
		this.classDocs.clear();
		this.classDocs.putAll(loadDocs(file));
		return !classDocs.isEmpty();
	}

	/**
	 * @return File containing source code. May be {@code null}.
	 */
	public File getClassSourceFile() {
		return classSourceFile;
	}

	/**
	 * @return File containing documentation. May be {@code null}.
	 */
	public File getClassDocsFile() {
		return classDocsFile;
	}

	/**
	 * Analyzes attached sources.
	 * This also allows workspace-wide name lookups for better type-resolving.
	 *
	 * @param workspace
	 * 		Context to analyze in. Allows application of a workspace-scoped type resolver.
	 *
	 * @return Map of class names to their parse result. If an
	 * {@link SourceCodeException} occured during analysis of a class
	 * then it's result may have {@link com.github.javaparser.ParseResult#isSuccessful()} be {@code false}.
	 */
	public Map<String, ParseResult<CompilationUnit>> analyzeSource(Workspace workspace) {
		Map<String,ParseResult<CompilationUnit>> copy = new HashMap<>();
		classSource.forEach((name, value) -> {
			try {
				copy.put(name, value.analyze(workspace));
			} catch(SourceCodeException ex) {
				error(ex, "Failed to parse source: {}", name);
				copy.put(name, ex.getResult());
			}
		});
		return copy;
	}

	/**
	 * @return {@code true} if the resource is a workspace's primary resource.
	 */
	public boolean isPrimary() {
		return isPrimary;
	}

	/**
	 * @param primary
	 *        {@code true} if the resource is a workspace's primary resource.
	 */
	public void setPrimary(boolean primary) {
		isPrimary = primary;
	}
}
// TODO: Allow resources to have update-checks, ex: the referenced resource is modified externally