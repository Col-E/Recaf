package software.coley.recaf.services.decompile.vineflower;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.decompile.BaseDecompilerConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Config for {@link VineflowerDecompiler}
 *
 * @author therathatter
 */
@ApplicationScoped
public class VineflowerConfig extends BaseDecompilerConfig {
    private final ObservableBoolean REMOVE_BRIDGE = new ObservableBoolean(true);
    private final ObservableBoolean REMOVE_SYNTHETIC = new ObservableBoolean(true);
    private final ObservableBoolean DECOMPILE_INNER = new ObservableBoolean(true);
    private final ObservableBoolean DECOMPILE_CLASS_1_4 = new ObservableBoolean(true);
    private final ObservableBoolean DECOMPILE_ASSERTIONS = new ObservableBoolean(true);
    private final ObservableBoolean HIDE_EMPTY_SUPER = new ObservableBoolean(true);
    private final ObservableBoolean HIDE_DEFAULT_CONSTRUCTOR = new ObservableBoolean(true);
    private final ObservableBoolean DECOMPILE_GENERIC_SIGNATURES = new ObservableBoolean(true);
    private final ObservableBoolean NO_EXCEPTIONS_RETURN = new ObservableBoolean(true);
    private final ObservableBoolean ENSURE_SYNCHRONIZED_MONITOR = new ObservableBoolean(true);
    private final ObservableBoolean DECOMPILE_ENUM = new ObservableBoolean(true);
    private final ObservableBoolean REMOVE_GET_CLASS_NEW = new ObservableBoolean(true);
    private final ObservableBoolean LITERALS_AS_IS = new ObservableBoolean(false);
    private final ObservableBoolean BOOLEAN_TRUE_ONE = new ObservableBoolean(true);
    private final ObservableBoolean ASCII_STRING_CHARACTERS = new ObservableBoolean(false);
    private final ObservableBoolean SYNTHETIC_NOT_SET = new ObservableBoolean(false);
    private final ObservableBoolean UNDEFINED_PARAM_TYPE_OBJECT = new ObservableBoolean(true);
    private final ObservableBoolean USE_DEBUG_VAR_NAMES = new ObservableBoolean(true);
    private final ObservableBoolean USE_METHOD_PARAMETERS = new ObservableBoolean(true);
    private final ObservableBoolean REMOVE_EMPTY_RANGES = new ObservableBoolean(true);
    private final ObservableBoolean FINALLY_DEINLINE = new ObservableBoolean(true);
    private final ObservableBoolean IDEA_NOT_NULL_ANNOTATION = new ObservableBoolean(true);
    private final ObservableBoolean LAMBDA_TO_ANONYMOUS_CLASS = new ObservableBoolean(false);
    private final ObservableBoolean BYTECODE_SOURCE_MAPPING = new ObservableBoolean(false);
    private final ObservableBoolean DUMP_CODE_LINES = new ObservableBoolean(false);
    private final ObservableBoolean IGNORE_INVALID_BYTECODE = new ObservableBoolean(false);
    private final ObservableBoolean VERIFY_ANONYMOUS_CLASSES = new ObservableBoolean(false);
    private final ObservableBoolean TERNARY_CONSTANT_SIMPLIFICATION = new ObservableBoolean(false);
    private final ObservableBoolean OVERRIDE_ANNOTATION = new ObservableBoolean(true);
    private final ObservableBoolean PATTERN_MATCHING = new ObservableBoolean(true);
    private final ObservableBoolean TRY_LOOP_FIX = new ObservableBoolean(true);
    private final ObservableBoolean TERNARY_CONDITIONS= new ObservableBoolean(true);
    private final ObservableBoolean SWITCH_EXPRESSIONS = new ObservableBoolean(true);
    private final ObservableBoolean SHOW_HIDDEN_STATEMENTS = new ObservableBoolean(false);
    private final ObservableBoolean SIMPLIFY_STACK_SECOND_PASS = new ObservableBoolean(true);
    private final ObservableBoolean VERIFY_VARIABLE_MERGES = new ObservableBoolean(false);
    private final ObservableBoolean DECOMPILE_PREVIEW = new ObservableBoolean(true);
    private final ObservableBoolean EXPLICIT_GENERIC_ARGUMENTS = new ObservableBoolean(false);
    private final ObservableBoolean INLINE_SIMPLE_LAMBDAS = new ObservableBoolean(true);
    private final ObservableBoolean USE_JAD_VARNAMING = new ObservableBoolean(false);
    private final ObservableBoolean USE_JAD_PARAMETER_NAMING = new ObservableBoolean(false);
    private final ObservableBoolean SKIP_EXTRA_FILES = new ObservableBoolean(false);
    private final ObservableBoolean WARN_INCONSISTENT_INNER_CLASSES = new ObservableBoolean(true);
    private final ObservableBoolean DUMP_BYTECODE_ON_ERROR = new ObservableBoolean(true);
    private final ObservableBoolean DUMP_EXCEPTION_ON_ERROR = new ObservableBoolean(true);
    private final ObservableBoolean DECOMPILER_COMMENTS = new ObservableBoolean(true);
    private final ObservableBoolean SOURCE_FILE_COMMENTS = new ObservableBoolean(false);
    private final ObservableBoolean DECOMPILE_COMPLEX_CONDYS = new ObservableBoolean(false);
    private final ObservableBoolean FORCE_JSR_INLINE = new ObservableBoolean(false);

