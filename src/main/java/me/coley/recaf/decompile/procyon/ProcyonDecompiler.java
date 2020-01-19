package me.coley.recaf.decompile.procyon;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.workspace.Workspace;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Procyon decompiler implementation.
 *
 * @author xxDark
 */
public final class ProcyonDecompiler extends Decompiler<Object> {
    @Override
    protected Map<String, Object> generateDefaultOptions() {
        Map<String, Object> options = new HashMap<>(17);
        options.put("merge-variables", false);
        options.put("force-explicit-imports", false);
        options.put("collapse-imports", false);
        options.put("force-explicit-type-arguments", false);
        options.put("retain-redundant-casts", false);
        options.put("flatten-switch-blocks", false);
        options.put("show-synthetic-members", true);
        options.put("verbose", false);
        options.put("unoptimized", false);
        options.put("exclude-nested-types", false);
        options.put("show-debug-line-numbers", false);
        options.put("retain-pointless-switches", false);
        options.put("unicode-output", false);
        options.put("eager-methods-loading", true);
        options.put("simplify-member-references", false);
        options.put("force-fully-qualified-references", false);
        options.put("disable-for-each-transforms", false);
        return options;
    }

    @Override
    public String decompile(Workspace workspace, String name) {
        ITypeLoader loader = new ComposedTypeLoader(Arrays.asList(
                new RecafTypeLoader(workspace), new InputTypeLoader()
        ));
        DecompilerSettings settings = new DecompilerSettings();
        Map<String, Object> options = getOptions();
        settings.setFlattenSwitchBlocks((Boolean) options.get("flatten-switch-blocks"));
        settings.setForceExplicitImports(!(Boolean) options.get("collapse-imports"));
        settings.setForceExplicitTypeArguments((Boolean) options.get("force-explicit-type-arguments"));
        settings.setRetainRedundantCasts((Boolean) options.get("retain-redundant-casts"));
        settings.setShowSyntheticMembers((Boolean) options.get("show-synthetic-members"));
        settings.setExcludeNestedTypes((Boolean) options.get("exclude-nested-types"));
        settings.setRetainPointlessSwitches((Boolean) options.get("retain-pointless-switches"));
        settings.setUnicodeOutputEnabled((Boolean) options.get("unicode-output"));
        settings.setMergeVariables((Boolean) options.get("merge-variables"));
        settings.setShowDebugLineNumbers((Boolean) options.get("show-debug-line-numbers"));
        settings.setSimplifyMemberReferences((Boolean) options.get("simplify-member-references"));
        settings.setForceFullyQualifiedReferences((Boolean) options.get("force-fully-qualified-references"));
        settings.setDisableForEachTransforms((Boolean) options.get("disable-for-each-transforms"));
        settings.setTypeLoader(loader);
        settings.setJavaFormattingOptions(JavaFormattingOptions.createDefault());
        MetadataSystem system = new MetadataSystem(loader);
        system.setEagerMethodLoadingEnabled((Boolean) options.get("eager-methods-loading"));
        TypeReference ref = system.lookupType(name);
        DecompilationOptions decompilationOptions = new DecompilationOptions();
        decompilationOptions.setSettings(settings);
        decompilationOptions.setFullDecompilation(true);
        StringWriter writer = new StringWriter();
        settings.getLanguage().decompileType(ref.resolve(), new PlainTextOutput(writer), decompilationOptions);
        return writer.toString();
    }
}
