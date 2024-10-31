package software.coley.recaf.services.decompile.forgeflower;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import recaf.relocation.libs.forgeflower.org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import recaf.relocation.libs.forgeflower.org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.AbstractJvmDecompiler;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.FakeFile;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.BasicWorkspaceFileResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Forgeflower decompiler implementation.
 *
 * @author meiMingle
 */
@ApplicationScoped
public class ForgeflowerDecompiler extends AbstractJvmDecompiler {
    public static final String NAME = "Forgeflower";
    private final ForgeflowerConfig config;
    private final IFernflowerLogger logger;

    /**
     * New Fernflower decompiler instance.
     *
     * @param config Decompiler configuration.
     */
    @Inject
    public ForgeflowerDecompiler(@Nonnull ForgeflowerConfig config) {
        // Change this version to be dynamic when / if the Fernflower authors make a function that returns the version...
        super(NAME, "2.0.674.2", config);
        this.config = config;
        logger = new ForgeflowerLogger(config);
    }

    @Nonnull
    @Override
    public DecompileResult decompileInternal(@Nonnull Workspace workspace, @Nonnull JvmClassInfo info) {
        Map<String, Object> fernflowerProperties = config.getFernflowerProperties();

        MyResultSaver saver = new MyResultSaver();
        MyBytecodeProvider provider = new MyBytecodeProvider(workspace);

        BaseDecompiler decompiler = new BaseDecompiler(
                provider,
                saver,
                fernflowerProperties,
                logger
        );

        try {
            String path = ((BasicWorkspaceFileResource) workspace.getPrimaryResource()).getFileInfo().getName() + "!" + info.getName() + ".class";
            decompiler.addSource(new FakeFile(path));
            List<InnerClassInfo> innerClasses = info.getInnerClasses();
            innerClasses.forEach(inner->{
                if (workspace.findClass(inner.getInnerClassName())!=null) {
                    decompiler.addSource(new FakeFile(((BasicWorkspaceFileResource) workspace.getPrimaryResource()).getFileInfo().getName() + "!" + inner.getName() + ".class"));
                }

            });
            decompiler.decompileContext();
            if (saver.getResult() == null || saver.getResult().isEmpty()) {
                return new DecompileResult(new IllegalStateException("Missing decompilation output"), 0);
            }

            return new DecompileResult(saver.getResult(), 0);
        } catch (Exception e) {
            return new DecompileResult(e, 0);
        }
    }


}
