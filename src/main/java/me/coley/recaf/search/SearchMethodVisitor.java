package me.coley.recaf.search;

import me.coley.recaf.Recaf;
import me.coley.recaf.parse.bytecode.Disassembler;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.InsnUtil;
import me.coley.recaf.util.Log;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.stream.Collectors;

import static me.coley.recaf.search.SearchCollector.ACC_NOT_FOUND;

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
	 */
	public SearchMethodVisitor(SearchCollector collector, Context.MemberContext context) {
		super(Recaf.ASM_VERSION);
		this.access = context.getAccess();
		this.name = context.getName();
		this.desc = context.getDesc();
		this.collector = collector;
		this.context = context;
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
		collector.queries(ClassReferenceQuery.class)
				.forEach(q -> {
					Type t = Type.getType(descriptor);
					if (t.getSort() == Type.ARRAY)
						t = t.getElementType();
					String type = t.getInternalName();
					q.match(collector.getAccess(type, ACC_NOT_FOUND), type);
					collector.addMatched(context.withLocal(index, name, descriptor), q);
				});
	}

	@Override
	public void visitInsn(int opcode) {
		super.visitInsn(opcode);
		if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.DCONST_1) {
			int value = InsnUtil.getValue(opcode);
			collector.queries(ValueQuery.class)
					.forEach(q -> {
						q.match(value);
						collector.addMatched(context.withInsn(last(), lastPos()), q);
					});
		}

	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		super.visitIntInsn(opcode, operand);
		collector.queries(ValueQuery.class)
				.forEach(q -> {
					q.match(operand);
					collector.addMatched(context.withInsn(last(), lastPos()), q);
				});	}

	@Override
	public void visitIincInsn(int var, int increment) {
		super.visitIincInsn(var, increment);
		collector.queries(ValueQuery.class)
				.forEach(q -> {
					q.match(increment);
					collector.addMatched(context.withInsn(last(), lastPos()), q);
				});
	}

	@Override
	public void visitTableSwitchInsn(
			int min, int max, Label dflt,  Label... labels) {
		super.visitTableSwitchInsn(min, max, dflt, labels);
		collector.queries(ValueQuery.class)
				.forEach(q -> {
					q.match(min);
					q.match(max);
					collector.addMatched(context.withInsn(last(), lastPos()), q);
				});
	}

	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		super.visitLookupSwitchInsn(dflt, keys, labels);
		collector.queries(ValueQuery.class)
				.forEach(q -> {
					for(int key : keys)
						q.match(key);
					collector.addMatched(context.withInsn(last(), lastPos()), q);
				});
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
					Type typee = type.contains(";") ? Type.getType(type) : Type.getObjectType(type);
					String types = typee.getSort() == Type.ARRAY ?
							typee.getElementType().getInternalName() : typee.getInternalName();
					q.match(collector.getAccess(types, ACC_NOT_FOUND), types);
					collector.addMatched(context.withInsn(last(), lastPos()), q);
				});
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		super.visitTryCatchBlock(start, end, handler, type);
		collector.queries(ClassReferenceQuery.class)
				.forEach(q -> {
					if (type == null)
						return;
					// "type" is already in internal format
					q.match(collector.getAccess(type, ACC_NOT_FOUND), type);
					collector.addMatched(context.withCatch(type), q);
				});
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		super.visitFieldInsn(opcode, owner,name, descriptor);
		Context.InsnContext insnContext = context.withInsn(last(), lastPos());
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
		collector.queries(MemberReferenceQuery.class)
				.forEach(q -> {
					q.match(collector.getAccess(handle.getOwner(), handle.getName(), handle.getDesc()),
							handle.getOwner(), handle.getName(), handle.getDesc());
					collector.addMatched(insnContext, q);
				});
		for(Object o : bootstrapMethodArguments) {
			if (o instanceof Handle) {
				Handle h = (Handle) o;
				collector.queries(MemberReferenceQuery.class)
						.forEach(q -> {
							q.match(collector.getAccess(h.getOwner(), h.getName(), h.getDesc()),
									h.getOwner(), h.getName(), h.getDesc());
							collector.addMatched(insnContext, q);
						});
			} else if (o instanceof String) {
				String s = (String) o;
				collector.queries(StringQuery.class)
						.forEach(q -> {
							q.match(s);
							collector.addMatched(insnContext, q);
						});
			} else if (o instanceof Number) {
				Number n = (Number) o;
				collector.queries(ValueQuery.class)
						.forEach(q -> {
							q.match(n);
							collector.addMatched(context.withInsn(last(), lastPos()), q);
						});
			}
		}
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
						String types = type.getSort() == Type.ARRAY ?
								type.getElementType().getInternalName() : type.getInternalName();
						q.match(collector.getAccess(types, Opcodes.ACC_ANNOTATION), types);
						collector.addMatched(insnContext, q);
					});
		} else if (value instanceof Handle) {
			Handle handle = (Handle) value;
			collector.queries(MemberReferenceQuery.class)
					.forEach(q -> {
						q.match(collector.getAccess(handle.getOwner(), handle.getName(), handle.getDesc()),
								handle.getOwner(), handle.getName(), handle.getDesc());
						collector.addMatched(insnContext, q);
					});
		} else if (value instanceof ConstantDynamic) {
			ConstantDynamic dynamic = (ConstantDynamic) value;
			Handle handle = dynamic.getBootstrapMethod();
			collector.queries(MemberReferenceQuery.class)
					.forEach(q -> {
						q.match(collector.getAccess(handle.getOwner(), handle.getName(), handle.getDesc()),
								handle.getOwner(), handle.getName(), handle.getDesc());
						collector.addMatched(insnContext, q);
					});
			for(int bsm = 0; bsm < dynamic.getBootstrapMethodArgumentCount(); bsm++) {
				Object o = dynamic.getBootstrapMethodArgument(bsm);
				if (o instanceof Handle) {
					Handle h = (Handle) o;
					collector.queries(MemberReferenceQuery.class)
							.forEach(q -> {
								q.match(collector.getAccess(h.getOwner(), h.getName(), h.getDesc()),
										h.getOwner(), h.getName(), h.getDesc());
								collector.addMatched(insnContext, q);
							});
				}
			}
		} else {
			collector.queries(ValueQuery.class)
					.forEach(q -> {
						q.match(value);
						collector.addMatched(insnContext, q);
					});
		}
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		// Don't check disassembled text on abstract methods
		if (AccessFlag.isAbstract(access))
			return;
		List<InsnTextQuery> insnTextQueries = collector.queries(InsnTextQuery.class).collect(Collectors.toList());
		if (!insnTextQueries.isEmpty()) {
			try {
				String code = new Disassembler().disassemble(this);
				insnTextQueries.forEach(q -> {
					q.match(code);
					collector.addMatched(context, q);
				});
			} catch(Exception ex) {
				String owner = context.getParent().getName();
				Log.error(ex, "Failed to disassemble method: " + owner + "." + name + desc);
			}
		}
	}

	private AbstractInsnNode last() {
		return instructions.getLast();
	}

	private int lastPos() {
		return instructions.size() - 1;
	}
}