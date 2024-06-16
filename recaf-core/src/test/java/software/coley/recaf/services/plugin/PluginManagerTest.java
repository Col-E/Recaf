package software.coley.recaf.services.plugin;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import software.coley.recaf.plugin.*;
import software.coley.recaf.services.plugin.discovery.DiscoveredPluginSource;
import software.coley.recaf.services.plugin.discovery.PluginDiscoverer;
import software.coley.recaf.services.plugin.zip.ZipPluginLoader;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.util.ZipCreationUtils;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.util.io.ByteSources;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for {@link PluginManager}
 */
public class PluginManagerTest extends TestBase {
	static PluginManager pluginManager;

	@BeforeAll
	static void setup() {
		pluginManager = recaf.get(PluginManager.class);
	}

	@AfterEach
	void verifyCleanSlate() {
		assertEquals(0, pluginManager.getPlugins().size(), "Plugins still loaded after test case");
	}

	@Test
	void testSingleLoadAndUnload() throws IOException {
		String id = "test-plugin";
		String name = id;
		String version = "test-version";
		String author = "test-author";
		String description = "test-description";
		String className = "test.PluginTest";
		DynamicType.Unloaded<Plugin> unloaded = new ByteBuddy()
				.subclass(Plugin.class)
				.name(className)
				.defineMethod("onEnable", void.class, Modifier.PUBLIC)
				.intercept(FixedValue.originType())
				.defineMethod("onDisable", void.class, Modifier.PUBLIC)
				.intercept(FixedValue.originType())
				.annotateType(new PluginInformationRecord(id, name, version, author, new String[0], description)).make();
		byte[] zip = ZipCreationUtils.createZip(Map.of(
				"test/PluginTest.class", unloaded.getBytes(),
				ZipPluginLoader.SERVICE_PATH, className.getBytes(StandardCharsets.UTF_8)
		));

		try {
			// Load the plugin
			ByteSource pluginSource = ByteSources.wrap(zip);
			PluginDiscoverer discoverer = () -> List.of(() -> pluginSource);
			PluginContainer<?> container = pluginManager.loadPlugins(discoverer).iterator().next();

			// Assert the information stuck
			PluginInfo information = container.info();
			assertEquals(id, information.id());
			assertEquals(name, information.name());
			assertEquals(version, information.version());
			assertEquals(author, information.author());
			assertEquals(description, information.description());

			// Assert the plugin is active
			assertEquals(1, pluginManager.getPlugins().size());
			assertSame(container, pluginManager.getPlugin(id));

			// Assert that loading the same plugin twice throws an exception, and does
			// not actually register a 2nd instance of the plugin.
			assertThrows(PluginException.class, () -> pluginManager.loadPlugins(discoverer),
					"Duplicate plugin loading should fail");
			assertEquals(1, pluginManager.getPlugins().size());
			assertSame(container, pluginManager.getPlugin(id));

			// Now unload it
			pluginManager.unloaderFor(id).commit();

			// Assert the plugin is no longer active
			assertEquals(0, pluginManager.getPlugins().size());
			assertNull(pluginManager.getPlugin(id));
		} catch (PluginException ex) {
			fail("Failed to load plugin", ex);
		}
	}

	@Test
	void testDependentChain() throws IOException {
		List<DiscoveredPluginSource> sources = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			String id = "test-plugin-" + i;
			String name = id;
			String version = "test-version";
			String author = "test-author";
			String description = "test-description";
			String className = "test.PluginTest" + i;
			String[] dependencies = i > 0 ? new String[]{"test-plugin-" + (i - 1)} : new String[0];
			DynamicType.Unloaded<Plugin> unloaded = new ByteBuddy()
					.subclass(Plugin.class)
					.name(className)
					.defineMethod("onEnable", void.class, Modifier.PUBLIC)
					.intercept(FixedValue.originType())
					.defineMethod("onDisable", void.class, Modifier.PUBLIC)
					.intercept(FixedValue.originType())
					.annotateType(new PluginInformationRecord(id, name, version, author, dependencies, description)).make();
			byte[] zip = ZipCreationUtils.createZip(Map.of(
					"test/PluginTest" + i + ".class", unloaded.getBytes(),
					ZipPluginLoader.SERVICE_PATH, className.getBytes(StandardCharsets.UTF_8)
			));
			sources.add(() -> ByteSources.wrap(zip));
		}


