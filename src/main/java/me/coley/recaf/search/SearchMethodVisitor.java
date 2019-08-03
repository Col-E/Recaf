package me.coley.recaf.search;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import static me.coley.recaf.search.SearchCollector.*;

/**
 * Visitor that adds matched results in methods to a result collector.
 *
 * @author Matt
 */
public class SearchMethodVisitor extends MethodNode {
	private final SearchCollector collector;
	private final Context.MemberContext context;

	/**
	 * @param collector
	 * 		Result collector.
	 * @param context
	 * 		Search context base.
	 * @param access
	 * 		Visited method access.
	 * @param name
	 * 		Visited method name.
	 * @param desc
	 * 		Visited method descriptor.
	 */
	public SearchMethodVisitor(SearchCollector collector, Context.ClassContext context, int access,
							  String name, String desc) {
		super(Opcodes.ASM7);
		this.collector = collector;
		this.context = context.withMember(access, name, desc);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return new SearchAnnotationVisitor(collector, context, descriptor);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor,
												 boolean visible) {
		return new SearchAnnotationVisitor(collector, context, descriptor);
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
		return new SearchAnnotationVisitor(collector, context, descriptor);
	}

	@Override
	public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor,
												 boolean visible) {
		return new SearchAnnotationVisitor(collector, context, descriptor);
	}

	@Override
	public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor,
													 boolean visible) {
		return new SearchAnnotationVisitor(collector, context, descriptor);
	}

	@Override
	public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start,
														  Label[] end, int[] index, String descriptor,
														  boolean visible) {
		return new SearchAnnotationVisitor(collector, context, descriptor);
	}

	@Override
	public void visitLocalVariable(String name, String descriptor, String signature, Label start,
								   Label end, int index) {
		super.visitLocalVariable(name, descriptor, signature, start, end, index);
		/*
		collector.queries(ClassReferenceQuery.class)
				.forEach(q -> {
					String types = Type.getType(descriptor).getInternalName();
					int access = collector.getAccess(types);
					q.match(access, types);
					collector.addMatched(context, q);
				});
				*/
		// TODO: Remove? Seems like overkill
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		super.visitIntInsn(opcode, operand);
		// TODO: Value
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		super.visitIincInsn(var, increment);
		// TODO: Value
	}

	@Override
	public void visitTableSwitchInsn(
			int min, int max, Label dflt,  Label... labels) {
		super.visitTableSwitchInsn(min, max, dflt, labels);
		// TODO: Value
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		super.visitLookupSwitchInsn(dflt, keys, labels);
		// TODO: Value
	}

	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		super.visitMultiANewArrayInsn(descriptor, numDimensions);
		collector.queries(ClassReferenceQuery.class)
				.forEach(q -> {
					String types = Type.getType(descriptor).getInternalName();
					q.match(collector.getAccess(types, ACC_NOT_FOUND), types);
					collector.addMatched(context.withInsn(last(), lastPos()), q);
				});
	}

	@Override
	public void visitTypeInsn(int opcode, String type) {
		super.visitTypeInsn(opcode, type);
		collector.queries(ClassReferenceQuery.class)
				.forEach(q -> {
					String types = Type.getObjectType(type).getInternalName();
					q.match(collector.getAccess(types, ACC_NOT_FOUND), types);
					collector.addMatched(context.withInsn(last(), lastPos()), q);
				});
	}

	@Override
	public void visitTryCatchBlock(Label start, final Label end, Label handler, String type) {
		super.visitTryCatchBlock(start, end, handler, type);
		/*
		collector.queries(ClassReferenceQuery.class)
				.forEach(q -> {
					String types = Type.getType(type).getInternalName();
					q.match(collector.getAccess(types, ACC_NOT_FOUND), types);
					collector.addMatched(context, q);
				});
				*/
		// TODO: Remove? Seems like overkill
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		super.visitFieldInsn(opcode, owner,name, descriptor);
		Context.InsnContext insnContext = context.withInsn(last(), lastPos());
		collector.queries(ClassReferenceQuery.class)
				.forEach(q -> {
					q.match(collector.getAccess(owner, ACC_NOT_FOUND), owner);
					collector.addMatched(insnContext, q);
				});
		collector.queries(MemberReferenceQuery.class)
				.forEach(q -> {
					q.match(collector.getAccess(owner, name, descriptor), owner, name, descriptor);
					collector.addMatched(insnContext, q);
				});
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean itf) {
		super.visitMethodInsn(opcode, owner, name, descriptor, itf);
		Context.InsnContext insnContext = context.withInsn(last(), lastPos());
		collector.queries(ClassReferenceQuery.class)
				.forEach(q -> {
					q.match(collector.getAccess(owner, ACC_NOT_FOUND), owner);
					collector.addMatched(insnContext, q);
				});
		collector.queries(MemberReferenceQuery.class)
				.forEach(q -> {
					q.match(collector.getAccess(owner, name, descriptor), owner, name, descriptor);
					collector.addMatched(insnContext, q);
				});
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle,
									   Object... bootstrapMethodArguments) {
		super.visitInvokeDynamicInsn(name, descriptor, handle, bootstrapMethodArguments);
		Context.InsnContext insnContext = context.withInsn(last(), lastPos());
		collector.queries(ClassReferenceQuery.class)
				.forEach(q -> {
					String owner = handle.getOwner();
					q.match(collector.getAccess(owner, ACC_NOT_FOUND), owner);
					collector.addMatched(insnContext, q);
				});
		collector.queries(MemberReferenceQuery.class)
				.forEach(q -> {
					q.match(collector.getAccess(handle.getOwner(), handle.getName(), handle.getDesc()),
							handle.getOwner(), handle.getName(), handle.getDesc());
					collector.addMatched(insnContext, q);
				});
		// TODO: Member reference (bsm args)
	}

	@Override
	public void visitLdcInsn(Object value) {
		super.visitLdcInsn(value);
		Context.InsnContext insnContext = context.withInsn(last(), lastPos());
		if (value instanceof String) {
			collector.queries(StringQuery.class)
					.forEach(q -> {
						q.match((String) value);
						collector.addMatched(insnContext, q);
					});
		} else if (value instanceof Type) {
			Type type = (Type) value;
			collector.queries(ClassReferenceQuery.class)
					.forEach(q -> {
						String types = type.getInternalName();
						q.match(collector.getAccess(types, Opcodes.ACC_ANNOTATION), types);
						collector.addMatched(insnContext, q);
					});
		} else if (value instanceof Handle) {
			Handle handle = (Handle) value;
			collector.queries(ClassReferenceQuery.class)
					.forEach(q -> {
						String owner = handle.getOwner();
						q.match(collector.getAccess(owner, ACC_NOT_FOUND), owner);
						collector.addMatched(insnContext, q);
					});
			collector.queries(MemberReferenceQuery.class)
					.forEach(q -> {
						q.match(collector.getAccess(handle.getOwner(), handle.getName(), handle.getDesc()),
								handle.getOwner(), handle.getName(), handle.getDesc());
						collector.addMatched(insnContext, q);
					});
		} else if (value instanceof ConstantDynamic) {
			ConstantDynamic dynamic = (ConstantDynamic) value;
			Handle handle = dynamic.getBootstrapMethod();
			collector.queries(ClassReferenceQuery.class)
					.forEach(q -> {
						String owner = handle.getOwner();
						q.match(collector.getAccess(owner, ACC_NOT_FOUND), owner);
						collector.addMatched(insnContext, q);
					});
			collector.queries(MemberReferenceQuery.class)
					.forEach(q -> {
						q.match(collector.getAccess(handle.getOwner(), handle.getName(), handle.getDesc()),
								handle.getOwner(), handle.getName(), handle.getDesc());
						collector.addMatched(insnContext, q);
					});
			// TODO: Member reference (bsm args)
		} else {
			// TODO: Value
		}
	}

	private AbstractInsnNode last() {
		return instructions.getLast();
	}

	private int lastPos() {
		return instructions.size() - 1;
	}
}