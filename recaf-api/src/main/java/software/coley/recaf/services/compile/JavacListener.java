package software.coley.recaf.services.compile;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * Diagnostic list wrapper.
 *
 * @author Matt Coley
 */
public interface JavacListener extends DiagnosticListener<JavaFileObject> {}
