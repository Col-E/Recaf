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
	 * Max number of threads to use in IO heavy tasks.
	 */
	@Conf(category = "other", key = "maxthreadsio")
	public int maxThreadsIO = 50;
	
	/**
	 * Max number of threads to use in computational tasks.
	 */
	@Conf(category = "other", key = "maxthreadslogic")
	public int maxThreadsLogic = 5;

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
