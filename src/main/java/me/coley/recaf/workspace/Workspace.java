package me.coley.recaf.workspace;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import me.coley.recaf.Recaf;
import me.coley.recaf.compiler.JavacCompiler;
import me.coley.recaf.control.Controller;
import me.coley.recaf.control.headless.HeadlessController;
import me.coley.recaf.graph.flow.FlowGraph;
import me.coley.recaf.graph.inheritance.HierarchyGraph;
import me.coley.recaf.mapping.AsmMappingUtils;
import me.coley.recaf.parse.javadoc.Javadocs;
import me.coley.recaf.parse.source.*;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.ThreadUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Input manager
 *
 * @author Matt
 */
public class Workspace {
	private static final LazyClasspathResource CP = LazyClasspathResource.get();
	private final Map<String, String> aggregatedMappings = new TreeMap<>();
	private final PhantomResource phantoms = new PhantomResource();
	private final JavaResource primary;
	private final List<JavaResource> libraries;
	private HierarchyGraph hierarchyGraph;
	private FlowGraph flowGraph;
	private ParserConfiguration config;

	/**
	 * Constructs a workspace.
	 *
	 * @param primary
	 * 		Primary resource containing the content to analyze and modify.
	 */
	public Workspace(JavaResource primary) {
		this(primary, new ArrayList<>());
	}

	/**
	 * Constructs a workspace.
	 *
	 * @param primary
	 * 		Primary resource containing the content to analyze and modify.
	 * @param libraries
	 * 		Backing resources used for reference.
	 */
	public Workspace(JavaResource primary, List<JavaResource> libraries) {
		this.primary = primary;
		this.primary.setPrimary(true);
		this.libraries = libraries;
	}

	/**
	 * @return Primary file being worked on.
	 */
	public JavaResource getPrimary() {
		return primary;
	}

	/**
	 * @return Libraries of the {@link #getPrimary() primary file}.
	 */
	public List<JavaResource> getLibraries() {
		return libraries;
	}

	/**
	 * @return Recaf managed resource containing phantom references.
	 */
	public PhantomResource getPhantoms() {
		return phantoms;
	}

	/**
	 * @return Inheritance hierarchy utility.
	 */
	public HierarchyGraph getHierarchyGraph() {
		if(hierarchyGraph == null)
			hierarchyGraph = new HierarchyGraph(this);
		return hierarchyGraph;
	}

	/**
	 * @return Method flow utility.
	 */
	public FlowGraph getFlowGraph() {
		if(flowGraph == null)
			flowGraph = new FlowGraph(this);
		return flowGraph;
	}

	/**
	 * @return Aggregated ASM mappings for the workspace.
	 */
	public Map<String, String> getAggregatedMappings() {
		return Collections.unmodifiableMap(aggregatedMappings);
	}

	// ====================================== RENAME UTILS ====================================== //

	private Set<String> definitionUpdatedClasses = Collections.emptySet();

	/**
	 * @return File location of temporary primary jar.
	 */
	public File getTemporaryPrimaryDefinitionJar() {
		return JavacCompiler.getCompilerClasspathDirectory().resolve("primary.jar").toFile();
	}

	/**
	 * Called when any definitions in the primary jar are updated. This is necessary when
	 * supporting recompilation since we will need updated class and members definitions.
	 *
	 * @param classes
	 * 		The set of class names that have been updated as a result of the definition changes.
	 */
	public void onPrimaryDefinitionChanges(Set<String> classes) {
		definitionUpdatedClasses = classes;
	}

	/**
	 * Updated after calls to {@link #onPrimaryDefinitionChanges(Set)}.
	 *
	 * @return The set of class names that have been updated as a result of the definition changes.
	 */
	public Set<String> getDefinitionUpdatedClasses() {
		return definitionUpdatedClasses;
	}

