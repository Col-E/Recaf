package software.coley.recaf.ui.pane.editing;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.Icons;

/**
 * Enum of available editors to display files with.
 *
 * @see FilePane
 */
public enum FileDisplayMode {
	HEX("menu.mode.file.hex", CarbonIcons.NUMBER_0),
	TEXT("menu.mode.file.text", CarbonIcons.STRING_TEXT),
	TEXT_BINARY_XML("menu.mode.file.text", CarbonIcons.STRING_TEXT),
	IMAGE("menu.mode.file.image", CarbonIcons.IMAGE),
	AUDIO("menu.mode.file.audio", CarbonIcons.VOLUME_UP),
	VIDEO("menu.mode.file.video", CarbonIcons.VIDEO),
	EXECUTABLE_PE("menu.mode.file.pe", CarbonIcons.CODE),
	EXECUTABLE_ELF("menu.mode.file.elf", CarbonIcons.CODE);

	private final String key;
	private final Ikon ikon;

	FileDisplayMode(@Nonnull String key, @Nonnull Ikon ikon) {
		this.key = key;
		this.ikon = ikon;
	}

	/**
	 * @return Translation key.
	 */
	@Nonnull
	public String getKey() {
		return key;
	}

	/**
	 * @return Display node.
	 */
	@Nonnull
	public Node newIcon() {
		if (this == EXECUTABLE_PE || this == EXECUTABLE_ELF)
			return Icons.getIconView(Icons.FILE_PROGRAM);
		return new FontIconView(ikon);
	}
}
