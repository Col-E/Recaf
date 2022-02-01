package me.coley.recaf.decompile.fallback.model;

import me.coley.cafedude.ConstPool;
import me.coley.cafedude.Method;
import me.coley.recaf.assemble.ast.Printable;
import me.coley.recaf.decompile.fallback.print.BasicMethodPrintStrategy;
import me.coley.recaf.decompile.fallback.print.ConstructorMethodPrintStrategy;
import me.coley.recaf.decompile.fallback.print.MethodPrintStrategy;
import me.coley.recaf.decompile.fallback.print.StaticInitMethodPrintStrategy;

/**
 * Basic method model wrapping a {@link Method}.
 *
 * @author Matt Coley
 */
public class MethodModel implements Printable {
	private final MethodPrintStrategy printStrategy;
	private final ClassModel owner;
	private final Method method;
	private final ConstPool pool;

	/**
	 * @param owner
	 * 		Declaring class.
	 * @param method
	 * 		Method wrapped.
	 */
	public MethodModel(ClassModel owner, Method method) {
		this.owner = owner;
		this.method = method;
		pool = owner.getClassFile().getPool();
		// Determine print strategy
		if (getName().equals("<init>")) {
			printStrategy = new ConstructorMethodPrintStrategy();
		} else if (getName().equals("<clinit>")) {
			printStrategy = new StaticInitMethodPrintStrategy();
		} else {
			printStrategy = new BasicMethodPrintStrategy();
		}
	}

	/**
	 * @return Method name.
	 */
	public String getName() {
		return pool.getUtf(method.getNameIndex());
	}

	/**
	 * @return Method descriptor.
	 */
	public String getDesc() {
		return pool.getUtf(method.getTypeIndex());
	}

	/**
	 * @return Method modifiers.
	 */
	public int getAccess() {
		return method.getAccess();
	}

	@Override
	public String print() {
		return printStrategy.print(owner, this);
	}
}
