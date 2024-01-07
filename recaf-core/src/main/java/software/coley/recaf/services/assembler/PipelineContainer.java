package software.coley.recaf.services.assembler;

import me.darknet.assembler.compiler.ClassRepresentation;
import software.coley.recaf.info.ClassInfo;

public class PipelineContainer {

    private final AssemblerPipeline<? extends ClassInfo, ? extends ClassRepresentation> pipeline;


    public PipelineContainer(AssemblerPipeline<? extends ClassInfo, ? extends ClassRepresentation> pipeline) {
        this.pipeline = pipeline;
    }
}
