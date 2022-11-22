package me.coley.recaf.cdi;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;

import java.lang.annotation.Annotation;
import java.util.Iterator;

/**
 * CDI container delegating actions to another.
 *
 * @author Matt Coley
 */
public class DelegatingContext implements SeContainer {
	private final SeContainer delegate;

	/**
	 * @param delegate
	 * 		Delegate container.
	 */
	public DelegatingContext(SeContainer delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public boolean isRunning() {
		return delegate.isRunning();
	}

	@Override
	public BeanManager getBeanManager() {
		return delegate.getBeanManager();
	}

	@Override
	public Instance<Object> select(Annotation... qualifiers) {
		return delegate.select(qualifiers);
	}

	@Override
	public <U> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
		return delegate.select(subtype, qualifiers);
	}

	@Override
	public <U> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
		return delegate.select(subtype, qualifiers);
	}

	@Override
	public boolean isUnsatisfied() {
		return delegate.isUnsatisfied();
	}

	@Override
	public boolean isAmbiguous() {
		return delegate.isAmbiguous();
	}

	@Override
	public void destroy(Object instance) {
		delegate.destroy(instance);
	}

	@Override
	public Handle<Object> getHandle() {
		return delegate.getHandle();
	}

	@Override
	public Iterable<? extends Handle<Object>> handles() {
		return delegate.handles();
	}

	@Override
	public Object get() {
		return delegate.get();
	}

	@Override
	public Iterator<Object> iterator() {
		return delegate.iterator();
	}
}
