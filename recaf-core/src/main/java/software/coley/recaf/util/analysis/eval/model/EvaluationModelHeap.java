package software.coley.recaf.util.analysis.eval.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.eval.EvaluationResult;
import software.coley.recaf.util.analysis.eval.InvokeDynamicExecutor;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.impl.ObjectValueImpl;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Identity-based heap for concurrency objects.
 * <p>
 * The heap deliberately does not use field caches.
 * Lifecycle and completion state are not JVM fields and must disappear with the evaluation context.
 *
 * @author Matt Coley
 */
public final class EvaluationModelHeap {
	private final Map<ObjectValue, Object> states = new IdentityHashMap<>();
	private long nextThreadId = 1;

	/**
	 * Creates the stable synthetic main thread.
	 *
	 * @return Main thread state.
	 */
	@Nonnull
	public ThreadState createMainThread() {
		ObjectValue value = object("java/lang/Thread");
		ThreadState state = new ThreadState(value, "java/lang/Thread", nextThreadId++, "main", null);
		state.setStatus(ThreadStatus.RUNNING);
		states.put(value, state);
		return state;
	}

	/**
	 * @param type
	 * 		Declared concrete evaluator type.
	 * @param parent
	 * 		Creating simulated thread.
	 *
	 * @return New thread state.
	 */
	@Nonnull
	public ThreadState createThread(@Nonnull String type, @Nullable ThreadState parent) {
		ObjectValue value = object(type);
		ThreadState state = new ThreadState(value, type, nextThreadId++, "Thread-" + (nextThreadId - 2), parent == null ? null : parent.value);
		states.put(value, state);
		return state;
	}

	/**
	 * @return New future state.
	 */
	@Nonnull
	public FutureState createFuture() {
		ObjectValue value = object("java/util/concurrent/CompletableFuture");
		FutureState state = new FutureState(value);
		states.put(value, state);
		return state;
	}

	/**
	 * Finds a thread state by identity.
	 *
	 * @param value
	 * 		Candidate value.
	 *
	 * @return Thread state, or {@code null} when it is not model-owned.
	 */
	@Nullable
	public ThreadState thread(@Nullable ReValue value) {
		return value instanceof ObjectValue object ? states.get(object) instanceof ThreadState state ? state : null : null;
	}

	/**
	 * Finds a future state by value identity.
	 *
	 * @param value
	 * 		Candidate value.
	 *
	 * @return Future state, or {@code null} when it is not model-owned.
	 */
	@Nullable
	public FutureState future(@Nullable ReValue value) {
		return value instanceof ObjectValue object ? states.get(object) instanceof FutureState state ? state : null : null;
	}

	/**
	 * @return New object value for the given type.
	 */
	@Nonnull
	private static ObjectValue object(@Nonnull String internalName) {
		return new ObjectValueImpl(Type.getObjectType(internalName), Nullness.NOT_NULL);
	}

	/**
	 * Thread lifecycle state.
	 */
	public enum ThreadStatus {NEW, READY, RUNNING, WAITING, TERMINATED, FAILED}

	/**
	 * Future lifecycle state.
	 */
	public enum FutureStatus {INCOMPLETE, SUCCESS, EXCEPTIONAL, CANCELLED}

	/**
	 * Completion kind for dependent futures.
	 */
	public enum CompletionKind {APPLY, ACCEPT, RUN, COMPOSE, EXCEPTIONALLY, HANDLE}

	/**
	 * Thread state for a model-owned thread.
	 */
	public static final class ThreadState {
		final ObjectValue value;
		final String type;
		final long id;
		final String defaultName;
		final ObjectValue parent;
		@Nullable
		InvokeDynamicExecutor.EvaluatedLambdaValue runnable;
		String name;
		private ThreadStatus status = ThreadStatus.NEW;
		private boolean started;
		boolean interrupted;
		@Nullable
		private EvaluationResult failure;

		private ThreadState(ObjectValue value, String type, long id, String name, ObjectValue parent) {
			this.value = value;
			this.type = type;
			this.id = id;
			this.name = name;
			this.defaultName = name;
			this.parent = parent;
		}

		/**
		 * @return {@code true} once the thread has been started.
		 */
		public boolean isStarted() {
			return started;
		}

		/**
		 * @param started
		 * 		Whether the thread has been started.
		 */
		public void setStarted(boolean started) {
			this.started = started;
		}

		/**
		 * @return Current lifecycle status.
		 */
		@Nonnull
		public ThreadStatus getStatus() {
			return status;
		}

		/**
		 * @param status
		 * 		New lifecycle status.
		 */
		public void setStatus(@Nonnull ThreadStatus status) {
			this.status = status;
		}

		/**
		 * @return Retained task failure, or {@code null} when the task has not failed.
		 */
		@Nullable
		public EvaluationResult getFailure() {
			return failure;
		}

		/**
		 * @param failure
		 * 		Retained task failure, or {@code null} when the task has not failed.
		 */
		public void setFailure(@Nullable EvaluationResult failure) {
			this.failure = failure;
		}
	}

	/**
	 * Future state for a model-owned future.
	 */
	public static final class FutureState {
		final ObjectValue value;
		FutureStatus status = FutureStatus.INCOMPLETE;
		@Nullable
		ReValue result;
		@Nullable
		ReValue exception;
		final java.util.List<Dependent> dependents = new java.util.ArrayList<>();

		private FutureState(ObjectValue value) {
			this.value = value;
		}
	}

	/**
	 * Dependent future state for a model-owned future.
	 */
	public static final class Dependent {
		@Nullable
		final InvokeDynamicExecutor.EvaluatedLambdaValue callable;
		final FutureState target;
		final CompletionKind kind;

		Dependent(@Nullable InvokeDynamicExecutor.EvaluatedLambdaValue callable, FutureState target, CompletionKind kind) {
			this.callable = callable;
			this.target = target;
			this.kind = kind;
		}
	}

}
