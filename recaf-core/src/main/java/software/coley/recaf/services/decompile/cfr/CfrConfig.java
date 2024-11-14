package software.coley.recaf.services.decompile.cfr;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.util.ClassFileVersion;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.decompile.BaseDecompilerConfig;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.util.StringUtil;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Config for {@link CfrDecompiler}
 *
 * @author Matt Coley
 * @see OptionsImpl CFR options
 */
@ApplicationScoped
@SuppressWarnings("all") // ignore unused refs / typos
@ExcludeFromJacocoGeneratedReport(justification = "Config POJO")
public class CfrConfig extends BaseDecompilerConfig {
	private final ObservableObject<BooleanOption> stringbuffer = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> stringbuilder = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> stringconcat = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> decodeenumswitch = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> sugarenums = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> decodestringswitch = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> previewfeatures = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> sealed = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> switchexpression = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> recordtypes = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> instanceofpattern = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> arrayiter = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> collectioniter = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> tryresources = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> decodelambdas = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> innerclasses = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> forbidmethodscopedclasses = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> forbidanonymousclasses = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> skipbatchinnerclasses = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> hideutf = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> hidelongstrings = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> removeboilerplate = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> removeinnerclasssynthetics = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> relinkconst = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> relinkconststring = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> liftconstructorinit = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> removedeadmethods = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> removebadgenerics = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> sugarasserts = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> sugarboxing = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> sugarretrolambda = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> showversion = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> decodefinally = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> tidymonitors = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> commentmonitors = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> lenient = new ObservableObject<>(BooleanOption.TRUE);
	private final ObservableObject<BooleanOption> comments = new ObservableObject<>(BooleanOption.FALSE);
	private final ObservableObject<TrooleanOption> forcetopsort = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<ClassFileVersion> forceclassfilever = new ObservableObject<>(null);
	private final ObservableObject<TrooleanOption> forloopaggcapture = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> forcetopsortaggress = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> forcetopsortnopull = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> forcecondpropagate = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> reducecondscope = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> forcereturningifs = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> ignoreexceptionsalways = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> antiobf = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> obfcontrol = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> obfattr = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> constobf = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> hidebridgemethods = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> ignoreexceptions = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> forceexceptionprune = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> aexagg = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> aexagg2 = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> recovertypeclash = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> recovertypehints = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> recover = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> eclipse = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> override = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> showinferrable = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> version = new ObservableObject<>(BooleanOption.FALSE);
	private final ObservableObject<BooleanOption> labelledblocks = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> j14classobj = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> hidelangimports = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> renamedupmembers = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> renamesmallmembers = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> renameillegalidents = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> renameenumidents = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> removedeadconditionals = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> aggressivedoextension = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> aggressiveduff = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableInteger aggressivedocopy = new ObservableInteger(0);
	private final ObservableInteger aggressivesizethreshold = new ObservableInteger(13000);
	private final ObservableObject<BooleanOption> staticinitreturn = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> usenametable = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> pullcodecase = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<TrooleanOption> allowmalformedswitch = new ObservableObject<>(TrooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> elidescala = new ObservableObject<>(BooleanOption.DEFAULT);
	private final ObservableObject<BooleanOption> usesignatures = new ObservableObject<>(BooleanOption.DEFAULT);

	@Inject
	public CfrConfig() {
		super("decompiler-cfr" + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>("stringbuffer", BooleanOption.class, stringbuffer));
		addValue(new BasicConfigValue<>("stringbuilder", BooleanOption.class, stringbuilder));
		addValue(new BasicConfigValue<>("stringconcat", BooleanOption.class, stringconcat));
		addValue(new BasicConfigValue<>("decodeenumswitch", BooleanOption.class, decodeenumswitch));
		addValue(new BasicConfigValue<>("sugarenums", BooleanOption.class, sugarenums));
		addValue(new BasicConfigValue<>("decodestringswitch", BooleanOption.class, decodestringswitch));
		addValue(new BasicConfigValue<>("previewfeatures", BooleanOption.class, previewfeatures));
		addValue(new BasicConfigValue<>("sealed", BooleanOption.class, sealed));
		addValue(new BasicConfigValue<>("switchexpression", BooleanOption.class, switchexpression));
		addValue(new BasicConfigValue<>("recordtypes", BooleanOption.class, recordtypes));
		addValue(new BasicConfigValue<>("instanceofpattern", BooleanOption.class, instanceofpattern));
		addValue(new BasicConfigValue<>("arrayiter", BooleanOption.class, arrayiter));
		addValue(new BasicConfigValue<>("collectioniter", BooleanOption.class, collectioniter));
		addValue(new BasicConfigValue<>("tryresources", BooleanOption.class, tryresources));
		addValue(new BasicConfigValue<>("decodelambdas", BooleanOption.class, decodelambdas));
		addValue(new BasicConfigValue<>("innerclasses", BooleanOption.class, innerclasses));
		addValue(new BasicConfigValue<>("forbidmethodscopedclasses", BooleanOption.class, forbidmethodscopedclasses));
		addValue(new BasicConfigValue<>("forbidanonymousclasses", BooleanOption.class, forbidanonymousclasses));
		addValue(new BasicConfigValue<>("skipbatchinnerclasses", BooleanOption.class, skipbatchinnerclasses));
		addValue(new BasicConfigValue<>("hideutf", BooleanOption.class, hideutf));
		addValue(new BasicConfigValue<>("hidelongstrings", BooleanOption.class, hidelongstrings));
		addValue(new BasicConfigValue<>("removeboilerplate", BooleanOption.class, removeboilerplate));
		addValue(new BasicConfigValue<>("removeinnerclasssynthetics", BooleanOption.class, removeinnerclasssynthetics));
		addValue(new BasicConfigValue<>("relinkconst", BooleanOption.class, relinkconst));
		addValue(new BasicConfigValue<>("relinkconststring", BooleanOption.class, relinkconststring));
		addValue(new BasicConfigValue<>("liftconstructorinit", BooleanOption.class, liftconstructorinit));
		addValue(new BasicConfigValue<>("removedeadmethods", BooleanOption.class, removedeadmethods));
		addValue(new BasicConfigValue<>("removebadgenerics", BooleanOption.class, removebadgenerics));
		addValue(new BasicConfigValue<>("sugarasserts", BooleanOption.class, sugarasserts));
		addValue(new BasicConfigValue<>("sugarboxing", BooleanOption.class, sugarboxing));
		addValue(new BasicConfigValue<>("sugarretrolambda", BooleanOption.class, sugarretrolambda));
		addValue(new BasicConfigValue<>("showversion", BooleanOption.class, showversion));
		addValue(new BasicConfigValue<>("decodefinally", BooleanOption.class, decodefinally));
		addValue(new BasicConfigValue<>("tidymonitors", BooleanOption.class, tidymonitors));
		addValue(new BasicConfigValue<>("commentmonitors", BooleanOption.class, commentmonitors));
		addValue(new BasicConfigValue<>("lenient", BooleanOption.class, lenient));
		addValue(new BasicConfigValue<>("comments", BooleanOption.class, comments));
		addValue(new BasicConfigValue<>("forcetopsort", TrooleanOption.class, forcetopsort));
		addValue(new BasicConfigValue<>("forceclassfilever", ClassFileVersion.class, forceclassfilever));
		addValue(new BasicConfigValue<>("forloopaggcapture", TrooleanOption.class, forloopaggcapture));
		addValue(new BasicConfigValue<>("forcetopsortaggress", TrooleanOption.class, forcetopsortaggress));
		addValue(new BasicConfigValue<>("forcetopsortnopull", TrooleanOption.class, forcetopsortnopull));
		addValue(new BasicConfigValue<>("forcecondpropagate", TrooleanOption.class, forcecondpropagate));
		addValue(new BasicConfigValue<>("reducecondscope", TrooleanOption.class, reducecondscope));
		addValue(new BasicConfigValue<>("forcereturningifs", TrooleanOption.class, forcereturningifs));
		addValue(new BasicConfigValue<>("ignoreexceptionsalways", BooleanOption.class, ignoreexceptionsalways));
		addValue(new BasicConfigValue<>("antiobf", BooleanOption.class, antiobf));
		addValue(new BasicConfigValue<>("obfcontrol", BooleanOption.class, obfcontrol));
		addValue(new BasicConfigValue<>("obfattr", BooleanOption.class, obfattr));
		addValue(new BasicConfigValue<>("constobf", BooleanOption.class, constobf));
		addValue(new BasicConfigValue<>("hidebridgemethods", BooleanOption.class, hidebridgemethods));
		addValue(new BasicConfigValue<>("ignoreexceptions", BooleanOption.class, ignoreexceptions));
		addValue(new BasicConfigValue<>("forceexceptionprune", TrooleanOption.class, forceexceptionprune));
		addValue(new BasicConfigValue<>("aexagg", TrooleanOption.class, aexagg));
		addValue(new BasicConfigValue<>("aexagg2", TrooleanOption.class, aexagg2));
		addValue(new BasicConfigValue<>("recovertypeclash", TrooleanOption.class, recovertypeclash));
		addValue(new BasicConfigValue<>("recovertypehints", TrooleanOption.class, recovertypehints));
		addValue(new BasicConfigValue<>("recover", BooleanOption.class, recover));
		addValue(new BasicConfigValue<>("eclipse", BooleanOption.class, eclipse));
		addValue(new BasicConfigValue<>("override", BooleanOption.class, override));
		addValue(new BasicConfigValue<>("showinferrable", BooleanOption.class, showinferrable));
		addValue(new BasicConfigValue<>("version", BooleanOption.class, version));
		addValue(new BasicConfigValue<>("labelledblocks", BooleanOption.class, labelledblocks));
		addValue(new BasicConfigValue<>("j14classobj", BooleanOption.class, j14classobj));
		addValue(new BasicConfigValue<>("hidelangimports", BooleanOption.class, hidelangimports));
		addValue(new BasicConfigValue<>("renamedupmembers", BooleanOption.class, renamedupmembers));
		addValue(new BasicConfigValue<>("renamesmallmembers", BooleanOption.class, renamesmallmembers));
		addValue(new BasicConfigValue<>("renameillegalidents", BooleanOption.class, renameillegalidents));
		addValue(new BasicConfigValue<>("renameenumidents", BooleanOption.class, renameenumidents));
		addValue(new BasicConfigValue<>("removedeadconditionals", TrooleanOption.class, removedeadconditionals));
		addValue(new BasicConfigValue<>("aggressivedoextension", TrooleanOption.class, aggressivedoextension));
		addValue(new BasicConfigValue<>("aggressiveduff", TrooleanOption.class, aggressiveduff));
		addValue(new BasicConfigValue<>("aggressivedocopy", int.class, aggressivedocopy));
		addValue(new BasicConfigValue<>("aggressivesizethreshold", int.class, aggressivesizethreshold));
		addValue(new BasicConfigValue<>("staticinitreturn", BooleanOption.class, staticinitreturn));
		addValue(new BasicConfigValue<>("usenametable", BooleanOption.class, usenametable));
		addValue(new BasicConfigValue<>("pullcodecase", BooleanOption.class, pullcodecase));
		addValue(new BasicConfigValue<>("allowmalformedswitch", TrooleanOption.class, allowmalformedswitch));
		addValue(new BasicConfigValue<>("elidescala", BooleanOption.class, elidescala));
		addValue(new BasicConfigValue<>("usesignatures", BooleanOption.class, usesignatures));
		registerConfigValuesHashUpdates();
	}

	/**
	 * Fetch help description from configuration parameter.
	 *
	 * @param name
	 * 		Parameter/option name.
	 *
	 * @return Help description string, may be {@code null}.
	 */
	@Nullable
	@SuppressWarnings("rawtypes")
	public static String getOptHelp(String name) {
		for (Field declaredField : OptionsImpl.class.getDeclaredFields()) {
			if (PermittedOptionProvider.ArgumentParam.class.isAssignableFrom(declaredField.getType())) {
				PermittedOptionProvider.ArgumentParam param = ReflectUtil.quietGet(null, declaredField);
				if (param != null && param.getName().equals(name))
					return getOptHelp(param);
			}
		}
		return null;
	}

	/**
	 * Fetch help description from configuration parameter.
	 *
	 * @param param
	 * 		Parameter.
	 *
	 * @return Help description string.
	 */
	@Nonnull
	private static String getOptHelp(PermittedOptionProvider.ArgumentParam<?, ?> param) {
		try {
			Field fn = PermittedOptionProvider.ArgumentParam.class.getDeclaredField("help");
			fn.setAccessible(true);
			String value = (String) fn.get(param);
			if (StringUtil.isNullOrEmpty(value))
				value = "";
			return value;
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Failed to fetch description from Cfr parameter, did" +
					" the backend change?");
		}
	}

