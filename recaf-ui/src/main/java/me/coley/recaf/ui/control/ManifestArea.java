package me.coley.recaf.ui.control;

import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.util.NodeEvents;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.model.PlainTextChange;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Syntax area implementation with a focus on Java jar manifests.
 *
 * @author Justus Garbe
 */
public class ManifestArea extends SyntaxArea {
	private static final Logger logger = Logging.get(ManifestArea.class);
	private String mainClass = "";

	/**
	 * @param problemTracking
	 * 		Optional problem tracking implementation to enable line problem indicators.
	 */
	public ManifestArea(ProblemTracking problemTracking) {
		super(Languages.MANIFEST, problemTracking);
		// Register keybind / mouse action to open the main class when interacted with
		NodeEvents.addKeyPressHandler(this, e -> {
			if (!StringUtil.isNullOrEmpty(mainClass) && Configs.keybinds().gotoDef.match(e)) {
				rangeCheck(getCaretPosition());
			}
		});
		setOnMousePressed((e) -> {
			if (!StringUtil.isNullOrEmpty(mainClass) && e.isPrimaryButtonDown()) {
				CharacterHit hit = hit(e.getX(), e.getY());
				int pos = hit.getInsertionIndex();
				rangeCheck(pos);
			}
		});
	}

	private void rangeCheck(int pos) {
		int start = getText().toLowerCase().indexOf("main-class: ") + "main-class: ".length();
		int end = start + mainClass.length();
		if (pos >= start && pos <= end) {
			openMainClassRef();
		}
	}

	private void openMainClassRef() {
		String internalName = mainClass.replace(".", "/");
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace == null) {
			logger.error("Cannot open main class, no workspace open");
			return;
		}
		ClassInfo info = workspace.getResources().getClass(internalName);
		if (info == null) {
			logger.error("Cannot open main class, '{}' not found in workspace", internalName);
			return;
		}
		CommonUX.openClass(info);
	}

	@Override
	protected void onPostStyle(PlainTextChange change) {
		try {
			// Leading whitespace should be trimmed, but a newline should always be present
			// at the end of the text input in order for the manifest tool to succeed in parsing.
			String formattedManifest = getText().trim() + "\n";
			String manifestLower = formattedManifest.toLowerCase();
			byte[] input = formattedManifest.getBytes(StandardCharsets.UTF_8);
			InputStream in = new ByteArrayInputStream(input);
			Manifest manifest = new Manifest(in);
			Attributes attr = manifest.getMainAttributes();
			try {
				this.mainClass = attr.getValue("Main-Class");
				if (mainClass == null)
					return;
				// Hacky manifest lime length limiting stuff
				int lineLength = make72Safe("main-class: " + mainClass);
				int start = manifestLower.indexOf("main-class: ") + "main-class: ".length();
				int end = start + lineLength - "main-class: ".length();
				// Some line wrapping cases lead the main class start/end positions to be off by one.
				if (getText().charAt(start) == ' ') {
					start++;
					end++;
				}
				// Clamp range for safety
				int len = getText().length();
				end = Math.min(len, end);
				// Underline the main class to visually indicate it can be opened
				int finalStart = start;
				int finalEnd = end;
				FxThreadUtil.run(() -> setStyle(finalStart, finalEnd, Arrays.asList("u", "cursor-pointer")));
			} catch (IllegalArgumentException e) {
				logger.error("Couldn't find Main-Class attribute in manifest");
			}
		} catch (IOException e) {
			logger.error("Failed to parse manifest", e);
		}
	}

	/**
	 * Hack to support manifest's limitation on line length.
	 *
	 * @param line
	 * 		Text input.
	 *
	 * @return Expected length.
	 */
	private static int make72Safe(String line) {
		int length = line.length();
		int index = 72;
		while (index < length) {
			index += 74; // + line width + line break ("\r\n")
			length += 3; // + line break ("\r\n") and space
		}
		return length;
	}
}
