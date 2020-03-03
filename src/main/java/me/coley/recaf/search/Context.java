package me.coley.recaf.search;

import me.coley.recaf.parse.bytecode.Disassembler;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Utility to allow results to easily be linked with their location.
 *
 * @param <T>
 * 		Parent context type.
 *
 * @author Matt
 */
@SuppressWarnings("unchecked")
public abstract class Context<T extends Context> implements Comparable<Context<?>> {
	private static final int WIDER_SCOPE = 1;
	private static final int DEEPER_SCOPE = -1;
	protected T parent;

	/**
	 * @return Parent context. {@code null} if the current context is a Class.
	 */
	public T getParent() {
		return parent;
	}

	/**
	 * Creates a context.
	 *
	 * @param access
	 * 		Class modifiers.
	 * @param name
	 * 		Name of class.
	 *
	 * @return Class context.
	 */
	public static ClassContext withClass(int access, String name) {
		return new ClassContext(access, name);
	}

	/**
	 * Appends an annotation context.
	 *
	 * @param type
	 * 		Annotation type.
	 *
	 * @return Annotation context.
	 */
	public AnnotationContext withAnno(String type) {
		return new AnnotationContext(this, type);
	}

	/**
	 * @param other
	 * 		Context to be compared
	 *
	 * @return {@code true} if both contexts are considered similar.
	 */
	public boolean isSimilar(Context<?> other) {
		return this == other || (this.getClass() == other.getClass() && this.compareTo(other) == 0);
	}

	/**
	 * @param other
	 * 		Context to be compared.
	 *
	 * @return {@code true} if this context contains the other.
	 */
	public abstract boolean contains(Context<?> other);

	/**
	 * Class context.
	 */
	public static class ClassContext extends Context<Context> {
		private final int access;
		private final String name;

		/**
		 * @param access
		 * 		Class modifiers.
		 * @param name
		 * 		Name of class.
		 */
		ClassContext(int access, String name) {
			this.access = access;
			this.name = name;
		}

		/**
		 * @return Class modifiers.
		 */
		public int getAccess() {
			return access;
		}

		/**
		 * @return Name of class.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Appends a member context.
		 *
		 * @param access
		 * 		Member modifiers.
		 * @param name
		 * 		Name of member.
		 * @param desc
		 * 		Descriptor of member.
		 *
		 * @return Member context.
		 */
		public MemberContext withMember(int access, String name, String desc) {
			return new MemberContext(this, access, name, desc);
		}

		@Override
		public int compareTo(Context<?> other) {
			if(other instanceof ClassContext) {
				ClassContext otherClass = (ClassContext) other;
				return name.compareTo(otherClass.name);
			}
			return WIDER_SCOPE;
		}