	/**
	 * Update the generated jar file
	 */
	public void analyzePhantoms() {
		Controller controller = Recaf.getController();
		if (controller == null || controller instanceof HeadlessController) {
			// If we're using a headless controller, we very likely do not need to create phantom references.
			// (Realistically, I doubt people will use the assembler in CLI mode)
			return;
		}
		// Skip if phantoms disabled
		if (!controller.config().assembler().phantoms)
			return;
		// Thread this so we don't hang any important threads.
		ThreadUtil.run(() -> {
			try {
				long start = System.currentTimeMillis();
				phantoms.populatePhantoms(primary.getClasses());
				Log.debug("Generated {} phantom classes in {} ms",
						phantoms.getClasses().size(), (System.currentTimeMillis() - start));
			} catch (Throwable t) {
				Log.error(t, "Failed to analyze phantom references for primary resource");
			}
		});
	}

	/**
	 * Update the aggregate ASM mappings in the workspace.
	 *
	 * @param newMappings    The additional ASM mappings that were added.
	 * @param changedClasses The set of class names that have been updated as a result of the definition changes.
	 */
	public void updateAggregateMappings(Map<String, String> newMappings, Set<String> changedClasses) {
		Map<String, String> usefulMappings = new HashMap<>();
		for (Map.Entry<String, String> newMapping : newMappings.entrySet()) {
			// only process mappings that actually caused changes in their own class
			String className = AsmMappingUtils.getClassNameFromAsmKey(newMapping.getKey());
			if (!changedClasses.contains(className)) {
				Log.trace("Omitting unused mapping: " + newMapping.getKey() + " -> " + newMapping.getValue());
				continue;
			}

			usefulMappings.put(newMapping.getKey(), newMapping.getValue());
		}
		AsmMappingUtils.applyMappingToExisting(this.aggregatedMappings, usefulMappings);
	}

	// ================================= CLASS / RESOURCE UTILS ================================= //

	/**
	 * @return Set of all class names loaded in the workspace.
	 */
	public Set<String> getClassNames() {
		Set<String> names = getPrimaryClassNames();
		names.addAll(getLibraryClassNames());
		return names;
	}

	/**
	 * @return Set of all class names loaded in the primary resource.
	 */
	public Set<String> getPrimaryClassNames() {
		return new HashSet<>(primary.getClasses().keySet());
	}

	/**
	 * @return Set of all class names loaded in the library resources.
	 */
	public Set<String> getLibraryClassNames() {
		Set<String> names = new HashSet<>();
		for(JavaResource resource : getLibraries())
			names.addAll(resource.getClasses().keySet());
		return names;
	}

	/**
	 * @return Set of all classes loaded in the primary resource.
	 */
	public Set<byte[]> getPrimaryClasses() {
		return new HashSet<>(primary.getClasses().values());
	}

	/**
	 * @return Set of all classes loaded in the library resources.
	 */
	public Set<byte[]> getLibraryClasses() {
		Set<byte[]> values = new HashSet<>();
		for(JavaResource resource : getLibraries())
			values.addAll(resource.getClasses().values());
		return values;
	}

