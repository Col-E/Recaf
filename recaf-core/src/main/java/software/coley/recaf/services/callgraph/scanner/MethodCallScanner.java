package software.coley.recaf.services.callgraph.scanner;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.callgraph.CallSite;

/**
 * Extracts method call sites from class contents.
 *
 * @author Matt Coley
 */
public interface MethodCallScanner {
	/**
	 * @param classInfo
	 * 		Class to check.
	 *
	 * @return {@code true} when this scanner supports the class format.
	 */
	boolean supports(@Nonnull ClassInfo classInfo);

	/**
	 * @param classInfo
	 * 		Class to scan.
	 * @param consumer
	 * 		Call-site consumer.
	 */
	void scan(@Nonnull ClassInfo classInfo, @Nonnull CallSiteConsumer consumer);

	/**
	 * Receives call sites emitted from a scanner.
	 */
	@FunctionalInterface
	interface CallSiteConsumer {
		/**
		 * @param callingMethod
		 * 		Method that owns the call site.
		 * @param callSite
		 * 		Observed call site.
		 */
		void accept(@Nonnull MethodMember callingMethod, @Nonnull CallSite callSite);
	}
}
