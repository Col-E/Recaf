package software.coley.recaf.services.workspace.patch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import jakarta.annotation.Nonnull;
import software.coley.recaf.info.Info;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.services.workspace.patch.model.JvmAssemblerPatch;
import software.coley.recaf.services.workspace.patch.model.RemovePath;
import software.coley.recaf.services.workspace.patch.model.TextFilePatch;
import software.coley.recaf.services.workspace.patch.model.WorkspacePatch;
import software.coley.recaf.util.StringDiff;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Patch serialization helper for {@link PatchProvider}.
 *
 * @author Matt Coley
 */
public class PatchSerialization {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String KEY_REMOVALS = "removals";
	private static final String KEY_CLASS_JVM_ASM_DIFFS = "class-jvm-asm-diffs";
	private static final String KEY_FILE_TEXT_DIFFS = "file-text-diffs";
	private static final String KEY_NAME = "name";
	private static final String KEY_DIFFS = "diffs";
	private static final String KEY_TYPE = "type";
	private static final String KEY_START_A = "start-a";
	private static final String KEY_END_A = "end-a";
	private static final String KEY_TEXT_A = "text-a";
	private static final String KEY_START_B = "start-b";
	private static final String KEY_END_B = "end-b";
	private static final String KEY_TEXT_B = "text-b";
	private static final String TYPE_CLASS = "class";
	private static final String TYPE_FILE = "file";

	private PatchSerialization() {}

	/**
	 * Maps a workspace patch into JSON.
	 *
	 * @param patch
	 * 		Patch to serialize.
	 *
	 * @return JSON string representation of the patch.
	 */
	@Nonnull
	public static String serialize(@Nonnull WorkspacePatch patch) {
		StringWriter out = new StringWriter();
		try {
			JsonWriter jw = GSON.newJsonWriter(out);
			List<RemovePath> removals = patch.removals();
			List<JvmAssemblerPatch> jvmAssemblerPatches = patch.jvmAssemblerPatches();
			List<TextFilePatch> textFilePatches = patch.textFilePatches();

			serializeRemovals(jw, removals);
			serializeJvmAsmPatches(jvmAssemblerPatches, jw);
			serializeTextPatches(textFilePatches, jw);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to create json writer for patch", ex);
		}

		return out.toString();
	}

	private static void serializeRemovals(@Nonnull JsonWriter jw, @Nonnull List<RemovePath> removals) throws IOException {
		jw.beginObject();
		if (!removals.isEmpty()) {
			jw.name(KEY_REMOVALS).beginArray();
			for (RemovePath removal : removals) {
				Info info = removal.path().getValueOfType(Info.class);
				if (info == null)
					continue;

				String name = info.getName();
				jw.beginObject();
				if (info.isClass()) {
					jw.name(KEY_TYPE).value(TYPE_CLASS);
					jw.name(KEY_NAME).value(name);
				} else if (info.isFile()) {
					jw.name(KEY_TYPE).value(TYPE_FILE);
					jw.name(KEY_NAME).value(name);
				}
				jw.endObject();
			}
			jw.endArray();
		}
	}

	private static void serializeJvmAsmPatches(@Nonnull List<JvmAssemblerPatch> jvmAssemblerPatches, @Nonnull JsonWriter jw) throws IOException {
		if (!jvmAssemblerPatches.isEmpty()) {
			jw.name(KEY_CLASS_JVM_ASM_DIFFS).beginArray();
			for (JvmAssemblerPatch classPatch : jvmAssemblerPatches) {
				String className = classPatch.path().getValue().getName();
				jw.beginObject();
				jw.name(KEY_NAME).value(className);
				jw.name(KEY_DIFFS).beginArray();
				for (StringDiff.Diff assemblerDiff : classPatch.assemblerDiffs())
					serializeStringDiff(jw, assemblerDiff);
				jw.endArray().endObject();
			}
			jw.endArray();
		}
	}

