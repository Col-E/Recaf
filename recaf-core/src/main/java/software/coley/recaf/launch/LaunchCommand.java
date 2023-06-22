package software.coley.recaf.launch;

import jakarta.annotation.Nullable;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import software.coley.recaf.Bootstrap;
import software.coley.recaf.RecafBuildConfig;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.StringUtil;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Launch arguments for Recaf.
 *
 * @author Matt Coley
 * @see LaunchArguments Bean accesible form availble to CDI components.
 */
@Command(name = "recaf", mixinStandardHelpOptions = true, version = RecafBuildConfig.VERSION,
		description = "Recaf: The modern Java reverse engineering tool.")
public class LaunchCommand implements Callable<Boolean> {
	@Option(names = {"-i", "--input"}, description = "Input to load into a workspace on startup.")
	private File input;
	@Option(names = {"-s", "--script"}, description = "Script to run on startup.")
	private File script;
	@Option(names = {"-h", "--headless"}, description = "Flag to skip over initializing the UI. Should be paired with -i or -s.")
	private boolean headless;
	@Option(names = {"-v", "--version"}, description = "Display the version information.")
	private boolean version;
	@Option(names = {"-l", "--listservices"}, description = "Display the version information.")
	private boolean listServices;

	@Override
	public Boolean call() throws Exception {
		if (version || listServices)
			System.out.println("======================= RECAF =======================");
		if (version) {
			System.out.printf("""
							VERSION:    %s
							GIT-COMMIT: %s
							GIT-TIME:   %s
							GIT-BRANCH: %s
							=====================================================
							""",
					RecafBuildConfig.VERSION,
					RecafBuildConfig.GIT_SHA,
					RecafBuildConfig.GIT_DATE,
					RecafBuildConfig.GIT_BRANCH
			);
			return true;
		}
		if (listServices) {
			try {
				BeanManager beanManager = Bootstrap.get().getContainer().getBeanManager();
				List<Bean<?>> beans = beanManager.getBeans(Service.class).stream()
						.sorted(Comparator.comparing(o -> o.getBeanClass().getName()))
						.toList();
				System.out.println("Services: " + beans.size());
				for (Bean<?> bean : beans)
					System.out.println(" - " + bean.getBeanClass().getName());
			} catch (Throwable t) {
				System.out.println("Error occurred iterating over services...");
				System.out.println(StringUtil.traceToString(t));
			}
			System.out.println("=====================================================");
			return true;
		}
		return false;
	}

	/**
	 * @return Input to load into a workspace on startup.
	 */
	@Nullable
	public File getInput() {
		return input;
	}

	/**
	 * @return Script to run on startup.
	 */
	@Nullable
	public File getScript() {
		return script;
	}

	/**
	 * @return Flag to skip over initializing the UI.
	 */
	public boolean isHeadless() {
		return headless;
	}
}
