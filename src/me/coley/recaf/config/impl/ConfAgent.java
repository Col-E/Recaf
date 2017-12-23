package me.coley.recaf.config.impl;

import me.coley.recaf.config.Config;

public class ConfAgent extends Config {
	/**
	 * Automatically update the tree UI when more classes are loaded.
	 */
	public boolean autoRefresh = true;
	
	public ConfAgent() {
		super("rcagent");
	}
}