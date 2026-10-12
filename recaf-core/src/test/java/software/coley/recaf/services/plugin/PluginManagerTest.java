package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import software.coley.recaf.cdi.InitializationEvent;
import software.coley.recaf.cdi.UiInitializationEvent;
import software.coley.recaf.plugin.Plugin;
import software.coley.recaf.services.compile.CompileMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PluginManager}.
 */
public class PluginManagerTest extends PluginTestBase {
	@Test
	void testLoadedPluginExposesInformationAndUnloads() throws Exception {
		String className = "test/PluginTest";
		PluginInfo expected = new PluginInfo("test-plugin", "test plugin", "test-version", "test-author", "test-description", Set.of(), Set.of());
		byte[] zip = pluginArchive(className, pluginSource(className, expected, "", ""));

		// Load the plugin and verify that it is registered and exposes the expected information.
		PluginContainer<?> container = pluginManager.loadPlugins(discoverArchives(zip)).iterator().next();
		assertEquals(expected, container.info());
		assertSame(container, pluginManager.getPlugin(expected.id()));
		assertEquals(1, pluginManager.getPlugins().size());

		// Unload the plugin and verify that it is no longer registered.
		pluginManager.unloaderFor(expected.id()).commit();
		assertNull(pluginManager.getPlugin(expected.id()));
		assertTrue(pluginManager.getPlugins().isEmpty());
	}

	@Test
	void testLoadingAlreadyLoadedPluginIsRejected() throws Exception {
		String id = "duplicate-load-plugin";
		String className = "test/DuplicateLoadPlugin";
		byte[] zip = pluginArchive(className, pluginSource(className, pluginInfo(id), "", ""));
		PluginContainer<?> loaded = pluginManager.loadPlugins(discoverArchives(zip)).iterator().next();

		// Attempting to load the same plugin again should fail, and the original plugin should remain loaded.
		assertThrows(PluginException.class, () -> pluginManager.loadPlugins(discoverArchives(zip)), "Loading an already loaded plugin should fail");
		assertSame(loaded, pluginManager.getPlugin(id), "A rejected load should keep the original plugin");
		assertEquals(1, pluginManager.getPlugins().size());

		pluginManager.unloaderFor(id).commit();
	}

	@Test
	void testUnloadingRootUnloadsWholeChain() throws Exception {
		// Each plugin depends on the previous one: 0 <- 1 <- 2
		byte[] zip0 = pluginArchive("test/PluginTest0",
				pluginSource("test/PluginTest0", pluginInfo("test-plugin-0"), "", ""));
		byte[] zip1 = pluginArchive("test/PluginTest1",
				pluginSource("test/PluginTest1", pluginInfo("test-plugin-1", "test-plugin-0"), "", ""));
		byte[] zip2 = pluginArchive("test/PluginTest2",
				pluginSource("test/PluginTest2", pluginInfo("test-plugin-2", "test-plugin-1"), "", ""));

		// Load all three plugins and verify that they are registered.
		pluginManager.loadPlugins(discoverArchives(zip0, zip1, zip2));
		assertEquals(3, pluginManager.getPlugins().size());

		// Unloading the root plugin should unload all dependants.
		pluginManager.unloaderFor("test-plugin-0").commit();
		assertTrue(pluginManager.getPlugins().isEmpty(), "Dependants should unload along with their dependency");
	}

	@Test
	void testDiamondDependencyUnloadsEachPluginOnce() throws Exception {
		// A is shared by B and C, and D depends on both B and C.
		byte[] zipA = countingPluginArchive("test/DiamondA", "diamond-a");
		byte[] zipB = countingPluginArchive("test/DiamondB", "diamond-b", "diamond-a");
		byte[] zipC = countingPluginArchive("test/DiamondC", "diamond-c", "diamond-a");
		byte[] zipD = countingPluginArchive("test/DiamondD", "diamond-d", "diamond-b", "diamond-c");
		pluginManager.loadPlugins(discoverArchives(zipA, zipB, zipC, zipD));
		assertEquals(4, pluginManager.getPlugins().size());

		// Unloading the shared root must unload every dependant, and must not run any lifecycle method twice.
		pluginManager.unloaderFor("diamond-a").commit();
		assertTrue(pluginManager.getPlugins().isEmpty());
		for (String id : List.of("diamond-a", "diamond-b", "diamond-c", "diamond-d")) {
			assertEquals(1, counterValue(enableKey(id)), id + " should be enabled once");
			assertEquals(1, counterValue(disableKey(id)), id + " should be disabled once");
		}
	}

