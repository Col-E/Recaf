package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;

/**
 * Config for {@link ControlFlowLines}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ControlFlowLinesConfig extends BasicConfigContainer {
	private final ObservableObject<ConnectionMode> connectionMode = new ObservableObject<>(ConnectionMode.ALL_CONNECTIONS);
	private final ObservableObject<LineRenderMode> renderMode = new ObservableObject<>(LineRenderMode.FLAT);

	@Inject
	public ControlFlowLinesConfig() {
		super(ConfigGroups.SERVICE_ASSEMBLER, "flow-lines" + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("connection-mode", ConnectionMode.class, connectionMode));
		addValue(new BasicConfigValue<>("render-mode", LineRenderMode.class, renderMode));
	}

	/**
	 * @return Current line connection mode.
	 */
	@Nonnull
	public ObservableObject<ConnectionMode> getConnectionMode() {
		return connectionMode;
	}

	/**
	 * @return Current line render mode.
	 */
	@Nonnull
	public ObservableObject<LineRenderMode> getRenderMode() {
		return renderMode;
	}

	/**
	 * Modes for how to render lines.
	 */
	public enum LineRenderMode {
		/**
		 * Simple flat lines.
		 */
		FLAT,
		/**
		 * Simple flat lines with some glowing.
		 */
		FLAT_GLOWING,
		/**
		 * Party time!
		 */
		RAINBOW_GLOWING
	}

	/**
	 * Modes for where to draw lines.
	 */
	public enum ConnectionMode {
		/**
		 * Show control flow connections for all flow edges.
		 */
		ALL_CONNECTIONS,
		/**
		 * Show control flow connections for only the current item.
		 */
		CURRENT_CONNECTION
	}
}
