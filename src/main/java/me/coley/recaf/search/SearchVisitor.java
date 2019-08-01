package me.coley.recaf.search;

import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.*;

import java.util.*;

/* TODO:

 - Member definition
 - Member reference
     - Field insn
     - Method insn
 - Member inheritance (Child of given)
     - Any level of parent
 - Strings
     - Annotation value
     - Field constant
     - Method LDC
 - Instruction text match

/////////////////////

 - Status (For eventual UI integration)
 */
public class SearchVisitor extends ClassVisitor {
	private final Map<Query, List<SearchResult>> results = new LinkedHashMap<>();
	private final Workspace workspace;
	private final Collection<Query> queries;

	/**
	 * Constructs a search visitor.
	 *
	 * @param workspace
	 * 		Workspace to pull additional references from.
	 * @param queries
	 * 		Queries to check for collecting results.
	 */
	public SearchVisitor(Workspace workspace, Collection<Query> queries) {
		super(Opcodes.ASM7);
		this.workspace = workspace;
		this.queries = queries;
	}

	/**
	 * Adds all results from the query to the {@link #getResults() results map}.
	 *
	 * @param param
	 * 		Query with results to add.
	 */
	private void addMatched(Query param) {
		List<SearchResult> matched = param.getMatched();
		results.computeIfAbsent(param, p -> new ArrayList<>()).addAll(matched);
		matched.clear();
	}

	/**
	 * @return Map of queries to their results.
	 */
	public Map<Query, List<SearchResult>> getResults() {
		return results;
	}

	/**
	 * @return Flattened list of the {@link #getResults() result map}.
	 */
	public List<SearchResult> getAllResults() {
		return results.values().stream().reduce((a, b) -> {
			a.addAll(b);
			return a;
		}).get();
	}

	// ========================== VISITOR IMPLEMENTATIONS ========================== //

	@Override
	public void visit(int version, int access, String name, String signature, String superName,
					  String[] interfaces) {
		queries.stream()
				.filter(q -> q instanceof ClassNameQuery)
				.map(q -> (ClassNameQuery) q)
				.forEach(q -> {
					q.match(access, name);
					addMatched(q);
				});
		queries.stream()
				.filter(q -> q instanceof ClassInheritanceQuery)
				.map(q -> (ClassInheritanceQuery) q)
				.forEach(q -> {
					q.match(access, name);
					addMatched(q);
				});
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		// TODO: Value search
		return null;
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String
			descriptor, boolean visible) {
		// TODO: Value search
		return null;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature,
								   Object value) {
		// TODO: Wrapper around this (collect results in here via field.visitEnd())
		// FieldNode field = new FieldNode(access, name, descriptor, signature, value);
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
									 String[] exceptions) {
		// TODO: Wrapper around this (collect results in here via method.visitEnd())
		// MethodNode method = new MethodNode(access, name, descriptor, signature, exceptions);
		return null;
	}
}
