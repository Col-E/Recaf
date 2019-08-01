package me.coley.recaf.search;

/**
 * Utility to allow results to easily be linked with their location.
 *
 * @author Matt
 */
public abstract class Context<T extends Context> {
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
	}
}