	/**
	 * @return CFR compatible string map for {@link CfrDriver.Builder#withOptions(Map)}.
	 */
	@Nonnull
	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<>();
		getValues().forEach((name, config) -> {
			Class<?> type = config.getType();
			if (type == BooleanOption.class) {
				// Boolean option, values should be 'true' or 'false'
				BooleanOption value = (BooleanOption) config.getValue();
				if (value != BooleanOption.DEFAULT) {
					String booleanName = value.name().toLowerCase();
					map.put(name, booleanName);
				}
			} else if (type == TrooleanOption.class) {
				// Troolean option, values should be 'true' or 'false' for respective cases.
				// The 'neither' option is selected when null is passed.
				TrooleanOption value = (TrooleanOption) config.getValue();
				if (value == TrooleanOption.NEITHER) {
					map.put(name, null);
				} else if (value != TrooleanOption.DEFAULT) {
					String booleanName = value.name().toLowerCase();
					map.put(name, booleanName);
				}
			} else if (type == Integer.class) {
				// Integer option, value is just int
				Integer value = (Integer) config.getValue();
				if (value != null) {
					map.put(name, value.toString());
				}
			} else if (type == ClassFileVersion.class) {
				// Class version option, values represented as 'MAJOR.MINOR'.
				// Java 8 would be '52.0'
				ClassFileVersion value = (ClassFileVersion) config.getValue();
				if (value != null) {
					// The 'toString()' handles the format for us.
					map.put(name, value.toString());
				}
			}
		});
		return map;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getStringbuffer() {
		return stringbuffer;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getStringbuilder() {
		return stringbuilder;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getStringconcat() {
		return stringconcat;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getDecodeenumswitch() {
		return decodeenumswitch;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getSugarenums() {
		return sugarenums;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getDecodestringswitch() {
		return decodestringswitch;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getPreviewfeatures() {
		return previewfeatures;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getSealed() {
		return sealed;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getSwitchexpression() {
		return switchexpression;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRecordtypes() {
		return recordtypes;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getInstanceofpattern() {
		return instanceofpattern;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getArrayiter() {
		return arrayiter;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getCollectioniter() {
		return collectioniter;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getTryresources() {
		return tryresources;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getDecodelambdas() {
		return decodelambdas;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getInnerclasses() {
		return innerclasses;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getForbidmethodscopedclasses() {
		return forbidmethodscopedclasses;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getForbidanonymousclasses() {
		return forbidanonymousclasses;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getSkipbatchinnerclasses() {
		return skipbatchinnerclasses;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getHideutf() {
		return hideutf;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getHidelongstrings() {
		return hidelongstrings;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRemoveboilerplate() {
		return removeboilerplate;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRemoveinnerclasssynthetics() {
		return removeinnerclasssynthetics;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRelinkconst() {
		return relinkconst;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRelinkconststring() {
		return relinkconststring;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getLiftconstructorinit() {
		return liftconstructorinit;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRemovedeadmethods() {
		return removedeadmethods;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRemovebadgenerics() {
		return removebadgenerics;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getSugarasserts() {
		return sugarasserts;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getSugarboxing() {
		return sugarboxing;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getSugarretrolambda() {
		return sugarretrolambda;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getShowversion() {
		return showversion;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getDecodefinally() {
		return decodefinally;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getTidymonitors() {
		return tidymonitors;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getCommentmonitors() {
		return commentmonitors;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getLenient() {
		return lenient;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getComments() {
		return comments;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getForcetopsort() {
		return forcetopsort;
	}

	@Nonnull
	public ObservableObject<ClassFileVersion> getForceclassfilever() {
		return forceclassfilever;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getForloopaggcapture() {
		return forloopaggcapture;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getForcetopsortaggress() {
		return forcetopsortaggress;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getForcetopsortnopull() {
		return forcetopsortnopull;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getForcecondpropagate() {
		return forcecondpropagate;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getReducecondscope() {
		return reducecondscope;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getForcereturningifs() {
		return forcereturningifs;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getIgnoreexceptionsalways() {
		return ignoreexceptionsalways;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getAntiobf() {
		return antiobf;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getObfcontrol() {
		return obfcontrol;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getObfattr() {
		return obfattr;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getConstobf() {
		return constobf;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getHidebridgemethods() {
		return hidebridgemethods;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getIgnoreexceptions() {
		return ignoreexceptions;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getForceexceptionprune() {
		return forceexceptionprune;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getAexagg() {
		return aexagg;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getAexagg2() {
		return aexagg2;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getRecovertypeclash() {
		return recovertypeclash;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getRecovertypehints() {
		return recovertypehints;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRecover() {
		return recover;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getEclipse() {
		return eclipse;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getOverride() {
		return override;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getShowinferrable() {
		return showinferrable;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getVersion() {
		return version;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getLabelledblocks() {
		return labelledblocks;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getJ14classobj() {
		return j14classobj;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getHidelangimports() {
		return hidelangimports;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRenamedupmembers() {
		return renamedupmembers;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRenamesmallmembers() {
		return renamesmallmembers;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRenameillegalidents() {
		return renameillegalidents;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getRenameenumidents() {
		return renameenumidents;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getRemovedeadconditionals() {
		return removedeadconditionals;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getAggressivedoextension() {
		return aggressivedoextension;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getAggressiveduff() {
		return aggressiveduff;
	}

	@Nonnull
	public ObservableInteger getAggressivedocopy() {
		return aggressivedocopy;
	}

	@Nonnull
	public ObservableInteger getAggressivesizethreshold() {
		return aggressivesizethreshold;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getStaticinitreturn() {
		return staticinitreturn;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getUsenametable() {
		return usenametable;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getPullcodecase() {
		return pullcodecase;
	}

	@Nonnull
	public ObservableObject<TrooleanOption> getAllowmalformedswitch() {
		return allowmalformedswitch;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getElidescala() {
		return elidescala;
	}

	@Nonnull
	public ObservableObject<BooleanOption> getUsesignatures() {
		return usesignatures;
	}

	/**
	 * Wrapper for CFR boolean values.
	 */
	public enum BooleanOption {
		DEFAULT,
		TRUE,
		FALSE
	}

	/**
	 * Wrapper for CFR troolean values.
	 */
	public enum TrooleanOption {
		DEFAULT,
		NEITHER,
		TRUE,
		FALSE
	}
}
