package software.coley.recaf.workspace.model.bundle;

import software.coley.recaf.info.ClassInfo;

/**
 * Common base for bundles containing {@link ClassInfo} child types.
 *
 * @param <I>
 * 		Bundle value type.
 *
 * @author Matt Coley
 * @see JvmClassBundle
 * @see AndroidClassBundle
 */
public interface ClassBundle<I extends ClassInfo> extends Bundle<I> {
}
