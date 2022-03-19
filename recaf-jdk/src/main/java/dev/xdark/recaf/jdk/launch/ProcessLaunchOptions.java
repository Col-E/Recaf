package dev.xdark.recaf.jdk.launch;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * JVM launch options.
 *
 * @author xDark
 */
public final class ProcessLaunchOptions {

	private final List<String> processArgs = new ArrayList<>();
	private final List<String> programArgs = new ArrayList<>();
	private Path jdkExecutable;

	public void addJvmArgs(String arg) {
		processArgs.add(arg);
	}

	public void addJvmArgs(String... args) {
		processArgs.addAll(Arrays.asList(args));
	}

	public void addJvmArgs(Iterable<String> iterable) {
		iterable.forEach(x -> processArgs.add(x)); // Don't refactor this, IDEA is stupid
	}

	public void addProgramArgs(String arg) {
		programArgs.add(arg);
	}

	public void addProgramArgs(String... args) {
		programArgs.addAll(Arrays.asList(args));
	}

	public void addProgramArgs(Iterable<String> iterable) {
		iterable.forEach(x -> programArgs.add(x)); // Don't refactor this, IDEA is stupid
	}

	public void setExecutable(Path jdkExecutable) {
		this.jdkExecutable = jdkExecutable;
	}

	public Process start(ProcessBuilder builder) throws IOException {
		List<String> command = new LinkedList<>();
		command.add(jdkExecutable.toString());
		command.addAll(processArgs);
		command.addAll(programArgs);
		return builder.command(command).start();
	}

	public Process start() throws IOException {
		return start(new ProcessBuilder());
	}
}
