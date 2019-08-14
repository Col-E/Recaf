package me.coley.recaf.workspace;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import me.coley.recaf.parse.javadoc.DocumentationParseException;
import me.coley.recaf.parse.javadoc.Javadocs;
import me.coley.recaf.parse.source.SourceCode;
import me.coley.recaf.parse.source.SourceCodeException;
import me.coley.recaf.util.struct.ListeningMap;
import org.apache.commons.io.IOUtils;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/**
 * An importable unit.
 *
 * @author Matt
 */
public abstract class JavaResource {
	private final ResourceKind kind;
	private List<String> skippedPrefixes = Collections.emptyList();
	private ListeningMap<String, byte[]> cachedClasses;
	private ListeningMap<String, byte[]> cachedResources;
	private final Map<String, History> classHistory = new HashMap<>();
	private final Map<String, History> resourceHistory = new HashMap<>();
	private final Set<String> dirtyClasses = new HashSet<>();
	private final Set<String> dirtyResources = new HashSet<>();
	private final Map<String, SourceCode> classSource = new HashMap<>();
	private final Map<String, Javadocs> classDocs = new HashMap<>();

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
	 * @return Set of resources that have been modified since initially loading.
	 */
	public Set<String> getDirtyResources() {
		return dirtyResources;
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
	 * @param name
	 * 		Resource name.
	 *
	 * @return History for resource. {@code null} if no save-states for the resource exist.
	 */
	public History getResourceHistory(String name) {
		return resourceHistory.get(name);
	}

	/**
	 * @return Map of class namees to sources.
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
	 * Create a save-state for the resource.
	 *
	 * @param name
	 * 		Resource name.
	 *
	 * @return {@code true} if the save-state was created successfully.
	 */
	public boolean createResourceSave(String name) {
		byte[] value = cachedResources.get(name);
		if(value == null)
			return false;
		History history = resourceHistory.computeIfAbsent(name, key -> new History(cachedResources, key));
		history.push(value);
		return true;
	}

	private void addResourceSave(String name, byte[] value) {
		History history = resourceHistory.computeIfAbsent(name, key -> new History(cachedResources, key));
		history.push(value);
	}

	/**
	 * @return Map of class names to their bytecode.
	 */
	public ListeningMap<String, byte[]> getClasses() {
		if(cachedClasses == null) {
			try {
				cachedClasses = new ListeningMap<>(loadClasses());
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
				Logger.error(ex, "Failed to load classes from resource \"{}\"", this);
			}
		}
		return cachedClasses;
	}

	/**
	 * @return Map of resource names to their raw data.
	 */
	public ListeningMap<String, byte[]> getResources() {
		if(cachedResources == null) {
			try {
				cachedResources = new ListeningMap<>(loadResources());
				cachedResources.getPutListeners().add((name, code) -> dirtyResources.add(name));
				cachedResources.getRemoveListeners().add(dirtyResources::remove);
				// Create initial save state
				for (Map.Entry<String, byte[]> e : cachedResources.entrySet()) {
					addResourceSave(e.getKey(), e.getValue());
				}
				// Add listener to create initial save states for newly made resources
				cachedResources.getPutListeners().add((name, code) -> {
					if (!cachedResources.containsKey(name)) {
						addResourceSave(name, code);
					}
				});
			} catch(IOException ex) {
				Logger.error(ex, "Failed to load resources from resource \"{}\"", this);
			}
		}
		return cachedResources;
	}

	/**
	 * Refresh this resource.
	 */
	public void invalidate() {
		// TODO: Store old listeners (if existing) to copy over to new maps
		cachedResources = null;
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
	 * @return Map of resource names to their raw data.
	 *
	 * @throws IOException
	 * 		When the resource could not be fetched or parsed.
	 */
	protected abstract Map<String, byte[]> loadResources() throws IOException;

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
					Logger.warn("Failed to parse source: " + name + " in " + file, ex);
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
					Logger.warn("Failed to parse docs: " + name + " in " + file, ex);
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
		this.classDocs.clear();
		this.classDocs.putAll(loadDocs(file));
		return !classDocs.isEmpty();
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
				Logger.warn("Failed to parse source: " + name, ex);
				copy.put(name, ex.getResult());
			}
		});
		return copy;
	}
}
// TODO: Allow resources to have update-checks, ex: the referenced resource is modified externally