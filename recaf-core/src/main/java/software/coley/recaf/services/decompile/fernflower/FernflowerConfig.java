package software.coley.recaf.services.decompile.fernflower;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.event.Level;
import recaf.relocation.libs.fernflower.org.jetbrains.java.decompiler.main.Fernflower;
import recaf.relocation.libs.fernflower.org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import recaf.relocation.libs.fernflower.org.jetbrains.java.decompiler.util.InterpreterUtil;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableObject;
import software.coley.observables.ObservableString;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.services.decompile.BaseDecompilerConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * FernflowerConfig
 * {@link org.jetbrains}
 *
 * @author meiMingle
 */
@ApplicationScoped
@SuppressWarnings("all") // ignore unused refs / typos
public class FernflowerConfig extends BaseDecompilerConfig {

    /**
     * Mapped from here {@link IFernflowerPreferences}
     */
    private final ObservableBoolean rbr = new ObservableBoolean(true);
    private final ObservableBoolean rsy = new ObservableBoolean(false);
    private final ObservableBoolean din = new ObservableBoolean(true);
    private final ObservableBoolean dc4 = new ObservableBoolean(true);
    private final ObservableBoolean das = new ObservableBoolean(true);
    private final ObservableBoolean hes = new ObservableBoolean(true);
    private final ObservableBoolean hdc = new ObservableBoolean(true);
    private final ObservableBoolean dgs = new ObservableBoolean(false);
    private final ObservableBoolean ner = new ObservableBoolean(true);
    private final ObservableBoolean esm = new ObservableBoolean(true);
    private final ObservableBoolean den = new ObservableBoolean(true);
    private final ObservableBoolean rgn = new ObservableBoolean(true);
    private final ObservableBoolean lit = new ObservableBoolean(false);
    private final ObservableBoolean bto = new ObservableBoolean(true);
    private final ObservableBoolean asc = new ObservableBoolean(false);
    private final ObservableBoolean nns = new ObservableBoolean(false);
    private final ObservableBoolean uto = new ObservableBoolean(true);
    private final ObservableBoolean udv = new ObservableBoolean(true);
    private final ObservableBoolean ump = new ObservableBoolean(true);
    private final ObservableBoolean rer = new ObservableBoolean(true);
    private final ObservableBoolean fdi = new ObservableBoolean(true);
    private final ObservableBoolean inn = new ObservableBoolean(true);
    private final ObservableBoolean lac = new ObservableBoolean(false);
    private final ObservableBoolean bsm = new ObservableBoolean(false);
    private final ObservableBoolean iib = new ObservableBoolean(false);
    private final ObservableBoolean vac = new ObservableBoolean(false);
    private final ObservableBoolean crp = new ObservableBoolean(false);
    private final ObservableBoolean cps = new ObservableBoolean(false);
    private final ObservableBoolean sfn = new ObservableBoolean(false);
    private final ObservableBoolean iec = new ObservableBoolean(false);
    private final ObservableBoolean cci = new ObservableBoolean(true);
    private final ObservableBoolean isl = new ObservableBoolean(true);
    private final ObservableBoolean ucrc = new ObservableBoolean(true);
    private final ObservableObject<Level> log = new ObservableObject<>(Level.WARN);
    private final ObservableInteger mpm = new ObservableInteger(0);
    private final ObservableBoolean ren = new ObservableBoolean(false);
    private final ObservableString urc = new ObservableString(null, (Function<String, String>) o -> {
        if (o == null || o.isBlank()) return null;
        return o;
    });

    private final ObservableBoolean nls = new ObservableBoolean(!InterpreterUtil.IS_WINDOWS);
    private final ObservableString ind = new ObservableString("   ");
    private final ObservableString ban = new ObservableString("// Recreated by Recaf (powered by FernFlower decompiler)\n\n");
    private final ObservableBoolean __unit_test_mode__ = new ObservableBoolean(false);
    private final ObservableBoolean __dump_original_lines__ = new ObservableBoolean(false);
    private final ObservableBoolean jvn = new ObservableBoolean(false);
    private final ObservableBoolean jpr = new ObservableBoolean(false);
    private final ObservableBoolean sef = new ObservableBoolean(false);

