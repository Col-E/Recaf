package me.coley.recaf.command.impl;

import me.coley.recaf.command.ControllerCommand;
import me.coley.recaf.workspace.*;
import picocli.CommandLine;

import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static me.coley.recaf.util.Log.*;

/**
 * Command for loading the a workspace from a file.
 *
 * @author Matt
 */
@CommandLine.Command(name = "workspaceinfo", description = "Print information about the current workspace.")
public class WorkspaceInfo extends ControllerCommand implements Callable<Void> {

	@Override
	public Void call() throws Exception {
		StringBuilder sb = new StringBuilder();
		// Print primary resource
		JavaResource primary = getWorkspace().getPrimary();
		append(sb, primary);
		// Print library resources
		int count = getWorkspace().getLibraries().size();
		sb.append("\nLibraries: ").append(count);
		if (count > 0) {
			sb.append("\n\n");
			for (JavaResource res : getWorkspace().getLibraries()) {
				append(sb, primary);
				sb.append("\n");
			}
		}
		info(sb.toString());
		return null;
	}

	private void append(StringBuilder sb, JavaResource resource) {
		sb.append(resource.getKind().name()).append(" : ");
		switch(resource.getKind()) {
			case CLASS:
			case JAR:
			case WAR:
			case DIRECTORY:
				sb.append(((FileSystemResource)resource).getPath().normalize());
				break;
			case MAVEN:
				sb.append(((MavenResource)resource).getCoords());
				break;
			case URL:
				sb.append(((UrlResource)resource).getUrl());
				break;
			case INSTRUMENTATION:
				sb.append("Instrumentation");
				break;
			case DEBUGGER:
				sb.append("Debugger");
				break;
			default:
				throw new IllegalStateException("Unknown resource kind: " + resource.getKind().name());
		}
		sb.append("\n- classes: ").append(resource.getClasses().size());
		for (String name : resource.getClasses().keySet().stream().sorted().collect(Collectors.toList()))
			sb.append("\n\t- ").append(name);
		sb.append("\n- files: ").append(resource.getFiles().size());
		for (String name : resource.getFiles().keySet().stream().sorted().collect(Collectors.toList()))
			sb.append("\n\t- ").append(name);
		sb.append("\n");
	}
}
