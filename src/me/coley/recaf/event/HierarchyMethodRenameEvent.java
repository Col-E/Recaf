package me.coley.recaf.event;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.event.Event;
import me.coley.recaf.Input;

/**
 * Event for when a method is renamed.
 * 
 * @author Matt
 */
public class HierarchyMethodRenameEvent extends Event {
	private final Set<MethodRenamed> renames = new HashSet<>();

	public void addRename(String owner, String old, String rename, String desc) {
		renames.add(new MethodRenamed(owner, old, rename, desc));
	}

	public Set<MethodRenamed> getRenamedMethods() {
		return renames;
	}

	/**
	 * Wrapper for renamed method.
	 * 
	 * @author Matt
	 */
	public class MethodRenamed {
		/**
		 * Class that owns the method.
		 */
		public final String owner;
		/**
		 * Old method name.
		 */
		public final String old;
		/**
		 * New method name.
		 */
		public final String rename;
		/**
		 * Method descriptor.
		 */
		public final String desc;

		public MethodRenamed(String owner, String old, String rename, String desc) {
			this.owner = owner;
			this.old = old;
			this.rename = rename;
			this.desc = desc;
		}

		/**
		 * Fetch MethodNode instance from the class that has undergone method
		 * remapping.
		 * 
		 * @return MethodNode instance.
		 */
		public MethodNode get() {
			// We need to fetch it this way rather than passing in the
			// ClassNode instance found in the event handler for
			// MethodRenameEvent in Input.
			// That was one of the issues that was responsible for breaking
			// the "linked method remapping" feature.
			ClassNode cn = Input.get().getClass(owner);
			for (MethodNode m : cn.methods) {
				if (m.desc.equals(desc) && m.name.equals(rename)) {
					return m;
				}
			}
			return null;
		}

		@Override
		public int hashCode() {
			return Objects.hash(owner, old, rename, desc);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof MethodRenamed) {
				return hashCode() == o.hashCode();
			}
			return false;
		}
	}

}