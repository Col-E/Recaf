package software.coley.recaf.services.comment;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.properties.builtin.InputFilePathProperty;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.tutorial.TutorialWorkspaceResource;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceRemoteVmResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A key to uniquely identify a comment in a workspace.
 *
 * @param workspaceHash
 * 		Hash of workspace.
 * @param pathHash
 * 		Hash of path where the comment resides.
 *
 * @author Matt Coley
 */
public record CommentKey(int workspaceHash, int pathHash) {
	/** The current username of the system should be unique enough to use as a salt, and should always exist. */
	private static final int SALT = System.getProperty("user.name", "recaf").hashCode();

	/**
	 * @param path
	 * 		Path in a workspace.
	 *
	 * @return Key for path location.
	 */
	@Nonnull
	public static CommentKey id(@Nonnull PathNode<?> path) {
		int workspaceHash = hashWorkspace(Objects.requireNonNull(path.getValueOfType(Workspace.class), "Path did not contain 'workspace' node"));
		int pathHash = hashPath(path);
		return new CommentKey(workspaceHash, pathHash);
	}

	/**
	 * @param workspace
	 * 		Workspace input.
	 *
	 * @return Hash of workspace.
	 */
	public static int hashWorkspace(@Nonnull Workspace workspace) {
		String input = workspaceInput(workspace);
		return input.hashCode();
	}

	/**
	 * @param path
	 * 		Path input.
	 *
	 * @return Hash of path.
	 */
	public static int hashPath(@Nonnull PathNode<?> path) {
		// We only want to hash portions of a path that we support.
		//  - Classes
		//  - Members (Fields/methods)
		int baseHash;
		if (path instanceof ClassPathNode classPath) {
			baseHash = classPath.getValue().getName().hashCode();
		} else if (path instanceof ClassMemberPathNode memberPath) {
			ClassMember member = memberPath.getValue();
			int hash = 31 * member.getName().hashCode() + member.getDescriptor().hashCode();
			PathNode<?> parent = path.getParent();
			if (parent != null)
				hash = 31 * hashPath(parent) + hash;
			baseHash = hash;
		} else {
			// Unsupported path content.
			return -1;
		}

		// Because class and member names are known to the authors of the code, they could predict the hashes.
		// Thus, we salt the hash with information from the local system, which cannot realistically be predicted.
		// This is a bit overkill since all that they could do is insert the same annotation we'd normally create,
		// but because the actual comment data is stored externally they can't insert unintended comments.
		return SALT + 31 * baseHash;
	}

	/**
	 * @param workspace
	 * 		Workspace instance.
	 *
	 * @return Path of input from workspace.
	 */
	@Nonnull
	static String workspaceInput(@Nonnull Workspace workspace) {
		// The workspace hashCode reflects the state of its contents (which updates as the user makes changes)
		// We want to generate a key based on some info that is consistent and re-producible over time.
		// Ideally we can get a sort of 'source' of the loaded content from each primary resource of the given workspace.
		WorkspaceResource resource = workspace.getPrimaryResource();
		switch (resource) {
			case WorkspaceFileResource fileResource -> {
				// Hash based on file path of input, or file name if full path not known.
				FileInfo fileInfo = fileResource.getFileInfo();
				Path path = InputFilePathProperty.get(fileInfo);
				return path == null ? fileInfo.getName() : path.toString();
			}
			case WorkspaceDirectoryResource directoryResource -> {
				// Hash based on directory path of input.
				return directoryResource.getDirectoryPath().toString();
			}
			case WorkspaceRemoteVmResource remoteVmResource -> {
				// Hash based on VM id, which is generally consistent when re-running the same application.
				return remoteVmResource.getVirtualMachine().id();
			}
			case TutorialWorkspaceResource tutorialResource -> {
				// Constant name for the tutorial.
				return TutorialWorkspaceResource.COMMENT_KEY;
			}
			default -> {
			}
		}

		// Unsupported workspace content.
		return "unknown-workspace";
	}

	/**
	 * @return Annotation descriptor of this comment key.
	 */
	@Nonnull
	public String annotationDescriptor() {
		String paddedWorkspaceHash = StringUtil.fillLeft(8, "0", Integer.toHexString(workspaceHash));
		String paddedPathHash = StringUtil.fillLeft(8, "0", Integer.toHexString(pathHash));
		return "LRecafComment_" + paddedWorkspaceHash + '_' + paddedPathHash + ';';
	}

	@Override
	public String toString() {
		return annotationDescriptor();
	}
}
