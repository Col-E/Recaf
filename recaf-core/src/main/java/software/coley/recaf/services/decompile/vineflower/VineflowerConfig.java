package software.coley.recaf.services.decompile.vineflower;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.slf4j.event.Level;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.services.decompile.BaseDecompilerConfig;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Config for {@link VineflowerDecompiler}
 *
 * @author therathatter
 * @see IFernflowerPreferences Source of value definitions.
 */
@ApplicationScoped
@SuppressWarnings("all") // ignore unused refs / typos
@ExcludeFromJacocoGeneratedReport(justification = "Config POJO")
public class VineflowerConfig extends BaseDecompilerConfig {
	private final ObservableObject<Level> loggingLevel = new ObservableObject<>(Level.WARN);
	private final ObservableBoolean removeBridge = new ObservableBoolean(true);
	private final ObservableBoolean removeSynthetic = new ObservableBoolean(true);
	private final ObservableBoolean decompileInner = new ObservableBoolean(true);
	private final ObservableBoolean decompileClass_1_4 = new ObservableBoolean(true);
	private final ObservableBoolean decompileAssertions = new ObservableBoolean(true);
	private final ObservableBoolean hideEmptySuper = new ObservableBoolean(true);
	private final ObservableBoolean hideDefaultConstructor = new ObservableBoolean(true);
	private final ObservableBoolean decompileGenericSignatures = new ObservableBoolean(true);
	private final ObservableBoolean noExceptionsReturn = new ObservableBoolean(true);
	private final ObservableBoolean ensureSynchronizedMonitor = new ObservableBoolean(true);
	private final ObservableBoolean decompileEnum = new ObservableBoolean(true);
	private final ObservableBoolean removeGetClassNew = new ObservableBoolean(true);
	private final ObservableBoolean literalsAsIs = new ObservableBoolean(false);
	private final ObservableBoolean booleanTrueOne = new ObservableBoolean(true);
	private final ObservableBoolean asciiStringCharacters = new ObservableBoolean(false);
	private final ObservableBoolean syntheticNotSet = new ObservableBoolean(false);
	private final ObservableBoolean undefinedParamTypeObject = new ObservableBoolean(true);
	private final ObservableBoolean useDebugVarNames = new ObservableBoolean(true);
	private final ObservableBoolean useMethodParameters = new ObservableBoolean(true);
	private final ObservableBoolean removeEmptyRanges = new ObservableBoolean(true);
	private final ObservableBoolean finallyDeinline = new ObservableBoolean(true);
	private final ObservableBoolean ideaNotNullAnnotation = new ObservableBoolean(true);
	private final ObservableBoolean lambdaToAnonymousClass = new ObservableBoolean(false);
	private final ObservableBoolean bytecodeSourceMapping = new ObservableBoolean(false);
	private final ObservableBoolean dumpCodeLines = new ObservableBoolean(false);
	private final ObservableBoolean ignoreInvalidBytecode = new ObservableBoolean(false);
	private final ObservableBoolean verifyAnonymousClasses = new ObservableBoolean(false);
	private final ObservableBoolean ternaryConstantSimplification = new ObservableBoolean(false);
	private final ObservableBoolean overrideAnnotation = new ObservableBoolean(true);
	private final ObservableBoolean patternMatching = new ObservableBoolean(true);
	private final ObservableBoolean tryLoopFix = new ObservableBoolean(true);
	private final ObservableBoolean ternaryConditions = new ObservableBoolean(true);
	private final ObservableBoolean switchExpressions = new ObservableBoolean(true);
	private final ObservableBoolean showHiddenStatements = new ObservableBoolean(false);
	private final ObservableBoolean simplifyStackSecondPass = new ObservableBoolean(true);
	private final ObservableBoolean verifyVariableMerges = new ObservableBoolean(false);
	private final ObservableBoolean decompilePreview = new ObservableBoolean(true);
	private final ObservableBoolean explicitGenericArguments = new ObservableBoolean(false);
	private final ObservableBoolean inlineSimpleLambdas = new ObservableBoolean(true);
	private final ObservableBoolean useJadVarNaming = new ObservableBoolean(false);
	private final ObservableBoolean useJadParameterNaming = new ObservableBoolean(false);
	private final ObservableBoolean skipExtraFiles = new ObservableBoolean(false);
	private final ObservableBoolean warnInconsistentInnerClasses = new ObservableBoolean(true);
	private final ObservableBoolean dumpBytecodeOnError = new ObservableBoolean(true);
	private final ObservableBoolean dumpExceptionOnError = new ObservableBoolean(true);
	private final ObservableBoolean decompilerComments = new ObservableBoolean(false);
	private final ObservableBoolean sourceFileComments = new ObservableBoolean(false);
	private final ObservableBoolean decompileComplexCondys = new ObservableBoolean(false);
	private final ObservableBoolean forceJsrInline = new ObservableBoolean(false);