	@Test
	void testCircularDependencyFailsDuringEnable() throws Exception {
		byte[] zipA = pluginArchive("test/CircularA",
				pluginSource("test/CircularA", pluginInfo("circular-a", "circular-b"), "", ""));
		byte[] zipB = pluginArchive("test/CircularB",
				pluginSource("test/CircularB", pluginInfo("circular-b", "circular-a"), "", ""));

		// The circular dependency should be detected during enablement, and the plugin manager should roll back the load.
		assertThrows(PluginException.class, () -> pluginManager.loadPlugins(discoverArchives(zipA, zipB)),
				"Circular plugin dependencies should fail during enablement");
		assertTrue(pluginManager.getPlugins().isEmpty(),
				"A failed circular load should not leave plugins loaded");
	}

	@Test
	void testBatchLoadedPluginCanReferenceDependencyClasses() throws Exception {
		String apiId = "api-plugin";
		String implId = "impl-plugin";
		String apiEntry = "test/ApiPlugin";
		String implEntry = "test/ImplPlugin";
		String key = property("batch.dependency");

		// The impl archive only contains its own entry point. It is compiled against the API classes, so it can only
		// enable if the API archive's classes are visible to it when both are loaded in the same batch.
		CompileMap apiClasses = compile(apiEntry, Map.of(
				apiEntry, pluginSource(apiEntry, pluginInfo(apiId), "", ""),
				"test/ApiType", javaSource("test/ApiType", """
						public class ApiType {
							public static String message() {
								return "dependency-ok";
							}
						}
						""")), Map.of());

		// The plugin writes to the key when it is enabled.
		String implSource = pluginSource(implEntry, pluginInfo(implId, apiId),
				"System.setProperty(\"" + key + "\", ApiType.message());", "");
		byte[] apiZip = archive(apiEntry, apiClasses, Map.of());
		byte[] implZip = archive(implEntry, compile(implEntry, Map.of(implEntry, implSource), apiClasses), Map.of());

		// Load both plugins in the same batch and verify that the impl plugin can see the API classes during enablement.
		Collection<PluginContainer<?>> containers = pluginManager.loadPlugins(discoverArchives(apiZip, implZip));
		assertEquals(2, containers.size());
		assertEquals("dependency-ok", System.getProperty(key), "Dependent plugin should see the API classes while enabling");

		// Unloading the dependency also unloads its dependant.
		pluginManager.unloaderFor(apiId).commit();
		assertFalse(pluginManager.isPluginLoaded(apiId));
		assertFalse(pluginManager.isPluginLoaded(implId));
	}

	@Test
	void testPluginsWithSameClassNameUseSeparateLoaders() throws Exception {
		String className = "test/DuplicateNamePlugin";
		String firstId = "duplicate-name-first";
		String secondId = "duplicate-name-second";
		byte[] firstZip = pluginArchive(className, pluginSource(className, pluginInfo(firstId), "", ""));
		byte[] secondZip = pluginArchive(className, pluginSource(className, pluginInfo(secondId), "", ""));

		// Both plugins define the same class name,
		// but they are loaded in the same batch so they must be isolated from each other.
		assertEquals(2, pluginManager.loadPlugins(discoverArchives(firstZip, secondZip)).size());
		Plugin first = pluginManager.getPlugin(firstId).plugin();
		Plugin second = pluginManager.getPlugin(secondId).plugin();
		assertEquals(first.getClass().getName(), second.getClass().getName(), "Both plugins should define the same class");
		assertNotSame(first.getClass().getClassLoader(), second.getClass().getClassLoader(),
				"Same-named plugin classes should come from separate class loaders");

		pluginManager.unloaderFor(firstId).commit();
		pluginManager.unloaderFor(secondId).commit();
	}

