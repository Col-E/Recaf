package me.coley.recaf.graph.call;

import dev.xdark.jlinker.LinkResolver;
import dev.xdark.jlinker.Result;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.util.MemoizedFunction;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
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
		Function<String, ClassInfo> classInfoFromPathResolver = MemoizedFunction.memoize(path -> workspace.getResources().getClass(path));
		Function<ClassInfo, BiFunction<String, String, MethodInfo>> methodMapGetter
				= MemoizedFunction.memoize(clazz -> (name, descriptor) -> getMethodMap(clazz).get(new Descriptor(name, descriptor)));
		// seems like a hack tho, needs feedback!
		resources.getClasses().forEach(info -> visitClass(info, classInfoFromPathResolver, methodMapGetter));
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

	private void visitClass(
			ClassInfo info,
			Function<String, ClassInfo> classInfoFromPathResolver,
			Function<ClassInfo, BiFunction<String, String, MethodInfo>> otherMethodInfoResolver
	) {
		visitClass(info, classInfoFromPathResolver, otherMethodInfoResolver.apply(info), otherMethodInfoResolver);
	}

	private void visitClass(
			ClassInfo info,
			Function<String, ClassInfo> classInfoFromPathResolver,
			BiFunction<String, String, MethodInfo> thisClassMethodInfoResolver,
			Function<ClassInfo, BiFunction<String, String, MethodInfo>> otherMethodInfoResolver
	) {
		LinkResolver<ClassInfo, MethodInfo, FieldInfo> resolver = LinkResolver.jvm();
		info.getClassReader().accept(new ClassVisitor(Opcodes.ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				MethodInfo info = thisClassMethodInfoResolver.apply(name, descriptor);
				if (info == null)
					return null;
				Map<MethodInfo, MutableCallGraphVertex> vertexMap = CallGraphRegistry.this.vertexMap;
				MutableCallGraphVertex vertex = vertexMap.computeIfAbsent(info, MutableCallGraphVertex::new);
				if (!vertex.visited) {
					vertex.visited = true;
					return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
						@Override
						public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
							ClassInfo callClassInfo = classInfoFromPathResolver.apply(owner);
							if (callClassInfo == null)
								return;
							MethodInfo call = otherMethodInfoResolver.apply(callClassInfo).apply(name, descriptor);
							if (call == null) {
								Result<dev.xdark.jlinker.Resolution<ClassInfo, MethodInfo>> result;
								switch (opcode) {
									case Opcodes.INVOKESPECIAL:
									case Opcodes.INVOKEVIRTUAL:
										result = resolver.resolveVirtualMethod(classInfo(callClassInfo, classInfoFromPathResolver), name, descriptor);
										break;
									case Opcodes.INVOKESTATIC:
										result = resolver.resolveStaticMethod(classInfo(callClassInfo, classInfoFromPathResolver), name, descriptor);
										break;
									case Opcodes.INVOKEINTERFACE:
										result = resolver.resolveInterfaceMethod(classInfo(callClassInfo, classInfoFromPathResolver), name, descriptor);
										break;
									default:
										throw new IllegalArgumentException("Opcode in visitMethodInsn must be INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.");
								}
								if (result.isSuccess()) {
									call = result.value().member().innerValue();
								}
								// should it log on else here? or would it be spam?
							}
							if (call == null)
								return;
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

	private static dev.xdark.jlinker.ClassInfo<ClassInfo> classInfo(@Nonnull ClassInfo node, Function<String, ClassInfo> fn) {
		return new dev.xdark.jlinker.ClassInfo<>() {
			@Override
			public ClassInfo innerValue() {
				return node;
			}

			@Override
			public int accessFlags() {
				return node.getAccess();
			}

			@Override
			public dev.xdark.jlinker.ClassInfo<ClassInfo> superClass() {
				String superName = node.getSuperName();
				if (superName == null) return null;
				final ClassInfo classInfo = fn.apply(superName);
				return classInfo == null ? null : classInfo(classInfo, fn);
			}

			@Override
			public List<dev.xdark.jlinker.ClassInfo<ClassInfo>> interfaces() {
				return node.getInterfaces().stream().map(x -> {
					final ClassInfo classInfo = fn.apply(x);
					return classInfo == null ? null : classInfo(classInfo, fn);
				}).filter(Objects::nonNull).collect(Collectors.toList());
			}

			@Override
			public dev.xdark.jlinker.MemberInfo<?> getMethod(String name, String descriptor) {
				for (MethodInfo method : node.getMethods()) {
					if (name.equals(method.getName()) && descriptor.equals(method.getDescriptor())) {
						return methodInfo(method);
					}
				}
				return null;
			}

			@Override
			public dev.xdark.jlinker.MemberInfo<?> getField(String name, String descriptor) {
				for (FieldInfo field : node.getFields()) {
					if (name.equals(field.getName()) && descriptor.equals(field.getDescriptor())) {
						return fieldInfo(field);
					}
				}
				return null;
			}
		};
	}

	private static dev.xdark.jlinker.MemberInfo<MethodInfo> methodInfo(MethodInfo node) {
		return new dev.xdark.jlinker.MemberInfo<>() {
			@Override
			public MethodInfo innerValue() {
				return node;
			}

			@Override
			public int accessFlags() {
				return node.getAccess();
			}

			@Override
			public boolean isPolymorphic() {
				return false;
			}
		};
	}

	private static dev.xdark.jlinker.MemberInfo<FieldInfo> fieldInfo(FieldInfo node) {
		return new dev.xdark.jlinker.MemberInfo<>() {
			@Override
			public FieldInfo innerValue() {
				return node;
			}

			@Override
			public int accessFlags() {
				return node.getAccess();
			}

			@Override
			public boolean isPolymorphic() {
				return false;
			}
		};
	}
}