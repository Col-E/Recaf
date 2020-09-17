package me.coley.recaf.decompile.cfr;

import me.coley.recaf.config.ConfDecompile;
import me.coley.recaf.control.Controller;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.util.AccessFlag;
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
		for(PermittedOptionProvider.ArgumentParam<?, ?> param : OptionsImpl.getFactory()
				.getArguments()) {
			String defaultValue = getOptValue(param);
			// Value is conditional based on version, just take the first given default.
			if (defaultValue != null && defaultValue.contains("if class"))
				defaultValue = defaultValue.substring(0, defaultValue.indexOf(" "));
			map.put(param.getName(), defaultValue);
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
		return clean(decompile, name);
	}

	/**
	 * Remove watermark &amp; oddities from decompilation output.
	 *
	 * @param decompilation
	 * 		Decompilation text.
	 * @param name
	 * 		Class name.
	 *
	 * @return Decompilation without watermark.
	 */
	private String clean(String decompilation, String name) {
		// Get rid of header comment
		if(decompilation.startsWith("/*\n * Decompiled with CFR"))
			decompilation = decompilation.substring(decompilation.indexOf("*/") + 3);
		// JavaParser does NOT like inline comments like this.
		decompilation = decompilation.replace("/* synthetic */ ", "");
		decompilation = decompilation.replace("/* bridge */ ", "");
		decompilation = decompilation.replace("/* enum */ ", "");
		decompilation = decompilation.replace(" - consider using --renameillegalidents true",
				" - recommend switching to table mode");
		// Fix inner class names being busted, "Outer.1" instead of "Outer$1"
		String simple = name.contains("/") ? name.substring(name.lastIndexOf('/') + 1) : name;
		if(simple.contains("$")) {
			// They have "." instead of "$"
			decompilation = decompilation.replace(simple.replace("$", "."), simple);
			// Inners decompiled as top-level can't have static qualifier
			String startText = decompilation.substring(0, decompilation.indexOf(simple));
			String startTextCopy = startText;
			Set<AccessFlag> allowed = AccessFlag.getApplicableFlags(AccessFlag.Type.CLASS);
			for (AccessFlag acc : AccessFlag.values()) {
				if (allowed.contains(acc))
					continue;
				if (startText.contains(acc.getName() + " ")) {
					startText = startText.replace(startText,
							startText.replace(acc.getCodeFriendlyName() + " ", ""));
				}
			}
			decompilation = decompilation.replace(startTextCopy, startText);
		}
		return decompilation;
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