    @Inject
    public FernflowerConfig() {
        super("decompiler-fernflower" + CONFIG_SUFFIX);
        addValue(new BasicConfigValue<>("rbr", boolean.class, rbr));
        addValue(new BasicConfigValue<>("rsy", boolean.class, rsy));
        addValue(new BasicConfigValue<>("din", boolean.class, din));
        addValue(new BasicConfigValue<>("dc4", boolean.class, dc4));
        addValue(new BasicConfigValue<>("das", boolean.class, das));
        addValue(new BasicConfigValue<>("hes", boolean.class, hes));
        addValue(new BasicConfigValue<>("hdc", boolean.class, hdc));
        addValue(new BasicConfigValue<>("dgs", boolean.class, dgs));
        addValue(new BasicConfigValue<>("ner", boolean.class, ner));
        addValue(new BasicConfigValue<>("esm", boolean.class, esm));
        addValue(new BasicConfigValue<>("den", boolean.class, den));
        addValue(new BasicConfigValue<>("rgn", boolean.class, rgn));
        addValue(new BasicConfigValue<>("lit", boolean.class, lit));
        addValue(new BasicConfigValue<>("bto", boolean.class, bto));
        addValue(new BasicConfigValue<>("asc", boolean.class, asc));
        addValue(new BasicConfigValue<>("nns", boolean.class, nns));
        addValue(new BasicConfigValue<>("uto", boolean.class, uto));
        addValue(new BasicConfigValue<>("udv", boolean.class, udv));
        addValue(new BasicConfigValue<>("ump", boolean.class, ump));
        addValue(new BasicConfigValue<>("rer", boolean.class, rer));
        addValue(new BasicConfigValue<>("fdi", boolean.class, fdi));
        addValue(new BasicConfigValue<>("inn", boolean.class, inn));
        addValue(new BasicConfigValue<>("lac", boolean.class, lac));
        addValue(new BasicConfigValue<>("bsm", boolean.class, bsm));
        addValue(new BasicConfigValue<>("iib", boolean.class, iib));
        addValue(new BasicConfigValue<>("vac", boolean.class, vac));
        addValue(new BasicConfigValue<>("crp", boolean.class, crp));
        addValue(new BasicConfigValue<>("cps", boolean.class, cps));
        addValue(new BasicConfigValue<>("sfn", boolean.class, sfn));
        addValue(new BasicConfigValue<>("iec", boolean.class, iec));
        addValue(new BasicConfigValue<>("cci", boolean.class, cci));
        addValue(new BasicConfigValue<>("isl", boolean.class, isl));
        addValue(new BasicConfigValue<>("ucrc", boolean.class, ucrc));
        addValue(new BasicConfigValue<>("log", Level.class, log));
        addValue(new BasicConfigValue<>("mpm", int.class, mpm));
        addValue(new BasicConfigValue<>("ren", boolean.class, ren));
        addValue(new BasicConfigValue<>("urc", String.class, urc));
        addValue(new BasicConfigValue<>("nls", boolean.class, nls));
        addValue(new BasicConfigValue<>("ind", String.class, ind));
        addValue(new BasicConfigValue<>("ban", String.class, ban));
        addValue(new BasicConfigValue<>("__unit_test_mode__", boolean.class, __unit_test_mode__));
        addValue(new BasicConfigValue<>("__dump_original_lines__", boolean.class, __dump_original_lines__));
        addValue(new BasicConfigValue<>("jvn", boolean.class, jvn));
        addValue(new BasicConfigValue<>("jpr", boolean.class, jpr));
        addValue(new BasicConfigValue<>("sef", boolean.class, sef));

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


    public ObservableObject<Level> getLoggingLevel() {
        return log;
    }
}
