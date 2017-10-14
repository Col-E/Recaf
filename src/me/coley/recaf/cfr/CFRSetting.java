package me.coley.recaf.cfr;

import java.util.HashMap;
import java.util.Map;

/**
 * Credit: <a href=
 * "https://github.com/helios-decompiler/Helios/blob/36cfe93bb42a4aa09be86bbeab99434b7c51053d/src/main/java/com/heliosdecompiler/helios/transformers/decompilers/CFRDecompiler.java#L80">
 * Samczung</a>
 *
 */
public enum CFRSetting {
	//@formatter:off
	DECODE_ENUM_SWITCH("decodeenumswitch", "Decode Enum Switch", true),
	SUGAR_ENUMS("sugarenums", "SugarEnums", true),
	DECODE_STRING_SWITCH("decodestringswitch", "Decode String Switch", true),
	ARRAYITER("arrayiter", "Arrayiter", true),
	COLLECTIONITER("collectioniter", "Collectioniter", true),
	INNER_CLASSES("innerclasses", "Inner Classes", false),
	REMOVE_BOILER_PLATE("removeboilerplate", "Remove Boiler Plate", true),
	REMOVE_INNER_CLASS_SYNTHETICS("removeinnerclasssynthetics", "Remove Inner Class Synthetics", true),
	DECODE_LAMBDAS("decodelambdas", "Decode Lambdas", true),
	HIDE_BRIDGE_METHODS("hidebridgemethods", "Hide Bridge Methods", false),
	LIFT_CONSTRUCTOR_INIT("liftconstructorinit", "Lift Constructor Init", true),
	REMOVE_DEAD_METHODS("removedeadmethods", "Remove Dead Methods", false),
	REMOVE_BAD_GENERICS("removebadgenerics", "Remove Bad Generics", true),
	SUGAR_ASSERTS("sugarasserts", "Sugar Asserts", true),
	SUGAR_BOXING("sugarboxing", "Sugar Boxing", true),
	SHOW_VERSION("showversion", "Show Version"),
	DECODE_FINALLY("decodefinally", "Decode Finally", true),
	TIDY_MONITORS("tidymonitors", "Tidy Monitors", true),
	LENIENT("lenient", "Lenient", true),
	DUMP_CLASS_PATH("dumpclasspath", "Dump Classpath"),
	COMMENTS("comments", "Comments"),
	FORCE_TOP_SORT("forcetopsort", "Force Top Sort", true),
	FORCE_TOP_SORT_AGGRESSIVE("forcetopsortaggress", "Force Top Sort Aggressive", true),
	STRINGBUFFER("stringbuffer", "StringBuffer"),
	STRINGBUILDER("stringbuilder", "StringBuilder", true),
	SILENT("silent", "Silent", true),
	RECOVER("recover", "Recover", true),
	ECLIPSE("eclipse", "Eclipse", true),
	OVERRIDE("override", "Override", true),
	SHOW_INFERRABLE("showinferrable", "Show Inferrable", false),
	FORCE_AGGRESSIVE_EXCEPTION_AGG("aexagg", "Force Aggressive Exception Aggregation", true),
	FORCE_COND_PROPAGATE("forcecondpropagate", "Force Conditional Propogation", true),
	HIDE_UTF("hideutf", "Hide UTF", true),
	HIDE_LONG_STRINGS("hidelongstrings", "Hide Long Strings"),
	COMMENT_MONITORS("commentmonitors", "Comment Monitors"),
	ALLOW_CORRECTING("allowcorrecting", "Allow Correcting", true),
	LABELLED_BLOCKS("labelledblocks", "Labelled Blocks", true),
	J14_CLASS_OBJ("j14classobj", "Java 1.4 Class Objects"),
	HIDE_LANG_IMPORTS("hidelangimports", "Hide Lang Imports", true),
	RECOVER_TYPE_CLASH("recovertypeclash", "Recover Type Clash", true),
	RECOVER_TYPE_HINTS("recovertypehints", "Recover Type Hints", true),
	FORCE_RETURNING_IFS("forcereturningifs", "Force Returning Ifs", true),
	FOR_LOOP_AGG_CAPTURE("forloopaggcapture", "For Loop Aggressive Capture", true),
	RENAME_ILLEGAL_IDENTIFIERS("renameillegalidents", "Rename illegal identifiers", false),
	RENAME_DUPE_MEMBERS("renamedupmembers", "Rename duplicated member names", false);
	//@formatter:on

	private final String name;
	private final String param;
	private boolean on;

	CFRSetting(String param, String name) {
		this(param, name, false);
	}

	CFRSetting(String param, String name, boolean on) {
		this.name = name;
		this.param = param;
		this.on = on;
	}

	public String getParam() {
		return param;
	}

	public String getText() {
		return name;
	}

	public boolean isEnabled() {
		return on;
	}

	public void setEnabled(boolean enabled) {
		this.on = enabled;
	}

	/** Obtain repreesntation of the CFR settings as a string map.
	 *
	 * @return &lt;String, String(of boolean)&gt; map of the settings and their
	 * current status.
	 */
	public static Map<String, String> toStringMap() {
		Map<String, String> options = new HashMap<>();
		for (CFRSetting setting : CFRSetting.values()) {
			options.put(setting.getParam(), String.valueOf(setting.isEnabled()));
		}
		return options;
	}
}
