package me.coley.recaf.event;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.event.Event;

/**
 * Event for when a method is renamed.
 * 
 * @author Matt
 */
public class HierarchyMethodRenameEvent extends Event {
	private final Set<MethodRenamed> renames = new HashSet<>();

	public void addRename(ClassNode owner, String old, String rename, String desc) {
		renames.add(new MethodRenamed(owner, old, rename, desc));
	}

	public Set<MethodRenamed> getRenamedMethods() {
		return renames;
	}

	public class MethodRenamed {
		public final ClassNode owner;
		public final String old;
		public final String rename;
		public final String desc;

		public MethodRenamed(ClassNode owner, String old, String rename, String desc) {
			this.owner = owner;
			this.old = old;
			this.rename = rename;
			this.desc = desc;
		}

		public MethodNode get() {
			for (MethodNode m : owner.methods) {
				if (m.desc.equals(desc) && m.name.equals(rename)) {
					return m;
				}
			}
			return null;
		}

		@Override
		public int hashCode() {
			return Objects.hash(owner.name, old, rename, desc);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof MethodRenamed) {
				MethodRenamed mr = (MethodRenamed) o;
				//@formatter:off
				return old.equals(mr.old) &&
						rename.equals(mr.rename) && 
						desc.equals(mr.desc) &&
						owner.name.equals(mr.owner.name);
				//@formatter:on
			}
			return false;
		}
	}

}