	private static void serializeTextPatches(@Nonnull List<TextFilePatch> textFilePatches, @Nonnull JsonWriter jw) throws IOException {
		if (!textFilePatches.isEmpty()) {
			jw.name(KEY_FILE_TEXT_DIFFS).beginArray();
			for (TextFilePatch textPatch : textFilePatches) {
				String fileName = textPatch.path().getValue().getName();
				jw.beginObject();
				jw.name(KEY_NAME).value(fileName);
				jw.name(KEY_DIFFS).beginArray();
				for (StringDiff.Diff assemblerDiff : textPatch.textDiffs())
					serializeStringDiff(jw, assemblerDiff);
				jw.endArray().endObject();
			}
			jw.endArray();
		}
		jw.endObject();
	}

	private static void serializeStringDiff(@Nonnull JsonWriter jw, @Nonnull StringDiff.Diff diff) throws IOException {
		jw.beginObject();
		jw.name(KEY_TYPE).value(diff.type().name());
		jw.name(KEY_START_A).value(diff.startA());
		jw.name(KEY_END_A).value(diff.endA());
		jw.name(KEY_TEXT_A).value(diff.textA());
		jw.name(KEY_START_B).value(diff.startB());
		jw.name(KEY_END_B).value(diff.endB());
		jw.name(KEY_TEXT_B).value(diff.textB());
		jw.endObject();
	}

	/**
	 * Maps a JSON file into a workspace patch.
	 *
	 * @param workspace
	 * 		Workspace to apply the patch to.
	 * @param patchContents
	 * 		JSON outlining patch contents.
	 *
	 * @return A workspace patch instance.
	 *
	 * @throws PatchGenerationException
	 * 		When the JSON file couldn't be parsed, or its contents could not be found in the workspace.
	 */
	@Nonnull
	public static WorkspacePatch deserialize(@Nonnull Workspace workspace, @Nonnull String patchContents) throws PatchGenerationException {
		List<RemovePath> removals = Collections.emptyList();
		List<JvmAssemblerPatch> jvmAssemblerPatches = Collections.emptyList();
		List<TextFilePatch> textFilePatches = Collections.emptyList();
		if (patchContents.isBlank() || patchContents.charAt(0) != '{' || patchContents.charAt(patchContents.length() - 1) != '}')
			return new WorkspacePatch(workspace, removals, jvmAssemblerPatches, textFilePatches);
		try {
			JsonReader jr = GSON.newJsonReader(new StringReader(patchContents));
			jr.beginObject();
			while (jr.hasNext()) {
				String name = jr.nextName();
				switch (name) {
					case KEY_CLASS_JVM_ASM_DIFFS -> jvmAssemblerPatches = deserializeClassJvmAsmDiffs(workspace, jr);
					case KEY_FILE_TEXT_DIFFS -> textFilePatches = deserializeFileTextDiffs(workspace, jr);
					case KEY_REMOVALS -> removals = deserializeRemovals(workspace, jr);
				}
			}
			jr.endObject();
			return new WorkspacePatch(workspace, removals, jvmAssemblerPatches, textFilePatches);
		} catch (Exception ex) {
			throw new PatchGenerationException(ex, "Failed to parse patch contents");
		}
	}

	@Nonnull
	private static List<RemovePath> deserializeRemovals(@Nonnull Workspace workspace, @Nonnull JsonReader jr) throws IOException, PatchGenerationException {
		List<RemovePath> removals = new ArrayList<>();
		jr.beginArray();
		while (jr.hasNext()) {
			String name = null;
			String type = null;
			jr.beginObject();
			while (jr.hasNext()) {
				String key = jr.nextName();
				if (key.equals(KEY_NAME))
					name = jr.nextString();
				else if (key.equals(KEY_TYPE))
					type = jr.nextString();
			}
			jr.endObject();

			// Construct the removal
			if (name != null) {
				// If the classes/files do not exist in the workspace then our job is already done,
				// and we don't need to include these in the final patch model.
				if (TYPE_CLASS.equals(type)) {
					FilePathNode path = workspace.findFile(name);
					if (path != null) removals.add(new RemovePath(path));
				} else if (TYPE_FILE.equals(type)) {
					ClassPathNode path = workspace.findClass(name);
					if (path != null) removals.add(new RemovePath(path));
				}
			}
		}
		jr.endArray();
		return removals;
	}

