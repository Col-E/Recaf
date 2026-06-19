package software.coley.recaf.services.analysis;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.analysis.entry.EntryPoint;
import software.coley.recaf.services.analysis.entry.EntryPointDiscovery;
import software.coley.recaf.services.analysis.entry.EntryPointKind;
import software.coley.recaf.services.analysis.entry.EntryAnalysisService;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static software.coley.recaf.services.analysis.AnalysisTestUtils.manifestElement;
import static software.coley.recaf.services.analysis.AnalysisTestUtils.mockManifest;
import static software.coley.recaf.test.TestClassUtils.createClass;
import static software.coley.recaf.test.TestClassUtils.fromBundle;
import static software.coley.recaf.test.TestClassUtils.fromClasses;
import static software.coley.recaf.test.TestClassUtils.fromFiles;
import static software.coley.recaf.test.TestDexUtils.newAndroidClass;

/**
 * Tests for {@link EntryAnalysisService}.
 */
class EntryAnalysisServiceTest extends TestBase {
	private static EntryAnalysisService service;

	@BeforeAll
	static void setupService() {
		service = recaf.get(EntryAnalysisService.class);
	}

	@Test
	void returnsEmptyWhenNoEntryPointsExist() {
		WorkspaceResource primary = new WorkspaceResourceBuilder().build();
		Workspace workspace = new BasicWorkspace(primary);

		assertEquals(0, service.findEntryPoints(workspace, primary).size());
	}

	@Test
	void findsJvmMainMethodEntryPoint() throws IOException {
		Workspace workspace = fromBundle(fromClasses(HelloWorld.class));

		List<EntryPoint> results = service.findEntryPoints(workspace, workspace.getPrimaryResource());

		assertEquals(1, results.size());
		EntryPoint entry = results.getFirst();
		ClassMemberPathNode path = entry.memberPath();
		assertNotNull(path);
		assertEquals(EntryPointKind.JVM_MAIN_METHOD, entry.kind());
		assertEquals(HelloWorld.class.getName().replace('.', '/'), entry.classPath().getValue().getName());
		assertEquals(entry.memberPath(), entry.targetPath());
		assertEquals("main", path.getValue().getName());
	}

	@Test
	void findsAndroidActivityEntryPoint() {
		BinaryXmlFileInfo manifest = mockManifest(manifestElement("activity", Map.of("name", "foo.MainActivity")));

		FileBundle files = fromFiles(manifest);
		WorkspaceResource primary = new WorkspaceResourceBuilder()
				.withAndroidClassBundles(Map.of("classes.dex", fromClasses(newAndroidClass("foo/MainActivity"))))
				.withFileBundle(files)
				.build();
		Workspace workspace = new BasicWorkspace(primary);

		List<EntryPoint> results = service.findEntryPoints(workspace, primary);

		assertEquals(1, results.size());
		EntryPoint entry = results.getFirst();
		assertEquals(EntryPointKind.ANDROID_ACTIVITY, entry.kind());
		assertEquals("foo/MainActivity", entry.classPath().getValue().getName());
		assertNull(entry.memberPath());
		assertEquals(entry.classPath(), entry.targetPath());
	}

	@Test
	void findsFabricModEntryPoint() {
		JvmClassInfo fabricClass = createClass("test/TestFabricMod", node -> {
			node.interfaces.add("net/fabricmc/api/ModInitializer");

			MethodNode initMethod = new MethodNode(Opcodes.ACC_PUBLIC, "onInitialize", "()V", null, null);
			initMethod.visitCode();
			initMethod.visitInsn(Opcodes.RETURN);
			initMethod.visitEnd();
			node.methods.add(initMethod);
		});

		Workspace workspace = fromBundle(fromClasses(fabricClass));
		List<EntryPoint> results = service.findEntryPoints(workspace, workspace.getPrimaryResource());

		assertEquals(1, results.size(), "Should find exactly one entry point");
		assertEquals(EntryPointKind.MC_FABRIC_MOD_INIT, results.getFirst().kind(), "Should be Fabric entry point");
	}

	@Test
	void findsForgeModEntryPoint() {
		JvmClassInfo forgeModClass = createClass("test/TestForgeMod", node -> {
			node.visitAnnotation("Lnet/minecraftforge/fml/common/Mod;", true).visitEnd();

			MethodNode setupMethod = new MethodNode(Opcodes.ACC_PUBLIC, "setup",
					"(Lnet/minecraftforge/fml/event/lifecycle/FMLCommonSetupEvent;)V", null, null);
			setupMethod.visitCode();
			setupMethod.visitInsn(Opcodes.RETURN);
			setupMethod.visitEnd();
			node.methods.add(setupMethod);
		});

		Workspace workspace = fromBundle(fromClasses(forgeModClass));
		List<EntryPoint> results = service.findEntryPoints(workspace, workspace.getPrimaryResource());

		assertEquals(1, results.size(), "Should find exactly one entry point");
		assertEquals(EntryPointKind.MC_FORGE_MOD_INIT, results.getFirst().kind(), "Should be Forge entry point");
	}

