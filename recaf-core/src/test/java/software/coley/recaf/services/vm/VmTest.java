package software.coley.recaf.services.vm;

import dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller;
import dev.xdark.ssvm.invoke.Argument;
import dev.xdark.ssvm.invoke.InvocationUtil;
import dev.xdark.ssvm.mirror.member.JavaMethod;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.dummy.ArrayStuff;
import software.coley.recaf.test.dummy.unoptimized.CountTo100;
import software.coley.recaf.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;
import static software.coley.recaf.test.TestClassUtils.fromBundle;
import static software.coley.recaf.test.TestClassUtils.fromClasses;

/**
 * Tests for {@link CommonVirtualService} / {@link VirtualInvoker} / {@link VirtualOptimizer}.
 */
public class VmTest extends TestBase {
	private static CommonVirtualService commonVirtualService;
	private static VirtualInvoker invoker;
	private static VirtualOptimizer optimizer;
	private static Workspace workspace;
	private static SupplyingClassLoaderInstaller.Helper helper;

	@BeforeAll
	static void setup() throws IOException, VmUnavailableException {
		// Configure file system pass-through so that we can see console output from the VM
		CommonVirtualServiceConfig config = recaf.get(CommonVirtualServiceConfig.class);
		config.useHostFileManager().setValue(true);

		// Initialize basic workspace
		workspace = fromBundle(fromClasses(
				CountTo100.class,
				ArrayStuff.class
		));
		recaf.get(WorkspaceManager.class).setCurrent(workspace);

		// Initialize VM services
		commonVirtualService = recaf.get(CommonVirtualService.class);
		invoker = recaf.get(VirtualInvoker.class);
		optimizer = recaf.get(VirtualOptimizer.class);
		helper = commonVirtualService.getCurrentWorkspaceHelper();
		commonVirtualService.getSharedVm().setOutStreamConsumer(System.out::println);
		commonVirtualService.getSharedVm().setErrStreamConsumer(System.err::println);
	}

	@Nested
	class Invoker {
		/**
		 * @throws VmUnavailableException
		 * 		When common VM isn't ready
		 * @see #testBasicService() Same logic but via {@link VirtualInvoker}
		 */
		@Test
		void testBasicRaw() throws VmUnavailableException {
			CommonVirtualService.CommonVM vm = commonVirtualService.getSharedVm();
			InvocationUtil invoker = vm.getInvocationUtil();

			try {
				InstanceClass targetClass = helper.loadClass(CountTo100.class.getName());
				JavaMethod targetMethod = targetClass.getMethod("run_0", "()V");
				invoker.invokeReference(targetMethod);
			} catch (ClassNotFoundException ex) {
				fail(ex);
			}
		}

		/**
		 * @throws VmUnavailableException
		 * 		When common VM isn't ready
		 * @see #testBasicRaw() Same logic but without {@link VirtualInvoker}
		 */
		@Test
		void testBasicService() throws VmUnavailableException {
			JvmClassInfo declaringClass = workspace.getPrimaryResource().getJvmClassBundle()
					.get(CountTo100.class.getName().replace('.', '/'));
			MethodMember methodTarget = declaringClass.getDeclaredMethod("run_0", "()V");

			Argument[] arguments = ArgumentBuilder.withinVM(commonVirtualService.getSharedVm())
					.withClassloader(helper.getClassLoaderInstance())
					.forMethod(declaringClass, methodTarget)
					.build();

			try {
				invoker.invokeVoid(declaringClass, methodTarget, arguments);
			} catch (IOException | ClassNotFoundException ex) {
				fail(ex);
			}
		}

		@Test
		void testBasicServiceWithArgs() throws VmUnavailableException {
			JvmClassInfo declaringClass = workspace.getPrimaryResource().getJvmClassBundle()
					.get(CountTo100.class.getName().replace('.', '/'));
			MethodMember methodTarget = declaringClass.getDeclaredMethod("main", "([Ljava/lang/String;)V");

			Argument[] arguments = ArgumentBuilder.withinVM(commonVirtualService.getSharedVm())
					.withClassloader(helper.getClassLoaderInstance())
					.forMethod(declaringClass, methodTarget)
					.add(new String[] { "0 1 2 3 4 5 6" })
					.build();

			try {
				invoker.invokeVoid(declaringClass, methodTarget, arguments);
			} catch (IOException | ClassNotFoundException ex) {
				fail(ex);
			}
		}
	}
}
