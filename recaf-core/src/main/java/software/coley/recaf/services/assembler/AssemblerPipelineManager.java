package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.darknet.assembler.compiler.ClassRepresentation;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.ServiceConfig;

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

    public AssemblerPipeline<? extends ClassInfo, ? extends ClassRepresentation> getPipeline(PathNode<?> node) {
        ClassInfo info = node.getValueOfType(ClassInfo.class);
        if(info == null)
            throw new IllegalStateException("Failed to find class info for node: " + node);
        if(info.isJvmClass()) {
            return jvmAssemblerPipeline;
        } else {
            // TODO: Implement when dalvik assembler pipeline is implemented
            throw new UnsupportedOperationException("Dalvik assembler pipeline is not implemented");
        }
    }

    public JvmAssemblerPipeline getJvmAssemblerPipeline() {
        return jvmAssemblerPipeline;
    }
    public AndroidAssemblerPipeline getAndroidAssemblerPipeline() {
        return androidAssemblerPipeline;
    }
}
