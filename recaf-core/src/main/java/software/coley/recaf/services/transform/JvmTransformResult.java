package software.coley.recaf.services.transform;

import software.coley.recaf.info.JvmClassInfo;

/**
 * Intermediate holder of transformations of workspace JVM classes.
 *
 * @author Matt Coley
 */
public interface JvmTransformResult extends TransformResult<JvmClassTransformer, JvmClassInfo> {}
