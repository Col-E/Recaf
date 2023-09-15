package software.coley.recaf.services.decompile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableString;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link DecompilerManager}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class DecompilerManagerConfig extends BasicConfigContainer implements ServiceConfig {
	public static final String KEY_PREF_JVM_DECOMPILER = "pref-jvm-decompiler";
	public static final String KEY_PREF_ANDROID_DECOMPILER = "pref-android-decompiler";
	private final ObservableString preferredJvmDecompiler = new ObservableString(null);
	private final ObservableString preferredAndroidDecompiler = new ObservableString(null);

	@Inject
	public DecompilerManagerConfig() {
		super(ConfigGroups.SERVICE_DECOMPILE, DecompilerManager.SERVICE_ID + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>(KEY_PREF_JVM_DECOMPILER, String.class, preferredJvmDecompiler));
		addValue(new BasicConfigValue<>(KEY_PREF_ANDROID_DECOMPILER, String.class, preferredAndroidDecompiler));
	}

	/**
	 * @return {@link JvmDecompiler#getName()} for preferred JVM decompiler to use in {@link DecompilerManager}.
	 */
	public ObservableString getPreferredJvmDecompiler() {
		return preferredJvmDecompiler;
	}

	/**
	 * @return {@link AndroidDecompiler#getName()} for preferred JVM decompiler to use in {@link DecompilerManager}.
	 */
	public ObservableString getPreferredAndroidDecompiler() {
		return preferredAndroidDecompiler;
	}
}