		@Override
		public boolean contains(Context<?> other) {
			// Check if member is in class
			if (other instanceof MemberContext)
				return (other.getParent().compareTo(this) == 0);
			else {
				// Get root context of other
				while (other.getParent() != null)
					other = other.getParent();
				// Check for match against this class
				return (other.compareTo(this) == 0);
			}
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * Member context.
	 */
	public static class MemberContext extends Context<ClassContext> {
		private final int access;
		private final String name;
		private final String desc;

		/**
		 * @param parent
		 * 		Parent context.
		 * @param access
		 * 		Member modifers.
		 * @param name
		 * 		Name of member.
		 * @param desc
		 * 		Descriptor of member.
		 */
		MemberContext(ClassContext parent, int access, String name, String desc) {
			this.parent = parent;
			this.access = access;
			this.name = name;
			this.desc = desc;
		}

		/**
		 * @return Member modifiers.
		 */
		public int getAccess() {
			return access;
		}

		/**
		 * @return Member name.
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return Member descriptor.
		 */
		public String getDesc() {
			return desc;
		}

		/**
		 * @return {@code true} if the {@link #getDesc() descriptor} outlines a field type.
		 */
		public boolean isField() {
			return !isMethod();
		}

		/**
		 * @return {@code true} if the {@link #getDesc() descriptor} outlines a method type.
		 */
		public boolean isMethod() {
			return desc.contains("(");
		}

		/**
		 * Appends an instruction context.
		 *
		 * @param insn
		 * 		Instruction value.
		 * @param pos
		 * 		Offset in the method instructions.
		 *
		 * @return Instruction context.
		 */
		public InsnContext withInsn(AbstractInsnNode insn, int pos) {
			return new InsnContext(this, insn, pos);
		}

		/**
		 * Appends a local variable context.
		 *
		 * @param index
		 * 		Variable index.
		 * @param name
		 * 		Variable name.
		 * @param descriptor
		 * 		Variable descriptor.
		 *
		 * @return Local variable context.
		 */
		public LocalContext withLocal(int index, String name, String descriptor) {
			return new LocalContext(this, index, name, descriptor);
		}

		/**
		 * Appends a catch block context.
		 *
		 * @param type
		 * 		Catch type.
		 *
		 * @return Catch block context.
		 */
		public CatchContext withCatch(String type) {
			return new CatchContext(this, type);
		}


		@Override
		public int compareTo(Context<?> other) {
			if(other instanceof ClassContext) {
				return DEEPER_SCOPE;
			} else if(other instanceof MemberContext) {
				if (parent.compareTo(other.parent) == 0) {
					MemberContext otherMember = (MemberContext) other;
					return (name + desc).compareTo(otherMember.name + otherMember.desc);
				}
			}
			return WIDER_SCOPE;
		}

		@Override
		public boolean contains(Context<?> other) {
			// Check if the other context is an instruction that resides in this method.
			if (other instanceof InsnContext)
				return (other.getParent().compareTo(this) == 0);
			return false;
		}

		@Override
		public String toString() {
			String suffix = isMethod() ? name + desc : name + " " + desc;
			return parent.toString() + " " + suffix;
		}
	}

	/**
	 * Method local variable context.
	 */
	public static class LocalContext extends Context<MemberContext> {
		private final int index;
		private final String name;
		private final String descriptor;

		/**
		 * @param parent
		 * 		Method parent context.
		 * @param index
		 * 		Variable index.
		 * @param name
		 * 		Variable name.
		 * @param descriptor
		 * 		Variable type descriptor.
		 */
		public LocalContext(MemberContext parent, int index, String name, String descriptor) {
			this.parent = parent;
			this.index = index;
			this.name = name;
			this.descriptor = descriptor;
		}

		/**
		 * @return Variable index.
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @return Variable name.
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return Variable type descriptor.
		 */
		public String getDescriptor() {
			return descriptor;
		}

		@Override
		public boolean contains(Context<?> other) {
			return false;
		}

		@Override
		public int compareTo(Context<?> other) {
			if(other instanceof MemberContext) {
				return DEEPER_SCOPE;
			} else if(other instanceof LocalContext) {
				if (parent.compareTo(other.parent) == 0) {
					LocalContext otherLocal = (LocalContext) other;
					return Integer.compare(index, otherLocal.index);
				}
			}
			return WIDER_SCOPE;
		}
	}

	/**
	 * Catch block context.
	 */
	public static class CatchContext extends Context<MemberContext> {
		private final String type;

		/**
		 * @param parent
		 * 		Method parent context.
		 * @param type
		 * 		Catch type.
		 */
		public CatchContext(MemberContext parent, String type) {
			this.parent = parent;
			this.type = type;
		}

		/**
		 * @return Catch type.
		 */
		public String getType() {
			return type;
		}

		@Override
		public boolean contains(Context<?> other) {
			return false;
		}

		@Override
		public int compareTo(Context<?> other) {
			if(other instanceof MemberContext) {
				return DEEPER_SCOPE;
			} else if(other instanceof CatchContext) {
				if (parent.compareTo(other.parent) == 0) {
					CatchContext otherCatch = (CatchContext) other;
					return type.compareTo(otherCatch.type);
				}
			}
			return WIDER_SCOPE;
		}
	}

	/**
	 * Instruction context.
	 */
	public static class InsnContext extends Context<MemberContext> {
		private final AbstractInsnNode insn;
		private final int pos;

		/**
		 * @param parent
		 * 		Parent context.
		 * @param insn
		 * 		Instruction value.
		 * 	 @param pos
		 * 		  		Offset in the method instructions.
		 */
		InsnContext(MemberContext parent, AbstractInsnNode insn, int pos) {
			this.parent = parent;
			this.insn = insn;
			this.pos = pos;
		}

		/**
		 * @return Instruction value.
		 */
		public AbstractInsnNode getInsn() {
			return insn;
		}

		@Override
		public int compareTo(Context<?> other) {
			if(other instanceof ClassContext) {
				return DEEPER_SCOPE;
			} else if(other instanceof MemberContext) {
				return DEEPER_SCOPE;
			} else if(other instanceof InsnContext) {
				if (parent.compareTo(other.parent) == 0) {
					InsnContext otherInsn = (InsnContext) other;
					return Integer.compare(pos, otherInsn.pos);
				}
			}
			// Most deep context, so always be "less than"
			return DEEPER_SCOPE;
		}

		@Override
		public boolean isSimilar(Context<?> other) {
			return this == other || (this.getClass() == other.getClass() && parent.compareTo(other.parent) == 0);
		}

		@Override
		public boolean contains(Context<?> other) {
			// Insns are the deepest scope, so it doesn't make sense to contain anything.
			return false;
		}

		@Override
		public String toString() {
			return parent.toString() + " " + pos + ":" + Disassembler.insn(insn);
		}
	}

	/**
	 * Annotation context.
	 */
	public static class AnnotationContext extends Context<Context> {
		private final String type;

		/**
		 * @param parent
		 * 		Parent context.
		 * @param type
		 * 		Annotation type.
		 */
		AnnotationContext(Context parent, String type) {
			this.parent = parent;
			this.type = type;
		}

		/**
		 * @return Annotation type.
		 */
		public String getType() {
			return type;
		}

		@Override
		@SuppressWarnings("unchecked")
		public int compareTo(Context<?> other) {
			if(other instanceof ClassContext) {
				return DEEPER_SCOPE;
			} else if(other instanceof MemberContext) {
				return WIDER_SCOPE;
			} else if(other instanceof InsnContext) {
				return WIDER_SCOPE;
			} else if(other instanceof AnnotationContext) {
				if (parent.compareTo(other.parent) == 0) {
					AnnotationContext otherAnno = (AnnotationContext) other;
					return type.compareTo(otherAnno.type);
				}
			}
			return WIDER_SCOPE;
		}

		@Override
		public boolean contains(Context<?> other) {
			// Check if the other context is an embedded annotation.
			if (other instanceof AnnotationContext) {
				return (other.getParent().compareTo(this) == 0);
			}
			return false;
		}

		@Override
		public String toString() {
			return "@" + type + " " + parent.toString();
		}
	}
}
