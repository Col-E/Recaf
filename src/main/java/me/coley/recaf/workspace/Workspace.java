package me.coley.recaf.workspace;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import me.coley.recaf.graph.flow.FlowGraph;
import me.coley.recaf.graph.inheritance.HierarchyGraph;
import me.coley.recaf.parse.source.SourceCodeException;
import me.coley.recaf.parse.source.WorkspaceTypeResolver;
import org.objectweb.asm.ClassReader;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Input manager
 *
 * @author Matt
 */
public class Workspace {
	private final JavaResource primary;
	private final List<JavaResource> libraries;
	private final HierarchyGraph hierarchyGraph;
	private final FlowGraph flowGraph;
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
		this.libraries = libraries;
		this.hierarchyGraph = new HierarchyGraph(this);
		this.flowGraph = new FlowGraph(this);
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
	 * @return Inheritance hierarchy utility.
	 */
	public HierarchyGraph getHierarchyGraph() {
		return hierarchyGraph;
	}

	/**
	 * @return Method flow utility.
	 */
	public FlowGraph getFlowGraph() {
		return flowGraph;
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
	 * @return {@code true} if one of the workspace sources contains the class.
	 */
	public boolean hasClass(String name) {
		if(primary.getClasses().containsKey(name))
			return true;
		for(JavaResource resource : getLibraries())
			if(resource.getClasses().containsKey(name))
				return true;
		return false;
	}

	/**
	 * @param name
	 * 		Resource name.
	 *
	 * @return {@code true} if one of the workspace sources contains the resource.
	 */
	public boolean hasResource(String name) {
		if(primary.getResources().containsKey(name))
			return true;
		for(JavaResource resource : getLibraries())
			if(resource.getResources().containsKey(name))
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
		for(JavaResource resource : getLibraries())
			ret = resource.getClasses().get(name);
		return ret;
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
	 * @param name
	 * 		Resource name.
	 *
	 * @return Resource binary by the given name.
	 */
	public byte[] getResource(String name) {
		byte[] ret = primary.getResources().get(name);
		if(ret != null)
			return ret;
		for(JavaResource resource : getLibraries())
			ret = resource.getResources().get(name);
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
		config = new ParserConfiguration().setSymbolResolver(new JavaSymbolSolver(solver));
	}
}