package me.coley.recaf.decompile.cfr;

import me.coley.recaf.config.ConfDecompile;
import me.coley.recaf.control.Controller;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.util.AccessFlag;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.util.getopt.OptionDecoderParam;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.getopt.PermittedOptionProvider;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * CFR decompiler implementation.
 *
 * @author Matt
 */
public class CfrDecompiler extends Decompiler<String> {
	
	DecompilationOutputCleaner outputCleaner = new DecompilationOutputCleaner();
	/**
	 * Initialize the decompiler wrapper.
	 *
	 * @param controller
	 * 		Controller with configuration to pull from and the workspace to pull classes from.
	 */
	public CfrDecompiler(Controller controller) {
		super(controller);
	}

	@Override
	protected Map<String, String> generateDefaultOptions() {
		Map<String, String> map = new HashMap<>();
		for (PermittedOptionProvider.ArgumentParam<?, ?> param : OptionsImpl.getFactory()
				.getArguments()) {
			String defaultValue = getOptValue(param);
			if (defaultValue != null) {
				// For options that depend on other options or class file specifics, ignore them
				if (defaultValue.contains("Value of option") || defaultValue.contains("if class"))
					continue;
				// Only populate boolean options
				if (defaultValue.equals("true") || defaultValue.equals("false"))
					map.put(param.getName(), defaultValue);
				// There are only a few non-boolean options, and even supplying 'recpass'
				// causes some issues in edge cases (https://github.com/leibnitz27/cfr/issues/253)
			}
		}
		ConfDecompile config = getController().config().decompile();
		if (config.showSynthetic) {
			// CFR doesn't have options against intentional marking of ACC_SYNTHETIC by obfuscators :/
			// This will only show ACC_BRIDGE but not ACC_SYNTHETIC
			map.put("hidebridgemethods", "false");
			// And this, will only show ACC_SYNTHETIC in certain cases so it isn't that useful
			// map.put("removeinnerclasssynthetics", "true");
		}
		return map;
	}

	@Override
	public String decompile(String name) {
		ClassSource source = new ClassSource(getController());
		SinkFactoryImpl sink = new SinkFactoryImpl();
		CfrDriver driver = new CfrDriver.Builder()
				.withClassFileSource(source)
				.withOutputSink(sink)
				.withOptions(getOptions())
				.build();
		driver.analyse(Collections.singletonList(name));
		String decompile = sink.getDecompilation();
		if (decompile == null)
			return "// ERROR: Failed to decompile '" + name + "'";
		return outputCleaner.clean(decompile, name);
	}

	/**
	 * Fetch default value from configuration parameter.
	 *
	 * @param param
	 * 		Parameter.
	 *
	 * @return Default value as string, may be {@code null}.
	 */
	private String getOptValue(PermittedOptionProvider.ArgumentParam<?, ?> param) {
		try {
			Field fn = PermittedOptionProvider.ArgumentParam.class.getDeclaredField("fn");
			fn.setAccessible(true);
			OptionDecoderParam<?, ?> decoder = (OptionDecoderParam<?, ?>) fn.get(param);
			return decoder.getDefaultValue();
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Failed to fetch default value from Cfr parameter, did" +
					" the backend change?");
		}
	}
}
