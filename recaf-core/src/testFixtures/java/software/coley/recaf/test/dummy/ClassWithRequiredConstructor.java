package software.coley.recaf.test.dummy;

import jakarta.annotation.Nonnull;
import software.coley.collections.delegate.DelegatingSortedSet;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Dummy class which has a constructor it must implement from its parent type.
 */
public class ClassWithRequiredConstructor extends DelegatingSortedSet<Object> {
	public ClassWithRequiredConstructor(@Nonnull SortedSet<Object> delegate) {
		super(delegate);
	}

	public ClassWithRequiredConstructor(@Nonnull Object[] array) {
		super(new TreeSet<>(Arrays.asList(array)));
	}
}