	@Test
	void testPluginsWithSameLocalBeanNameUseIndependentContainers() throws Exception {
		String firstId = "local-bean-first";
		String secondId = "local-bean-second";
		String firstEntry = "test/FirstLocalBeanPlugin";
		String secondEntry = "test/SecondLocalBeanPlugin";
		String firstKey = property("local.first");
		String secondKey = property("local.second");
		byte[] firstZip = pluginArchive(firstEntry, Map.of(
				firstEntry, injectedValuePlugin(firstEntry, pluginInfo(firstId), firstKey),
				"shared/LocalValue", localValueSource("first")), Map.of());
		byte[] secondZip = pluginArchive(secondEntry, Map.of(
				secondEntry, injectedValuePlugin(secondEntry, pluginInfo(secondId), secondKey),
				"shared/LocalValue", localValueSource("second")), Map.of());

		// Discovery order is reversed so the result cannot depend on which plugin happens to be constructed first.
		pluginManager.loadPlugins(discoverArchives(secondZip, firstZip));
		assertEquals("first", System.getProperty(firstKey));
		assertEquals("second", System.getProperty(secondKey));

		pluginManager.unloaderFor(firstId).commit();
		pluginManager.unloaderFor(secondId).commit();
	}

	@Test
	void testUndefinableArchiveClassFailsLoadAndReleasesRuntime() throws Exception {
		String id = "plugin-cdi-missing-class";
		String className = "test/MissingClassPlugin";

		// The zero-length class is listed for CDI discovery but cannot be defined.
		// The partially built CDI container must be shut down, which the base class verifies after this test.
		byte[] zip = pluginArchive(className, Map.of(className, pluginSource(className, pluginInfo(id), "", "")),
				Map.of("broken/Missing.class", new byte[0]));

		// Loading the junk class should fail.
		PluginException failure = assertThrows(PluginException.class,
				() -> pluginManager.loadPlugins(discoverArchives(zip)));
		assertTrue(failure.getMessage().contains("broken.Missing"),
				() -> "Failure should name the class that could not be defined: " + failure.getMessage());
	}

