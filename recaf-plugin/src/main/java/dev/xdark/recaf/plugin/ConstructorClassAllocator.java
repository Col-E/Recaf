package dev.xdark.recaf.plugin;

/**
 * Allocator implementation supporting simple no-arg constructors.
 *
 * @author Matt Coley
 */
public class ConstructorClassAllocator implements ClassAllocator {
	@Override
	public <T> T instance(Class<T> cls) throws AllocationException {
		try {
			return cls.getConstructor().newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new AllocationException(cls, ex);
		}
	}
}
