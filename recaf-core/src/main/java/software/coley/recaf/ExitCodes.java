package software.coley.recaf;

/**
 * Exit codes for Recaf calling {@link System#exit(int)}.
 *
 * @author Matt Coley
 */
public class ExitCodes {
	public static final int SUCCESS = 0;
	public static final int ERR_FX_UNKNOWN = 100;
	public static final int ERR_FX_CLASS_NOT_FOUND = 101;
	public static final int ERR_FX_NO_SUCH_METHOD = 102;
	public static final int ERR_FX_INVOKE_TARGET = 103;
	public static final int ERR_FX_ACCESS_TARGET = 104;
	public static final int ERR_FX_OLD_VERSION = 105;
	public static final int ERR_FX_UNKNOWN_VERSION = 106;
	public static final int INTELLIJ_TERMINATION = 130;
	public static final int ERR_CDI_INIT_FAILURE = 150;
	public static final int ERR_NOT_A_JDK = 160;

}
