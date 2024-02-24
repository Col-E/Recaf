package software.coley.recaf.services.vm;

import dev.xdark.ssvm.classloading.SupplyingClassLoaderInstaller;
import dev.xdark.ssvm.invoke.Argument;
import dev.xdark.ssvm.invoke.InvocationUtil;
import dev.xdark.ssvm.mirror.member.JavaMethod;
import dev.xdark.ssvm.mirror.type.InstanceClass;
import dev.xdark.ssvm.value.sink.BlackholeValueSink;
import dev.xdark.ssvm.value.sink.ValueSink;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.Service;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;

/**
 * Service for invoking methods in the current {@link Workspace} via SSVM.
 * VM initialization and configuration is handled by {@link CommonVirtualService}.
 *
 * @author Matt Coley
 * @see ArgumentBuilder Helper for creating {@link Argument} arrays.
 */
@WorkspaceScoped
public class VirtualInvoker implements Service {
	public static final String SERVICE_ID = "virtual-invoker";
	private final CommonVirtualService virtualService;
	private final VirtualInvokerConfig config;
	private final Workspace workspace;
	private SupplyingClassLoaderInstaller.Helper helper;

	@Inject
	public VirtualInvoker(@Nonnull CommonVirtualService virtualService,
						  @Nonnull VirtualInvokerConfig config,
						  @Nonnull Workspace workspace) {
		this.virtualService = virtualService;
		this.config = config;
		this.workspace = workspace;
	}

	/**
	 * Invokes a method returning {@code void}.
	 *
	 * @param declaringClass
	 * 		Class defining the method.
	 * @param method
	 * 		Method to invoke.
	 * @param arguments
	 * 		Arguments to pass to the method when executing.
	 *
	 * @throws VmUnavailableException
	 * 		When the shared VM is not accessible.
	 * @throws IOException
	 * 		When the classloader helper cannot load content from the current workspace.
	 * @throws ClassNotFoundException
	 * 		When the given class cannot be found within the VM's classloader helper.
	 */
	public void invokeVoid(@Nonnull JvmClassInfo declaringClass, @Nonnull MethodMember method, @Nonnull Argument[] arguments)
			throws VmUnavailableException, IOException, ClassNotFoundException {
		String methodDescriptor = method.getDescriptor();
		String methodName = method.getName();
		String className = declaringClass.getQualifiedName();
		invoke(BlackholeValueSink.INSTANCE, className, methodName, methodDescriptor, arguments);
	}

	// TODO: Copy the above but for other return types

	/**
	 * @param sink
	 * 		Sink to feed results into, and fetch as the return value.
	 * @param className
	 * 		Name of class declaring method to invoke.
	 * @param methodName
	 * 		Name of method to invoke.
	 * @param methodDescriptor
	 * 		Descriptor of method to invoke.
	 * @param arguments
	 * 		Arguments to pass to the method when executing.
	 * @param <R>
	 * 		Sink return type.
	 *
	 * @return Sink.
	 *
	 * @throws VmUnavailableException
	 * 		When the shared VM is not accessible.
	 * @throws IOException
	 * 		When the classloader helper cannot load content from the current workspace.
	 * @throws ClassNotFoundException
	 * 		When the given class cannot be found within the VM's classloader helper.
	 */
	private <R extends ValueSink> R invoke(@Nonnull R sink, @Nonnull String className, @Nonnull String methodName,
										   @Nonnull String methodDescriptor, @Nonnull Argument[] arguments)
			throws VmUnavailableException, IOException, ClassNotFoundException {
		var helper = lazyGetHelper();
		InstanceClass declaringClassInstance = helper.loadClass(className);
		JavaMethod declaredMethod = declaringClassInstance.getMethod(methodName, methodDescriptor);
		InvocationUtil invoker = virtualService.getSharedVm().getInvocationUtil();
		return invoker.invoke(declaredMethod, sink, arguments);
	}

	@Nonnull
	private SupplyingClassLoaderInstaller.Helper lazyGetHelper() throws VmUnavailableException, IOException {
		if (helper == null)
			helper = virtualService.createWorkspaceHelper(workspace);
		return helper;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public VirtualInvokerConfig getServiceConfig() {
		return config;
	}
}