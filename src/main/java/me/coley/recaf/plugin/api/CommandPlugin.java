package me.coley.recaf.plugin.api;

import java.util.concurrent.Callable;

/**
 * A plugin that can be registered as a pico-cli command.
 * It will need the correct annotations applied to it in the implementing class.
 * <hr>
 * See <a href="https://picocli.info/">picocli.info</a> for more information on how to create
 * a functional command.
 *
 * @author Matt
 */
public interface CommandPlugin extends BasePlugin, Callable<Void> {}