	/**
	 * @return Set of all classes loaded in the primary resource as
	 * {@link org.objectweb.asm.ClassReader}.
	 */
	public Set<ClassReader> getPrimaryClassReaders() {
		return getPrimaryClasses().stream()
				.map(ClassReader::new)
				.collect(Collectors.toSet());
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return The resource that contains the class.
	 */
	public JavaResource getContainingResourceForClass(String name) {
		if(getPrimary().getClasses().containsKey(name))
			return primary;
		for(JavaResource resource : getLibraries())
			if(resource.getClasses().containsKey(name))
				return resource;
		if(CP.getClasses().containsKey(name))
			return CP;
		else if (phantoms.getClasses().containsKey(name))
			return phantoms;
		return null;
	}


	/**
	 * @param name
	 * 		File name.
	 *
	 * @return The resource that contains the file.
	 */
	public JavaResource getContainingResourceForFile(String name) {
		if(getPrimary().getFiles().containsKey(name))
			return primary;
		for(JavaResource resource : getLibraries())
			if(resource.getFiles().containsKey(name))
				return resource;
		return null;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return {@code true} if one of the workspace sources contains the class.
	 */
	public boolean hasClass(String name) {
		if (primary.getClasses().containsKey(name))
			return true;
		for (JavaResource resource : getLibraries())
			if (resource.getClasses().containsKey(name))
				return true;
		if (CP.getClasses().containsKey(name))
			return true;
		else
			return phantoms.getClasses().containsKey(name);
	}

	/**
	 * @param name
	 * 		Resource name.
	 *
	 * @return {@code true} if one of the workspace sources contains the resource.
	 */
	public boolean hasFile(String name) {
		if(primary.getFiles().containsKey(name))
			return true;
		for(JavaResource resource : getLibraries())
			if(resource.getFiles().containsKey(name))
				return true;
		return false;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Raw bytecode of the class by the given name.
	 */
	public byte[] getRawClass(String name) {
		byte[] ret = primary.getClasses().get(name);
		if(ret != null)
			return ret;
		for(JavaResource resource : getLibraries()) {
			ret = resource.getClasses().get(name);
			if(ret != null)
				return ret;
		}
		if (CP.getClasses().containsKey(name))
			return CP.getClasses().get(name);
		else if (phantoms.getClasses().containsKey(name))
			return phantoms.getClasses().get(name);
		return null;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return {@link org.objectweb.asm.ClassReader} for the given class.
	 */
	public ClassReader getClassReader(String name) {
		byte[] ret = getRawClass(name);
		if(ret != null)
			return new ClassReader(ret);
		return null;
	}

	/**
	 * @param flags
	 * 		Writer flags.
	 *
	 * @return {@link ClassWriter} capable of frame-generation.
	 */
	public WorkspaceClassWriter createWriter(int flags) {
		return new WorkspaceClassWriter(this, flags);
	}

	/**
	 * @param name
	 * 		Resource name.
	 *
	 * @return Resource binary by the given name.
	 */
	public byte[] getFile(String name) {
		byte[] ret = primary.getFiles().get(name);
		if(ret != null)
			return ret;
		for(JavaResource resource : getLibraries())
			ret = resource.getFiles().get(name);
		if(ret != null)
			return ret;
		return null;
	}

	// ================================= SOURCE / JAVADOC UTILS ================================= //

	/**
	 * Analyzes attached sources of all resources.
	 * This also allows workspace-wide name lookups for better type-resolving.
	 *
	 * @return Map of class names to their parse result. If an
	 * {@link SourceCodeException} occured during analysis of a class
	 * then it's result may have {@link com.github.javaparser.ParseResult#isSuccessful()} be {@code false}.
	 */
	public Map<String, ParseResult<CompilationUnit>> analyzeSources() {
		return Stream.concat(Stream.of(primary), libraries.stream())
				.flatMap(resource -> resource.analyzeSource(this).entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * @param name
	 * 		Internal name of a Java class.
	 *
	 * @return Source wrapper of class.
	 */
	public SourceCode getSource(String name) {
		SourceCode code = primary.getClassSource(name);
		if(code != null)
			return code;
		for(JavaResource resource : libraries)
			if((code = resource.getClassSource(name)) != null)
				break;
		return code;
	}

	/**
	 * @return JavaParser config to assist in resolving symbols.
	 */
	public ParserConfiguration getSourceParseConfig() {
		if (config == null)
			updateSourceConfig();
		return config;
	}

	/**
	 * Creates a source config with a type resolver that can access all types in the workspace.
	 */
	public void updateSourceConfig() {
		TypeSolver solver = new WorkspaceTypeResolver(this);
		config = new ParserConfiguration()
				.setSymbolResolver(new JavaSymbolSolver(solver))
				.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_16);
	}

	/**
	 * @param name
	 * 		Internal name of a Java class.
	 *
	 * @return Javadocs wrapper of class.
	 */
	public Javadocs getClassDocs(String name) {
		Javadocs docs = primary.getClassDocs(name);
		if(docs != null)
			return docs;
		for(JavaResource resource : libraries)
			if((docs = resource.getClassDocs(name)) != null)
				break;
		return docs;
	}
}