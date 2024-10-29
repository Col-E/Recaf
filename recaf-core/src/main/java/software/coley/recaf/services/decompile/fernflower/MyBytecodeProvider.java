package software.coley.recaf.services.decompile.fernflower;

import recaf.relocation.libs.fernflower.org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.workspace.model.Workspace;


/**
 * MyBytecodeProvider
 *
 * @author meiMingle
 */
public class MyBytecodeProvider implements IBytecodeProvider {
    Workspace workspace;

    public MyBytecodeProvider(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public byte[] getBytecode(String absolutePath, String internalPath) {
        String[] split = absolutePath.split("!");
        String path = split[0];
        String name = split[1];

        ClassPathNode aClass = workspace.findClass(name.substring(0, name.lastIndexOf('.')));
        return aClass.getValue().asJvmClass().getBytecode();
    }
}