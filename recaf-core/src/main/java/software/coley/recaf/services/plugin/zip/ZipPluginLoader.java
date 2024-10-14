package software.coley.recaf.services.plugin.zip;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.cafedude.InvalidClassException;
import software.coley.cafedude.classfile.ClassFile;
import software.coley.cafedude.classfile.annotation.Annotation;
import software.coley.cafedude.classfile.annotation.ArrayElementValue;
import software.coley.cafedude.classfile.annotation.ElementValue;
import software.coley.cafedude.classfile.annotation.Utf8ElementValue;
import software.coley.cafedude.classfile.attribute.AnnotationsAttribute;
import software.coley.cafedude.classfile.attribute.Attribute;
import software.coley.cafedude.io.ClassFileReader;
import software.coley.collections.Unchecked;
import software.coley.lljzip.ZipIO;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.recaf.plugin.*;
import software.coley.recaf.services.plugin.PluginException;
import software.coley.recaf.services.plugin.PluginInfo;
import software.coley.recaf.services.plugin.PluginLoader;
import software.coley.recaf.services.plugin.PreparedPlugin;
import software.coley.recaf.util.IOUtil;
import software.coley.recaf.util.io.ByteSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Zip plugin loader.
 *
 * @author xDark
 */
public final class ZipPluginLoader implements PluginLoader {
	private static final String PLUGIN_INFORMATION_DESC = Type.getDescriptor(PluginInformation.class);
	public static final String SERVICE_PATH = servicePath();

	@Nullable
	@Override
	public PreparedPlugin prepare(@Nonnull ByteSource source) throws PluginException {
		ZipArchive zip;
		try {
			zip = ZipIO.readJvm(source.mmap());
		} catch (IOException ex) {
			return null;
		}
		ZipArchiveView archiveView;
		try {
			archiveView = new ZipArchiveView(zip);
		} catch (Throwable t) {
			try {
				zip.close();
			} catch (IOException ignored) {}
			throw t;
		}
		var zs = new ZipSource(archiveView);
		try {
			ByteSource resPluginImplementation = zs.findResource(SERVICE_PATH);
			if (resPluginImplementation == null) {
				throw new PluginException("Cannot find %s resource".formatted(SERVICE_PATH));
			}

			// Cannot use ServiceLoader here, it will attempt to instantiate the plugin,
			// which is what we *don't* want at this point.
			String pluginClassName;
			try (BufferedReader reader = IOUtil.toBufferedReader(resPluginImplementation.openStream())) {
				pluginClassName = reader.readLine();
				{
					String extraLine;
					while ((extraLine = reader.readLine()) != null) {
						if (!extraLine.isEmpty())
							throw new PluginException("Extra line %s".formatted(extraLine));
					}
				}
			}

			// Find plugin class.
			ByteSource resPluginClass = zs.findResource(pluginClassName.replace('.', '/') + ".class");
			if (resPluginClass == null) {
				throw new PluginException("Plugin class %s doesn't exist".formatted(pluginClassName));
			}

			// Find @PluginInformation annotation.
			ClassFile cf;
			try (InputStream in = resPluginClass.openStream()) {
				ClassFileReader reader = new ClassFileReader();
				cf = reader.read(in.readAllBytes());
			} catch (InvalidClassException ex) {
				throw new PluginException(ex);
			}
			PluginInfo info = null;
			for (Attribute attr : cf.getAttributes()) {
				if (!(attr instanceof AnnotationsAttribute annotations)) continue;
				if (!annotations.isVisible()) continue;
				for (Annotation annotation : annotations.getAnnotations()) {
					if (!PLUGIN_INFORMATION_DESC.equals(annotation.getType().getText())) continue;
					// Collect information.
					info = parsePluginInfo(annotation);
					break;
				}
			}
			if (info == null) {
				throw new PluginException("Missing @PluginInformation annotation");
			}
			PreparedPlugin preparedPlugin = new ZipPreparedPlugin(info, pluginClassName, zs);
			zs = null;
			return preparedPlugin;
		} catch (IOException ex) {
			throw new PluginException(ex);
		} finally {
			if (zs != null) {
				try {
					zs.close();
				} catch (IOException ignored) {}
			}
		}
	}

	@Nonnull
	private static PluginInfo parsePluginInfo(@Nonnull Annotation annotation) throws PluginException {
		PluginInfo info = PluginInfo.empty();
		for (var e : annotation.getValues().entrySet()) {
			String name = e.getKey().getText();
			ElementValue value = e.getValue();
			info = switch (name) {
				case "id" -> info.withId(string(value));
				case "name" -> info.withName(string(value));
				case "version" -> info.withVersion(string(value));
				case "author" -> info.withAuthor(string(value));
				case "description" -> info.withDescription(string(value));
				case "dependencies" -> info.withDependencies(stringSet(value));
				case "softDependencies" -> info.withSoftDependencies(stringSet(value));
				default -> info;
			};
		}
		return info;
	}

	@Nonnull
	private static String servicePath() {
		return "META-INF/services/%s".formatted(Plugin.class.getName());
	}

	@SafeVarargs
	private static <V extends ElementValue, R> R extractValue(@Nonnull ElementValue value, Function<V, R> extractor, V... typeHint) throws PluginException {
		Class<?> type = typeHint.getClass().getComponentType();
		V v;
		try {
			//noinspection unchecked
			v = ((Class<V>) type).cast(value);
		} catch (ClassCastException ex) {
			throw new PluginException("%s is not an instance of %s".formatted(value, type.getSimpleName()));
		}
		return extractor.apply(v);
	}

	@Nonnull
	private static String string(@Nonnull ElementValue value) throws PluginException {
		return extractValue(value, (Utf8ElementValue elem) -> elem.getValue().getText());
	}

	@Nonnull
	private static Set<String> stringSet(@Nonnull ElementValue value) throws PluginException {
		return extractValue(value, (ArrayElementValue array) -> array
				.getArray()
				.stream()
				.map(Unchecked.function(ZipPluginLoader::string))
				.collect(Collectors.toUnmodifiableSet()));
	}
}
