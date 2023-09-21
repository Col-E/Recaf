package software.coley.recaf.services.assembler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableString;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

@ApplicationScoped
public class AssemblerPipelineGeneralConfig extends BasicConfigContainer implements ServiceConfig {

    private final ObservableString disassemblyIndent = new ObservableString("    ");

    @Inject
    public AssemblerPipelineGeneralConfig() {
        super(ConfigGroups.SERVICE_ASSEMBLER, AssemblerPipelineManager.SERVICE_ID + ConfigGroups.PACKAGE_SPLIT
                + "general" + CONFIG_SUFFIX);

        addValue(new BasicConfigValue<>("disassembly_indent", String.class, disassemblyIndent));
    }

    public ObservableString getDisassemblyIndent() {
        return disassemblyIndent;
    }
}
