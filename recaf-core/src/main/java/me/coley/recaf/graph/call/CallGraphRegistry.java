package me.coley.recaf.graph.call;

import dev.xdark.jlinker.LinkResolver;
import dev.xdark.jlinker.Result;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MemberSignature;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.util.CancelSignal;
import me.coley.recaf.util.MemoizedFunction;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceListener;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import me.coley.recaf.workspace.resource.Resources;
import org.objectweb.asm.*;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CallGraphRegistry implements WorkspaceListener, ResourceClassListener {
	private static final Logger LOGGER = Logging.get(CallGraphRegistry.class);
	private Map<ClassInfo, Map<Descriptor, MethodInfo>> methodMap = new HashMap<>();
	private final Map<MethodInfo, MutableCallGraphVertex> vertexMap = new HashMap<>();
	private final Map<String, Set<UnresolvedCall>> unresolvedCalls = new HashMap<>();
	private final Workspace workspace;

	public CallGraphRegistry(Workspace workspace) {
		this.workspace = workspace;
	}

	public static CallGraphRegistry createAndLoad(Workspace workspace) {
		CallGraphRegistry registry = new CallGraphRegistry(workspace);
		workspace.addListener(registry);
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
		methodMap = new HashMap<>();
		LOGGER.info("Loaded {} vertices, {} unresolved calls", vertexMap.size(), unresolvedCalls.values().stream().mapToInt(Set::size).sum());
	}

	@Nullable
	private static MethodInfo resolveMethodInfo(
			LinkResolver<ClassInfo, MethodInfo, FieldInfo> resolver,
			Function<String, ClassInfo> classInfoFromPathResolver,
			int opcode,
			ClassInfo callClassInfo,
			String name,
			String descriptor
	) {
		Result<dev.xdark.jlinker.Resolution<ClassInfo, MethodInfo>> result;
		switch (opcode) {
			case Opcodes.INVOKESPECIAL:
			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.H_INVOKESPECIAL:
			case Opcodes.H_INVOKEVIRTUAL:
				result = resolver.resolveVirtualMethod(classInfo(callClassInfo, classInfoFromPathResolver), name, descriptor);
				break;
			case Opcodes.INVOKESTATIC:
			case Opcodes.H_INVOKESTATIC:
				result = resolver.resolveStaticMethod(classInfo(callClassInfo, classInfoFromPathResolver), name, descriptor);
				break;
			case Opcodes.INVOKEINTERFACE:
			case Opcodes.H_INVOKEINTERFACE:
				result = resolver.resolveInterfaceMethod(classInfo(callClassInfo, classInfoFromPathResolver), name, descriptor);
				break;
			default:
				throw new IllegalArgumentException("Opcode in visitMethodInsn must be INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.");
		}
		if (result.isSuccess()) {
			return result.value().member().innerValue();
		}
		return null;
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
		final LinkResolver<ClassInfo, MethodInfo, FieldInfo> resolver = LinkResolver.jvm();
		BiFunction<String, String, MethodInfo> thisClassMethodInfoResolver = otherMethodInfoResolver.apply(info);
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
						public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
							if (!"java/lang/invoke/LambdaMetafactory".equals(bootstrapMethodHandle.getOwner())
									|| !"metafactory".equals(bootstrapMethodHandle.getName())
									|| !"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;".equals(bootstrapMethodHandle.getDesc())) {
								super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
								return;
							}
							Object handleObj = bootstrapMethodArguments.length == 3 ? bootstrapMethodArguments[1] : null;
							if (!(handleObj instanceof Handle)) {
								super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
								return;
							}
							Handle handle = (Handle) handleObj;
							switch (handle.getTag()) {
								case Opcodes.H_INVOKESPECIAL:
								case Opcodes.H_INVOKEVIRTUAL:
								case Opcodes.H_INVOKESTATIC:
								case Opcodes.H_INVOKEINTERFACE:
									visitMethodInsn(handle.getTag(), handle.getOwner(), handle.getName(), handle.getDesc(), handle.isInterface());
							}
						}

						@Override
						public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
							visitMethodInstruction(opcode, owner, name, descriptor, vertex, classInfoFromPathResolver, otherMethodInfoResolver, resolver, vertexMap);
						}
					};
				}
				return null;
			}
		}, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
	}

	private void visitMethodInstruction(
			int opcode, String owner, String name, String descriptor, MutableCallGraphVertex vertex,
			Function<String, ClassInfo> classInfoFromPathResolver,
			Function<ClassInfo, BiFunction<String, String, MethodInfo>> otherMethodInfoResolver,
			LinkResolver<ClassInfo, MethodInfo, FieldInfo> resolver,
			Map<MethodInfo, MutableCallGraphVertex> vertexMap
	) {
		ClassInfo callClassInfo = classInfoFromPathResolver.apply(owner);
		if (callClassInfo == null) {
			unresolvedCalls.computeIfAbsent(owner, k -> new HashSet<>()).add(new UnresolvedCall(opcode, owner, name, descriptor, vertex));
			return;
		}
		MethodInfo call = otherMethodInfoResolver.apply(callClassInfo).apply(name, descriptor);
		if (call == null) {
			try {
				call = resolveMethodInfo(resolver, classInfoFromPathResolver, opcode, callClassInfo, name, descriptor);
			} catch (CancelSignal ignored) {}
			// should it log on else here? or would it be spam?
		}
		if (call == null) {
			unresolvedCalls.computeIfAbsent(owner, k -> new HashSet<>()).add(new UnresolvedCall(opcode, owner, name, descriptor, vertex));
			return;
		}
		MutableCallGraphVertex nestedVertex =
				vertexMap.computeIfAbsent(call, MutableCallGraphVertex::new);
		vertex.getCalls().add(nestedVertex);
		nestedVertex.getCallers().add(vertex);
	}

	private void updateUnresolved(
			Function<String, ClassInfo> classInfoFromPathResolver,
			Function<ClassInfo, BiFunction<String, String, MethodInfo>> otherMethodInfoResolver,
			LinkResolver<ClassInfo, MethodInfo, FieldInfo> resolver
	) {
		Set<UnresolvedCall> calls = unresolvedCalls.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
		unresolvedCalls.clear();
		calls.forEach(unresolvedCall -> visitMethodInstruction(
				unresolvedCall.opcode,
				unresolvedCall.owner,
				unresolvedCall.name,
				unresolvedCall.descriptor,
				unresolvedCall.vertex,
				classInfoFromPathResolver,
				otherMethodInfoResolver,
				resolver,
				vertexMap)
		);
	}

	@Override
	public void onAddLibrary(Workspace workspace, Resource library) {
		Function<String, ClassInfo> classInfoFromPathResolver
				= MemoizedFunction.memoize(path ->
				library.getClasses().stream().filter(c -> c.getName().equals(path)).findAny().orElseGet(() -> workspace.getResources().getClass(path))
		);
		Function<ClassInfo, BiFunction<String, String, MethodInfo>> methodMapGetter
				= MemoizedFunction.memoize(clazz -> (name, descriptor) -> getMethodMap(clazz).get(new Descriptor(name, descriptor)));
		updateUnresolved(classInfoFromPathResolver, methodMapGetter, LinkResolver.jvm());
		library.getClasses().forEach(c -> visitClass(c, classInfoFromPathResolver, methodMapGetter));
		LOGGER.info("There are now {} vertices, and {} unresolved calls", vertexMap.size(), unresolvedCalls.values().stream().mapToInt(Set::size).sum());
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		final Function<String, ClassInfo> classInfoFromPathResolver
				= MemoizedFunction.memoize(path -> workspace.getResources().getClass(path));
		final Function<ClassInfo, BiFunction<String, String, MethodInfo>> methodMapGetter
				= MemoizedFunction.memoize(clazz -> (name, descriptor) -> getMethodMap(clazz).get(new Descriptor(name, descriptor)));
		updateUnresolved(classInfoFromPathResolver, methodMapGetter, LinkResolver.jvm());
		visitClass(newValue, classInfoFromPathResolver, methodMapGetter);
	}

	@Override
	public void onRemoveLibrary(Workspace workspace, Resource library) {
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {

	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {

	}

	private static class UnresolvedCall {
		final int opcode;
		final String owner;
		final String name;
		final String descriptor;
		final MutableCallGraphVertex vertex;

		private UnresolvedCall(int opcode, String owner, String name, String descriptor, MutableCallGraphVertex vertex) {
			this.opcode = opcode;
			this.owner = owner;
			this.name = name;
			this.descriptor = descriptor;
			this.vertex = vertex;
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
	}

	protected static final class Descriptor {
		private final String name, desc;

		Descriptor(String name, String desc) {
			this.name = name;
			this.desc = desc;
		}

		Descriptor(MemberSignature signature) {
			this.name = signature.getName();
			this.desc = signature.getDescriptor();
		}

		Descriptor(MethodInfo info) {
			this(info.getMemberSignature());
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
				if (classInfo == null) throw CancelSignal.get();
				return classInfo(classInfo, fn);
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
