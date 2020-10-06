package me.coley.recaf.search;

import me.coley.recaf.Recaf;
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
	 * 		Field context.
	 */
	public SearchFieldVisitor(SearchCollector collector, Context.MemberContext context) {
		super(Recaf.ASM_VERSION);
		this.collector = collector;
		this.context = context;
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