package me.coley.recaf.decompile.procyon;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import me.coley.recaf.config.ConfDecompile;
import me.coley.recaf.control.Controller;
import me.coley.recaf.decompile.Decompiler;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Procyon decompiler implementation.
 *
 * @author xxDark
 */
public final class ProcyonDecompiler extends Decompiler<Boolean> {
    /**
     * Initialize the decompiler wrapper.
     *
     * @param controller
     * 		Controller with configuration to pull from and the workspace to pull classes from.
     */
    public ProcyonDecompiler(Controller controller) {
        super(controller);
    }

    @Override
    protected Map<String, Boolean> generateDefaultOptions() {
        ConfDecompile config = getController().config().decompile();
        Map<String, Boolean> options = new HashMap<>(17);
        options.put("merge-variables", false);
        options.put("force-explicit-imports", false);
        options.put("collapse-imports", false);
        options.put("force-explicit-type-arguments", false);
        options.put("retain-redundant-casts", false);
        options.put("flatten-switch-blocks", false);
        options.put("show-synthetic-members", config.showSynthetic);
        options.put("verbose", false);
        options.put("unoptimized", false);
        options.put("exclude-nested-types", false);
        options.put("show-debug-line-numbers", false);
        options.put("retain-pointless-switches", false);
        options.put("unicode-output", true);
        options.put("eager-methods-loading", true);
        options.put("simplify-member-references", false);
        options.put("force-fully-qualified-references", false);
        options.put("disable-for-each-transforms", false);
        return options;
    }

    @Override
    public String decompile(String name) {
        ITypeLoader loader = new ComposedTypeLoader(Arrays.asList(
                new RecafTypeLoader(getController()), new InputTypeLoader()
        ));
        Map<String, Boolean> options = getOptions();
        DecompilerSettings settings = new DecompilerSettings();
        settings.setFlattenSwitchBlocks(options.get("flatten-switch-blocks"));
        settings.setForceExplicitImports(!options.get("collapse-imports"));
        settings.setForceExplicitTypeArguments(options.get("force-explicit-type-arguments"));
        settings.setRetainRedundantCasts(options.get("retain-redundant-casts"));
        settings.setShowSyntheticMembers(options.get("show-synthetic-members"));
        settings.setExcludeNestedTypes(options.get("exclude-nested-types"));
        settings.setRetainPointlessSwitches(options.get("retain-pointless-switches"));
        settings.setUnicodeOutputEnabled(options.get("unicode-output"));
        settings.setMergeVariables(options.get("merge-variables"));
        settings.setShowDebugLineNumbers(options.get("show-debug-line-numbers"));
        settings.setSimplifyMemberReferences(options.get("simplify-member-references"));
        settings.setForceFullyQualifiedReferences(options.get("force-fully-qualified-references"));
        settings.setDisableForEachTransforms(options.get("disable-for-each-transforms"));
        settings.setTypeLoader(loader);
        settings.setJavaFormattingOptions(JavaFormattingOptions.createDefault());
        MetadataSystem system = new MetadataSystem(loader);
        system.setEagerMethodLoadingEnabled(options.get("eager-methods-loading"));
        TypeReference ref = system.lookupType(name);
        DecompilationOptions decompilationOptions = new DecompilationOptions();
        decompilationOptions.setSettings(settings);
        decompilationOptions.setFullDecompilation(true);
        StringWriter writer = new StringWriter();
        settings.getLanguage().decompileType(ref.resolve(), new PlainTextOutput(writer), decompilationOptions);
        return writer.toString();
    }
}