	public static void main(String[] args) {
		for (Field field : IFernflowerPreferences.class.getDeclaredFields()) {
			try {
				IFernflowerPreferences.Name name = field.getDeclaredAnnotation(IFernflowerPreferences.Name.class);
				String key = (String) field.get(null);
				System.out.println("service.decompile.impl.decompiler-vineflower-config." + key + "=" + name.value());
			} catch (Throwable t) {}
		}
	}

	@Inject
	public VineflowerConfig() {
		super("decompiler-vineflower" + CONFIG_SUFFIX);

		addValue(new BasicConfigValue<>("logging-level", Level.class, loggingLevel));
		addValue(new BasicConfigValue<>("remove-bridge", boolean.class, removeBridge));
		addValue(new BasicConfigValue<>("remove-synthetic", boolean.class, removeSynthetic));
		addValue(new BasicConfigValue<>("decompile-inner", boolean.class, decompileInner));
		addValue(new BasicConfigValue<>("decompile-java4", boolean.class, decompileClass_1_4));
		addValue(new BasicConfigValue<>("decompile-assert", boolean.class, decompileAssertions));
		addValue(new BasicConfigValue<>("hide-empty-super", boolean.class, hideEmptySuper));
		addValue(new BasicConfigValue<>("hide-default-constructor", boolean.class, hideDefaultConstructor));
		addValue(new BasicConfigValue<>("decompile-generics", boolean.class, decompileGenericSignatures));
		addValue(new BasicConfigValue<>("incorporate-returns", boolean.class, noExceptionsReturn));
		addValue(new BasicConfigValue<>("ensure-synchronized-monitors", boolean.class, ensureSynchronizedMonitor));
		addValue(new BasicConfigValue<>("decompile-enums", boolean.class, decompileEnum));
		addValue(new BasicConfigValue<>("decompile-preview", boolean.class, decompilePreview));
		addValue(new BasicConfigValue<>("remove-getclass", boolean.class, removeGetClassNew));
		addValue(new BasicConfigValue<>("keep-literals", boolean.class, literalsAsIs));
		addValue(new BasicConfigValue<>("boolean-as-int", boolean.class, booleanTrueOne));
		addValue(new BasicConfigValue<>("ascii-strings", boolean.class, asciiStringCharacters));
		addValue(new BasicConfigValue<>("synthetic-not-set", boolean.class, syntheticNotSet));
		addValue(new BasicConfigValue<>("undefined-as-object", boolean.class, undefinedParamTypeObject));
		addValue(new BasicConfigValue<>("use-lvt-names", boolean.class, useDebugVarNames));
		addValue(new BasicConfigValue<>("use-method-parameters", boolean.class, useMethodParameters));
		addValue(new BasicConfigValue<>("remove-empty-try-catch", boolean.class, removeEmptyRanges));
		addValue(new BasicConfigValue<>("decompile-finally", boolean.class, finallyDeinline));
		addValue(new BasicConfigValue<>("lambda-to-anonymous-class", boolean.class, lambdaToAnonymousClass));
		addValue(new BasicConfigValue<>("bytecode-source-mapping", boolean.class, bytecodeSourceMapping));
		addValue(new BasicConfigValue<>("__dump_original_lines__", boolean.class, dumpCodeLines));
		addValue(new BasicConfigValue<>("ignore-invalid-bytecode", boolean.class, ignoreInvalidBytecode));
		addValue(new BasicConfigValue<>("verify-anonymous-classes", boolean.class, verifyAnonymousClasses));
		addValue(new BasicConfigValue<>("ternary-constant-simplification", boolean.class, ternaryConstantSimplification));
		addValue(new BasicConfigValue<>("pattern-matching", boolean.class, patternMatching));
		addValue(new BasicConfigValue<>("try-loop-fix", boolean.class, tryLoopFix));
		addValue(new BasicConfigValue<>("ternary-in-if", boolean.class, ternaryConditions));
		addValue(new BasicConfigValue<>("decompile-switch-expressions", boolean.class, switchExpressions));
		addValue(new BasicConfigValue<>("show-hidden-statements", boolean.class, showHiddenStatements));
		addValue(new BasicConfigValue<>("override-annotation", boolean.class, overrideAnnotation));
		addValue(new BasicConfigValue<>("simplify-stack", boolean.class, simplifyStackSecondPass));
		addValue(new BasicConfigValue<>("verify-merges", boolean.class, verifyVariableMerges));
		addValue(new BasicConfigValue<>("explicit-generics", boolean.class, explicitGenericArguments));
		addValue(new BasicConfigValue<>("inline-simple-lambdas", boolean.class, inlineSimpleLambdas));
		addValue(new BasicConfigValue<>("skip-extra-files", boolean.class, skipExtraFiles));
		addValue(new BasicConfigValue<>("warn-inconsistent-inner-attributes", boolean.class, warnInconsistentInnerClasses));
		addValue(new BasicConfigValue<>("dump-bytecode-on-error", boolean.class, dumpBytecodeOnError));
		addValue(new BasicConfigValue<>("dump-exception-on-error", boolean.class, dumpExceptionOnError));
		addValue(new BasicConfigValue<>("decompiler-comments", boolean.class, decompilerComments));
		addValue(new BasicConfigValue<>("sourcefile-comments", boolean.class, sourceFileComments));
		addValue(new BasicConfigValue<>("decompile-complex-constant-dynamic", boolean.class, decompileComplexCondys));
		addValue(new BasicConfigValue<>("force-jsr-inline", boolean.class, forceJsrInline));

		registerConfigValuesHashUpdates();
	}

