package software.coley.recaf.services.ssvm;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableLong;
import software.coley.observables.ObservableString;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link CommonVirtualService}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class CommonVirtualServiceConfig extends BasicConfigContainer implements ServiceConfig {
	public static final String KEY_USE_ALTERNATIVE_BOOT = "fake-classpath";
	public static final String KEY_ALTERNATIVE_BOOT_PATH = "fake-classpath";
	public static final String KEY_FAKE_CLASSPATH = "fake-classpath";
	public static final String KEY_TIME_OFFSET = "time-offset-ms";
	public static final String KEY_USE_HOST_FM = "use-host-file-manager";
	public static final String KEY_HOST_FM_READ = "host-file-manager-allow-read";
	public static final String KEY_HOST_FM_WRITE = "host-file-manager-allow-write";
	public static final String KEY_MAX_INT_STEPS = "max-interpreted-steps-per-method";
	private final ObservableBoolean useAlternateJdkBootPath = new ObservableBoolean(false);
	private final ObservableString alternateJdkPath = new ObservableString("");
	private final ObservableString fakeClasspath = new ObservableString("");
	private final ObservableLong timeOffsetMillis = new ObservableLong(0);
	private final ObservableBoolean useHostFileManager = new ObservableBoolean(false);
	private final ObservableBoolean hostAllowRead = new ObservableBoolean(false);
	private final ObservableBoolean hostAllowWrite = new ObservableBoolean(false);
	private final ObservableInteger maxInterpreterStepsPerMethod = new ObservableInteger(Integer.MAX_VALUE - 1);

	@Inject
	public CommonVirtualServiceConfig() {
		super(ConfigGroups.SERVICE_DEOBFUSCATION, CommonVirtualService.SERVICE_ID + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>(KEY_USE_ALTERNATIVE_BOOT, boolean.class, useAlternateJdkBootPath));
		addValue(new BasicConfigValue<>(KEY_ALTERNATIVE_BOOT_PATH, String.class, alternateJdkPath));
		addValue(new BasicConfigValue<>(KEY_FAKE_CLASSPATH, String.class, fakeClasspath));
		addValue(new BasicConfigValue<>(KEY_TIME_OFFSET, long.class, timeOffsetMillis));
		addValue(new BasicConfigValue<>(KEY_USE_HOST_FM, boolean.class, useHostFileManager));
		addValue(new BasicConfigValue<>(KEY_HOST_FM_READ, boolean.class, hostAllowRead));
		addValue(new BasicConfigValue<>(KEY_HOST_FM_WRITE, boolean.class, hostAllowWrite));
		addValue(new BasicConfigValue<>(KEY_MAX_INT_STEPS, int.class, maxInterpreterStepsPerMethod));
	}

	/**
	 * @return Boolean wrapper, where {@code true} will initialize the VM with classes from an alternate JDK,
	 * specified by {@link #getAlternateJdkPath()}.
	 */
	@Nonnull
	public ObservableBoolean useAlternateJdkBootPath() {
		return useAlternateJdkBootPath;
	}

	/**
	 * @return Path to alternative JDK to use as the boot path.
	 */
	@Nonnull
	public ObservableString getAlternateJdkPath() {
		return alternateJdkPath;
	}

	/**
	 * @return Fake classpath to set within the VM.
	 */
	@Nonnull
	public ObservableString getFakeClasspath() {
		return fakeClasspath;
	}

	/**
	 * @return Offset in millis to shift time within the VM.
	 */
	@Nonnull
	public ObservableLong getTimeOffsetMillis() {
		return timeOffsetMillis;
	}

	/**
	 * @return {@code true} allows the VM to read/write files on the host machine based on:
	 * {@link #hostAllowRead()} and {@link #hostAllowWrite()}.
	 */
	@Nonnull
	public ObservableBoolean useHostFileManager() {
		return useHostFileManager;
	}

	/**
	 * @return {@code true} allows the VM to read files on the host machine.
	 * Requires {@link #useHostFileManager()}.
	 */
	@Nonnull
	public ObservableBoolean hostAllowRead() {
		return hostAllowRead;
	}

	/**
	 * @return {@code true} allows the VM to write files on the host machine.
	 * Requires {@link #useHostFileManager()}.
	 */
	@Nonnull
	public ObservableBoolean hostAllowWrite() {
		return hostAllowWrite;
	}

	/**
	 * @return Maximum number of interpretation steps per method before aborting.
	 */
	@Nonnull
	public ObservableInteger getMaxInterpreterStepsPerMethod() {
		return maxInterpreterStepsPerMethod;
	}
}
