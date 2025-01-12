package software.coley.recaf.test;

import org.jboss.weld.proxy.WeldClientProxy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.parallel.Isolated;
import software.coley.recaf.Bootstrap;
import software.coley.recaf.Recaf;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.TestEnvironment;

import java.lang.annotation.Annotation;

/**
 * Common base for test classes using the recaf application and ensures child classes
 * are marked as test env via {@link TestEnvironment#isTestEnv()}.
 */
@Isolated
public class TestBase {
	protected static final Recaf recaf;
	protected static WorkspaceManager workspaceManager;

	static {
		Bootstrap.setWeldConsumer(w -> w.addPackage(true, TestConfigSetup.class));
		recaf = Bootstrap.get();

		// Trigger the test-default bean to load
		recaf.get(TestConfigSetup.class).configure();
	}

	@BeforeAll
	public static void setupWorkspaceManager() {
		TestEnvironment.initTestEnv();

		// We'll use this a lot so may as well grab it
		workspaceManager = recaf.get(WorkspaceManager.class);
		workspaceManager.setCurrent(null);
	}

	/**
	 * CDI will wrap beans in proxies and update the backing value when appropriate.
	 * If you want to verify that the initialized value has changed you cannot compare
	 * the proxy instances, you must compare the values they delegate to.
	 *
	 * @param proxy
	 * 		Proxy, assumed to be from {@link Recaf#get(Class)} or {@link Recaf#get(Class, Annotation...)}.
	 * @param <T>
	 * 		Bean type.
	 *
	 * @return Unwrapped value, without the proxy.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T unwrapProxy(T proxy) {
		if (proxy instanceof WeldClientProxy weldProxy) {
			return (T) weldProxy.getMetadata().getContextualInstance();
		}
		return proxy;
	}
}