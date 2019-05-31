package me.coley.recaf.common;

import me.coley.recaf.util.FileIO;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base start hook to satisfy Recaf's IO requirements.
 * Additionally isolates testing by changing the current directory.
 *
 * @author Matt
 */
public interface RecafTest {
	@BeforeAll
	static void setup() {
		File current = new File(System.getProperty("user.dir"));
		File subdir = new File(current, "testing");
		// Reset dir
		if(!subdir.exists()) {
			subdir.mkdir();
		} else {
			subdir.delete();
			subdir.mkdir();
		}
		// Create dummy config
		File conf = new File(subdir, "rc-config");
		conf.mkdir();
		String[] files = new String[]{"rc_asm", "rc_display", "rc_cfr", "rc_other"};
		try {
			for(String file : files) {
				File confImpl = new File(conf, file + ".json");
				FileIO.writeFile(confImpl.getAbsolutePath(), "{}");
			}
		} catch(IOException e) {
			fail("Failed to generate dummy test directory.", e);
		}
		// Set dir as current location
		System.setProperty("user.dir", subdir.getAbsolutePath());
	}
}
