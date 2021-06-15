package me.coley.recaf.android;

import me.coley.recaf.TestUtils;
import me.coley.recaf.android.cf.MutableClassDef;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.baksmali.formatter.BaksmaliWriter;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MutableTypeTests extends TestUtils {
	@Test
	void test() throws IOException {
		// Test if our copied instances are the same by checking the disassembled text.
		Path source = sourcesDir.resolve("Sample.dex");
		Opcodes opcodes = Opcodes.getDefault();
		BaksmaliOptions options = new BaksmaliOptions();
		try (InputStream inputStream = new ByteArrayInputStream(Files.readAllBytes(source))){
			DexBackedDexFile file = DexBackedDexFile.fromInputStream(opcodes, inputStream);
			for (DexBackedClassDef originalClass : file.getClasses()) {
				MutableClassDef copiedClass = new MutableClassDef(originalClass);
				ClassDefinition originalDefinition = new ClassDefinition(options, originalClass);
				ClassDefinition copiedDefinition = new ClassDefinition(options, copiedClass);
				// Disassemble
				StringWriter originalWriter = new StringWriter();
				StringWriter copiedWriter = new StringWriter();
				originalDefinition.writeTo(new BaksmaliWriter(originalWriter));
				copiedDefinition.writeTo(new BaksmaliWriter(copiedWriter));
				// Since the fields/methods are stored in a set we cannot guarantee the order of their appearence.
				// But if all sorted lines are equal then that's ok.
				SortedSet<String> original = new TreeSet<>(Arrays.asList(originalWriter.toString().split("\n+")));
				SortedSet<String> copied = new TreeSet<>(Arrays.asList(copiedWriter.toString().split("\n+")));
				assertEquals(original, copied);
			}
		}
	}
}