	@Test
	void findsBukkitPluginEntryPoint() {
		JvmClassInfo bukkitPluginClass = createClass("test/TestBukkitPlugin", node -> {
			node.superName = "org/bukkit/plugin/java/JavaPlugin";

			MethodNode onEnableMethod = new MethodNode(Opcodes.ACC_PUBLIC, "onEnable", "()V", null, null);
			onEnableMethod.visitCode();
			onEnableMethod.visitInsn(Opcodes.RETURN);
			onEnableMethod.visitEnd();
			node.methods.add(onEnableMethod);
		});

		Workspace workspace = fromBundle(fromClasses(bukkitPluginClass));
		List<EntryPoint> results = service.findEntryPoints(workspace, workspace.getPrimaryResource());

		assertEquals(1, results.size(), "Should find exactly one entry point");
		assertEquals(EntryPointKind.MC_BUKKIT_PLUGIN_INIT, results.getFirst().kind(), "Should be Bukkit entry point");
	}

	@Test
	void findsVelocityPluginEntryPoint() {
		JvmClassInfo velocityPluginClass = createClass("test/TestVelocityPlugin", node -> {
			node.visitAnnotation("Lcom/velocitypowered/api/plugin/Plugin;", true).visitEnd();

			MethodNode initMethod = new MethodNode(Opcodes.ACC_PUBLIC, "onInit",
					"(Lcom/velocitypowered/api/event/proxy/ProxyInitializeEvent;)V", null, null);
			initMethod.visitCode();
			initMethod.visitInsn(Opcodes.RETURN);
			initMethod.visitEnd();
			node.methods.add(initMethod);
		});

		Workspace workspace = fromBundle(fromClasses(velocityPluginClass));
		List<EntryPoint> results = service.findEntryPoints(workspace, workspace.getPrimaryResource());

		assertEquals(1, results.size(), "Should find exactly one entry point");
		assertEquals(EntryPointKind.MC_VELOCITY_PLUGIN_INIT, results.getFirst().kind(), "Should be Velocity entry point");
	}

	@Test
	void supportsRuntimeDiscoveryRegistration() throws IOException {
		Workspace workspace = fromBundle(fromClasses(HelloWorld.class));
		WorkspaceResource primary = workspace.getPrimaryResource();
		EntryPointKind customKind = new EntryPointKind("test-entry-point", "Test entry point");
		EntryPointDiscovery discovery = new EntryPointDiscovery() {
			@Nonnull
			@Override
			public EntryPointKind kind() {
				return customKind;
			}

			@Override
			public int getPriority() {
				return 5;
			}

			@Nonnull
			@Override
			public List<EntryPoint> findEntryPoints(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource) {
				JvmClassBundle bundle = resource.getJvmClassBundle();
				JvmClassInfo cls = bundle.get(HelloWorld.class.getName().replace('.', '/'));
				if (cls == null)
					return List.of();
				return List.of(new EntryPoint(kind(), PathNodes.classPath(workspace, resource, bundle, cls), null));
			}
		};

		service.registerDiscovery(discovery);
		try {
			assertEquals(1, service.findEntryPoints(workspace, primary).stream()
					.filter(entry -> entry.kind().equals(customKind))
					.count());
		} finally {
			service.unregisterDiscovery(customKind.id());
		}

		assertEquals(0, service.findEntryPoints(workspace, primary).stream()
				.filter(entry -> entry.kind().equals(customKind))
				.count());
	}

	@Test
	void findsMultipleModernJvmMainMethodsInOneClass() {
		JvmClassInfo cls = createClass("demo/MultiMain", node -> {
			node.version = 65;

			MethodNode argsMain = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main",
					"([Ljava/lang/String;)V", null, null);
			argsMain.visitCode();
			argsMain.visitInsn(Opcodes.RETURN);
			argsMain.visitMaxs(0, 1);
			argsMain.visitEnd();
			node.methods.add(argsMain);

			MethodNode noArgMain = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main",
					"()V", null, null);
			noArgMain.visitCode();
			noArgMain.visitInsn(Opcodes.RETURN);
			noArgMain.visitMaxs(0, 0);
			noArgMain.visitEnd();
			node.methods.add(noArgMain);
		});
		Workspace workspace = fromBundle(fromClasses(cls));

		List<EntryPoint> results = service.findEntryPoints(workspace, workspace.getPrimaryResource());

		assertEquals(2, results.size());
		assertEquals(List.of("([Ljava/lang/String;)V", "()V"), results.stream()
				.map(EntryPoint::memberPath)
				.map(path -> path.getValue().getDescriptor())
				.toList());
	}
}
