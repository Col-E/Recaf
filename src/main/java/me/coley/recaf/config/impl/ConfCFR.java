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
	public boolean aexAgg = false;
	@Conf(category = "cfr", key = "aggressivesizethreshold")
	public int aggressivesizethreshold = 15000;
	@Conf(category = "cfr", key = "allowcorrecting")
	public boolean allowCorrecting = true;
	@Conf(category = "cfr", key = "arrayiter")
	public boolean arrayIter = true;
	@Conf(category = "cfr", key = "collectioniter")
	public boolean collectionIter = true;
	@Conf(category = "cfr", key = "commentmonitors")
	public boolean commentMonitors = false;
	@Conf(category = "cfr", key = "comments")
	public boolean comments = true;
	@Conf(category = "cfr", key = "decodeenumswitch")
	public boolean decodeEnumSwitch = true;
	@Conf(category = "cfr", key = "decodefinally")
	public boolean decodeFinally = true;
	@Conf(category = "cfr", key = "decodelambdas")
	public boolean decodeLambdas = true;
	@Conf(category = "cfr", key = "decodestringswitch")
	public boolean decodeStringSwitch = true;
	@Conf(category = "cfr", key = "dumpclasspath")
	public boolean dumpClasspath = false;
	@Conf(category = "cfr", key = "eclipse")
	public boolean eclipse = true;
	@Conf(category = "cfr", key = "elidescala")
	public boolean elidescala = false;
	@Conf(category = "cfr", key = "forcecondpropagate")
	public boolean forceCondPropagate = false;
	@Conf(category = "cfr", key = "forceexceptionprune")
	public boolean forceExceptionPrune = false;
	@Conf(category = "cfr", key = "forcereturningifs")
	public boolean forceReturningIfs = false;
	@Conf(category = "cfr", key = "forcetopsort")
	public boolean forceTopSort = false;
	@Conf(category = "cfr", key = "forcetopsortaggress")
	public boolean forceTopSortAggress = false;
	@Conf(category = "cfr", key = "forloopaggcapture")
	public boolean forLoopAggCapture = false;
	@Conf(category = "cfr", key = "hidebridgemethods")
	public boolean hideBridgeMethods = true;
	@Conf(category = "cfr", key = "hidelangimports")
	public boolean hideLangImports = true;
	@Conf(category = "cfr", key = "hidelongstrings")
	public boolean hideLongStrings = false;
	@Conf(category = "cfr", key = "hideutf")
	public boolean hideUTF = true;
	@Conf(category = "cfr", key = "innerclasses")
	public boolean innerClasses = true;
	@Conf(category = "cfr", key = "j14classobj")
	public boolean j14ClassObj = false;
	@Conf(category = "cfr", key = "labelledblocks")
	public boolean labelledBlocks = true;
	@Conf(category = "cfr", key = "lenient")
	public boolean lenient = true;
	@Conf(category = "cfr", key = "liftconstructorinit")
	public boolean liftConstructorInit = true;
	@Conf(category = "cfr", key = "override")
	public boolean override = true;
	@Conf(category = "cfr", key = "pullcodecase")
	public boolean pullCodeCase = false;
	@Conf(category = "cfr", key = "recover")
	public boolean recover = true;
	@Conf(category = "cfr", key = "recovertypeclash")
	public boolean recoverTypeClash = false;
	@Conf(category = "cfr", key = "recovertypehints")
	public boolean recoverTypeHints = false;
	@Conf(category = "cfr", key = "recpass")
	public int recpass = 0;
	@Conf(category = "cfr", key = "relinkconststring")
	public boolean relinkConstString = true;
	@Conf(category = "cfr", key = "removebadgenerics")
	public boolean removeBadGenerics = true;
	@Conf(category = "cfr", key = "removeboilerplate")
	public boolean removeBoilerPlate = true;
	@Conf(category = "cfr", key = "removedeadmethods")
	public boolean removeDeadMethods = true;
	@Conf(category = "cfr", key = "removeinnerclasssynthetics")
	public boolean removeInnerClassSynthetics = true;
	@Conf(category = "cfr", key = "renamedupmembers")
	public boolean renameDupMembers = false;
	@Conf(category = "cfr", key = "renameenumidents")
	public boolean renameEnumIdents = false;
	@Conf(category = "cfr", key = "renameillegalidents")
	public boolean renameIllegalIdents = false;
	@Conf(category = "cfr", key = "renamesmallmembers")
	public int renameSmallMembers = 0;
	@Conf(category = "cfr", key = "showinferrable")
	public boolean showInferrable = false;
	@Conf(category = "cfr", key = "showops")
	public int showOps = 0;
	@Conf(category = "cfr", key = "showversion")
	public boolean showVersion = false;
	@Conf(category = "cfr", key = "silent")
	public boolean silent = true;
	@Conf(category = "cfr", key = "staticinitreturn")
	public boolean staticInitReturn = true;
	@Conf(category = "cfr", key = "stringbuffer")
	public boolean stringBuffer = false;
	@Conf(category = "cfr", key = "stringbuilder")
	public boolean stringBuilder = true;
	@Conf(category = "cfr", key = "stringconcat")
	public boolean stringConcat = true;
	@Conf(category = "cfr", key = "sugarasserts")
	public boolean sugarAsserts = true;
	@Conf(category = "cfr", key = "sugarboxing")
	public boolean sugarBoxing = true;
	@Conf(category = "cfr", key = "sugarenums")
	public boolean sugarEnums = true;
	@Conf(category = "cfr", key = "switchexpression")
	public boolean switchExpression = true;
	@Conf(category = "cfr", key = "tidymonitors")
	public boolean tidyMonitors = true;
	@Conf(category = "cfr", key = "tryresources")
	public boolean tryResources = true;
	@Conf(category = "cfr", key = "usenametable")
	public boolean useNameTable = true;

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


	public static void main(String[] args) {

	}

	// Used to automatically check if there are new CFR args in OptionsImpl
	// @formatter:off
/*
	private static void checkMissingArgs() {
		try {
			for (Field f : Reflect.fields(OptionsImpl.class)) {
				if (AccessFlag.isPublic(f.getModifiers())) {
					@SuppressWarnings("rawtypes")
					ArgumentParam arg = (ArgumentParam) f.get(null);
					String name = arg.getName();
					boolean has = false;
					for (Field ff : Reflect.fields(ConfCFR.class)) {
						if (ff.getName().toLowerCase().equals(name)) {
							has = true;
						}
					}
					if (!has) {
						System.out.println("Missing CFR option: " + name);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
*/
}
