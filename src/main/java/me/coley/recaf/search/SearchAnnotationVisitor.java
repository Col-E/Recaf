package me.coley.recaf.search;

import me.coley.recaf.Recaf;
import org.objectweb.asm.*;

import java.lang.reflect.Array;

/**
 * Visitor that adds matched results in annotations to a result collector.
 *
 * @author Matt
 */
public class SearchAnnotationVisitor extends AnnotationVisitor {
	private final SearchCollector collector;
	private final Context.AnnotationContext context;

	/**
	 * Constructs an annotation search visitor.
	 *
	 * @param collector
	 * 		Result collector.
	 * @param context
	 * 		Current search context.
	 * @param descriptor
	 * 		Annotation type.
	 */
	public SearchAnnotationVisitor(SearchCollector collector, Context<?> context, String
			descriptor) {
		super(Recaf.ASM_VERSION);
		this.collector = collector;
		this.context = context.withAnno(descriptor);
		collector.queries(ClassReferenceQuery.class)
				.forEach(q -> {
					String type = Type.getType(descriptor).getInternalName();
					q.match(collector.getAccess(type, Opcodes.ACC_ANNOTATION), type);
					collector.addMatched(this.context, q);
				});
	}

	@Override
	public void visit(String name, Object value) {
		// Skip null
		if (value == null) {
			return;
		}
		if (value instanceof String) {
			collector.queries(StringQuery.class)
					.forEach(q -> {
						q.match((String) value);
						collector.addMatched(context, q);
					});
		} else if (value instanceof Number) {
			collector.queries(ValueQuery.class)
					.forEach(q -> {
						q.match(value);
						collector.addMatched(context, q);
					});
		} else if (value instanceof Character){
			int cval = (Character) value;
			collector.queries(ValueQuery.class)
					.forEach(q -> {
						q.match(cval);
						collector.addMatched(context, q);
					});
		} else if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			Object[] array = new Object[length];
			for(int i = 0; i < length; i++)
				array[i] = Array.get(value, i);
			collector.queries(ValueQuery.class)
					.forEach(q -> {
						for (Object i : array)
							q.match(i);
						collector.addMatched(context, q);
					});
		}
	}

	@Override
	public void visitEnum(String name, String descriptor, String value) {
		collector.queries(ClassReferenceQuery.class)
				.forEach(q -> {
					String type = Type.getType(descriptor).getInternalName();
					q.match(collector.getAccess(type, Opcodes.ACC_ANNOTATION), type);
					collector.addMatched(context, q);
				});
		collector.queries(StringQuery.class)
				.forEach(q -> {
					q.match(value);
					collector.addMatched(context, q);
				});
	}

	@Override
	public AnnotationVisitor visitAnnotation(String name, String descriptor) {
		return new SearchAnnotationVisitor(collector, context, descriptor);
	}

	@Override
	public AnnotationVisitor visitArray(final String name) {
		return this;
	}
}