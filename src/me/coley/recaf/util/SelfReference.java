package me.coley.recaf.util;

import java.io.File;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import me.coley.recaf.Recaf;

public class SelfReference {
	private final File file;
	private final boolean isJar;

	private SelfReference(File file) {
		this.file = file;
		this.isJar = file.getName().toLowerCase().endsWith(".jar");
	}

	/**
	 * @return File reference to self.
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @return File path to self.
	 */
	public String getPath() {
		return file.getAbsolutePath();
	}

	/**
	 * @return Is the current executable context a jar file.
	 */
	public boolean isJar() {
		return isJar;
	}

	/**
	 * @return List of language files recognized.
	 */
	public List<String> getLangs() {
		return getFiles("resources/lang/", ".json", false, false);
	}

	/**
	 * @return List of styles recognized.
	 */
	public List<String> getStyles() {
		//@formatter:off
		List<String> files = getFiles("resources/style/", ".css", false, false);
		// Map the stylesheets to distinct theme names.
		// Each theme may have multiple files.
		files = files.stream()
				.filter(f -> f.startsWith("common-"))
				.map(f -> f.substring(f.indexOf("-") + 1))
				.distinct()
				.collect(Collectors.toList());
		//@formatter:on
		return files;
	}

	/**
	 * @param prefix
	 *            File prefix to match.
	 * @param postfix
	 *            File postfix to match <i>(file extension)</i>.
	 * @param includePrefix
	 *            Flag to include prefix in output.
	 * @param includePostfix
	 *            Flag to include postfix in output.
	 * @return List of matching files.
	 */
	private List<String> getFiles(String prefix, String postfix, boolean includePrefix, boolean includePostfix) {
		List<String> list = new ArrayList<>();
		if (isJar()) {
			// Read self as jar
			try (ZipFile file = new ZipFile(getFile())) {
				Enumeration<? extends ZipEntry> entries = file.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					// skip directories
					if (entry.isDirectory()) continue;
					String name = entry.getName();
					if (name.startsWith(prefix) && name.endsWith(postfix)) {
						String lang = name;
						if (!includePrefix) {
							lang = lang.substring(prefix.length());
						}
						if (!includePostfix) {
							lang = lang.substring(0, lang.length() - postfix.length());
						}
						list.add(lang);
					}
				}
			} catch (Exception e) {}
		} else {
			// Read self as file directory
			File dir = new File(getFile(), prefix);
			for (File file : dir.listFiles()) {
				String name = file.getName();
				if (name.endsWith(postfix)) {
					String lang = name;
					if (includePrefix) {
						lang = prefix + lang;
					}
					if (!includePostfix) {
						lang = lang.substring(0, lang.length() - postfix.length());
					}
					list.add(lang);
				}
			}
		}
		return list;
	}

	/**
	 * @return Recaf executable context.
	 * @throws URISyntaxException
	 *             Thrown if the file reference could not be resolved.
	 */
	public static SelfReference get() throws URISyntaxException {
		CodeSource codeSource = Recaf.class.getProtectionDomain().getCodeSource();
		File selfFile = new File(codeSource.getLocation().toURI().getPath());
		return new SelfReference(selfFile);
	}
}
