package software.coley.recaf.services.analysis.antitamper;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.deobfuscation.transform.generic.CycleClassRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.DuplicateAnnotationRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalAnnotationRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalSignatureRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.LongAnnotationRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.LongExceptionRemovingTransformer;
import software.coley.recaf.services.transform.AndroidClassTransformer;
import software.coley.recaf.services.transform.AndroidTransformResult;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.services.transform.TransformationApplierService;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;

/**
 * Analyzer for built-in anti-reversal transformation opportunities.
 * <p>
 * This isn't intended to be a comprehensive <i>"deobfuscate everything"</i> set, but just what is needed
 * to prevent common anti-reversal techniques from killing the decompilation process.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TransformerImpactAntiReversalAnalyzer implements AntiReversalAnalyzer<TransformerImpactAnalysis> {
	public static final String SERVICE_ID = "transformer-impact-analysis";
	private static final Logger logger = Logging.get(TransformerImpactAntiReversalAnalyzer.class);
	private final TransformationApplierService transformationApplierService;

	@Inject
	public TransformerImpactAntiReversalAnalyzer(@Nonnull TransformationApplierService transformationApplierService) {
		this.transformationApplierService = transformationApplierService;
	}

	/**
	 * @return Default anti-reversal transformers for JVM usage.
	 */
	@Nonnull
	public List<Class<? extends JvmClassTransformer>> getJvmTransformerClasses() {
		return List.of(
				// Remove classes with cycles in inheritance
				CycleClassRemovingTransformer.class,

				// Remove illegally formed annotations
				IllegalAnnotationRemovingTransformer.class,

				// Remove illegally formed generic signatures
				IllegalSignatureRemovingTransformer.class,

				// Remove bogus duplicate annotations
				DuplicateAnnotationRemovingTransformer.class,

				// Remove annoying long annotations
				LongAnnotationRemovingTransformer.class,

				// Remove annoying long exceptions
				LongExceptionRemovingTransformer.class
		);
	}

	/**
	 * @return Default anti-reversal transformers for Android usage.
	 */
	@Nonnull
	public List<Class<? extends AndroidClassTransformer>> getAndroidTransformerClasses() {
		return List.of();
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public Class<TransformerImpactAnalysis> getResultType() {
		return TransformerImpactAnalysis.class;
	}

	@Nonnull
	@Override
	public TransformerImpactAnalysis analyze(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource) {
		List<Class<? extends JvmClassTransformer>> jvmTransformers = getJvmTransformerClasses();
		List<Class<? extends AndroidClassTransformer>> androidTransformers = getAndroidTransformerClasses();

		// TODO: Android transformers not yet implemented.
		JvmTransformResult jvmResult;
		AndroidTransformResult androidResult = null;
		try {
			jvmResult = transformationApplierService.newApplier(workspace).transformJvm(jvmTransformers);
		} catch (TransformationException ex) {
			logger.error("Failed applying JVM anti-reversal transformers", ex);
			jvmResult = null;
		}

		TransformerImpactAnalysis.JVM jvm = new TransformerImpactAnalysis.JVM(jvmResult, jvmTransformers, null);
		TransformerImpactAnalysis.Android android = new TransformerImpactAnalysis.Android(androidResult, androidTransformers, null);
		return new TransformerImpactAnalysis(jvm, android);
	}
}
