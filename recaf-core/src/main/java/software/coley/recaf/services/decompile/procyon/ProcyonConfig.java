package software.coley.recaf.services.decompile.procyon;

import com.strobel.assembler.metadata.CompilerTarget;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.BytecodeOutputOptions;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.decompile.DecompilerConfig;

/**
 * Config for {@link ProcyonDecompiler}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ProcyonConfig extends BasicConfigContainer implements DecompilerConfig {
	private final ObservableBoolean includeLineNumbersInBytecode = new ObservableBoolean(true);
	private final ObservableBoolean showSyntheticMembers = new ObservableBoolean(false);
	private final ObservableBoolean alwaysGenerateExceptionVariableForCatchBlocks = new ObservableBoolean(true);
	private final ObservableBoolean forceFullyQualifiedReferences = new ObservableBoolean(false);
	private final ObservableBoolean forceExplicitImports = new ObservableBoolean(true);
	private final ObservableBoolean forceExplicitTypeArguments = new ObservableBoolean(false);
	private final ObservableBoolean flattenSwitchBlocks = new ObservableBoolean(false);
	private final ObservableBoolean excludeNestedTypes = new ObservableBoolean(false);
	private final ObservableBoolean retainRedundantCasts = new ObservableBoolean(false);
	private final ObservableBoolean retainPointlessSwitches = new ObservableBoolean(false);
	private final ObservableBoolean isUnicodeOutputEnabled = new ObservableBoolean(false);
	private final ObservableBoolean includeErrorDiagnostics = new ObservableBoolean(true);
	private final ObservableBoolean mergeVariables = new ObservableBoolean(false);
	private final ObservableBoolean disableForEachTransforms = new ObservableBoolean(false);
	private final ObservableBoolean showDebugLineNumbers = new ObservableBoolean(false);
	private final ObservableBoolean simplifyMemberReferences = new ObservableBoolean(false);
	private final ObservableBoolean arePreviewFeaturesEnabled = new ObservableBoolean(false);
	private final ObservableInteger textBlockLineMinimum = new ObservableInteger(3);
	private final ObservableObject<CompilerTarget> forcedCompilerTarget = new ObservableObject<>(null);
	private final ObservableObject<BytecodeOutputOptions> bytecodeOutputOptions = new ObservableObject<>(BytecodeOutputOptions.createDefault());
	private int hash;

	@Inject
	public ProcyonConfig() {
		super(ConfigGroups.SERVICE_DECOMPILE, "decompiler-procyon" + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("includeLineNumbersInBytecode", Boolean.class, includeLineNumbersInBytecode));
		addValue(new BasicConfigValue<>("showSyntheticMembers", Boolean.class, showSyntheticMembers));
		addValue(new BasicConfigValue<>("alwaysGenerateExceptionVariableForCatchBlocks", Boolean.class, alwaysGenerateExceptionVariableForCatchBlocks));
		addValue(new BasicConfigValue<>("forceFullyQualifiedReferences", Boolean.class, forceFullyQualifiedReferences));
		addValue(new BasicConfigValue<>("forceExplicitImports", Boolean.class, forceExplicitImports));
		addValue(new BasicConfigValue<>("forceExplicitTypeArguments", Boolean.class, forceExplicitTypeArguments));
		addValue(new BasicConfigValue<>("flattenSwitchBlocks", Boolean.class, flattenSwitchBlocks));
		addValue(new BasicConfigValue<>("excludeNestedTypes", Boolean.class, excludeNestedTypes));
		addValue(new BasicConfigValue<>("retainRedundantCasts", Boolean.class, retainRedundantCasts));
		addValue(new BasicConfigValue<>("retainPointlessSwitches", Boolean.class, retainPointlessSwitches));
		addValue(new BasicConfigValue<>("isUnicodeOutputEnabled", Boolean.class, isUnicodeOutputEnabled));
		addValue(new BasicConfigValue<>("includeErrorDiagnostics", Boolean.class, includeErrorDiagnostics));
		addValue(new BasicConfigValue<>("mergeVariables", Boolean.class, mergeVariables));
		addValue(new BasicConfigValue<>("disableForEachTransforms", Boolean.class, disableForEachTransforms));
		addValue(new BasicConfigValue<>("showDebugLineNumbers", Boolean.class, showDebugLineNumbers));
		addValue(new BasicConfigValue<>("simplifyMemberReferences", Boolean.class, simplifyMemberReferences));
		addValue(new BasicConfigValue<>("arePreviewFeaturesEnabled", Boolean.class, arePreviewFeaturesEnabled));
		addValue(new BasicConfigValue<>("textBlockLineMinimum", Integer.class, textBlockLineMinimum));
		addValue(new BasicConfigValue<>("forcedCompilerTarget", CompilerTarget.class, forcedCompilerTarget));
		addValue(new BasicConfigValue<>("bytecodeOutputOptions", BytecodeOutputOptions.class, bytecodeOutputOptions));
	}

	/**
	 * @return Settings wrapper.
	 */
	@Nonnull
	public DecompilerSettings toSettings() {
		DecompilerSettings decompilerSettings = new DecompilerSettings();
		decompilerSettings.setIncludeLineNumbersInBytecode(includeLineNumbersInBytecode.getValue());
		decompilerSettings.setShowSyntheticMembers(showSyntheticMembers.getValue());
		decompilerSettings.setAlwaysGenerateExceptionVariableForCatchBlocks(alwaysGenerateExceptionVariableForCatchBlocks.getValue());
		decompilerSettings.setForceFullyQualifiedReferences(forceFullyQualifiedReferences.getValue());
		decompilerSettings.setForceExplicitImports(forceExplicitImports.getValue());
		decompilerSettings.setForceExplicitTypeArguments(forceExplicitTypeArguments.getValue());
		decompilerSettings.setFlattenSwitchBlocks(flattenSwitchBlocks.getValue());
		decompilerSettings.setExcludeNestedTypes(excludeNestedTypes.getValue());
		decompilerSettings.setRetainRedundantCasts(retainRedundantCasts.getValue());
		decompilerSettings.setRetainPointlessSwitches(retainPointlessSwitches.getValue());
		decompilerSettings.setUnicodeOutputEnabled(isUnicodeOutputEnabled.getValue());
		decompilerSettings.setIncludeErrorDiagnostics(includeErrorDiagnostics.getValue());
		decompilerSettings.setMergeVariables(mergeVariables.getValue());
		decompilerSettings.setDisableForEachTransforms(disableForEachTransforms.getValue());
		decompilerSettings.setShowDebugLineNumbers(showDebugLineNumbers.getValue());
		decompilerSettings.setSimplifyMemberReferences(simplifyMemberReferences.getValue());
		decompilerSettings.setPreviewFeaturesEnabled(arePreviewFeaturesEnabled.getValue());
		decompilerSettings.setTextBlockLineMinimum(textBlockLineMinimum.getValue());
		decompilerSettings.setForcedCompilerTarget(forcedCompilerTarget.getValue());
		decompilerSettings.setBytecodeOutputOptions(bytecodeOutputOptions.getValue());
		return decompilerSettings;
	}

	@Nonnull
	public ObservableBoolean getIncludeLineNumbersInBytecode() {
		return includeLineNumbersInBytecode;
	}

	@Nonnull
	public ObservableBoolean getShowSyntheticMembers() {
		return showSyntheticMembers;
	}

	@Nonnull
	public ObservableBoolean getAlwaysGenerateExceptionVariableForCatchBlocks() {
		return alwaysGenerateExceptionVariableForCatchBlocks;
	}

	@Nonnull
	public ObservableBoolean getForceFullyQualifiedReferences() {
		return forceFullyQualifiedReferences;
	}

	@Nonnull
	public ObservableBoolean getForceExplicitImports() {
		return forceExplicitImports;
	}

	@Nonnull
	public ObservableBoolean getForceExplicitTypeArguments() {
		return forceExplicitTypeArguments;
	}

	@Nonnull
	public ObservableBoolean getFlattenSwitchBlocks() {
		return flattenSwitchBlocks;
	}

	@Nonnull
	public ObservableBoolean getExcludeNestedTypes() {
		return excludeNestedTypes;
	}

	@Nonnull
	public ObservableBoolean getRetainRedundantCasts() {
		return retainRedundantCasts;
	}

	@Nonnull
	public ObservableBoolean getRetainPointlessSwitches() {
		return retainPointlessSwitches;
	}

	@Nonnull
	public ObservableBoolean getIsUnicodeOutputEnabled() {
		return isUnicodeOutputEnabled;
	}

	@Nonnull
	public ObservableBoolean getIncludeErrorDiagnostics() {
		return includeErrorDiagnostics;
	}

	@Nonnull
	public ObservableBoolean getMergeVariables() {
		return mergeVariables;
	}

	@Nonnull
	public ObservableBoolean getDisableForEachTransforms() {
		return disableForEachTransforms;
	}

	@Nonnull
	public ObservableBoolean getShowDebugLineNumbers() {
		return showDebugLineNumbers;
	}

	@Nonnull
	public ObservableBoolean getSimplifyMemberReferences() {
		return simplifyMemberReferences;
	}

	@Nonnull
	public ObservableBoolean getArePreviewFeaturesEnabled() {
		return arePreviewFeaturesEnabled;
	}

	@Nonnull
	public ObservableInteger getTextBlockLineMinimum() {
		return textBlockLineMinimum;
	}

	@Nonnull
	public ObservableObject<CompilerTarget> getForcedCompilerTarget() {
		return forcedCompilerTarget;
	}

	@Nonnull
	public ObservableObject<BytecodeOutputOptions> getBytecodeOutputOptions() {
		return bytecodeOutputOptions;
	}

	@Override
	public int getConfigHash() {
		return hash;
	}

	@Override
	public void setConfigHash(int hash) {
		this.hash = hash;
	}
}