	@Test
	void testPluginCdiInjectionBridgesRecafAndDiscoversLocalProducer() throws Exception {
		// This test looks scary because there's a lot of plugin code defined here but the gist is:
		// - The plugin has a local bean and a local producer.
		// - The plugin injects Recaf's workspace manager and current workspace.
		// - The plugin injects the local bean and the local producer's bean.
		// - The plugin writes to system properties to verify that all of the above injections worked.
		String id = "plugin-cdi-injection";
		String entry = "test/RecafBridgePlugin";
		String key = property("cdi.injection");
		Map<String, String> sources = Map.of(
				entry, javaSource(entry, """
						import jakarta.enterprise.inject.spi.BeanManager;
						import jakarta.inject.Inject;
						import software.coley.recaf.plugin.Plugin;
						import software.coley.recaf.plugin.PluginInformation;
						import software.coley.recaf.services.workspace.WorkspaceManager;
						import software.coley.recaf.workspace.model.Workspace;
						%s
						public class RecafBridgePlugin implements Plugin {
							private final LocalService service;
							private final LocalProduct product;
							private final WorkspaceManager workspaceManager;
							private final Workspace workspace;
							private final BeanManager beanManager;
						
							@Inject
							public RecafBridgePlugin(LocalService service, LocalProduct product,
							                         WorkspaceManager workspaceManager, Workspace workspace,
							                         BeanManager beanManager) {
								this.service = service;
								this.product = product;
								this.workspaceManager = workspaceManager;
								this.workspace = workspace;
								this.beanManager = beanManager;
							}
						
							@Override
							public void onEnable() {
								System.setProperty("%2$s.service", service.value());
								System.setProperty("%2$s.product", product.value());
								System.setProperty("%2$s.workspace", String.valueOf(
										workspace != null && workspaceManager != null && workspaceManager.getCurrent() == workspace));
								System.setProperty("%2$s.localBeans", String.valueOf(
										beanManager.getBeans(LocalService.class).size() == 1));
							}
						
							@Override
							public void onDisable() {}
						}
						""".formatted(informationAnnotation(pluginInfo(id)), key)),
				"test/LocalService", javaSource("test/LocalService", """
						import jakarta.enterprise.context.ApplicationScoped;
						@ApplicationScoped
						public class LocalService {
							public String value() {
								return "local";
							}
						}
						"""),
				"test/LocalProduct", javaSource("test/LocalProduct", """
						public class LocalProduct {
							private final String value;
						
							public LocalProduct(String value) {
								this.value = value;
							}
						
							public String value() {
								return value;
							}
						}
						"""),
				"test/LocalProductFactory", javaSource("test/LocalProductFactory", """
						import jakarta.enterprise.context.ApplicationScoped;
						import jakarta.enterprise.context.Dependent;
						import jakarta.enterprise.inject.Produces;
						@ApplicationScoped
						public class LocalProductFactory {
							@Produces
							@Dependent
							public LocalProduct create() {
								return new LocalProduct("produced");
							}
						}
						"""));

		// All properties should be written to, verifying that the plugin can inject core container beans and its own local beans.
		pluginManager.loadPlugins(discoverArchives(pluginArchive(entry, sources, Map.of())));
		assertEquals("local", System.getProperty(key + ".service"), "Plugin should inject its own local bean");
		assertEquals("produced", System.getProperty(key + ".product"), "Plugin should inject a local producer's bean");
		assertEquals("true", System.getProperty(key + ".workspace"), "Plugin should inject Recaf's current workspace");
		assertEquals("true", System.getProperty(key + ".localBeans"), "Plugin container should discover its own local bean");

		pluginManager.unloaderFor(id).commit();
	}

	@Test
	void testPluginLocalScopedBeansAreDestroyedOnUnload() throws Exception {
		String id = "plugin-local-scope-unload";
		String appKey = property("scoped.app.unload");
		String dependentKey = property("scoped.dependent.unload");
		byte[] zip = scopedLifecycleArchive("test/ScopedUnloadPlugin", id, appKey, dependentKey, false);

		// Load the plugin and verify that the application-scoped
		// and dependent-scoped beans are not destroyed while the plugin is loaded.
		pluginManager.loadPlugins(discoverArchives(zip));
		assertNull(System.getProperty(appKey), "Application-scoped bean should not be destroyed while loaded");
		assertNull(System.getProperty(dependentKey), "Dependent bean should not be destroyed while loaded");

		// Unload the plugin and verify that the application-scoped and dependent-scoped beans are destroyed.
		// The property values are written to by the @PreDestroy methods of the beans.
		pluginManager.unloaderFor(id).commit();
		assertEquals("app-released", System.getProperty(appKey));
		assertEquals("dependent-released", System.getProperty(dependentKey));
	}

	@Test
	void testPluginLocalScopedBeansAreDestroyedOnFailedEnableRollback() throws Exception {
		String id = "plugin-local-scope-rollback";
		String appKey = property("scoped.app.rollback");
		String dependentKey = property("scoped.dependent.rollback");
		byte[] zip = scopedLifecycleArchive("test/ScopedRollbackPlugin", id, appKey, dependentKey, true);

		// The plugin fails to enable, which should roll back the load and destroy the application-scoped and dependent-scoped beans.
		assertThrows(PluginException.class, () -> pluginManager.loadPlugins(discoverArchives(zip)));
		assertEquals("app-released", System.getProperty(appKey));
		assertEquals("dependent-released", System.getProperty(dependentKey));
		assertTrue(pluginManager.getPlugins().isEmpty());
	}

