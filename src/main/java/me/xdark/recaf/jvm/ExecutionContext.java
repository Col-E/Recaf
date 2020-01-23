package me.xdark.recaf.jvm;

import me.coley.recaf.util.InsnUtil;
import me.xdark.recaf.jvm.classloading.ClassLoader;
import org.objectweb.asm.tree.LabelNode;

public final class ExecutionContext<R> {
	private final ExecutionStack stack;
	private final Object[] locals;
	private final ClassLoader classLoader;
	private final Method method;
	private final VirtualMachine vm;
	private int cursor;

	public ExecutionContext(int maxStack, int maxLocals, Method method, VirtualMachine vm) {
		this.stack = new ExecutionStack(maxStack);
		this.locals = new Object[maxLocals];
		this.classLoader = method.getDeclaringClass().getClassLoader();
		this.method = method;
		this.vm = vm;
	}

	public void push(Object v) {
		stack.push(v);
	}

	public <V> V pop() {
		return stack.pop();
	}

	public void clearStack() {
		stack.clear();
	}

	public void store(int index, Object v) {
		locals[index] = v;
	}

	public <V> V load(int index) {
		return (V) locals[index];
	}

	public boolean isStackEmpty() {
		return stack.isEmpty();
	}

	public void jump(LabelNode labelNode) {
		setCursor(InsnUtil.index(labelNode));
	}

	public int nextCursor() {
		return cursor++;
	}

	public void setCursor(int cursor) {
		this.cursor = cursor;
	}

	public void complete(R result) {
		stack.clear();
		stack.push(result);
		stop();
	}

	public void stop() {
		throw ExecutionCancelSignal.INSTANCE;
	}

	public Long popLong() throws VMException {
		Object top = pop();
		if (top != VMTop.INSTANCE) {
			throw new InvalidBytecodeException("Expected VMTop, but got: " + top);
		}
		Object v = pop();
		if (!(v instanceof Long)) {
			throw new InvalidBytecodeException("Expected to pop long, but got: " + v);
		}
		return (Long) v;
	}

	public Double popDouble() throws VMException {
		Object top = pop();
		if (top != VMTop.INSTANCE) {
			throw new InvalidBytecodeException("Expected VMTop, but got: " + top);
		}
		Object v = pop();
		if (!(v instanceof Double)) {
			throw new InvalidBytecodeException("Expected to pop double, but got: " + v);
		}
		return (Double) v;
	}

	public int popInteger() throws VMException {
		Object v = pop();
		if (!(v instanceof Number)) {
			throw new InvalidBytecodeException("Expected to pop int, but got: " + v);
		}
		return ((Number) v).intValue();
	}

	public Float popFloat() throws VMException {
		Object v = pop();
		if (!(v instanceof Float)) {
			throw new InvalidBytecodeException("Expected to pop float, but got: " + v);
		}
		return (Float) v;
	}

	public void pushTop(Object v) {
		push(v);
		push(VMTop.INSTANCE);
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public Method getMethod() {
		return method;
	}

	public VirtualMachine getVM() {
		return vm;
	}
}
