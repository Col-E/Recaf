package software.coley.recaf.cli.command;

import picocli.CommandLine;
import software.coley.recaf.Recaf;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.services.workspace.io.ResourceImporter;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.DecompileResult;

import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "decompile",
        description = "Decompiles a class from a given input file."
)
public class DecompileCommand implements Callable<Void>, RecafCommand {
    @CommandLine.Option(names = {"-i", "--input"}, required = true, description = "The input file (JAR, WAR, etc.)")
    private File inputFile;

    @CommandLine.Option(names = {"-c", "--class"}, required = true, description = "The fully qualified name of the class to decompile.")
    private String className;

    private Recaf recaf;

    @Override
    public Void call() throws Exception {
        if (recaf == null) {
            System.err.println("Recaf instance not initialized.");
            return null;
        }

        ResourceImporter importer = recaf.get(ResourceImporter.class);
        WorkspaceResource resource = importer.importResource(inputFile);
        Workspace workspace = new BasicWorkspace(resource);

        JvmClassInfo classInfo = workspace.getPrimaryResource().getJvmClassBundle().get(className);

        if (classInfo == null) {
            System.err.println("Class not found: " + className);
            return null;
        }

        DecompilerManager decompilerManager = recaf.get(DecompilerManager.class);
        DecompileResult result = decompilerManager.decompile(workspace, classInfo).get();

        if (result.getType() == DecompileResult.ResultType.SUCCESS) {
            System.out.println(result.getText());
        } else {
            System.err.println("Decompilation failed: " + result.getException().getMessage());
        }

        return null;
    }

    @Override
    public void setRecaf(Recaf recaf) {
        this.recaf = recaf;
    }
}
