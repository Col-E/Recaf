package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import software.coley.recaf.config.BasicConfigContainer;

import static software.coley.recaf.config.ConfigGroups.SERVICE_DECOMPILE_IMPL;

/**
 * Base class for fields needed by all decompiler configurations
 *
 * @author therathatter
 */
public class BaseDecompilerConfig extends BasicConfigContainer implements DecompilerConfig {
	private int hash = 0;

	/**
	 * @param id
	 * 		Container ID.
	 */
	public BaseDecompilerConfig(@Nonnull String id) {
		super(SERVICE_DECOMPILE_IMPL, id);
	}

	@Override
	public int getHash() {
		return hash;
	}

	@Override
	public void setHash(int hash) {
		this.hash = hash;
	}
}
