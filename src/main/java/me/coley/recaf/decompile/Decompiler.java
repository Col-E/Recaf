package me.coley.recaf.decompile;

public abstract class Decompiler {

	// TODO: Find a way to implement options that works nicely with all decompilers
	// - CFR (Strings are decoded)
	// ---- https://github.com/Col-E/Recaf/blob/master/src/main/java/me/coley/recaf/util/CFRPipeline.java
	// - Fernflower
	// ---- https://github.com/fesh0r/fernflower/blob/master/src/org/jetbrains/java/decompiler/main/decompiler/BaseDecompiler.java

	abstract String decompile(byte[] code);
}