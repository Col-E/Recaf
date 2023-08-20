package me.coley.recaf.graph.call;

import dev.xdark.jlinker.LinkResolver;
import dev.xdark.jlinker.Result;
import me.coley.recaf.code.*;
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
	private final Map<ClassInfo, Map<Descriptor, MethodInfo>> methodMap = new HashMap<>();
	private final Map<MethodInfo, MutableCallGraphVertex> vertexMap = new HashMap<>();
	private final Map<String, Set<UnresolvedCall>> unresolvedCalls = new HashMap<>();
	private final Workspace workspace;

	public CallGraphRegistry(Workspace workspace) {
		this.workspace = workspace;
	}

	public static CallGraphRegistry createAndLoad(Workspace workspace) {
		CallGraphRegistry registry = create(workspace);
		registry.load();
		return registry;
	}

	public static CallGraphRegistry create(Workspace workspace) {
		CallGraphRegistry registry = new CallGraphRegistry(workspace);
		workspace.addListener(registry);
		return registry;
	}

	public void clear() {
		methodMap.clear();
		vertexMap.clear();
		unresolvedCalls.clear();
	}

	@Nullable
	public CallGraphVertex getVertex(MethodInfo info) {
		return vertexMap.get(info);
	}

	public Map<String, Set<UnresolvedCall>> getUnresolvedCalls() {
		return unresolvedCalls;
	}

	public void load() {
		Resources resources = workspace.getResources();
		Function<String, ClassInfo> classInfoFromPathResolver = MemoizedFunction.memoize(path -> workspace.getResources().getClass(path));
		Function<ClassInfo, BiFunction<String, String, MethodInfo>> methodMapGetter
				= clazz -> (name, descriptor) -> getMethodMap(clazz).get(new Descriptor(name, descriptor));
		// seems like a hack tho, needs feedback!
		final CachedLinkResolver resolver = new CachedLinkResolver();
		resources.getClasses().forEach(info -> visitClass(info, classInfoFromPathResolver, methodMapGetter, resolver));
		methodMap.clear();
		LOGGER.debug("Loaded {} vertices for {} classes, {} unresolved calls for {} methods in {} classes", vertexMap.size(),
				vertexMap.keySet().stream().map(MethodInfo::getOwner).distinct().count(),
				unresolvedCalls.values().stream().mapToInt(Set::size).sum(),
				unresolvedCalls.values().stream().flatMap(Collection::stream).map(UnresolvedCall::getVertex).distinct().count(),
				unresolvedCalls.values().stream().flatMap(Collection::stream)
						.map(UnresolvedCall::getVertex).map(MutableCallGraphVertex::getMethodInfo)
						.map(MemberInfo::getOwner)
						.distinct().count());
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
		final dev.xdark.jlinker.ClassInfo<ClassInfo> classInfo = classInfo(callClassInfo, classInfoFromPathResolver);
		switch (opcode) {
			case Opcodes.INVOKESPECIAL:
			case Opcodes.H_INVOKESPECIAL:
				result = resolver.resolveSpecialMethod(classInfo, name, descriptor);
				break;
			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.H_INVOKEVIRTUAL:
				result = resolver.resolveVirtualMethod(classInfo, name, descriptor);
				break;
			case Opcodes.INVOKESTATIC:
			case Opcodes.H_INVOKESTATIC:
				result = resolver.resolveStaticMethod(classInfo, name, descriptor);
				break;
			case Opcodes.INVOKEINTERFACE:
			case Opcodes.H_INVOKEINTERFACE:
				result = resolver.resolveInterfaceMethod(classInfo, name, descriptor);
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
			Function<ClassInfo, BiFunction<String, String, MethodInfo>> otherMethodInfoResolver,
			LinkResolver<ClassInfo, MethodInfo, FieldInfo> resolver
	) {
		BiFunction<String, String, MethodInfo> thisClassMethodInfoResolver = otherMethodInfoResolver.apply(info);
		info.getClassReader().accept(new MethodCallsResolverClassVisitor(thisClassMethodInfoResolver, classInfoFromPathResolver, otherMethodInfoResolver, resolver), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
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
		MutableCallGraphVertex nestedVertex = vertexMap.computeIfAbsent(call, MutableCallGraphVertex::new);
		vertex.getCalls().add(nestedVertex);
		nestedVertex.getCallers().add(vertex);
	}

	private void updateUnresolved(
			Function<String, ClassInfo> classInfoFromPathResolver,
			Function<ClassInfo, BiFunction<String, String, MethodInfo>> otherMethodInfoResolver,
			LinkResolver<ClassInfo, MethodInfo, FieldInfo> resolver
	) {
		final int oldSum = unresolvedCalls.values().stream().mapToInt(Set::size).sum();
		LOGGER.debug("Resolving {} unresolved calls...", oldSum);
		Set<UnresolvedCall> calls = unresolvedCalls.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
		unresolvedCalls.clear();
		calls.forEach(unresolvedCall -> visitMethodInstruction(
				unresolvedCall.getOpcode(),
				unresolvedCall.getOwner(),
				unresolvedCall.getName(),
				unresolvedCall.getDescriptor(),
				unresolvedCall.getVertex(),
				classInfoFromPathResolver,
				otherMethodInfoResolver,
				resolver,
				vertexMap)
		);
		final int newSum = unresolvedCalls.values().stream().mapToInt(Set::size).sum();
		if (oldSum == newSum) LOGGER.debug("The number of unresolved calls didn't change.");
		else if (oldSum < newSum) LOGGER.debug("{} new unresolved calls added, there are {} now.", newSum - oldSum, newSum);
		else LOGGER.debug("{} unresolved calls resolved, there are {} now.", oldSum - newSum, newSum);
	}

	@Override
	public void onAddLibrary(Workspace workspace, Resource library) {
		LOGGER.debug("Adding {} classes...", library.getClasses().size());
		Function<String, ClassInfo> classInfoFromPathResolver
				= MemoizedFunction.memoize(path ->
				library.getClasses().stream().filter(c -> c.getName().equals(path)).findAny().orElseGet(() -> workspace.getResources().getClass(path))
		);
		Function<ClassInfo, BiFunction<String, String, MethodInfo>> methodMapGetter
				= clazz -> (name, descriptor) -> getMethodMap(clazz).get(new Descriptor(name, descriptor));
		updateUnresolved(classInfoFromPathResolver, methodMapGetter, LinkResolver.jvm());
		final CachedLinkResolver resolver = new CachedLinkResolver();
		library.getClasses().forEach(c -> visitClass(c, classInfoFromPathResolver, methodMapGetter, resolver));
		LOGGER.debug("There are now {} vertices for {} classes, {} unresolved calls for {} methods in {} classes", vertexMap.size(),
				vertexMap.keySet().stream().map(MethodInfo::getOwner).distinct().count(),
				unresolvedCalls.values().stream().mapToInt(Set::size).sum(),
				unresolvedCalls.values().stream().flatMap(Collection::stream).map(UnresolvedCall::getVertex).distinct().count(),
				unresolvedCalls.values().stream().flatMap(Collection::stream)
						.map(UnresolvedCall::getVertex).map(MutableCallGraphVertex::getMethodInfo)
						.map(MemberInfo::getOwner)
						.distinct().count());
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		LOGGER.debug("Adding {} methods...", newValue.getMethods().size());
		final Function<String, ClassInfo> classInfoFromPathResolver
				= MemoizedFunction.memoize(path -> workspace.getResources().getClass(path));
		final Function<ClassInfo, BiFunction<String, String, MethodInfo>> methodMapGetter
				= clazz -> (name, descriptor) -> getMethodMap(clazz).get(new Descriptor(name, descriptor));
		updateUnresolved(classInfoFromPathResolver, methodMapGetter, LinkResolver.jvm());
		visitClass(newValue, classInfoFromPathResolver, methodMapGetter, new CachedLinkResolver());
		LOGGER.debug("There are now {} vertices for {} classes, {} unresolved calls for {} methods in {} classes", vertexMap.size(),
				vertexMap.keySet().stream().map(MethodInfo::getOwner).distinct().count(),
				unresolvedCalls.values().stream().mapToInt(Set::size).sum(),
				unresolvedCalls.values().stream().flatMap(Collection::stream).map(UnresolvedCall::getVertex).distinct().count(),
				unresolvedCalls.values().stream().flatMap(Collection::stream)
						.map(UnresolvedCall::getVertex).map(MutableCallGraphVertex::getMethodInfo)
						.map(MemberInfo::getOwner)
						.distinct().count());
	}

	@Override
	public void onRemoveLibrary(Workspace workspace, Resource library) {
		//		LOGGER.debug("Removing {} classes...", library.getClasses().size());
		//		updateRemovedMethods(workspace, library.getClasses().values());
		//		LOGGER.debug("There are now {} vertices, and {} unresolved calls", vertexMap.size(), unresolvedCalls.values().stream().mapToInt(Set::size).sum());
		clear();
		load();
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {
		//		LOGGER.debug("Removing {} methods...", oldValue.getMethods().size());
		//		updateRemovedMethods(workspace, Set.of(oldValue));
		//		LOGGER.debug("There are now {} vertices, and {} unresolved calls.", vertexMap.size(), unresolvedCalls.values().stream().mapToInt(Set::size).sum());
		clear();
		load();
	}

	private void updateRemovedMethods(Workspace workspace, Collection<ClassInfo> removedClasses) {
		// Doesn't work as expected, as it leaves - for whatever reason - zombie vertices and calls
		// So, it's just "cleaner" to reload the whole thing...
		// I leave this, so someday we can return to it, and have an idea of what to do
		Set<String> removedClassNames = removedClasses.stream().map(ClassInfo::getName).collect(Collectors.toSet());
		methodMap.keySet().removeIf(c -> removedClassNames.contains(c.getName()));
		unresolvedCalls.keySet().removeIf(removedClassNames::contains);
		unresolvedCalls.values().removeIf(cs -> {
			cs.removeIf(c -> removedClassNames.contains(c.getVertex().getMethodInfo().getOwner()));
			return cs.isEmpty();
		});
		Set<CallGraphVertex> affectedVertices = vertexMap.values().stream()
				.filter(v -> removedClassNames.contains(v.getMethodInfo().getOwner()))
				.flatMap(vertex -> {
					// removed -/> called
					for (CallGraphVertex m : vertex.getCalls()) {
						m.getCallers().remove(vertex);
					}
					// callers -> removed
					return vertex.getCallers().stream();
				})
				.filter(m -> removedClassNames.contains(m.getMethodInfo().getOwner()))
				.collect(Collectors.toSet());
		vertexMap.values().removeIf(v -> removedClassNames.contains(v.getMethodInfo().getOwner()));
		// these are all MutableCallGraphVertex, we know it, right? riiight?
		//noinspection unchecked,rawtypes
		for (MutableCallGraphVertex affectedVertex : (Set<MutableCallGraphVertex>) (Set) affectedVertices) {
			affectedVertex.visited = false;
			// callers -/> called
			for (CallGraphVertex call : affectedVertex.getCalls()) {
				call.getCallers().remove(affectedVertex);
			}
			affectedVertex.getCalls().clear();
		}
		final Set<String> affectedClasses = affectedVertices.stream()
				.map(v -> v.getMethodInfo().getOwner()).collect(Collectors.toSet());
		Function<String, ClassInfo> classInfoFromPathResolver = MemoizedFunction.memoize(path -> workspace.getResources().getClass(path));
		final Set<ClassInfo> existingAffectedClasses = affectedClasses.stream()
				.map(classInfoFromPathResolver)
				.filter(Objects::nonNull).collect(Collectors.toSet());
		LOGGER.debug("After affecting {} classes, there are now {} vertices, and {} unresolved calls. Rewiring {} existing classes.", affectedClasses.size(), vertexMap.size(), unresolvedCalls.values().stream().mapToInt(Set::size).sum(), existingAffectedClasses.size());
		Function<ClassInfo, BiFunction<String, String, MethodInfo>> methodMapGetter
				= clazz -> (name, descriptor) -> getMethodMap(clazz).get(new Descriptor(name, descriptor));
		final CachedLinkResolver resolver = new CachedLinkResolver();
		existingAffectedClasses.forEach(classInfo -> visitClass(classInfo, classInfoFromPathResolver, methodMapGetter, resolver));
	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
		onRemoveClass(resource, oldValue);
		onNewClass(resource, newValue);
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

	private static dev.xdark.jlinker.ClassInfo<ClassInfo> classInfo(
			@Nonnull ClassInfo node, Function<String, ClassInfo> pathToClass) {
		return new dev.xdark.jlinker.ClassInfo<>() {
			private dev.xdark.jlinker.ClassInfo<ClassInfo> superClass = null;
			private List<dev.xdark.jlinker.ClassInfo<ClassInfo>> interfaces = null;
			private final BiFunction<String, String, dev.xdark.jlinker.MemberInfo<?>> methodResolver = MemoizedFunction.memoize((name, descriptor) -> {
				for (MethodInfo method : node.getMethods()) {
					if (name.equals(method.getName()) && descriptor.equals(method.getDescriptor())) {
						return methodInfo(method);
					}
				}
				return null;
			});

			private final BiFunction<String, String, dev.xdark.jlinker.MemberInfo<?>> fieldResolver = MemoizedFunction.memoize((name, descriptor) -> {
				for (FieldInfo field : node.getFields()) {
					if (name.equals(field.getName()) && descriptor.equals(field.getDescriptor())) {
						return fieldInfo(field);
					}
				}
				return null;
			});

			@Override
			public ClassInfo innerValue() {
				return node;
			}

			@Override
			public int accessFlags() {
				return node.getAccess();
			}

			@Nonnull
			@Override
			public dev.xdark.jlinker.ClassInfo<ClassInfo> superClass() {
				if (superClass != null) return superClass;
				String superName = node.getSuperName();
				// should it just return null instead?
				if (superName == null) throw CancelSignal.get();
				final ClassInfo classInfo = pathToClass.apply(superName);
				if (classInfo == null) throw CancelSignal.get();
				return superClass = classInfo(classInfo, pathToClass);
			}

			@Nonnull
			@Override
			public List<dev.xdark.jlinker.ClassInfo<ClassInfo>> interfaces() {
				if (interfaces != null) return interfaces;
				return interfaces = node.getInterfaces().stream().map(x -> {
					final ClassInfo classInfo = pathToClass.apply(x);
					return classInfo == null ? null : classInfo(classInfo, pathToClass);
				}).filter(Objects::nonNull).collect(Collectors.toList());
			}

			@Override
			public dev.xdark.jlinker.MemberInfo<?> getMethod(String name, String descriptor) {
				return methodResolver.apply(name, descriptor);
			}

			@Override
			public dev.xdark.jlinker.MemberInfo<?> getField(String name, String descriptor) {
				return fieldResolver.apply(name, descriptor);
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

	private class MethodCallsResolverClassVisitor extends ClassVisitor {
		private final BiFunction<String, String, MethodInfo> thisClassMethodInfoResolver;
		private final Function<String, ClassInfo> classInfoFromPathResolver;
		private final Function<ClassInfo, BiFunction<String, String, MethodInfo>> otherMethodInfoResolver;
		private final LinkResolver<ClassInfo, MethodInfo, FieldInfo> resolver;

		public MethodCallsResolverClassVisitor(BiFunction<String, String, MethodInfo> thisClassMethodInfoResolver, Function<String, ClassInfo> classInfoFromPathResolver, Function<ClassInfo, BiFunction<String, String, MethodInfo>> otherMethodInfoResolver, LinkResolver<ClassInfo, MethodInfo, FieldInfo> resolver) {
			super(Opcodes.ASM9);
			this.thisClassMethodInfoResolver = thisClassMethodInfoResolver;
			this.classInfoFromPathResolver = classInfoFromPathResolver;
			this.otherMethodInfoResolver = otherMethodInfoResolver;
			this.resolver = resolver;
		}

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
	}
}
