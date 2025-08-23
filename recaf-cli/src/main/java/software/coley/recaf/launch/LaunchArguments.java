package software.coley.recaf.launch;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.services.file.RecafDirectoriesConfig;

import java.io.File;

/**
 * Launch arguments of Recaf.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class LaunchArguments {
	private final RecafDirectoriesConfig directoriesConfig;
	private LaunchCommand command;
	private String[] args = new String[0];

	@Inject
	public LaunchArguments(@Nonnull RecafDirectoriesConfig directoriesConfig) {
		this.directoriesConfig = directoriesConfig;
	}

	/**
	 * @param command
	 * 		PicoCli command impl for receiving inputs.
	 */
	public void setCommand(@Nonnull LaunchCommand command) {
		// Only allow setting it once.
		if (this.command == null)
			this.command = command;
	}

	/**
	 * @param args
	 * 		Literal args used to launch recaf.
	 */
	public void setRawArgs(@Nonnull String[] args) {
		// Only allow setting it once.
		if (this.args == null)
			this.args = args;
	}

	/**
	 * @return Literal args used to launch recaf.
	 */
	@Nonnull
	public String[] getArgs() {
		return args;
	}

	/**
	 * @return Input to load into a workspace on startup.
	 */
	@Nullable
	public File getInput() {
		if (command == null) return null;
		return command.getInput();
	}

	/**
	 * @return Script to run on startup.
	 *
	 * @see #getScriptInScriptsDirectory() Alternative to check for the existence of the script file path
	 * in the script directory.
	 */
	@Nullable
	public File getScript() {
		if (command == null) return null;
		return command.getScript();
	}

	/**
	 * @return Script to run on startup.
	 *
	 * @see #getScript() Alternative to check for the existence of the script file path
	 * relative to the current directory.
	 */
	@Nullable
	public File getScriptInScriptsDirectory() {
		File script = getScript();
		if (script == null) return null;
		return directoriesConfig.getScriptsDirectory().resolve(script.getName()).toFile();
	}

	/**
	 * @return Flag to skip over initializing the UI.
	 */
	public boolean isHeadless() {
		if (command == null) return false;
		return command.isHeadless();
	}
}
