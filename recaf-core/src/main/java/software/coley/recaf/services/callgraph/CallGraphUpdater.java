package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.callgraph.resolver.CallResolver;
import software.coley.recaf.services.callgraph.scanner.AndroidMethodCallScanner;
import software.coley.recaf.services.callgraph.scanner.JvmMethodCallScanner;
import software.coley.recaf.services.callgraph.scanner.MethodCallScanner;
import software.coley.recaf.util.collect.MultiMap;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared call-graph mutation logic for all supported class formats.
 *
 * @author Matt Coley
 */
public class CallGraphUpdater {
	private static final DebuggingLogger logger = Logging.get(CallGraphUpdater.class);
	private final Map<ClassInfo, ClassMethodsContainer> classToMethodsContainer = Collections.synchronizedMap(new IdentityHashMap<>());
	private final MultiMap<String, MethodRef, Set<MethodRef>> unresolvedDeclarations = MultiMap.from(
			new ConcurrentHashMap<>(),
			ConcurrentHashMap::newKeySet);
	private final MultiMap<String, UnresolvedCall, Set<UnresolvedCall>> unresolvedReferences = MultiMap.from(
			new ConcurrentHashMap<>(),
			ConcurrentHashMap::newKeySet);
	private final MethodCallScanner jvmScanner = new JvmMethodCallScanner();
	private final MethodCallScanner androidScanner = new AndroidMethodCallScanner();
	private final CallResolver callResolver;

	public CallGraphUpdater(@Nonnull Workspace workspace) {
		callResolver = new CallResolver(workspace);
	}

	/**
	 * @param classInfo
	 * 		Class to get the method container for.
	 *
	 * @return Method container for the class, created if it does not already exist.
	 */
	@Nonnull
	public ClassMethodsContainer getClassMethodsContainer(@Nonnull ClassInfo classInfo) {
		return classToMethodsContainer.computeIfAbsent(classInfo, ClassMethodsContainer::new);
	}

	/**
	 * @param method
	 * 		Method to get the vertex for.
	 *
	 * @return Vertex for the method, or {@code null} if the declaring class is not yet known to the graph.
	 */
	@Nullable
	public MethodVertex getVertex(@Nonnull MethodMember method) {
		// For any method belonging to a class in the workspace, this shouldn't ever be null.
		// But just to be on the safe side and treat this as a nullable return.
		ClassInfo declaringClass = method.getDeclaringClass();
		if (declaringClass == null)
			return null;
		return getClassMethodsContainer(declaringClass).getVertex(method);
	}

	public synchronized void visitClass(@Nonnull ClassInfo classInfo) {
		// Fill in any declarations that were previously unresolved by this class being added.
		updateUnresolvedDeclarations(classInfo);

		// Scan the class for method calls and attempt to resolve them.
		// If they cannot be resolved, they will be added to the pending list
		// until a future class is added that can satisfy them.
		MethodCallScanner scanner = scannerFor(classInfo);
		if (scanner == null)
			return;
		ClassMethodsContainer classMethodsContainer = getClassMethodsContainer(classInfo);
		scanner.scan(classInfo, (callingMethod, callSite) -> {
			MutableMethodVertex callingVertex = (MutableMethodVertex) classMethodsContainer.getVertex(callingMethod);
			onMethodCalled(classInfo, callingVertex, callSite);
		});

		// For any pending calls that are waiting on this class to be added,
		// attempt to resolve them now that the class is present.
		resolvePending(classInfo.getName());
	}

	/**
	 * Notifies the graph of an update to a given class.
	 *
	 * @param oldCls
	 * 		Old class model.
	 * @param newCls
	 * 		New class model.
	 */
	public synchronized void updateClass(@Nonnull ClassInfo oldCls, @Nonnull ClassInfo newCls) {
		removeClass(oldCls);
		normalizeUnresolvedCalls(newCls);
		visitClass(newCls);
	}