	/**
	 * @return Map of values to pass to the {@link Fernflower} instance.
	 */
	@Nonnull
	protected Map<String, Object> getFernflowerProperties() {
		Map<String, Object> properties = new HashMap<>(IFernflowerPreferences.DEFAULTS);
		getValues().forEach((key, value) -> {
			if (value.getValue() instanceof Boolean bool)
				properties.put(key, bool ? "1" : "0");
		});
		return properties;
	}

	/**
	 * @return Level to use for {@link VineflowerLogger}.
	 */
	@Nonnull
	public ObservableObject<Level> getLoggingLevel() {
		return loggingLevel;
	}

	@Nonnull
	public ObservableBoolean getRemoveBridge() {
		return removeBridge;
	}

	@Nonnull
	public ObservableBoolean getRemoveSynthetic() {
		return removeSynthetic;
	}

	@Nonnull
	public ObservableBoolean getDecompileInner() {
		return decompileInner;
	}

	@Nonnull
	public ObservableBoolean getDecompileClass_1_4() {
		return decompileClass_1_4;
	}

	@Nonnull
	public ObservableBoolean getDecompileAssertions() {
		return decompileAssertions;
	}

	@Nonnull
	public ObservableBoolean getHideEmptySuper() {
		return hideEmptySuper;
	}

	@Nonnull
	public ObservableBoolean getHideDefaultConstructor() {
		return hideDefaultConstructor;
	}

	@Nonnull
	public ObservableBoolean getDecompileGenericSignatures() {
		return decompileGenericSignatures;
	}

	@Nonnull
	public ObservableBoolean getNoExceptionsReturn() {
		return noExceptionsReturn;
	}

	@Nonnull
	public ObservableBoolean getEnsureSynchronizedMonitor() {
		return ensureSynchronizedMonitor;
	}

	@Nonnull
	public ObservableBoolean getDecompileEnum() {
		return decompileEnum;
	}

	@Nonnull
	public ObservableBoolean getRemoveGetClassNew() {
		return removeGetClassNew;
	}

	@Nonnull
	public ObservableBoolean getLiteralsAsIs() {
		return literalsAsIs;
	}

	@Nonnull
	public ObservableBoolean getBooleanTrueOne() {
		return booleanTrueOne;
	}

