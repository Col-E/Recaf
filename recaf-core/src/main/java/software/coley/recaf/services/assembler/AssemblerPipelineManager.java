package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.darknet.assembler.compiler.ClassRepresentation;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.Service;

/**
 * Assembler implementations manager.
 *
 * @author Justus Garbe
 */
@ApplicationScoped
public class AssemblerPipelineManager implements Service {
	public static final String SERVICE_ID = "assembler-pipeline";
	private final AssemblerPipelineGeneralConfig config;
	private final JvmAssemblerPipeline jvmAssemblerPipeline;
	private final AndroidAssemblerPipeline androidAssemblerPipeline;

	@Inject
	public AssemblerPipelineManager(@Nonnull AssemblerPipelineGeneralConfig config,
									@Nonnull JvmAssemblerPipeline jvmAssemblerPipeline,
									@Nonnull AndroidAssemblerPipeline androidAssemblerPipeline) {
		this.config = config;
		this.jvmAssemblerPipeline = jvmAssemblerPipeline;
		this.androidAssemblerPipeline = androidAssemblerPipeline;
	}

	/**
	 * Automatically pick a pipeline for the content in the given path.
	 *
	 * @param path
	 * 		Path to some item in the workspace to get an assembler pipeline for.
	 *
	 * @return Either a {@link JvmAssemblerPipeline} or {@link AndroidAssemblerPipeline} based on the path contents.
	 */
	@Nonnull
	public AssemblerPipeline<? extends ClassInfo, ? extends ClassRepresentation> getPipeline(@Nonnull PathNode<?> path) {
		ClassInfo info = path.getValueOfType(ClassInfo.class);
		if (info == null)
			throw new IllegalStateException("Failed to find class info for node: " + path);
		if (info.isJvmClass()) {
			return jvmAssemblerPipeline;
		} else {
			// TODO: Implement when dalvik assembler pipeline is implemented
			throw new UnsupportedOperationException("Dalvik assembler pipeline is not implemented");
		}
	}

	/**
	 * @return Assembler pipeline for JVM classes.
	 */
	@Nonnull
	public JvmAssemblerPipeline getJvmAssemblerPipeline() {
		return jvmAssemblerPipeline;
	}

	/**
	 * @return Assembler pipeline for Dalvik classes.
	 */
	@Nonnull
	public AndroidAssemblerPipeline getAndroidAssemblerPipeline() {
		return androidAssemblerPipeline;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public AssemblerPipelineGeneralConfig getServiceConfig() {
		return config;
	}
}
