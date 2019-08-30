package me.coley.recaf.command.impl;

import me.coley.recaf.workspace.*;
import org.tinylog.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Command for loading the a workspace from a file.
 *
 * @author Matt
 */
@CommandLine.Command(name = "workspaceinfo", description = "Print information about the current workspace.")
public class WorkspaceInfo extends WorkspaceCommand implements Callable<Void> {

	@Override
	public Void call() throws Exception {
		StringBuilder sb = new StringBuilder();
		// Print primary resource
		JavaResource primary = workspace.getPrimary();
		append(sb, primary);
		// Print library resources
		int count = workspace.getLibraries().size();
		sb.append("\nLibraries: " + count);
		if (count > 0) {
			sb.append("\n\n");
			for (JavaResource res : workspace.getLibraries()) {
				append(sb, primary);
				sb.append("\n");
			}
		}
		Logger.info(sb.toString());
		return null;
	}

	private void append(StringBuilder sb, JavaResource resource) {
		sb.append(resource.getKind().name() + " : ");
		switch(resource.getKind()) {
			case CLASS:
			case JAR:
				sb.append(((FileSystemResource)resource).getFile());
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
			default:
				throw new IllegalStateException("Unknown resource kind: " + resource.getKind().name());
		}
		sb.append("\n\t- classes: " + resource.getClasses().size());
		for (String name : resource.getClasses().keySet())
			sb.append("\n\t\t- " + name);
		sb.append("\n\t- resources: " + resource.getResources().size());
		for (String name : resource.getResources().keySet())
			sb.append("\n\t\t- " + name);
		sb.append("\n");
	}
}
