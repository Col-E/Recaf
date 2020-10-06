package me.coley.recaf.search;

import me.coley.recaf.Recaf;
import org.objectweb.asm.*;

/**
 * Visitor that adds matched results in classes to a result collector.
 *
 * @author Matt
 */
public class SearchClassVisitor extends ClassVisitor {
	private final SearchCollector collector;
	private Context.ClassContext context;

	/**
	 * @param collector
	 * 		Result collector.
	 */
	public SearchClassVisitor(SearchCollector collector) {
		super(Recaf.ASM_VERSION);
		this.collector = collector;
	}

	/**
	 * @return Root search context.
	 */
	public Context.ClassContext getContext() {
		return context;
	}

	@Override
	public void visit(int version, int access, String name, String sig, String superName, String[] interfaces) {
		context = Context.withClass(access, name);
		collector.queries(ClassNameQuery.class)
				.forEach(q -> {
					q.match(access, name);
					collector.addMatched(context, q);
				});
		collector.queries(ClassInheritanceQuery.class)
				.forEach(q -> {
					q.match(access, name);
					collector.addMatched(context, q);
				});
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return new SearchAnnotationVisitor(collector, context, descriptor);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int ref, TypePath typePath, String descriptor, boolean visible) {
		return new SearchAnnotationVisitor(collector, context, descriptor);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature,
								   Object value) {
		Context.MemberContext fieldContext = context.withMember(access, name, descriptor);
		if (value instanceof String) {
			collector.queries(StringQuery.class)
					.forEach(q -> {
						q.match((String) value);
						collector.addMatched(fieldContext, q);
					});
		} else {
			collector.queries(ValueQuery.class)
					.forEach(q -> {
						q.match(value);
						collector.addMatched(fieldContext, q);
					});
		}
		collector.queries(MemberDefinitionQuery.class)
				.forEach(q -> {
					q.match(access, context.getName(), name, descriptor);
					collector.addMatched(fieldContext, q);
				});
		return new SearchFieldVisitor(collector, fieldContext);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String sig, String[] ex) {
		Context.MemberContext methodContext = context.withMember(access, name, descriptor);
		collector.queries(MemberDefinitionQuery.class)
				.forEach(q -> {
					q.match(access, context.getName(), name, descriptor);
					collector.addMatched(methodContext, q);
				});
		return new SearchMethodVisitor(collector, methodContext);
	}
}