	@Test
	void testPluginUnloadReleasesCdiInstance() throws Exception {
		String id = "release-plugin";
		String key = property("release.unload");
		byte[] zip = lifecyclePluginArchive("test/ReleasePlugin", id, key, "");

		// Load the plugin, unload it, and verify that the @PreDestroy method is called on unload.
		pluginManager.loadPlugins(discoverArchives(zip));
		assertNull(System.getProperty(key), "@PreDestroy should not run before unload");
		pluginManager.unloaderFor(id).commit();
		assertEquals("released", System.getProperty(key), "@PreDestroy should run on unload");
	}

	@Test
	void testFailedEnableReleasesCdiInstance() throws Exception {
		String id = "release-failed-plugin";
		String key = property("release.rollback");
		byte[] zip = lifecyclePluginArchive("test/ReleaseFailedPlugin", id, key,
				"throw new IllegalStateException(\"Intentional enable failure\");");

		// Plugin throws on enable, which should roll back the load and call @PreDestroy on the plugin instance.
		assertNull(System.getProperty(key));
		assertThrows(PluginException.class, () -> pluginManager.loadPlugins(discoverArchives(zip)));
		assertEquals("released", System.getProperty(key), "@PreDestroy should run when enabling rolls back");
		assertTrue(pluginManager.getPlugins().isEmpty());
	}

	@Test
	void testFailedEnableCallsDisableDuringRollback() throws Exception {
		String id = "failing-enable-plugin";
		String className = "test/FailingEnablePlugin";
		byte[] zip = pluginArchive(className, pluginSource(className, pluginInfo(id),
				counterIncrement(enableKey(id)) + "\nthrow new IllegalStateException(\"Intentional enable failure\");",
				counterIncrement(disableKey(id))));

		// Plugin throws on enable, which should roll back the load and call onDisable on the plugin instance.
		assertThrows(PluginException.class, () -> pluginManager.loadPlugins(discoverArchives(zip)), "Plugin loading should fail when onEnable throws");
		assertEquals(1, counterValue(enableKey(id)), "onEnable should run once");
		assertEquals(1, counterValue(disableKey(id)), "onDisable should run once during rollback");
		assertTrue(pluginManager.getPlugins().isEmpty(), "Failed plugin should not remain loaded");
	}

	@Test
	void testStartupEventsForwardAndIsolatePluginObserverFailures() throws Exception {
		String initOrder = property("startup.init.order");
		String uiOrder = property("startup.ui.order");

		// Plugin 'b' is discovered first, but events are forwarded in plugin ID order, so 'a' is always notified first.
		byte[] firstZip = startupObserverArchive("test/FirstStartupPlugin", "a-startup-observer", "a", true);
		byte[] secondZip = startupObserverArchive("test/SecondStartupPlugin", "b-startup-observer", "b", false);
		pluginManager.loadPlugins(discoverArchives(secondZip, firstZip));

		recaf.getContainer().getBeanContainer().getEvent().fire(new InitializationEvent());
		recaf.getContainer().getBeanContainer().getEvent().fire(new UiInitializationEvent());

		// Plugin 'a' throws from its init observer, which must not stop plugin 'b' from being notified.
		assertEquals("ab", System.getProperty(initOrder));
		assertEquals("ab", System.getProperty(uiOrder));

		pluginManager.unloaderFor("a-startup-observer").commit();
		pluginManager.unloaderFor("b-startup-observer").commit();
	}