	@Nonnull
	private static List<JvmAssemblerPatch> deserializeClassJvmAsmDiffs(@Nonnull Workspace workspace, @Nonnull JsonReader jr) throws IOException, PatchGenerationException {
		List<JvmAssemblerPatch> patches = new ArrayList<>();
		jr.beginArray();
		while (jr.hasNext()) {
			String name = null;
			List<StringDiff.Diff> diffs = Collections.emptyList();
			jr.beginObject();
			while (jr.hasNext()) {
				String key = jr.nextName();
				if (key.equals(KEY_NAME))
					name = jr.nextString();
				else if (key.equals(KEY_DIFFS))
					diffs = deserializeStringDiffs(jr);
			}
			jr.endObject();

			// Construct the patch
			if (name != null && !diffs.isEmpty()) {
				ClassPathNode classPath = workspace.findJvmClass(name);
				if (classPath == null)
					throw new PatchGenerationException("'" + name + "' cannot be found in the given workspace");
				patches.add(new JvmAssemblerPatch(classPath, diffs));
			}
		}
		jr.endArray();
		return patches;
	}

	@Nonnull
	private static List<TextFilePatch> deserializeFileTextDiffs(@Nonnull Workspace workspace, @Nonnull JsonReader jr) throws IOException, PatchGenerationException {
		List<TextFilePatch> patches = new ArrayList<>();
		jr.beginArray();
		while (jr.hasNext()) {
			String name = null;
			List<StringDiff.Diff> diffs = Collections.emptyList();
			jr.beginObject();
			while (jr.hasNext()) {
				String key = jr.nextName();
				if (key.equals(KEY_NAME))
					name = jr.nextString();
				else if (key.equals(KEY_DIFFS))
					diffs = deserializeStringDiffs(jr);
			}
			jr.endObject();

			// Construct the patch
			if (name != null && !diffs.isEmpty()) {
				FilePathNode filePath = workspace.findFile(name);
				if (filePath == null)
					throw new PatchGenerationException("'" + name + "' cannot be found in the given workspace");
				patches.add(new TextFilePatch(filePath, diffs));
			}
		}
		jr.endArray();
		return patches;
	}

	@Nonnull
	private static List<StringDiff.Diff> deserializeStringDiffs(@Nonnull JsonReader jr) throws IOException {
		List<StringDiff.Diff> diffs = new ArrayList<>();
		jr.beginArray();
		while (jr.hasNext())
			diffs.add(deserializeStringDiff(jr));
		jr.endArray();
		return diffs;
	}

	@Nonnull
	private static StringDiff.Diff deserializeStringDiff(@Nonnull JsonReader jr) throws IOException {
		StringDiff.DiffType type = null;
		int startA = -1;
		int startB = -1;
		int endA = -1;
		int endB = -1;
		String textA = null;
		String textB = null;

		jr.beginObject();
		while (jr.hasNext()) {
			String key = jr.nextName();
			switch (key) {
				case KEY_TYPE -> type = StringDiff.DiffType.valueOf(jr.nextString());
				case KEY_TEXT_A -> textA = jr.nextString();
				case KEY_TEXT_B -> textB = jr.nextString();
				case KEY_START_A -> startA = jr.nextInt();
				case KEY_START_B -> startB = jr.nextInt();
				case KEY_END_A -> endA = jr.nextInt();
				case KEY_END_B -> endB = jr.nextInt();
			}
		}
		jr.endObject();

		if (type == null)
			throw new IOException("String diff missing key: " + KEY_TYPE);
		if (textA == null)
			throw new IOException("String diff missing key: " + KEY_TEXT_A);
		if (textB == null)
			throw new IOException("String diff missing key: " + KEY_TEXT_B);
		if (startA == -1)
			throw new IOException("String diff missing key: " + KEY_START_A);
		if (startB == -1)
			throw new IOException("String diff missing key: " + KEY_START_B);
		if (endA == -1)
			throw new IOException("String diff missing key: " + KEY_END_A);
		if (endB == -1)
			throw new IOException("String diff missing key: " + KEY_END_B);

		return new StringDiff.Diff(type, startA, startB, endA, endB, textA, textB);
	}
}
