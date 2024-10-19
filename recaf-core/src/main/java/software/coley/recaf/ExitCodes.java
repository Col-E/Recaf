package software.coley.recaf;

/**
 * Exit codes for Recaf calling {@link System#exit(int)}.
 *
 * @author Matt Coley
 */
public class ExitCodes {
	public static final int SUCCESS = 0;
	public static final int ERR_UNKNOWN = 100;
	public static final int ERR_CLASS_NOT_FOUND = 101;
	public static final int ERR_NO_SUCH_METHOD = 102;
	public static final int ERR_INVOKE_TARGET = 103;
	public static final int ERR_ACCESS_TARGET = 104;
	public static final int ERR_OLD_JFX_VERSION = 105;
	public static final int ERR_UNKNOWN_JFX_VERSION = 106;
	public static final int ERR_CDI_INIT_FAILURE = 107;
	public static final int INTELLIJ_TERMINATION = 130;

}
