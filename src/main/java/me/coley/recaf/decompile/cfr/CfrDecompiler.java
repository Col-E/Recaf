package me.coley.recaf.decompile.cfr;

import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.workspace.Workspace;
import org.benf.cfr.reader.api.*;
import org.benf.cfr.reader.util.getopt.*;

import java.lang.reflect.Field;
import java.util.*;

/**
 * CFR decompiler implementation.
 *
 * @author Matt
 */
public class CfrDecompiler extends Decompiler<String> {
	@Override
	protected Map<String, String> generateDefaultOptions() {
		Map<String, String> map = new HashMap<>();
		for(PermittedOptionProvider.ArgumentParam<?, ?> param : OptionsImpl.getFactory()
				.getArguments()) {
			String defaultValue = getOptValue(param);
			// Value is conditional based on version, just take the first given default.
			if (defaultValue != null && defaultValue.contains("if class"))
				defaultValue = defaultValue.substring(0, defaultValue.indexOf(" "));
			map.put(param.getName(), defaultValue);
		}
		return map;
	}

	@Override
	public String decompile(Workspace workspace, String name) {
		ClassSource source = new ClassSource(workspace);
		SinkFactoryImpl sink = new SinkFactoryImpl();
		CfrDriver driver = new CfrDriver.Builder()
				.withClassFileSource(source)
				.withOutputSink(sink)
				.withOptions(getOptions())
				.build();
		driver.analyse(Collections.singletonList(name));
		String decompile = sink.getDecompilation();
		return clean(decompile);
	}

	/**
	 * Remove watermark from decompilation output.
	 *
	 * @param decompile
	 * 		Decompilation text.
	 *
	 * @return Decompilation without watermark.
	 */
	private String clean(String decompile) {
		if(decompile.startsWith("/*"))
			return decompile.substring(decompile.indexOf("*/") + 3);
		return decompile;
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
		} catch(ReflectiveOperationException ex) {
			throw new IllegalStateException("Failed to fetch default value from Cfr parameter, did" +
					" the backend change?");
		}
	}
}
