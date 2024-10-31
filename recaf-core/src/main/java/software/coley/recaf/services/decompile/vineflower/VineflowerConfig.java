package software.coley.recaf.services.decompile.vineflower;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.event.Level;
import recaf.relocation.libs.vineflower.org.jetbrains.java.decompiler.main.Fernflower;
import recaf.relocation.libs.vineflower.org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableObject;
import software.coley.observables.ObservableString;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.services.decompile.BaseDecompilerConfig;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Config for {@link VineflowerDecompiler}
 *
 * @author therathatter
 * @see IFernflowerPreferences Source of value definitions.
 */
@ApplicationScoped
@SuppressWarnings("all") // ignore unused refs / typos
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
    private final ObservableBoolean __dumpOriginalLines__ = new ObservableBoolean(false);
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
    private final ObservableBoolean __unitTestMode__ = new ObservableBoolean(false);
    private final ObservableString banner = new ObservableString("// Recreated by Recaf (powered by VineFlower decompiler)\n\n");
    private final ObservableBoolean dumpCodeLines = new ObservableBoolean(false);
    private final ObservableBoolean dumpTextTokens = new ObservableBoolean(false);
    private final ObservableString errorMessage = new ObservableString("Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)");
    private final ObservableBoolean includeClasspath = new ObservableBoolean(false);
    private final ObservableString includeRuntime = new ObservableString("");
    private final ObservableString indentString = new ObservableString("   ");
    private final ObservableBoolean markCorrespondingSynthetics = new ObservableBoolean(false);
    private final ObservableBoolean maxTimePerMethod = new ObservableBoolean(false);
    private final ObservableBoolean newLineSeparator = new ObservableBoolean(true);
    private final ObservableInteger preferredLineLength = new ObservableInteger(160);
    private final ObservableBoolean removeImports = new ObservableBoolean(false);
    private final ObservableBoolean renameMembers = new ObservableBoolean(false);
    private final ObservableString threadCount = new ObservableString(String.valueOf(Runtime.getRuntime().availableProcessors()));
    private final ObservableString userRenamerClass = new ObservableString(null, (Function<String, String>) o -> {
        if (o == null || o.isBlank()) return null;
        return o;
    });

    public static void main(String[] args) {
        for (Field field : IFernflowerPreferences.class.getDeclaredFields()) {
            try {
                IFernflowerPreferences.Name name = field.getDeclaredAnnotation(IFernflowerPreferences.Name.class);
                String key = (String) field.get(null);
                System.out.println("service.decompile.impl.decompiler-vineflower-config." + key + "=" + name.value());
            } catch (Throwable t) {
            }
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
        addValue(new BasicConfigValue<>("__dump_original_lines__", boolean.class, __dumpOriginalLines__));
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
        addValue(new BasicConfigValue<>("__unit_test_mode__", boolean.class, __unitTestMode__));
        addValue(new BasicConfigValue<>("banner", String.class, banner));
        addValue(new BasicConfigValue<>("dump-code-lines", boolean.class, dumpCodeLines));
        addValue(new BasicConfigValue<>("dump-text-tokens", boolean.class, dumpTextTokens));
        addValue(new BasicConfigValue<>("error-message", String.class, errorMessage));
        addValue(new BasicConfigValue<>("include-classpath", boolean.class, includeClasspath));
        addValue(new BasicConfigValue<>("include-runtime", String.class, includeRuntime));
        addValue(new BasicConfigValue<>("indent-string", String.class, indentString));
        addValue(new BasicConfigValue<>("mark-corresponding-synthetics", boolean.class, markCorrespondingSynthetics));
        addValue(new BasicConfigValue<>("max-time-per-method", boolean.class, maxTimePerMethod));
        addValue(new BasicConfigValue<>("new-line-separator", boolean.class, newLineSeparator));
        addValue(new BasicConfigValue<>("preferred-line-length", int.class, preferredLineLength));
        addValue(new BasicConfigValue<>("remove-imports", boolean.class, removeImports));
        addValue(new BasicConfigValue<>("rename-members", boolean.class, renameMembers));
        addValue(new BasicConfigValue<>("thread-count", String.class, threadCount));
        addValue(new BasicConfigValue<>("user-renamer-class", String.class, userRenamerClass));
        
        registerConfigValuesHashUpdates();
    }

    /**
     * @return Map of values to pass to the {@link Fernflower} instance.
     */
    @Nonnull
    protected Map<String, Object> getFernflowerProperties() {
        Map<String, Object> properties = new HashMap<>(IFernflowerPreferences.DEFAULTS);
        getValues().forEach((key, value) -> {
            if (value.getValue() instanceof Boolean bool) {
                properties.put(key, bool ? "1" : "0");
            } else if (value.getValue() instanceof String str) {
                properties.put(key, str);
            } else if (value.getValue() instanceof Level level) {
                properties.put(key, level.name());
            } else if (value.getValue() instanceof Integer integer) {
                properties.put(key, integer.toString());
            }
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

}
