package me.coley.recaf.config.impl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import me.coley.recaf.Logging;
import me.coley.recaf.config.Conf;
import me.coley.recaf.config.Config;
import me.coley.recaf.util.Reflect;

/**
 * Options for CFR decompiler. Information for each option can be found in the
 * language files.
 * 
 * @author Matt
 */
public class ConfCFR extends Config {
	@Conf(category = "cfr", key = "aexagg")
	private boolean aexAgg = true;
	@Conf(category = "cfr", key = "allowcorrecting")
	private boolean allowCorrecting = true;
	@Conf(category = "cfr", key = "arrayiter")
	private boolean arrayIter = true;
	@Conf(category = "cfr", key = "collectioniter")
	private boolean collectionIter = true;
	@Conf(category = "cfr", key = "commentmonitors")
	private boolean commentMonitors = false;
	@Conf(category = "cfr", key = "comments")
	private boolean comments = false;
	@Conf(category = "cfr", key = "decodeenumswitch")
	private boolean decodeEnumSwitch = true;
	@Conf(category = "cfr", key = "decodefinally")
	private boolean decodeFinally = true;
	@Conf(category = "cfr", key = "decodelambdas")
	private boolean decodeLambdas = true;
	@Conf(category = "cfr", key = "decodestringswitch")
	private boolean decodeStringSwitch = true;
	@Conf(category = "cfr", key = "dumpclasspath")
	private boolean dumpClasspath = false;
	@Conf(category = "cfr", key = "eclipse")
	private boolean eclipse = true;
	@Conf(category = "cfr", key = "elidescala")
	private boolean elidescala = true;
	@Conf(category = "cfr", key = "tryresources")
	private boolean tryresources = true;
	@Conf(category = "cfr", key = "forcecondpropagate")
	private boolean forceCondPropagate = true;
	@Conf(category = "cfr", key = "forceexceptionprune")
	private boolean forceExceptionPrune = false;
	@Conf(category = "cfr", key = "forcereturningifs")
	private boolean forceReturningIfs = true;
	@Conf(category = "cfr", key = "forcetopsort")
	private boolean forceTopSort = true;
	@Conf(category = "cfr", key = "forcetopsortaggress")
	private boolean forceTopSortAggress = true;
	@Conf(category = "cfr", key = "forloopaggcapture")
	private boolean forLoopAggCapture = false;
	@Conf(category = "cfr", key = "hidebridgemethods")
	private boolean hideBridgeMethods = false;
	@Conf(category = "cfr", key = "hidelangimports")
	private boolean hideLangImports = true;
	@Conf(category = "cfr", key = "hidelongstrings")
	private boolean hideLongStrings = false;
	@Conf(category = "cfr", key = "hideutf")
	private boolean hideUTF = true;
	@Conf(category = "cfr", key = "innerclasses")
	private boolean innerClasses = true;
	@Conf(category = "cfr", key = "j14classobj")
	private boolean j14ClassObj = false;
	@Conf(category = "cfr", key = "labelledblocks")
	private boolean labelledBlocks = true;
	@Conf(category = "cfr", key = "lenient")
	private boolean lenient = true;
	@Conf(category = "cfr", key = "liftconstructorinit")
	private boolean liftConstructorInit = true;
	@Conf(category = "cfr", key = "override")
	private boolean override = true;
	@Conf(category = "cfr", key = "pullcodecase")
	private boolean pullCodeCase = false;
	@Conf(category = "cfr", key = "recover")
	private boolean recover = true;
	@Conf(category = "cfr", key = "recovertypeclash")
	private boolean recoverTypeClash = true;
	@Conf(category = "cfr", key = "recovertypehints")
	private boolean recoverTypeHints = true;
	@Conf(category = "cfr", key = "recpass")
	private int recpass = 0;
	@Conf(category = "cfr", key = "removebadgenerics")
	private boolean removeBadGenerics = true;
	@Conf(category = "cfr", key = "removeboilerplate")
	private boolean removeBoilerPlate = true;
	@Conf(category = "cfr", key = "removedeadmethods")
	private boolean removeDeadMethods = true;
	@Conf(category = "cfr", key = "removeinnerclasssynthetics")
	private boolean removeInnerClassSynthetics = true;
	@Conf(category = "cfr", key = "renamedupmembers")
	private boolean renameDupMembers = false;
	@Conf(category = "cfr", key = "renameenumidents")
	private boolean renameEnumIdents = false;
	@Conf(category = "cfr", key = "renameillegalidents")
	private boolean renameIllegalIdents = false;
	@Conf(category = "cfr", key = "renamesmallmembers")
	private boolean renameSmallMembers = false;
	@Conf(category = "cfr", key = "showinferrable")
	private boolean showInferrable = true;
	@Conf(category = "cfr", key = "showops")
	private int showOps = 0;
	@Conf(category = "cfr", key = "showversion")
	private boolean showVersion = false;
	@Conf(category = "cfr", key = "silent")
	private boolean silent = true;
	@Conf(category = "cfr", key = "staticinitreturn")
	private boolean staticInitReturn = false;
	@Conf(category = "cfr", key = "stringbuffer")
	private boolean stringBuffer = false;
	@Conf(category = "cfr", key = "stringbuilder")
	private boolean stringBuilder = true;
	@Conf(category = "cfr", key = "sugarasserts")
	private boolean sugarAsserts = true;
	@Conf(category = "cfr", key = "sugarboxing")
	private boolean sugarBoxing = true;
	@Conf(category = "cfr", key = "sugarenums")
	private boolean sugarEnums = true;
	@Conf(category = "cfr", key = "tidymonitors")
	private boolean tidyMonitors = true;
	@Conf(category = "cfr", key = "usenametable")
	private boolean useNameTable = true;

	public ConfCFR() {
		super("rc_cfr");
		load();
	}

	/**
	 * @return &lt;String, String(of boolean)&gt; map of the settings and their
	 *         current status.
	 */
	public Map<String, String> toStringMap() {
		Map<String, String> options = new HashMap<>();
		for (Field f : Reflect.fields(ConfCFR.class)) {
			try {
				String name = f.getName().toLowerCase();
				String text = String.valueOf(f.get(this));
				options.put(name, text);
			} catch (Exception e) {
				Logging.error(e);
			}
		}
		return options;
	}

	/**
	 * Static getter.
	 * 
	 * @return ConfASM instance.
	 */
	public static ConfCFR instance() {
		return ConfCFR.instance(ConfCFR.class);
	}
}
