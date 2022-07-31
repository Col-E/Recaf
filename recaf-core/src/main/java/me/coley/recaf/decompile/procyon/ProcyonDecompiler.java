package me.coley.recaf.decompile.procyon;

import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.decompile.DecompileOption;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.RuntimeResource;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Procyon decompiler.
 *
 * @author xDark
 */
public class ProcyonDecompiler extends Decompiler {

	public ProcyonDecompiler() {
		super("Procyon", "0.6.0");
	}

	@Override
	protected String decompileImpl(Map<String, DecompileOption<?>> options, Workspace workspace, ClassInfo classInfo) {
		ITypeLoader loader = new CompositeTypeLoader(
				new TargetedTypeLoader(classInfo),
				new WorkspaceTypeLoader(workspace),
				new ResourceTypeLoader(RuntimeResource.get())
		);
		DecompilerSettings settings = new DecompilerSettings();
		settings.setTypeLoader(loader);
		settings.setJavaFormattingOptions(JavaFormattingOptions.createDefault());
		MetadataSystem system = new MetadataSystem(loader);
		TypeReference ref = system.lookupType(classInfo.getName());
		DecompilationOptions decompilationOptions = new DecompilationOptions();
		decompilationOptions.setSettings(settings);
		decompilationOptions.setFullDecompilation(true);
		StringWriter writer = new StringWriter();
		settings.getLanguage().decompileType(ref.resolve(), new PlainTextOutput(writer), decompilationOptions);
		return writer.toString();
	}

	@Override
	protected Map<String, DecompileOption<?>> createDefaultOptions() {
		return new HashMap<>();
	}

	private static final class TargetedTypeLoader implements ITypeLoader {
		private final ClassInfo info;

		TargetedTypeLoader(ClassInfo info) {
			this.info = info;
		}

		@Override
		public boolean tryLoadType(String internalName, Buffer buffer) {
			ClassInfo info = this.info;
			if (internalName.equals(info.getName())) {
				byte[] data = info.getValue();
				buffer.position(0);
				buffer.putByteArray(data, 0, data.length);
				buffer.position(0);
				return true;
			}
			return false;
		}
	}
}
