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
 - Instruction text match

/////////////////////

 - Add instruction context
 	- Its a better idea than adding an Instruction field to the result class
 - Status (For eventual UI integration)
 - Smart skipping if queries don't need to visit fields/methods/annotations
    - Also can do ClassReader.SKIP_CODE in some cases
 */
public class SearchVisitor extends ClassVisitor {
	private final Map<Query, List<SearchResult>> results = new LinkedHashMap<>();
	private final Workspace workspace;
	private final Collection<Query> queries;
	private Context.ClassContext context;

	/**
	 * Constructs a class search visitor.
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
	 * @param context
	 * 		Optional context to add to results.
	 * @param quert
	 * 		Query with results to add.
	 */
	private void addMatched(Context<?> context, Query quert) {
		List<SearchResult> matched = quert.getMatched();
		if(context != null)
			matched.forEach(res -> res.setContext(context));
		results.computeIfAbsent(quert, p -> new ArrayList<>()).addAll(matched);
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
		context = Context.withClass(access, name);
		queries.stream()
				.filter(q -> q instanceof ClassNameQuery)
				.map(q -> (ClassNameQuery) q)
				.forEach(q -> {
					q.match(access, name);
					addMatched(null, q);
				});
		queries.stream()
				.filter(q -> q instanceof ClassInheritanceQuery)
				.map(q -> (ClassInheritanceQuery) q)
				.forEach(q -> {
					q.match(access, name);
					addMatched(null, q);
				});
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return new AnnotationSearchVisitor(context, descriptor);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String
			descriptor, boolean visible) {
		return new AnnotationSearchVisitor(context, descriptor);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature,
								   Object value) {
		if (value instanceof String) {
			queries.stream()
					.filter(q -> q instanceof StringQuery)
					.map(q -> (StringQuery) q)
					.forEach(q -> {
						q.match((String) value);
						addMatched(context, q);
					});
		}
		// TODO: Value
		// TODO: Definition
		return new FieldSearchVisitor(context, access, name, descriptor);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
									 String[] exceptions) {
		// TODO: Definition
		return new MethodSearchVisitor(context, access, name, descriptor);
	}

	private class FieldSearchVisitor extends FieldVisitor {
		private final Context.MemberContext context;

		FieldSearchVisitor(Context.ClassContext context, int access, String name, String desc) {
			super(Opcodes.ASM7);
			this.context = context.withMember(access, name, desc);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			return new AnnotationSearchVisitor(context, descriptor);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String
				descriptor, boolean visible) {
			return new AnnotationSearchVisitor(context, descriptor);
		}
	}

	private class MethodSearchVisitor extends MethodVisitor {
		private final Context.MemberContext context;

		MethodSearchVisitor(Context.ClassContext context, int access, String name, String desc) {
			super(Opcodes.ASM7);
			this.context = context.withMember(access, name, desc);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			return new AnnotationSearchVisitor(context, descriptor);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor,
													 boolean visible) {
			return new AnnotationSearchVisitor(context, descriptor);
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
			return new AnnotationSearchVisitor(context, descriptor);
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor,
													 boolean visible) {
			return new AnnotationSearchVisitor(context, descriptor);
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor,
														 boolean visible) {
			return new AnnotationSearchVisitor(context, descriptor);
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start,
															  Label[] end, int[] index, String descriptor,
															  boolean visible) {
			return new AnnotationSearchVisitor(context, descriptor);
		}

		@Override
		public void visitLocalVariable(String name, String descriptor, String signature, Label start,
									   Label end, int index) {
			queries.stream()
					.filter(q -> q instanceof ClassReferenceQuery)
					.map(q -> (ClassReferenceQuery) q)
					.forEach(q -> {
						String types = Type.getType(descriptor).getInternalName();
						int access = getAccess(types);
						q.match(access, types);
						addMatched(context, q);
					});
		}

		@Override
		public void visitIntInsn(int opcode, int operand) {
			// TODO: Value
		}

		@Override
		public void visitIincInsn(int var, int increment) {
			// TODO: Value
		}

		@Override
		public void visitTableSwitchInsn(
				int min, int max, Label dflt,  Label... labels) {
			// TODO: Value
		}

		@Override
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			// TODO: Value
		}