	@Test
	void testPluginResourcesAreVisibleThroughGetResources() throws Exception {
		// The plugin will attempt to enumerate the resource through ClassLoader#getResources,
		// and validate that it can see the resource and that the content is as expected.
		String id = "resource-enumeration-plugin";
		String className = "test/ResourceEnumerationPlugin";
		String resourceName = "META-INF/services/example.Service";
		String resourceContent = "example.Implementation";
		String key = property("resources.content");
		String onEnable = """
				try {
					java.util.Enumeration<java.net.URL> resources = getClass().getClassLoader().getResources("%s");
					StringBuilder content = new StringBuilder();
					while (resources.hasMoreElements()) {
						try (var in = resources.nextElement().openStream()) {
							content.append(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
						}
					}
					System.setProperty("%s", content.toString());
				} catch (java.io.IOException ex) {
					throw new java.io.UncheckedIOException(ex);
				}
				""".formatted(resourceName, key);
		byte[] zip = pluginArchive(className, Map.of(className, pluginSource(className, pluginInfo(id), onEnable, "")),
				Map.of(resourceName, resourceContent.getBytes(StandardCharsets.UTF_8)));

		// It should load/run without issue.
		pluginManager.loadPlugins(discoverArchives(zip));
		assertEquals(resourceContent, System.getProperty(key), "Plugin should see exactly its own resource through ClassLoader#getResources");
		pluginManager.unloaderFor(id).commit();
	}

	@Test
	void testPluginResourceUrlsIncludePluginId() throws Exception {
		// The plugin will attempt to get the resource through ClassLoader#getResource,
		// and validate that the URL contains the plugin ID,
		// which indicates that the resource is scoped to the plugin's classloader.
		String id = "scoped-resource-plugin";
		String className = "test/ScopedResourcePlugin";
		String resourceName = "plugin-resource.txt";
		String key = property("resources.url");
		String onEnable = "System.setProperty(\"%s\", String.valueOf(getClass().getClassLoader().getResource(\"%s\")));"
				.formatted(key, resourceName);
		byte[] zip = pluginArchive(className, Map.of(className, pluginSource(className, pluginInfo(id), onEnable, "")),
				Map.of(resourceName, "scoped".getBytes(StandardCharsets.UTF_8)));

		pluginManager.loadPlugins(discoverArchives(zip));
		String url = System.getProperty(key);
		assertTrue(url.contains(id), () -> "Resource URL should be scoped to the plugin ID, but was: " + url);
		pluginManager.unloaderFor(id).commit();
	}

	@Nonnull
	private static byte[] countingPluginArchive(@Nonnull String className, @Nonnull String id,
	                                            @Nonnull String... dependencies) throws IOException {
		return pluginArchive(className, pluginSource(className, pluginInfo(id, dependencies),
				counterIncrement(enableKey(id)), counterIncrement(disableKey(id))));
	}

	@Nonnull
	private static String enableKey(@Nonnull String id) {
		return property("enable." + id);
	}

	@Nonnull
	private static String disableKey(@Nonnull String id) {
		return property("disable." + id);
	}

	@Nonnull
	private static String injectedValuePlugin(@Nonnull String className, @Nonnull PluginInfo info,
	                                          @Nonnull String key) {
		String name = simpleName(className);
		return javaSource(className, """
				import jakarta.inject.Inject;
				import shared.LocalValue;
				import software.coley.recaf.plugin.Plugin;
				import software.coley.recaf.plugin.PluginInformation;
				%s
				public class %s implements Plugin {
					private final LocalValue value;
				
					@Inject
					public %s(LocalValue value) {
						this.value = value;
					}
				
					@Override
					public void onEnable() {
						System.setProperty("%s", value.value());
					}
				
					@Override
					public void onDisable() {}
				}
				""".formatted(informationAnnotation(info), name, name, key));
	}

	@Nonnull
	private static String localValueSource(@Nonnull String value) {
		return javaSource("shared/LocalValue", """
				import jakarta.enterprise.context.ApplicationScoped;
				@ApplicationScoped
				public class LocalValue {
					public String value() {
						return "%s";
					}
				}
				""".formatted(value));
	}

	@Nonnull
	private static byte[] lifecyclePluginArchive(@Nonnull String className, @Nonnull String id,
	                                             @Nonnull String key, @Nonnull String onEnable) throws IOException {
		String source = javaSource(className, """
				import jakarta.annotation.PreDestroy;
				import software.coley.recaf.plugin.Plugin;
				import software.coley.recaf.plugin.PluginInformation;
				%s
				public class %s implements Plugin {
					@Override
					public void onEnable() {
						%s
					}
				
					@Override
					public void onDisable() {}
				
					@PreDestroy
					public void release() {
						System.setProperty("%s", "released");
					}
				}
				""".formatted(informationAnnotation(pluginInfo(id)), simpleName(className), onEnable, key));
		return pluginArchive(className, source);
	}

