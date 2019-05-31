package me.coley.recaf.config.impl;

import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.Agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import me.coley.recaf.config.Conf;
import me.coley.recaf.config.Config;

/**
 * Options for ASM reading and writing.
 * 
 * @author Matt
 */
public class ConfASM extends Config {
	/**
	 * Version of ASM to use.
	 * <hr>
	 * Not typically an issue unless you are using the agent functionality on a
	 * process that has a different version of ASM. For instance attaching to an
	 * ASM4 program and using ASM5 will have ASM throw an exception.
	 */
	@Conf(category = "asm", key = "version")
	public ASMVersion version = ASMVersion.V7;
	/**
	 * Flag for ASM output, compute maximum stack/local sizes for methods.
	 */
	@Conf(category = "asm", key = "out.computemaxs")
	public boolean computeMax = true;
	/**
	 * Flag for ASM output, compute stack-frames for methods.
	 * <hr>
	 * Enabled by default so users do not have to worry about messing up the
	 * stack and having to edit the frames.
	 */
	@Conf(category = "asm", key = "out.computeframes")
	public boolean computeFrams = true;
	/**
	 * Option for toggling whether exporting jars should use reflection-lookups
	 * for classes not found in the loaded input. Used for determining proper
	 * parent hierarchy.
	 */
	@Conf(category = "asm", key = "out.reflectionexport")
	public boolean reflectionExport = true;
	/**
	 * Flag for ASM input, skip reading method code.
	 */
	@Conf(category = "asm", key = "in.skipcode")
	public boolean skipCode;
	/**
	 * Flag for ASM input, skip reading debug information <i>(variable names,
	 * line numbers, etc.)</i>.
	 */
	@Conf(category = "asm", key = "in.skipdebug")
	public boolean skipDebug;
	/**
	 * Flag for ASM input, skip reading stack-frames in method code.
	 * <hr>
	 * Enabled by default since frames are recalculated by default when Recaf
	 * exports changes.
	 */
	@Conf(category = "asm", key = "in.skipframes")
	public boolean skipFrames = true;
	/**
	 * Flag for ASM input, expand stack-frames into a standard format in method
	 * code.
	 */
	@Conf(category = "asm", key = "in.expandframes")
	public boolean expandFrames;
	/**
	 * Used to indicate if linked<i>(Overrides / parents)</i> methods should be
	 * renamed when updating a method's name.
	 */
	@Conf(category = "asm", key = "edit.linkedmethods")
	public boolean linkedMethodReplace = true;
	/**
	 * Used to disallow renaming of locked methods' names. <i>(Required
	 * {@link #useLinkedMethodRenaming() linked method renaming} to be used.
	 */
	@Conf(category = "asm", key = "edit.locklibmethods")
	public boolean lockLibraryMethods = true;
	/**
	 * Option for alerting users that edits have produced invalid bytecode
	 * during editing.
	 */
	@Conf(category = "asm", key = "edit.verify")
	public boolean verify = true;

	public ConfASM() {
		super("rc_asm");
		load();
		ensureVersionComplaince();
	}

	/**
	 * @return ASM version.
	 */
	public int getVersion() {
		return version.value;
	}

	/**
	 * @return Flags to be used in {@code ClassWriter}s.
	 */
	public int getOutputFlags() {
		int flags = 0;
		if (computeMax) flags |= ClassWriter.COMPUTE_MAXS;
		if (computeFrams) flags |= ClassWriter.COMPUTE_FRAMES;
		return flags;
	}

	/**
	 * @return {@code ClassWriter} with configuration's flags.
	 */
	public int getInputFlags() {
		int flags = 0;
		if (skipCode) flags |= ClassReader.SKIP_CODE;
		if (skipDebug) flags |= ClassReader.SKIP_DEBUG;
		if (skipFrames) flags |= ClassReader.SKIP_FRAMES;
		if (expandFrames) flags |= ClassReader.EXPAND_FRAMES;
		return flags;
	}

	/**
	 * @return {@code true} if the flags will ignore max-stack and max-local
	 *         attributes and recalculate them automatically. {@code false} if
	 *         recalculation not supported by {@link #getOutputFlags()}.
	 */
	public boolean ignoreMaxs() {
		return computeMax | computeFrams;
	}

	/**
	 * @return {@code true} if the flags will ignore stack-map attributes and
	 *         recalculate them automatically. {@code false} if recalculation
	 *         not supported by {@link #getOutputFlags()}.
	 */
	public boolean ignoreStack() {
		return computeFrams;
	}

	/**
	 * @return {@code true} if exporting can use reflection to find classes not
	 *         found in the input file.
	 */
	public boolean useReflection() {
		return reflectionExport;
	}

	/**
	 * @return {@code true} if method renaming should update linked methods as
	 *         opposed to only the single method being renamed in the UI.
	 */
	public boolean useLinkedMethodRenaming() {
		return linkedMethodReplace;
	}

	/**
	 * @return {@code true} if library methods <i>(Methods not defined in the
	 *         input, but extended in the input)</i> should not be renamed.
	 */
	public boolean doLockLibraryMethod() {
		return lockLibraryMethods;
	}

	/**
	 * @return {@code true} if edits should be verified to alert users of
	 *         invalid changes.
	 */
	public boolean doVerify() {
		return verify;
	}

	@Override
	protected JsonValue convert(Class<?> type, Object value) {
		if (type.equals(ASMVersion.class)) {
			return Json.value(((ASMVersion) value).name());
		}
		return null;
	}

	@Override
	protected Object parse(Class<?> type, JsonValue value) {
		if (type.equals(ASMVersion.class)) {
			return ASMVersion.valueOf(value.asString());
		}
		return null;
	}

	/**
	 * Ensures when attaching to another java process that there is no conflict
	 * with the ClassWriter/Reader versions. This isn't a great fix but should
	 * work most of the time.
	 */
	private void ensureVersionComplaince() {
		// Only execute when invoked as an agent.
		if (!Agent.isActive()) {
			return;
		}
		// Compiler replaces field ref's with ldc constants of field values, so
		// references to fields isn't an issue. Just the value being used in the
		// ClassWriter is. Thus we ensure we don't use a future version in-case
		// recaf is attached to a process with an outdated ASM library.
		int value = version.value;
		try {
			Opcodes.class.getDeclaredField("ASM7");
			// Loaded version is ASM7, so we're fine using anything.
		} catch (NoSuchFieldException e1) {
			try {
				Opcodes.class.getDeclaredField("ASM6");
				// Loaded version is ASM6, can't go higher.
				if (value > Opcodes.ASM6) {
					version = ASMVersion.V6;
				}
			} catch (NoSuchFieldException e2) {
				try {
					Opcodes.class.getDeclaredField("ASM5");
					// Loaded version is ASM5, so we must use it.
					if (value > Opcodes.ASM5) {
						version = ASMVersion.V5;
					}
				} catch (NoSuchFieldException e3) {
					// Assume ASM4, we're not supporting anything further back.
					if (value > Opcodes.ASM4) {
						version = ASMVersion.V4;
					}
				}
			}
		} catch (Exception e) {
			Logging.warn(e);
		}
	}

	/**
	 * Static getter.
	 * 
	 * @return ConfASM instance.
	 */
	public static ConfASM instance() {
		return ConfASM.instance(ConfASM.class);
	}

	private enum ASMVersion {
		V4(Opcodes.ASM4), V5(Opcodes.ASM5), V6(Opcodes.ASM6), V7(Opcodes.ASM7);
		int value;

		ASMVersion(int value) {
			this.value = value;
		}
	}
}
