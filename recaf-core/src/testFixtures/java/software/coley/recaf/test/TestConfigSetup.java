package software.coley.recaf.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.services.compile.JavacCompilerConfig;

/**
 * Configures default values for the test environment. Some of our values that are best enabled by default
 * for users aren't the best for testing the 'base case' of things during test development.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TestConfigSetup {
	private final JavacCompilerConfig javac;

	@Inject
	public TestConfigSetup(JavacCompilerConfig javac) {
		this.javac = javac;
	}
	
	public void configure() {
		// Do not generate phantoms by default when using the compiler service
		javac.getGeneratePhantoms().setValue(false);
	}
}
