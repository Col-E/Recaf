package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.Set;

/**
 * Utility methods for constructing paths.
 * <p/>
 * Generally you should use this only when the path creation is
 * a <i>"one time"</i> action. For instance, if you want to make a path to a single method, use
 * {@link #memberPath(Workspace, WorkspaceResource, Bundle, ClassInfo, ClassMember)}.
 * <p/>
 * However, if you want to make a path to <i>all methods in a class</i> then you would use
 * {@link #classPath(Workspace, WorkspaceResource, Bundle, ClassInfo)} to get a {@link ClassPathNode}
 * and then use {@link ClassPathNode#child(ClassMember)} for each member. This reduces the number of redundant
 * allocations of parent path node types in the chain.
 *
 * @author Matt Coley
 */
public class PathNodes {
	private PathNodes() {
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 *
	 * @return Path to a workspace.
	 */
	@Nonnull
	public static WorkspacePathNode workspacePath(@Nonnull Workspace workspace) {
		return new WorkspacePathNode(workspace);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 *
	 * @return Path to resource.
	 */
	@Nonnull
	public static ResourcePathNode resourcePath(@Nonnull Workspace workspace,
	                                            @Nonnull WorkspaceResource resource) {
		// Base case, resource is top-level in the workspace.
		WorkspaceResource containingResource = resource.getContainingResource();
		if (containingResource == null)
			return workspacePath(workspace).child(resource);

		// Resource is embedded, so we need to represent the path a bit differently.
		WorkspaceResource rootResource = containingResource;
		while (rootResource.getContainingResource() != null)
			rootResource = rootResource.getContainingResource();
		return workspacePath(workspace).child(rootResource).embeddedChildContainer().child(resource);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 *
	 * @return Path to bundle.
	 */
	@Nonnull
	public static BundlePathNode bundlePath(@Nonnull Workspace workspace,
	                                        @Nonnull WorkspaceResource resource,
	                                        @Nonnull Bundle<?> bundle) {
		return resourcePath(workspace, resource).child(bundle);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 * @param directory
	 * 		Directory or package name to wrap into path.
	 *
	 * @return Path to directory or package <i>(Depending on bundle type)</i>.
	 */
	@Nonnull
	public static DirectoryPathNode directoryPath(@Nonnull Workspace workspace,
	                                              @Nonnull WorkspaceResource resource,
	                                              @Nonnull Bundle<?> bundle,
	                                              @Nullable String directory) {
		return bundlePath(workspace, resource, bundle).child(directory);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 * @param cls
	 * 		Class to wrap into path.
	 *
	 * @return Path to class.
	 */
	@Nonnull
	public static ClassPathNode classPath(@Nonnull Workspace workspace,
	                                      @Nonnull WorkspaceResource resource,
	                                      @Nonnull Bundle<?> bundle,
	                                      @Nonnull ClassInfo cls) {
		return directoryPath(workspace, resource, bundle, cls.getPackageName()).child(cls);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 * @param cls
	 * 		Class to wrap into path.
	 * @param member
	 * 		Member to wrap into path.
	 *
	 * @return Path to class member <i>(field or method)</i>.
	 */
	@Nonnull
	public static ClassMemberPathNode memberPath(@Nonnull Workspace workspace,
	                                             @Nonnull WorkspaceResource resource,
	                                             @Nonnull Bundle<?> bundle,
	                                             @Nonnull ClassInfo cls,
	                                             @Nonnull ClassMember member) {
		return classPath(workspace, resource, bundle, cls).child(member);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 * @param cls
	 * 		Class to wrap into path.
	 * @param member
	 * 		Member to wrap into path.
	 * @param annotation
	 * 		Annotation on member to wrap into path.
	 *
	 * @return Path to annotation on the member.
	 */
	@Nonnull
	public static AnnotationPathNode annotationPath(@Nonnull Workspace workspace,
	                                                @Nonnull WorkspaceResource resource,
	                                                @Nonnull Bundle<?> bundle,
	                                                @Nonnull ClassInfo cls,
	                                                @Nonnull ClassMember member,
	                                                @Nonnull AnnotationInfo annotation) {
		return memberPath(workspace, resource, bundle, cls, member).childAnnotation(annotation);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 * @param cls
	 * 		Class to wrap into path.
	 * @param method
	 * 		Method to wrap into path.
	 * @param variable
	 * 		Variable in method to wrap into path.
	 *
	 * @return Path to variable in the method.
	 */
	@Nonnull
	public static LocalVariablePathNode variablePath(@Nonnull Workspace workspace,
	                                                 @Nonnull WorkspaceResource resource,
	                                                 @Nonnull Bundle<?> bundle,
	                                                 @Nonnull ClassInfo cls,
	                                                 @Nonnull MethodMember method,
	                                                 @Nonnull LocalVariable variable) {
		return memberPath(workspace, resource, bundle, cls, method).childVariable(variable);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 * @param cls
	 * 		Class to wrap into path.
	 * @param method
	 * 		Method to wrap into path.
	 * @param insn
	 * 		Instruction in method to wrap into path.
	 * @param index
	 * 		Index of the instruction within the method code.
	 *
	 * @return Path to instruction in the method.
	 */
	@Nonnull
	public static InstructionPathNode instructionPath(@Nonnull Workspace workspace,
	                                                  @Nonnull WorkspaceResource resource,
	                                                  @Nonnull Bundle<?> bundle,
	                                                  @Nonnull ClassInfo cls,
	                                                  @Nonnull MethodMember method,
	                                                  @Nonnull AbstractInsnNode insn,
	                                                  int index) {
		return memberPath(workspace, resource, bundle, cls, method).childInsn(insn, index);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 * @param cls
	 * 		Class to wrap into path.
	 * @param method
	 * 		Method to wrap into path.
	 * @param thrownType
	 * 		Type thrown by the method to wrap into path.
	 *
	 * @return Path to the thrown type on the method.
	 */
	@Nonnull
	public static ThrowsPathNode throwsPath(@Nonnull Workspace workspace,
	                                        @Nonnull WorkspaceResource resource,
	                                        @Nonnull Bundle<?> bundle,
	                                        @Nonnull ClassInfo cls,
	                                        @Nonnull MethodMember method,
	                                        @Nonnull String thrownType) {
		return memberPath(workspace, resource, bundle, cls, method).childThrows(thrownType);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 * @param cls
	 * 		Class to wrap into path.
	 * @param method
	 * 		Method to wrap into path.
	 * @param caughtType
	 * 		Exception type caught by a {@code catch(T)} block to wrap into path.
	 *
	 * @return Path to any {@code catch(T)} block in the method of the given exception type.
	 */
	@Nonnull
	public static CatchPathNode catchPath(@Nonnull Workspace workspace,
	                                      @Nonnull WorkspaceResource resource,
	                                      @Nonnull Bundle<?> bundle,
	                                      @Nonnull ClassInfo cls,
	                                      @Nonnull MethodMember method,
	                                      @Nonnull String caughtType) {
		return memberPath(workspace, resource, bundle, cls, method).childCatch(caughtType);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 * @param cls
	 * 		Class to wrap into path.
	 * @param innerCls
	 * 		Inner class to wrap into path.
	 *
	 * @return Path to inner class.
	 */
	@Nonnull
	public static InnerClassPathNode innerClassPath(@Nonnull Workspace workspace,
	                                                @Nonnull WorkspaceResource resource,
	                                                @Nonnull Bundle<?> bundle,
	                                                @Nonnull ClassInfo cls,
	                                                @Nonnull InnerClassInfo innerCls) {
		return classPath(workspace, resource, bundle, cls).child(innerCls);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 * @param cls
	 * 		Class to wrap into path.
	 * @param annotation
	 * 		Annotation on the class to wrap into path.
	 *
	 * @return Path to annotation on the class.
	 */
	@Nonnull
	public static AnnotationPathNode annotationPath(@Nonnull Workspace workspace,
	                                                @Nonnull WorkspaceResource resource,
	                                                @Nonnull Bundle<?> bundle,
	                                                @Nonnull ClassInfo cls,
	                                                @Nonnull AnnotationInfo annotation) {
		return classPath(workspace, resource, bundle, cls).child(annotation);
	}

	/**
	 * @param workspace
	 * 		Workspace to wrap into path.
	 * @param resource
	 * 		Resource to wrap into path.
	 * @param bundle
	 * 		Bundle to wrap into path.
	 * @param file
	 * 		File to wrap into path.
	 *
	 * @return Path to file.
	 */
	@Nonnull
	public static FilePathNode filePath(@Nonnull Workspace workspace,
	                                    @Nonnull WorkspaceResource resource,
	                                    @Nonnull Bundle<?> bundle,
	                                    @Nonnull FileInfo file) {
		return directoryPath(workspace, resource, bundle, file.getDirectoryName()).child(file);
	}

	/**
	 * @param identifier
	 * 		Unique identifier for this path.
	 *
	 * @return Path of unique identifier.
	 */
	@Nonnull
	public static PathNode<?> unique(@Nonnull String identifier) {
		return new ArbitraryStringPathNode(identifier);
	}

	/**
	 * A path node that just holds an arbitrary string.
	 * <p/>
	 * Intended for use in the UI where displayed panels are intended to be navigable for tracking, not actually
	 * navigable in terms of their relationship to some location in a workspace.
	 *
	 * @see #unique(String)
	 */
	private static class ArbitraryStringPathNode extends AbstractPathNode<Object, Object> {
		/**
		 * @param value
		 * 		Value instance.
		 */
		protected ArbitraryStringPathNode(@Nonnull String value) {
			super("uid", null, value);
		}

		@Nonnull
		@Override
		public Set<String> directParentTypeIds() {
			return Collections.emptySet();
		}

		@Override
		public int localCompare(PathNode<?> o) {
			return 0;
		}
	}
}
