package software.coley.recaf.services.analysis.antitamper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.services.transform.AndroidClassTransformer;
import software.coley.recaf.services.transform.AndroidTransformResult;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.services.transform.TransformationException;

import java.util.List;

/**
 * Transformation impact result.
 *
 * @param jvm
 * 		Result of the transformation process for JVM contents.
 * @param android
 * 		Result of the transformation process for Android contents.
 *
 * @author Matt Coley
 */
public record TransformerImpactAnalysis(@Nonnull JVM jvm,
                                        @Nonnull Android android) implements AntiReversalAnalysisResult {
	/**
	 * Transformation impact result for JVM contents.
	 *
	 * @param result
	 * 		Result of the transformation process, including any errors that occurred.
	 * 		Can be {@code null} if the transformation process did not complete.
	 * @param transformerClasses
	 * 		List of transformer classes that were applied to the input classes.
	 * @param transformError
	 * 		Exception thrown if the transformation process failed.
	 * 		Will be {@code null} if the transformation process completed successfully.
	 */
	public record JVM(@Nullable JvmTransformResult result,
	                  @Nonnull List<Class<? extends JvmClassTransformer>> transformerClasses,
	                  @Nullable TransformationException transformError) implements AntiReversalAnalysisResult {
	}

	/**
	 * Transformation impact result for Android contents.
	 *
	 * @param result
	 * 		Result of the transformation process, including any errors that occurred.
	 * 		Can be {@code null} if the transformation process did not complete.
	 * @param transformerClasses
	 * 		List of transformer classes that were applied to the input classes.
	 * @param transformError
	 * 		Exception thrown if the transformation process failed.
	 * 		Will be {@code null} if the transformation process completed successfully.
	 */
	public record Android(@Nullable AndroidTransformResult result,
	                      @Nonnull List<Class<? extends AndroidClassTransformer>> transformerClasses,
	                      @Nullable TransformationException transformError) implements AntiReversalAnalysisResult {
	}
}
