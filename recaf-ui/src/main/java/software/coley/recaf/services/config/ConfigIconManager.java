package software.coley.recaf.services.config;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.Service;

import java.util.HashMap;
import java.util.Map;

import static software.coley.recaf.config.ConfigGroups.*;

/**
 * Manages icons to display for {@link ConfigContainer} and {@link ConfigValue} entries when displayed in the UI.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ConfigIconManager implements Service {
	public static final String ID = "config-icons";
	private final Map<String, Ikon> valueIcons = new HashMap<>();
	private final Map<String, Ikon> containerIcons = new HashMap<>();
	private final Map<String, Ikon> groupIcons = new HashMap<>();
	private final ConfigIconManagerConfig config;

	@Inject
	public ConfigIconManager(@Nonnull ConfigIconManagerConfig config) {
		this.config = config;

		// Add defaults
		registerGroup(SERVICE, CarbonIcons.DATA_CLASS);
		registerGroup(SERVICE_ANALYSIS, CarbonIcons.COGNITIVE);
		registerGroup(SERVICE_COMPILE, CarbonIcons.CODE);
		registerGroup(SERVICE_DEBUG, CarbonIcons.DEBUG);
		registerGroup(SERVICE_DECOMPILE, CarbonIcons.CODE);
		registerGroup(SERVICE_IO, CarbonIcons.ARROWS_HORIZONTAL);
		registerGroup(SERVICE_MAPPING, CarbonIcons.MAP_BOUNDARY);
		registerGroup(SERVICE_PLUGIN, CarbonIcons.PLUG);
		registerGroup(SERVICE_UI, CarbonIcons.DICOM_OVERLAY);
	}

	/**
	 * @param id
	 *        {@link ConfigValue#getId()}.
	 * @param icon
	 * 		Icon to set.
	 */
	public void registerValue(@Nonnull String id, @Nonnull Ikon icon) {
		valueIcons.putIfAbsent(id, icon);
	}

	/**
	 * @param id
	 *        {@link ConfigContainer#getId()}.
	 * @param icon
	 * 		Icon to set.
	 */
	public void registerContainer(@Nonnull String id, @Nonnull Ikon icon) {
		containerIcons.putIfAbsent(id, icon);
	}

	/**
	 * @param group
	 *        {@link ConfigContainer#getGroup()}.
	 * @param icon
	 * 		Icon to set.
	 */
	public void registerGroup(@Nonnull String group, @Nonnull Ikon icon) {
		groupIcons.putIfAbsent(group, icon);
	}

	/**
	 * @param value
	 * 		Value to get icon of.
	 *
	 * @return Associated icon. May be {@code null} for no association.
	 */
	@Nullable
	public Ikon getValueIcon(@Nonnull ConfigValue<?> value) {
		return getValueIcon(value.getId());
	}

	/**
	 * @param id
	 * 		Value of a {@link ConfigValue#getId()}.
	 *
	 * @return Associated icon. May be {@code null} for no association.
	 */
	@Nullable
	public Ikon getValueIcon(@Nonnull String id) {
		return valueIcons.get(id);
	}

	/**
	 * @param container
	 * 		Container to get icon of for it's {@link ConfigContainer#getId()}.
	 *
	 * @return Associated icon. May be {@code null} for no association.
	 */
	@Nullable
	public Ikon getContainerIcon(@Nonnull ConfigContainer container) {
		return getContainerIcon(container.getId());
	}

	/**
	 * @param id
	 * 		Value of a {@link ConfigContainer#getId()}.
	 *
	 * @return Associated icon. May be {@code null} for no association.
	 */
	@Nullable
	public Ikon getContainerIcon(@Nonnull String id) {
		return containerIcons.get(id);
	}

	/**
	 * @param container
	 * 		Container to get icon of for it's {@link ConfigContainer#getGroup()}.
	 *
	 * @return Associated icon. May be {@code null} for no association.
	 */
	@Nullable
	public Ikon getGroupIcon(@Nonnull ConfigContainer container) {
		return getGroupIcon(container.getGroup());
	}

	/**
	 * @param group
	 * 		Value of a {@link ConfigContainer#getGroup()}.
	 *
	 * @return Associated icon. May be {@code null} for no association.
	 */
	@Nullable
	public Ikon getGroupIcon(@Nonnull String group) {
		return groupIcons.get(group);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return ID;
	}

	@Nonnull
	@Override
	public ConfigIconManagerConfig getServiceConfig() {
		return config;
	}
}
