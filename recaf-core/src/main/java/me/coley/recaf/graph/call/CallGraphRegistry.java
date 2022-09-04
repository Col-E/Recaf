package me.coley.recaf.graph.call;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CallGraphRegistry {
	private Map<ClassInfo, Map<Descriptor, MethodInfo>> methodMap = new HashMap<>();
	private final Map<MethodInfo, MutableCallGraphVertex> vertexMap = new HashMap<>();
	private final Workspace workspace;

	public CallGraphRegistry(Workspace workspace) {
		this.workspace = workspace;
	}

	public static CallGraphRegistry createAndLoad(Workspace workspace) {
		CallGraphRegistry registry = new CallGraphRegistry(workspace);
		registry.load();
		return registry;
	}

	public @Nullable CallGraphVertex getVertex(MethodInfo info) {
		return vertexMap.get(info);
	}

	public void load() {
		Resources resources = workspace.getResources();
		resources.getClasses().forEach(this::visitClass);
		methodMap = null;
	}

	private Map<Descriptor, MethodInfo> getMethodMap(ClassInfo info) {
		return methodMap.computeIfAbsent(info, k ->
				k.getMethods()
						.stream()
						.collect(Collectors.toMap(
								Descriptor::new,
								Function.identity()
						))
		);
	}

	private void visitClass(ClassInfo info) {
		Map<Descriptor, MethodInfo> methodMap = getMethodMap(info);
		info.getClassReader().accept(new ClassVisitor(Opcodes.ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				MethodInfo info = methodMap.get(new Descriptor(name, descriptor));
				Map<MethodInfo, MutableCallGraphVertex> vertexMap = CallGraphRegistry.this.vertexMap;
				MutableCallGraphVertex vertex = vertexMap.computeIfAbsent(info, MutableCallGraphVertex::new);
				if (!vertex.visited) {
					vertex.visited = true;
					return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
						@Override
						public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
							ClassInfo info = workspace.getResources().getClass(owner);
							if (info == null) {
								return;
							}
							MethodInfo call = getMethodMap(info).get(new Descriptor(name, descriptor));
							MutableCallGraphVertex nestedVertex = vertexMap.computeIfAbsent(call, MutableCallGraphVertex::new);
							vertex.getCalls().add(nestedVertex);
							nestedVertex.getCallers().add(vertex);
						}
					};
				}
				return null;
			}
		}, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
	}

	private static final class Descriptor {
		private final String name, desc;

		Descriptor(String name, String desc) {
			this.name = name;
			this.desc = desc;
		}

		Descriptor(MethodInfo info) {
			this.name = info.getName();
			this.desc = info.getDescriptor();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Descriptor that = (Descriptor) o;

			if (!name.equals(that.name)) return false;
			return desc.equals(that.desc);
		}

		@Override
		public int hashCode() {
			int result = name.hashCode();
			result = 31 * result + desc.hashCode();
			return result;
		}
	}
}
