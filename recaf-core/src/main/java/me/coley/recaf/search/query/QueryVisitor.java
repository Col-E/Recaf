package me.coley.recaf.search.query;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.search.result.Result;
import me.coley.recaf.search.result.ResultBuilder;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Common architecture for creating class visitors that power {@link Query} implementations.
 * The protected {@code addX(...)} allow for child implementations to cleanly add matched items
 * in the form of lambda references.
 * <br>
 * For example a {@link #visitField(int, String, String, String, Object)}:
 * <pre>
 * public FieldVisitor visitField(int acc, String name, String desc, String sig, Object value) {
 *    FieldVisitor fv = super.visitField(acc, name, desc, sig, value);
 *    whenMatched(value, builder -> addField(builder, name, desc));
 *    return fv;
 * }
 * </pre>
 *
 * @author Matt Coley
 */
public abstract class QueryVisitor extends ClassVisitor {
	protected final List<Result> results = new ArrayList<>();
	protected final Resource resource;
	protected final QueryVisitor delegate;
	protected ClassInfo currentClass;

	/**
	 * @param resource
	 * 		The resource containing the classes being visited.
	 * @param visitor
	 * 		Delegate query visitor, may be {@code null}.
	 */
	public QueryVisitor(Resource resource, QueryVisitor visitor) {
		super(RecafConstants.getAsmVersion(), visitor);
		this.resource = resource;
		this.delegate = visitor;
	}

	/**
	 * @return The resource containing the classes being visited.
	 */
	public Resource getResource() {
		return resource;
	}

	/**
	 * @return Delegate visitor for chained queries.
	 */
	public QueryVisitor getDelegate() {
		return delegate;
	}

	/**
	 * @return Matched results of the current visitor.
	 */
	public List<Result> getResults() {
		return results;
	}

	/**
	 * Stores search results into a collection.
	 *
	 * @param collection
	 * 		Collection to store results into.
	 */
	public void storeResults(Collection<? super Result> collection) {
		collection.addAll(getResults());
		QueryVisitor delegate = getDelegate();
		if (delegate != null)
			delegate.storeResults(collection);
	}

	/**
	 * @return Matched results of the current visitor and any {@link #getDelegate() delegate} visitors.
	 */
	public List<Result> getAllResults() {
		List<Result> list = new ArrayList<>(getResults());
		if (getDelegate() != null)
			list.addAll(getDelegate().getAllResults());
		return list;
	}

	protected void addFileText(ResultBuilder builder, FileInfo fileInfo) {
		builder.inFile(fileInfo)
				.then(results::add);
	}

	protected void addField(ResultBuilder builder, String name, String desc) {
		builder.inClass(currentClass)
				.inField(currentClass.findField(name, desc))
				.then(results::add);
	}

	protected void addMethod(ResultBuilder builder, String name, String desc) {
		builder.inClass(currentClass)
				.inMethod(currentClass.findMethod(name, desc))
				.then(results::add);
	}

	protected void addMethodInsn(ResultBuilder builder, String name, String desc, AbstractInstruction instruction) {
		builder.inClass(currentClass)
				.inMethod(currentClass.findMethod(name, desc))
				.withInstruction(instruction)
				.then(results::add);
	}

	protected void addClassAnno(ResultBuilder builder, String annotationType) {
		builder.inClass(currentClass)
				.inAnnotation(annotationType)
				.then(results::add);
	}

	protected void addFieldAnno(ResultBuilder builder, FieldInfo info, String annotationType) {
		addFieldAnno(builder, info.getName(), info.getDescriptor(), annotationType);
	}

	protected void addFieldAnno(ResultBuilder builder, String name, String descriptor, String annotationType) {
		builder.inClass(currentClass)
				.inField(currentClass.findField(name, descriptor))
				.inAnnotation(annotationType)
				.then(results::add);
	}

	protected void addMethodAnno(ResultBuilder builder, MethodInfo info, String annotationType) {
		addMethodAnno(builder, info.getName(), info.getDescriptor(), annotationType);
	}

	protected void addMethodAnno(ResultBuilder builder, String name, String descriptor, String annotationType) {
		builder.inClass(currentClass)
				.inMethod(currentClass.findMethod(name, descriptor))
				.inAnnotation(annotationType)
				.then(results::add);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		currentClass = resource.getClasses().get(name);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	/**
	 * Additional 'visitor' for handling files in a workspace.
	 *
	 * @param fileInfo
	 * 		File visited.
	 */
	public abstract void visitFile(FileInfo fileInfo);
}
