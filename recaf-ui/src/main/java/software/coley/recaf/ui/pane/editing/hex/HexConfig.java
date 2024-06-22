package software.coley.recaf.ui.pane.editing.hex;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;

/**
 * Config for hex editor.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class HexConfig extends BasicConfigContainer {
	private final ObservableBoolean showAddress = new ObservableBoolean(true);
	private final ObservableBoolean showAscii = new ObservableBoolean(true);
	private final ObservableInteger rowLength = new ObservableInteger(16);
	private final ObservableInteger rowSplitInterval = new ObservableInteger(8);

	@Inject
	public HexConfig() {
		super(ConfigGroups.SERVICE_UI, "hex" + CONFIG_SUFFIX);
	}

	/**
	 * @return {@code true} to show the current row's address/offset.
	 * {@code false} to hide the address/offset display.
	 */
	@Nonnull
	public ObservableBoolean getShowAddress() {
		return showAddress;
	}

	/**
	 * @return {@code true} to show the ascii column.
	 * {@code false} to hide the ascii column.
	 */
	@Nonnull
	public ObservableBoolean getShowAscii() {
		return showAscii;
	}

	/**
	 * @return Number of items to show per row.
	 */
	@Nonnull
	public ObservableInteger getRowLength() {
		return rowLength;
	}

	/**
	 * @return Number of items between each split in the hex column.
	 */
	@Nonnull
	public ObservableInteger getRowSplitInterval() {
		return rowSplitInterval;
	}
}
