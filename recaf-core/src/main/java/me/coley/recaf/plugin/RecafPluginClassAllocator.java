package me.coley.recaf.plugin;

import dev.xdark.recaf.plugin.AllocationException;
import dev.xdark.recaf.plugin.ClassAllocator;
import dev.xdark.recaf.plugin.ConstructorClassAllocator;
import me.coley.recaf.cdi.RecafContainer;

import static me.coley.recaf.cdi.BeanUtils.*;

/**
 * Class allocator that supports CDI beans in addition to standard classes.
 * Any class annotated with a recognized scope will be loaded by the CDI container.
 * Otherwise, it is assumed the class has a no-args public constructor which can be directly invoked to create an instance.
 *
 * @author Matt Coley
 */
public class RecafPluginClassAllocator implements ClassAllocator {
	private final ClassAllocator allocator = new ConstructorClassAllocator();

	@Override
	public <T> T instance(Class<T> cls) throws AllocationException {
		if (isBean(cls) && (hasInjects(cls) || isWorkspaceBean(cls))) {
			// Beans that have injects should be created by the CDI container.
			// Beans that are @WorkspaceScoped must be created by the CDI container.
			return RecafContainer.get(cls);
		} else {
			// For our purposes we can just use simple constructor allocation.
			// There is nothing to be injected and no state needs to be managed.
			return allocator.instance(cls);
		}
	}
}
