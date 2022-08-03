package me.coley.recaf.decompile.procyon;

import com.strobel.Procyon;
import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
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
		super("Procyon", Procyon.version());
	}

	@Override
	protected String decompileImpl(Map<String, DecompileOption<?>> options, Workspace workspace, ClassInfo classInfo) {
		ITypeLoader loader = new CompositeTypeLoader(
				new TargetedTypeLoader(classInfo.getName(), applyPreInterceptors(classInfo.getValue())),
				new WorkspaceTypeLoader(workspace),
				new ResourceTypeLoader(RuntimeResource.get())
		);
		DecompilerSettings settings = DecompilerSettings.javaDefaults();
		settings.setForceExplicitImports(true);
		settings.setTypeLoader(loader);
		MetadataSystem system = new MetadataSystem(loader);
		TypeReference ref = system.lookupType(classInfo.getName());
		DecompilationOptions decompilationOptions = new DecompilationOptions();
		decompilationOptions.setSettings(settings);
		StringWriter writer = new StringWriter();
		settings.getLanguage().decompileType(ref.resolve(), new PlainTextOutput(writer), decompilationOptions);
		return writer.toString();
	}

	@Override
	protected Map<String, DecompileOption<?>> createDefaultOptions() {
		// TODO: Populate procyon options from 'DecompilerSettings'
		return new HashMap<>();
	}

	/**
	 * Type loader to load a single class file.
	 * Used as the first loader within a {@link CompositeTypeLoader} such that it overrides any
	 * following type loader that could also procure the same class info.
	 */
	private static final class TargetedTypeLoader implements ITypeLoader {
		private final String name;
		private final byte[] data;

		private TargetedTypeLoader(String name, byte[] data) {
			this.name = name;
			this.data = data;
		}

		@Override
		public boolean tryLoadType(String internalName, Buffer buffer) {
			if (internalName.equals(name)) {
				byte[] data = this.data;
				buffer.position(0);
				buffer.putByteArray(data, 0, data.length);
				buffer.position(0);
				return true;
			}
			return false;
		}
	}
}
