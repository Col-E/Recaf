package software.coley.recaf.services.analysis.entry;

import jakarta.annotation.Nonnull;

/**
 * Descriptor of an entry point kind.
 *
 * @param id
 * 		Unique entry kind identifier.
 * @param displayName
 * 		User-facing display name.
 *
 * @author Matt Coley
 */
public record EntryPointKind(@Nonnull String id, @Nonnull String displayName) {
	public static final EntryPointKind JVM_MAIN_METHOD = new EntryPointKind("jvm-main-method", "Java main(String[])");
	public static final EntryPointKind ANDROID_ACTIVITY = new EntryPointKind("android-activity", "Android activity");
}
