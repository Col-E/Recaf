package me.coley.recaf.graph.call;

import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.util.AccessFlag;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class UnresolvedCall {
	private final int opcode;
	private final String owner;
	private final String name;
	private final String descriptor;
	private final MutableCallGraphVertex vertex;
	private MethodInfo dummyMethodInfo;

	UnresolvedCall(int opcode, String owner, String name, String descriptor, MutableCallGraphVertex vertex) {
		this.opcode = opcode;
		this.owner = owner;
		this.name = name;
		this.descriptor = descriptor;
		this.vertex = vertex;
	}

	public int getOpcode() {
		return opcode;
	}

	public String getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getDescriptor() {
		return descriptor;
	}

	public MutableCallGraphVertex getVertex() {
		return vertex;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		UnresolvedCall that = (UnresolvedCall) o;

		if (opcode != that.opcode) return false;
		if (!owner.equals(that.owner)) return false;
		if (!name.equals(that.name)) return false;
		if (!descriptor.equals(that.descriptor)) return false;
		return vertex.equals(that.vertex);
	}

	@Override
	public int hashCode() {
		int result = opcode;
		result = 31 * result + owner.hashCode();
		result = 31 * result + name.hashCode();
		result = 31 * result + descriptor.hashCode();
		result = 31 * result + vertex.hashCode();
		return result;
	}

	public MethodInfo asMethodInfo() {
		if (dummyMethodInfo != null) return dummyMethodInfo;
		return dummyMethodInfo = new UnresolvedMethodInfo(
				owner, name, descriptor,
				"",
				opcode == Opcodes.INVOKESTATIC ? AccessFlag.ACC_STATIC.getMask() : 0,
				List.of(), this);
	}

	public static class UnresolvedMethodInfo extends MethodInfo {
		private final UnresolvedCall unresolvedCall;

		/**
		 * @param owner
		 * 		Name of type defining the member.
		 * @param name
		 * 		Method name.
		 * @param descriptor
		 * 		Method descriptor.
		 * @param signature
		 * 		Method generic signature.
		 * @param access
		 * 		Method access modifiers.
		 * @param exceptions
		 * 		Exception types thrown.
		 * @param unresolvedCall
		 *    Backref to UnresolvedCall
		 */
		private UnresolvedMethodInfo(String owner, String name, String descriptor, String signature, int access, List<String> exceptions, UnresolvedCall unresolvedCall) {
			super(owner, name, descriptor, signature, access, exceptions);
			this.unresolvedCall = unresolvedCall;
		}

		public UnresolvedCall getUnresolvedCall() {
			return unresolvedCall;
		}
	}
}
