package me.coley.recaf.workspace;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import me.coley.recaf.parse.javadoc.Javadocs;
import me.coley.recaf.parse.source.SourceCode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Resource that defers to some backing resource.
 *
 * @author Matt
 */
public class DeferringResource extends JavaResource {
	private JavaResource backing;

	/**
	 * Constructs a java resource.
	 *
	 * @param kind
	 * 		The kind of resource implementation.
	 */
	public DeferringResource(ResourceKind kind) {
		super(kind);
	}

	/**
	 * @param backing
	 * 		Resource to defer to.
	 */
	public void setBacking(JavaResource backing) {
		this.backing = backing;
	}

	/**
	 * @return Deferred resource.
	 */
	public JavaResource getBacking() {
		return backing;
	}

	// ====================== Overrides pointing to backing resource ====================== //

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		return backing.loadClasses();
	}

	@Override
	protected Map<String, byte[]> loadFiles() throws IOException {
		return backing.loadFiles();
	}

	@Override
	public List<String> getSkippedPrefixes() {
		return backing.getSkippedPrefixes();
	}

	@Override
	public void setSkippedPrefixes(List<String> skippedPrefixes) {
		backing.setSkippedPrefixes(skippedPrefixes);
	}

	@Override
	public Set<String> getDirtyClasses() {
		return backing.getDirtyClasses();
	}

	@Override
	public Set<String> getDirtyFiles() {
		return backing.getDirtyFiles();
	}

	@Override
	public History getClassHistory(String name) {
		return backing.getClassHistory(name);
	}

	@Override
	public Map<String, History> getClassHistory() {
		return backing.getClassHistory();
	}

	@Override
	public History getFileHistory(String name) {
		return backing.getFileHistory(name);
	}

	@Override
	public Map<String, History> getFileHistory() {
		return backing.getFileHistory();
	}

	@Override
	public boolean createClassSave(String name) {
		return backing.createClassSave(name);
	}

	@Override
	public boolean createFileSave(String name) {
		return backing.createFileSave(name);
	}

	@Override
	public Map<String, SourceCode> getClassSources() {
		return backing.getClassSources();
	}

	@Override
	public SourceCode getClassSource(String name) {
		return backing.getClassSource(name);
	}

	@Override
	public boolean setClassSources(Path path) throws IOException {
		return backing.setClassSources(path);
	}

	@Override
	public Map<String, ParseResult<CompilationUnit>> analyzeSource(Workspace workspace) {
		return backing.analyzeSource(workspace);
	}

	@Override
	public Map<String, Javadocs> getClassDocs() {
		return backing.getClassDocs();
	}

	@Override
	public Javadocs getClassDocs(String name) {
		return backing.getClassDocs(name);
	}

	@Override
	public boolean setClassDocs(Path path) throws IOException {
		return backing.setClassDocs(path);
	}

	@Override
	public void invalidate() {
		backing.invalidate();
	}

	@Override
	public ResourceLocation getShortName() {
		return backing.getShortName();
	}

	@Override
	public ResourceLocation getName() {
		return backing.getName();
	}
}