	/**
	 * Notifies the graph of the removal of a given class.
	 *
	 * @param cls
	 * 		Removed class model.
	 */
	public synchronized void removeClass(@Nonnull ClassInfo cls) {
		callResolver.onClassRemoved(cls);

		String clsName = cls.getName();
		ClassMethodsContainer container = getClassMethodsContainer(cls);
		Set<MethodRef> unresolvedWithinOwner = unresolvedDeclarations.get(clsName);
		for (MethodVertex vertex : container.getVertices()) {
			MethodRef ref = vertex.getMethod();
			if (vertex instanceof MutableMethodVertex mutableMethodVertex) {
				Collection<CallEdge> incomingEdges = mutableMethodVertex.getCallerEdges();
				for (CallEdge edge : incomingEdges) {
					MethodMember callingMethod = edge.caller().getResolvedMethod();
					ClassInfo callingClass = callingMethod.getDeclaringClass();
					if (callingClass == null)
						continue;

					// If the calling method is from a class that is not the one being removed,
					// then this is an unresolved reference that needs to be tracked until it can be resolved again.
					unresolvedReferences.put(clsName,
							new UnresolvedCall(callingClass, edge.caller().getMethod(), edge.callSite()));
				}
				mutableMethodVertex.prune();
				unresolvedWithinOwner.add(ref);
			} else {
				logger.warn("Could not prune reference of unknown type: {}", ref);
			}
		}

		classToMethodsContainer.remove(cls);
		removeStaleUnresolvedCallsForRemovedClass(cls);
	}

	/**
	 * @return Map of class names to method references that are called but not resolved to a known method vertex.
	 */
	@Nonnull
	public MultiMap<String, MethodRef, Set<MethodRef>> getUnresolvedDeclarations() {
		return unresolvedDeclarations;
	}

	/**
	 * Called when a class is added/updated.
	 * Removes any unresolved declarations that are now satisfied by the presence of the class.
	 *
	 * @param classInfo
	 * 		Class that was added or updated.
	 */
	private void updateUnresolvedDeclarations(@Nonnull ClassInfo classInfo) {
		String owner = classInfo.getName();
		for (MethodMember method : classInfo.getMethods())
			unresolvedDeclarations.remove(owner, new MethodRef(owner, method.getName(), method.getDescriptor()));
	}

	/**
	 * Called when a class is added/updated.
	 * Attempts to resolve any pending calls that were waiting on the presence of the class.
	 *
	 * @param ownerName
	 * 		Internal name of class that was added/updated.
	 */
	private void resolvePending(@Nonnull String ownerName) {
		Collection<UnresolvedCall> pendingCalls = unresolvedReferences.getIfPresent(ownerName);
		if (pendingCalls.isEmpty())
			return;

		Set<UnresolvedCall> snapshot = new HashSet<>(pendingCalls);
		for (UnresolvedCall unresolvedCall : snapshot) {
			ClassMethodsContainer callingContainer = classToMethodsContainer.get(unresolvedCall.callingClass());
			if (callingContainer == null)
				continue;

			MutableMethodVertex callingVertex = (MutableMethodVertex) callingContainer.getVertex(
					unresolvedCall.callingMethod().name(),
					unresolvedCall.callingMethod().desc());
			if (callingVertex == null)
				continue;

			onMethodCalled(unresolvedCall.callingClass(), callingVertex, unresolvedCall.callSite());
		}
	}

