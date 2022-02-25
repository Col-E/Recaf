package me.coley.recaf.decompile.fernflower;

import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.util.jar.Manifest;

/**
 * Due to our modified usage of FernFlower this is needed just to satisfy some backend references.
 *
 * @author Matt
 */
public class DummyCollector implements IResultSaver {

	@Override
	public void saveFolder(String s) {}

	@Override
	public void copyFile(String s, String s1, String s2) {}

	@Override
	public void saveClassFile(String s, String s1, String s2, String s3, int[] ints) {}

	@Override
	public void createArchive(String s, String s1, Manifest manifest) {}

	@Override
	public void saveDirEntry(String s, String s1, String s2) {}

	@Override
	public void copyEntry(String s, String s1, String s2, String s3) {}

	@Override
	public void closeArchive(String s, String s1) {}
}