		try {
			// Load the plugin
			PluginDiscoverer discoverer = () -> sources;
			pluginManager.loadPlugins(discoverer);

			// Assert the plugins are active
			assertEquals(3, pluginManager.getPlugins().size());

			// Now unload them
			pluginManager.unloaderFor("test-plugin-0").commit();

			// Assert the plugins are no longer active
			assertEquals(0, pluginManager.getPlugins().size());
			for (int i = 0; i < 3; i++) {
				String id = "test-plugin-" + i;
				assertNull(pluginManager.getPlugin(id));
			}
		} catch (PluginException ex) {
			fail("Failed to load plugins", ex);
		}
	}

	@Test
	void testPluginWithResourceLoading() throws IOException {
		String className = "DummyPluginBody";
		byte[] classBytes;
		{
			/*
			@PluginInformation(id = "dummy", name = "dummy", version = "1.0")
			public class DummyPluginBody implements Plugin {
				@Override
				public void onEnable() {
					try (var s = getClass().getResourceAsStream("file.txt")) {
						System.out.println(new String(s.readAllBytes()));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				@Override
				public void onDisable() {}
			}
			 */

			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			cw.visit(V22, ACC_PUBLIC | ACC_SUPER, className, null, "java/lang/Object", new String[]{"software/coley/recaf/plugin/Plugin"});
			AnnotationVisitor av = cw.visitAnnotation("Lsoftware/coley/recaf/plugin/PluginInformation;", true);
			av.visit("id", "dummy");
			av.visit("name", "dummy");
			av.visit("version", "1.0");
			av.visitEnd();
			MethodVisitor mv;
			{
				mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
				mv.visitInsn(RETURN);
				mv.visitLabel(new Label());
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}
			{
				mv = cw.visitMethod(ACC_PUBLIC, "onEnable", "()V", null, null);
				mv.visitCode();
				Label label0 = new Label();
				Label label1 = new Label();
				Label label2 = new Label();
				mv.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
				Label label3 = new Label();
				Label label4 = new Label();
				Label label5 = new Label();
				mv.visitTryCatchBlock(label3, label4, label5, "java/lang/Throwable");
				Label label6 = new Label();
				Label label7 = new Label();
				Label label8 = new Label();
				mv.visitTryCatchBlock(label6, label7, label8, "java/lang/Exception");
				mv.visitLabel(label6);
				mv.visitLineNumber(10, label6);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
				mv.visitLdcInsn("file.txt");
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
				mv.visitVarInsn(ASTORE, 1);
				mv.visitLabel(label0);
				mv.visitLineNumber(11, label0);
				mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
				mv.visitTypeInsn(NEW, "java/lang/String");
				mv.visitInsn(DUP);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "readAllBytes", "()[B", false);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V", false);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
				mv.visitLabel(label1);
				mv.visitLineNumber(12, label1);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitJumpInsn(IFNULL, label7);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);
				mv.visitJumpInsn(GOTO, label7);
				mv.visitLabel(label2);
				mv.visitLineNumber(10, label2);
				mv.visitVarInsn(ASTORE, 2);
				mv.visitVarInsn(ALOAD, 1);
				Label label9 = new Label();
				mv.visitJumpInsn(IFNULL, label9);
				mv.visitLabel(label3);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "close", "()V", false);
				mv.visitLabel(label4);
				mv.visitJumpInsn(GOTO, label9);
				mv.visitLabel(label5);
				mv.visitVarInsn(ASTORE, 3);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitVarInsn(ALOAD, 3);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V", false);
				mv.visitLabel(label9);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitInsn(ATHROW);
				mv.visitLabel(label7);
				mv.visitLineNumber(14, label7);
				Label label10 = new Label();
				mv.visitJumpInsn(GOTO, label10);
				mv.visitLabel(label8);
				mv.visitLineNumber(12, label8);
				mv.visitVarInsn(ASTORE, 1);
				Label label11 = new Label();
				mv.visitLabel(label11);
				mv.visitLineNumber(13, label11);
				mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
				mv.visitInsn(DUP);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
				mv.visitInsn(ATHROW);
				mv.visitLabel(label10);
				mv.visitLineNumber(15, label10);
				mv.visitInsn(RETURN);
				Label label12 = new Label();
				mv.visitLabel(label12);
				mv.visitLocalVariable("s", "Ljava/io/InputStream;", null, label0, label7, 1);
				mv.visitLocalVariable("e", "Ljava/lang/Exception;", null, label11, label10, 1);
				mv.visitLocalVariable("this", "Lsoftware/coley/recaf/services/plugin/DummyPluginBody;", null, label6, label12, 0);
				mv.visitMaxs(4, 4);
				mv.visitEnd();
			}
			{
				mv = cw.visitMethod(ACC_PUBLIC, "onDisable", "()V", null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitInsn(RETURN);
				mv.visitLabel(new Label());
				mv.visitMaxs(0, 1);
				mv.visitEnd();
			}
			cw.visitEnd();
			classBytes = cw.toByteArray();
		}
		byte[] zip = ZipCreationUtils.createZip(Map.of(
				className + ".class", classBytes,
				ZipPluginLoader.SERVICE_PATH, className.getBytes(StandardCharsets.UTF_8),
				"file.txt", "Dummy plugin says 'hello world'!".getBytes(StandardCharsets.UTF_8)
		));

		try {
			// Load the plugin
			ByteSource pluginSource = ByteSources.wrap(zip);
			PluginDiscoverer discoverer = () -> List.of(() -> pluginSource);
			PluginContainer<?> container = pluginManager.loadPlugins(discoverer).iterator().next();

			// Now unload it
			pluginManager.unloaderFor("dummy").commit();
		} catch (PluginException ex) {
			fail("Failed to load plugin", ex);
		}
	}

	@SuppressWarnings("all")
	private record PluginInformationRecord(String id, String name, String version, String author, String[] dependencies,
	                                       String description) implements PluginInformation {
		@Override
		public Class<? extends Annotation> annotationType() {
			return PluginInformation.class;
		}

		@Override
		public String[] softDependencies() {
			return new String[0];
		}
	}
}