	@Nonnull
	private static byte[] scopedLifecycleArchive(@Nonnull String pluginClass, @Nonnull String id,
	                                             @Nonnull String appKey, @Nonnull String dependentKey,
	                                             boolean failEnable) throws IOException {
		String applicationBean = "test/ScopedApplicationBean";
		String dependentBean = "test/ScopedDependentBean";
		String failure = failEnable ? "throw new IllegalStateException(\"Intentional plugin enable failure\");" : "";
		String name = simpleName(pluginClass);
		String entrySource = javaSource(pluginClass, """
				import jakarta.inject.Inject;
				import software.coley.recaf.plugin.Plugin;
				import software.coley.recaf.plugin.PluginInformation;
				%s
				public class %s implements Plugin {
					private final ScopedApplicationBean applicationBean;
					private final ScopedDependentBean dependentBean;
				
					@Inject
					public %s(ScopedApplicationBean applicationBean, ScopedDependentBean dependentBean) {
						this.applicationBean = applicationBean;
						this.dependentBean = dependentBean;
					}
				
					@Override
					public void onEnable() {
						applicationBean.value();
						dependentBean.value();
						%s
					}
				
					@Override
					public void onDisable() {}
				}
				""".formatted(informationAnnotation(pluginInfo(id)), name, name, failure));
		String applicationBeanSource = javaSource(applicationBean, """
				import jakarta.annotation.PreDestroy;
				import jakarta.enterprise.context.ApplicationScoped;
				@ApplicationScoped
				public class ScopedApplicationBean {
					public String value() {
						return "application";
					}
				
					@PreDestroy
					public void release() {
						System.setProperty("%s", "app-released");
					}
				}
				""".formatted(appKey));
		String dependentBeanSource = javaSource(dependentBean, """
				import jakarta.annotation.PreDestroy;
				import jakarta.enterprise.context.Dependent;
				@Dependent
				public class ScopedDependentBean {
					public String value() {
						return "dependent";
					}
				
					@PreDestroy
					public void release() {
						System.setProperty("%s", "dependent-released");
					}
				}
				""".formatted(dependentKey));
		return pluginArchive(pluginClass, Map.of(
				pluginClass, entrySource,
				applicationBean, applicationBeanSource,
				dependentBean, dependentBeanSource), Map.of());
	}

	@Nonnull
	private static byte[] startupObserverArchive(@Nonnull String pluginClass, @Nonnull String id,
	                                             @Nonnull String suffix, boolean throwOnInit) throws IOException {
		String listenerClass = "test/StartupListener";
		String failure = throwOnInit ? "throw new IllegalStateException(\"Intentional startup observer failure\");" : "";
		String listenerSource = javaSource(listenerClass, """
				import jakarta.enterprise.context.ApplicationScoped;
				import jakarta.enterprise.event.Observes;
				import software.coley.recaf.cdi.InitializationEvent;
				import software.coley.recaf.cdi.UiInitializationEvent;
				@ApplicationScoped
				public class StartupListener {
					private static void append(String key, String value) {
						System.setProperty(key, System.getProperty(key, "") + value);
					}
				
					public void onInit(@Observes InitializationEvent event) {
						append("%1$s", "%3$s");
						%2$s
					}
				
					public void onUiInit(@Observes UiInitializationEvent event) {
						append("%4$s", "%3$s");
					}
				}
				""".formatted(property("startup.init.order"), failure, suffix, property("startup.ui.order")));
		return pluginArchive(pluginClass, Map.of(
				pluginClass, pluginSource(pluginClass, pluginInfo(id), "", ""),
				listenerClass, listenerSource), Map.of());
	}
}
