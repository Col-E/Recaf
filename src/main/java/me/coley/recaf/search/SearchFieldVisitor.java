package me.coley.recaf.search;

import org.objectweb.asm.*;

/**
 * Visitor that adds matched results in fields to a result collector.
 *
 * @author Matt
 */
public class SearchFieldVisitor extends FieldVisitor {
	private final SearchCollector collector;
	private final Context.MemberContext context;

	/**
	 * @param collector
	 * 		Result collector.
	 * @param context
	 * 		Search context base.
	 * @param access
	 * 		Visited field access.
	 * @param name
	 * 		Visited field name.
	 * @param desc
	 * 		Visited field descriptor.
	 */
	public SearchFieldVisitor(SearchCollector collector, Context.ClassContext context, int access,
							  String name, String desc) {
		super(Opcodes.ASM7);
		this.collector = collector;
		this.context = context.withMember(access, name, desc);
	}

	/**
	 * @return Field search context.
	 */
	public Context.MemberContext getContext() {
		return context;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return new SearchAnnotationVisitor(collector, context, descriptor);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String
			descriptor, boolean visible) {
		return new SearchAnnotationVisitor(collector, context, descriptor);
	}
}