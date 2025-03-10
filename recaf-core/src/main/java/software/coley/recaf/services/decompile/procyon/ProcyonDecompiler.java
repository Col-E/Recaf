package software.coley.recaf.services.decompile.procyon;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.strobel.Procyon;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.CompositeTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.AbstractJvmDecompiler;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.json.GsonProvider;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Procyon decompiler implementation.
 *
 * @author xDark
 */
@ApplicationScoped
public class ProcyonDecompiler extends AbstractJvmDecompiler {
	public static final String NAME = "Procyon";
	private final ProcyonConfig config;

	/**
	 * New Procyon decompiler instance.
	 *
	 * @param gsonProvider
	 * 		Gson provider to register deserialization with.
	 * @param config
	 * 		Config instance.
	 */
	@Inject
	public ProcyonDecompiler(@Nonnull GsonProvider gsonProvider, @Nonnull ProcyonConfig config) {
		super(NAME, Procyon.version(), config);
		this.config = config;

		// Support for mapping the 'language' model we store in the config.
		LanguageTypeAdapter adapter = new LanguageTypeAdapter();
		gsonProvider.addTypeAdapterFactory(new TypeAdapterFactory() {
			@Override
			@SuppressWarnings("unchecked")
			public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
				if (Language.class.isAssignableFrom(type.getRawType()))
					return (TypeAdapter<T>) adapter;
				return null;
			}
		});
	}

	@Nonnull
	@Override
	protected DecompileResult decompileInternal(@Nonnull Workspace workspace, @Nonnull JvmClassInfo classInfo) {
		String name = classInfo.getName();
		byte[] bytecode = classInfo.getBytecode();
		ITypeLoader loader = new CompositeTypeLoader(
				new TargetedTypeLoader(name, bytecode),
				new WorkspaceTypeLoader(workspace)
		);
		DecompilerSettings settings = config.toSettings();
		settings.setTypeLoader(loader);
		MetadataSystem system = new MetadataSystem(loader);
		TypeReference ref = system.lookupType(name);
		DecompilationOptions decompilationOptions = new DecompilationOptions();
		decompilationOptions.setSettings(settings);
		StringWriter writer = new StringWriter();
		settings.getLanguage().decompileType(ref.resolve(), new PlainTextOutput(writer), decompilationOptions);
		String decompile = writer.toString();
		int configHash = getConfig().getHash();
		if (decompile == null)
			return new DecompileResult(new IllegalStateException("Missing decompilation output"), configHash);
		return new DecompileResult(decompile, configHash);
	}

	/**
	 * Type loader to load a single class file.
	 * Used as the first loader within a {@link CompositeTypeLoader} such that it overrides any
	 * following type loader that could also procure the same class info.
	 */
	private record TargetedTypeLoader(String name, byte[] data) implements ITypeLoader {
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

	/**
	 * Adapter to read/write {@link Language} values from/to {@link ProcyonConfig}.
	 */
	private static class LanguageTypeAdapter extends TypeAdapter<Language> {
		@Override
		public void write(JsonWriter writer, Language language) throws IOException {
			writer.value(language.getName());
		}

		@Override
		public Language read(JsonReader reader) throws IOException {
			if (reader.hasNext()) {
				String name = reader.nextString();
				return Languages.all().stream()
						.filter(l -> l.getName().equals(name))
						.findFirst()
						.orElse(Languages.java());
			}
			return Languages.java();
		}
	}
}
