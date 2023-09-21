package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AndroidAssemblerPipeline {

    @Inject
    public AndroidAssemblerPipeline(@Nonnull AssemblerPipelineGeneralConfig config) {
        // TODO: Implement when dalvik assembler pipeline is implemented
    }

}