	@Nonnull
	public ObservableBoolean getAsciiStringCharacters() {
		return asciiStringCharacters;
	}

	@Nonnull
	public ObservableBoolean getSyntheticNotSet() {
		return syntheticNotSet;
	}

	@Nonnull
	public ObservableBoolean getUndefinedParamTypeObject() {
		return undefinedParamTypeObject;
	}

	@Nonnull
	public ObservableBoolean getUseDebugVarNames() {
		return useDebugVarNames;
	}

	@Nonnull
	public ObservableBoolean getUseMethodParameters() {
		return useMethodParameters;
	}

	@Nonnull
	public ObservableBoolean getRemoveEmptyRanges() {
		return removeEmptyRanges;
	}

	@Nonnull
	public ObservableBoolean getFinallyDeinline() {
		return finallyDeinline;
	}

	@Nonnull
	public ObservableBoolean getIdeaNotNullAnnotation() {
		return ideaNotNullAnnotation;
	}

	@Nonnull
	public ObservableBoolean getLambdaToAnonymousClass() {
		return lambdaToAnonymousClass;
	}

	@Nonnull
	public ObservableBoolean getBytecodeSourceMapping() {
		return bytecodeSourceMapping;
	}

	@Nonnull
	public ObservableBoolean getDumpCodeLines() {
		return dumpCodeLines;
	}

	@Nonnull
	public ObservableBoolean getIgnoreInvalidBytecode() {
		return ignoreInvalidBytecode;
	}

	@Nonnull
	public ObservableBoolean getVerifyAnonymousClasses() {
		return verifyAnonymousClasses;
	}

	@Nonnull
	public ObservableBoolean getTernaryConstantSimplification() {
		return ternaryConstantSimplification;
	}

	@Nonnull
	public ObservableBoolean getOverrideAnnotation() {
		return overrideAnnotation;
	}

	@Nonnull
	public ObservableBoolean getPatternMatching() {
		return patternMatching;
	}

	@Nonnull
	public ObservableBoolean getTryLoopFix() {
		return tryLoopFix;
	}

	@Nonnull
	public ObservableBoolean getTernaryConditions() {
		return ternaryConditions;
	}

	@Nonnull
	public ObservableBoolean getSwitchExpressions() {
		return switchExpressions;
	}

	@Nonnull
	public ObservableBoolean getShowHiddenStatements() {
		return showHiddenStatements;
	}

	@Nonnull
	public ObservableBoolean getSimplifyStackSecondPass() {
		return simplifyStackSecondPass;
	}

	@Nonnull
	public ObservableBoolean getVerifyVariableMerges() {
		return verifyVariableMerges;
	}

	@Nonnull
	public ObservableBoolean getDecompilePreview() {
		return decompilePreview;
	}

	@Nonnull
	public ObservableBoolean getExplicitGenericArguments() {
		return explicitGenericArguments;
	}

	@Nonnull
	public ObservableBoolean getInlineSimpleLambdas() {
		return inlineSimpleLambdas;
	}

	@Nonnull
	public ObservableBoolean getUseJadVarNaming() {
		return useJadVarNaming;
	}

	@Nonnull
	public ObservableBoolean getUseJadParameterNaming() {
		return useJadParameterNaming;
	}

	@Nonnull
	public ObservableBoolean getSkipExtraFiles() {
		return skipExtraFiles;
	}

	@Nonnull
	public ObservableBoolean getWarnInconsistentInnerClasses() {
		return warnInconsistentInnerClasses;
	}

	@Nonnull
	public ObservableBoolean getDumpBytecodeOnError() {
		return dumpBytecodeOnError;
	}

	@Nonnull
	public ObservableBoolean getDumpExceptionOnError() {
		return dumpExceptionOnError;
	}

	@Nonnull
	public ObservableBoolean getDecompilerComments() {
		return decompilerComments;
	}

	@Nonnull
	public ObservableBoolean getSourceFileComments() {
		return sourceFileComments;
	}

	@Nonnull
	public ObservableBoolean getDecompileComplexCondys() {
		return decompileComplexCondys;
	}

	@Nonnull
	public ObservableBoolean getForceJsrInline() {
		return forceJsrInline;
	}
}
