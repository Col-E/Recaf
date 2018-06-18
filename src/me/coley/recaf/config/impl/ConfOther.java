package me.coley.recaf.config.impl;

import me.coley.recaf.config.Conf;
import me.coley.recaf.config.Config;

/**
 * Options for miscellaneous things.
 * 
 * @author Matt
 */
public class ConfOther extends Config {
	/**
	 * Max number of threads to use in multi-threaded tasks.
	 */
	@Conf(category = "other", key = "maxthreads")
	public int maxThreads = 1;

	public ConfOther() {
		super("rc_other");
		load();
	}

	/**
	 * Static getter.
	 * 
	 * @return ConfDisplay instance.
	 */
	public static ConfOther instance() {
		return ConfOther.instance(ConfOther.class);
	}
}