	/**
	 * Called when a method call is observed in a class.
	 * Attempts to resolve the call and link the caller and callee vertices together.
	 *
	 * @param callingClass
	 * 		The class that is making the method call.
	 * @param callingMethod
	 * 		The method that is making the method call.
	 * @param callSite
	 * 		The call site of the method call.
	 */
	private void onMethodCalled(@Nonnull ClassInfo callingClass,
	                            @Nonnull MutableMethodVertex callingMethod,
	                            @Nonnull CallSite callSite) {
		ResolvedMethodCall resolvedCall = callResolver.resolve(callingClass, callSite);
		UnresolvedCall unresolvedCall = new UnresolvedCall(callingClass, callingMethod.getMethod(), callSite);
		if (resolvedCall != null) {
			// Get method vertex of the resolved call and link it to the calling method vertex.
			ClassMethodsContainer resolvedClass = getClassMethodsContainer(resolvedCall.owner());
			MutableMethodVertex resolvedVertex = (MutableMethodVertex) resolvedClass.getVertex(resolvedCall.method());
			callingMethod.addCall(resolvedVertex, callSite);

			// Now that we have the method vertex, we can remove any pending unresolved declarations
			// or references that were waiting on this call to be resolved.
			Collection<MethodRef> unresolvedWithinOwner = unresolvedDeclarations.getIfPresent(callSite.owner());
			if (unresolvedWithinOwner.remove(callSite.methodRef())) {
				if (unresolvedWithinOwner.isEmpty())
					unresolvedDeclarations.remove(callSite.owner());
				logger.debugging(l -> l.info("Satisfy unresolved call {}", callSite.methodRef()));
			}

			// Same idea but with pending references instead of declarations.
			Collection<UnresolvedCall> unresolvedCalls = unresolvedReferences.getIfPresent(callSite.owner());
			if (unresolvedCalls.remove(unresolvedCall)) {
				if (unresolvedCalls.isEmpty())
					unresolvedReferences.remove(callSite.owner());
				logger.debugging(l -> l.info("Satisfy unresolved reference from {} to {}",
						unresolvedCall.callingMethod(),
						callSite.methodRef()));
			}
		} else {
			// If the call cannot be resolved, we need to track it as an unresolved declaration
			// and reference until it can be resolved in the future.
			unresolvedDeclarations.put(callSite.owner(), callSite.methodRef());
			unresolvedReferences.put(callSite.owner(), unresolvedCall);
			logger.debugging(l -> l.warn("Cannot resolve method: {}", callSite.methodRef()));
		}
	}

	/**
	 * Called when a class is updated. Updates references in {@link #unresolvedReferences}.
	 *
	 * @param updated
	 * 		Class that was updated.
	 */
	private void normalizeUnresolvedCalls(@Nonnull ClassInfo updated) {
		String updatedName = updated.getName();
		for (String owner : new HashSet<>(unresolvedReferences.keySet())) {
			Collection<UnresolvedCall> calls = unresolvedReferences.getIfPresent(owner);
			if (calls.isEmpty())
				continue;

			// Check if any of the pending calls are from the updated class.
			// If so, update the calling class reference to point to the new class model.
			Set<UnresolvedCall> snapshot = new HashSet<>(calls);
			for (UnresolvedCall unresolvedCall : snapshot) {
				ClassInfo callingClass = unresolvedCall.callingClass();
				if (callingClass != updated && updatedName.equals(callingClass.getName()) && calls.remove(unresolvedCall))
					unresolvedReferences.put(owner,
							new UnresolvedCall(updated, unresolvedCall.callingMethod(), unresolvedCall.callSite()));
			}

			// If all the pending calls for this owner are from the updated class,
			// we can just remove them and re-add them when we re-scan the class.
			if (calls.isEmpty())
				unresolvedReferences.remove(owner);
		}
	}

	/**
	 * Called when a class is removed. Removes any pending calls from the removed class in {@link #unresolvedReferences}.
	 *
	 * @param removed
	 * 		Removed class model.
	 */
	private void removeStaleUnresolvedCallsForRemovedClass(@Nonnull ClassInfo removed) {
		for (String owner : new HashSet<>(unresolvedReferences.keySet())) {
			Collection<UnresolvedCall> calls = unresolvedReferences.getIfPresent(owner);
			if (calls.isEmpty())
				continue;

			Set<UnresolvedCall> snapshot = new HashSet<>(calls);
			for (UnresolvedCall unresolvedCall : snapshot)
				if (unresolvedCall.callingClass() == removed)
					calls.remove(unresolvedCall);

			if (calls.isEmpty())
				unresolvedReferences.remove(owner);
		}
	}

	@Nullable
	private MethodCallScanner scannerFor(@Nonnull ClassInfo classInfo) {
		if (jvmScanner.supports(classInfo))
			return jvmScanner;
		if (androidScanner.supports(classInfo))
			return androidScanner;
		return null;
	}
}