    @Inject
    public VineflowerConfig() {
        super(ConfigGroups.SERVICE_DECOMPILE, "decompiler-vineflower" + CONFIG_SUFFIX);
        addValue(new BasicConfigValue<>( "rbr", boolean.class, REMOVE_BRIDGE));
        addValue(new BasicConfigValue<>( "rsy", boolean.class, REMOVE_SYNTHETIC));
        addValue(new BasicConfigValue<>( "din", boolean.class, DECOMPILE_INNER));
        addValue(new BasicConfigValue<>( "dc4", boolean.class, DECOMPILE_CLASS_1_4));
        addValue(new BasicConfigValue<>( "das", boolean.class, DECOMPILE_ASSERTIONS));
        addValue(new BasicConfigValue<>( "hes", boolean.class, HIDE_EMPTY_SUPER));
        addValue(new BasicConfigValue<>( "hdc", boolean.class, HIDE_DEFAULT_CONSTRUCTOR));
        addValue(new BasicConfigValue<>( "dgs", boolean.class, DECOMPILE_GENERIC_SIGNATURES));
        addValue(new BasicConfigValue<>( "ner", boolean.class, NO_EXCEPTIONS_RETURN));
        addValue(new BasicConfigValue<>( "esm", boolean.class, ENSURE_SYNCHRONIZED_MONITOR));
        addValue(new BasicConfigValue<>( "den", boolean.class, DECOMPILE_ENUM));
        addValue(new BasicConfigValue<>( "dpr", boolean.class, DECOMPILE_PREVIEW));
        addValue(new BasicConfigValue<>( "rgn", boolean.class, REMOVE_GET_CLASS_NEW));
        addValue(new BasicConfigValue<>( "lit", boolean.class, LITERALS_AS_IS));
        addValue(new BasicConfigValue<>( "bto", boolean.class, BOOLEAN_TRUE_ONE));
        addValue(new BasicConfigValue<>( "asc", boolean.class, ASCII_STRING_CHARACTERS));
        addValue(new BasicConfigValue<>( "nns", boolean.class, SYNTHETIC_NOT_SET));
        addValue(new BasicConfigValue<>( "uto", boolean.class, UNDEFINED_PARAM_TYPE_OBJECT));
        addValue(new BasicConfigValue<>( "udv", boolean.class, USE_DEBUG_VAR_NAMES));
        addValue(new BasicConfigValue<>( "ump", boolean.class, USE_METHOD_PARAMETERS));
        addValue(new BasicConfigValue<>( "rer", boolean.class, REMOVE_EMPTY_RANGES));
        addValue(new BasicConfigValue<>( "fdi", boolean.class, FINALLY_DEINLINE));
        addValue(new BasicConfigValue<>( "inn", boolean.class, IDEA_NOT_NULL_ANNOTATION));
        addValue(new BasicConfigValue<>( "lac", boolean.class, LAMBDA_TO_ANONYMOUS_CLASS));
        addValue(new BasicConfigValue<>( "bsm", boolean.class, BYTECODE_SOURCE_MAPPING));
        addValue(new BasicConfigValue<>( "dcl", boolean.class, DUMP_CODE_LINES));
        addValue(new BasicConfigValue<>( "iib", boolean.class, IGNORE_INVALID_BYTECODE));
        addValue(new BasicConfigValue<>( "vac", boolean.class, VERIFY_ANONYMOUS_CLASSES));
        addValue(new BasicConfigValue<>( "tcs", boolean.class, TERNARY_CONSTANT_SIMPLIFICATION));
        addValue(new BasicConfigValue<>( "pam", boolean.class, PATTERN_MATCHING));
        addValue(new BasicConfigValue<>( "tlf", boolean.class, TRY_LOOP_FIX));
        addValue(new BasicConfigValue<>( "tco", boolean.class, TERNARY_CONDITIONS));
        addValue(new BasicConfigValue<>( "swe", boolean.class, SWITCH_EXPRESSIONS));
        addValue(new BasicConfigValue<>( "shs", boolean.class, SHOW_HIDDEN_STATEMENTS));
        addValue(new BasicConfigValue<>( "ovr", boolean.class, OVERRIDE_ANNOTATION));
        addValue(new BasicConfigValue<>( "ssp", boolean.class, SIMPLIFY_STACK_SECOND_PASS));
        addValue(new BasicConfigValue<>( "vvm", boolean.class, VERIFY_VARIABLE_MERGES));
        addValue(new BasicConfigValue<>( "ega", boolean.class, EXPLICIT_GENERIC_ARGUMENTS));
        addValue(new BasicConfigValue<>( "isl", boolean.class, INLINE_SIMPLE_LAMBDAS));
        addValue(new BasicConfigValue<>( "jvn", boolean.class, USE_JAD_VARNAMING));
        addValue(new BasicConfigValue<>( "jpr", boolean.class, USE_JAD_PARAMETER_NAMING));
        addValue(new BasicConfigValue<>( "sef", boolean.class, SKIP_EXTRA_FILES));
        addValue(new BasicConfigValue<>( "win", boolean.class, WARN_INCONSISTENT_INNER_CLASSES));
        addValue(new BasicConfigValue<>( "dbe", boolean.class, DUMP_BYTECODE_ON_ERROR));
        addValue(new BasicConfigValue<>( "dee", boolean.class, DUMP_EXCEPTION_ON_ERROR));
        addValue(new BasicConfigValue<>( "dec", boolean.class, DECOMPILER_COMMENTS));
        addValue(new BasicConfigValue<>( "sfc", boolean.class, SOURCE_FILE_COMMENTS));
        addValue(new BasicConfigValue<>( "dcc", boolean.class, DECOMPILE_COMPLEX_CONDYS));
        addValue(new BasicConfigValue<>( "fji", boolean.class, FORCE_JSR_INLINE));
        registerConfigValuesHashUpdates();
    }

    Map<String, Object> getFernflowerProperties() {
        Map<String, Object> properties = new HashMap<>(IFernflowerPreferences.DEFAULTS);

        getValues().forEach((key, value) -> {
            if (value.getValue() instanceof Boolean) {
                properties.put(key, (Boolean)value.getValue() ? "1" : "0");
            }
        });

        return properties;
    }
}