		@Override
		public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
			queries.stream()
					.filter(q -> q instanceof ClassReferenceQuery)
					.map(q -> (ClassReferenceQuery) q)
					.forEach(q -> {
						String types = Type.getType(descriptor).getInternalName();
						int access = getAccess(types);
						q.match(access, types);
						addMatched(context, q);
					});
		}

		@Override
		public void visitTypeInsn(int opcode, String type) {
			queries.stream()
					.filter(q -> q instanceof ClassReferenceQuery)
					.map(q -> (ClassReferenceQuery) q)
					.forEach(q -> {
						String types = Type.getObjectType(type).getInternalName();
						int access = getAccess(types);
						q.match(access, types);
						addMatched(context, q);
					});
		}

		@Override
		public void visitTryCatchBlock(Label start, final Label end, Label handler, String type) {
			queries.stream()
					.filter(q -> q instanceof ClassReferenceQuery)
					.map(q -> (ClassReferenceQuery) q)
					.forEach(q -> {
						String types = Type.getType(type).getInternalName();
						int access = getAccess(types);
						q.match(access, types);
						addMatched(context, q);
					});
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
			queries.stream()
					.filter(q -> q instanceof ClassReferenceQuery)
					.map(q -> (ClassReferenceQuery) q)
					.forEach(q -> {
						int access = getAccess(owner);
						q.match(access, owner);
						addMatched(context, q);
					});
			// TODO: Member reference
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean itf) {
			// TODO: Member reference
		}

		@Override
		public void visitInvokeDynamicInsn(String name, String descriptor, Handle
				bootstrapMethodHandle, Object... bootstrapMethodArguments) {
			// TODO: Member reference
		}

		@Override
		public void visitLdcInsn(Object value) {
			if (value instanceof String) {
				queries.stream()
						.filter(q -> q instanceof StringQuery)
						.map(q -> (StringQuery) q)
						.forEach(q -> {
							q.match((String) value);
							addMatched(context, q);
						});
			} else if (value instanceof Type) {
				Type type = (Type) value;
				queries.stream()
						.filter(q -> q instanceof ClassReferenceQuery)
						.map(q -> (ClassReferenceQuery) q)
						.forEach(q -> {
							String types = type.getInternalName();
							int access = getAccess(types, Opcodes.ACC_ANNOTATION);
							q.match(access, types);
							addMatched(context, q);
						});
			} else if (value instanceof Handle) {
				// TODO: Member reference
			} else if (value instanceof ConstantDynamic) {
				// TODO: Member reference
			} else {
				// TODO: Value
			}
		}
	}

	private class AnnotationSearchVisitor extends AnnotationVisitor {
		private final Context.AnnotationContext context;

		/**
		 * Constructs an annotation search visitor.
		 *
		 * @param context
		 * 		Current search context.
		 * @param descriptor
		 * 		Annotation type.
		 */
		AnnotationSearchVisitor(Context<?> context, String descriptor) {
			super(Opcodes.ASM7);
			queries.stream()
					.filter(q -> q instanceof ClassReferenceQuery)
					.map(q -> (ClassReferenceQuery) q)
					.forEach(q -> {
						String type = Type.getType(descriptor).getInternalName();
						int access = getAccess(type, Opcodes.ACC_ANNOTATION);
						q.match(access, type);
						addMatched(context, q);
					});
			this.context = context.withAnno(descriptor);
		}

		@Override
		public void visit(String name, Object value) {
			// Skip null
			if (value == null) {
				return;
			}
			if (value instanceof String) {
				queries.stream()
						.filter(q -> q instanceof StringQuery)
						.map(q -> (StringQuery) q)
						.forEach(q -> {
							q.match((String) value);
							addMatched(null, q);
						});
			} else if (value instanceof Number) {
				// TODO: Value
			} else if (value instanceof Character){
				// TODO: Value
			} else if (value.getClass().isArray()) {
				if (value instanceof int[]) {
					// TODO: Value
				}
			}
		}

		@Override
		public void visitEnum(String name, String descriptor, String value) {
			queries.stream()
					.filter(q -> q instanceof ClassReferenceQuery)
					.map(q -> (ClassReferenceQuery) q)
					.forEach(q -> {
						String type = Type.getType(descriptor).getInternalName();
						int access = getAccess(type, Opcodes.ACC_ANNOTATION);
						q.match(access, type);
						addMatched(context, q);
					});
			queries.stream()
					.filter(q -> q instanceof StringQuery)
					.map(q -> (StringQuery) q)
					.forEach(q -> {
						q.match(value);
						addMatched(null, q);
					});
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String descriptor) {
			return new AnnotationSearchVisitor(context, descriptor);
		}

		@Override
		public AnnotationVisitor visitArray(final String name) {
			return this;
		}
	}

	private int getAccess(String name) {
		return getAccess(name, -1);
	}

	private int getAccess(String name, int defaultAcc) {
		// Descriptor format
		if(name.endsWith(";"))
			throw new IllegalStateException("Must use internal name, not descriptor!");
		// Get access
		if(workspace.hasClass(name))
			return workspace.getClassReader(name).getAccess();
		// Unknown
		return defaultAcc;
	}
}
