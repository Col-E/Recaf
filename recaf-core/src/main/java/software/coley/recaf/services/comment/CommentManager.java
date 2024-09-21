package software.coley.recaf.services.comment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.davidmoten.text.utils.WordWrap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.EagerInitialization;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.decompile.filter.JvmBytecodeFilter;
import software.coley.recaf.services.decompile.filter.OutputTextFilter;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.services.json.GsonProvider;
import software.coley.recaf.services.mapping.*;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.TestEnvironment;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager for comment tracking on {@link ClassInfo} and {@link ClassMember} content in workspaces.
 *
 * @author Matt Coley
 */
@EagerInitialization // We need to eagerly init so that we can register hooks in decompilation
@ApplicationScoped
public class CommentManager implements Service, CommentUpdateListener, CommentContainerListener {
	public static final String SERVICE_ID = "comments";
	private static final Logger logger = Logging.get(CommentManager.class);
	/** Map of workspace comment impls used to fire off listener calls, delegates to persist map entries */
	private final Map<String, DelegatingWorkspaceComments> delegatingMap = new ConcurrentHashMap<>();
	/** Map of workspace comment impls modeling only data. Used for persistence. */
	private final Map<String, PersistWorkspaceComments> persistMap = new ConcurrentHashMap<>();
	private final List<CommentUpdateListener> commentUpdateListeners = new CopyOnWriteArrayList<>();
	private final List<CommentContainerListener> commentContainerListeners = new CopyOnWriteArrayList<>();
	private final WorkspaceManager workspaceManager;
	private final RecafDirectoriesConfig directoriesConfig;
	private final CommentManagerConfig config;
	private final GsonProvider gsonProvider;

	@Inject
	public CommentManager(@Nonnull DecompilerManager decompilerManager, @Nonnull WorkspaceManager workspaceManager,
	                      @Nonnull MappingListeners mappingListeners, @Nonnull GsonProvider gsonProvider,
	                      @Nonnull RecafDirectoriesConfig directoriesConfig, @Nonnull CommentManagerConfig config) {
		this.workspaceManager = workspaceManager;
		this.gsonProvider = gsonProvider;
		this.directoriesConfig = directoriesConfig;
		this.config = config;

		// Register input filter to insert comment identifier annotations.
		JvmBytecodeFilter keyInsertingFilter = new JvmBytecodeFilter() {
			@Nonnull
			@Override
			public byte[] filter(@Nonnull Workspace workspace, @Nonnull JvmClassInfo initialClassInfo, @Nonnull byte[] bytecode) {
				// Skip if comment insertion is disabled.
				if (!config.getEnableCommentDisplay().hasValue())
					return bytecode;

				// Skip if there are no comments in the workspace.
				WorkspaceComments comments = getWorkspaceComments(workspace);
				if (comments == null)
					return bytecode;

				// Skip if the class is not found in the workspace.
				ClassPathNode classPath = workspace.findClass(initialClassInfo.getName());
				if (classPath == null)
					return bytecode;

				// Skip if there are no comments for the class.
				ClassComments classComments = comments.getClassComments(classPath);
				if (classComments == null)
					return bytecode;

				// Adapt with comment annotations.
				ClassWriter writer = new ClassWriter(0);
				ClassReader reader = new ClassReader(bytecode);
				reader.accept(new CommentInsertingVisitor(classComments, classPath, writer), 0);
				return writer.toByteArray();
			}
		};
		OutputTextFilter keyReplacementFilter = new OutputTextFilter() {
			private static final String KEY = "@RecafComment_";

			@Nonnull
			@Override
			public String filter(@Nonnull Workspace workspace, @Nonnull ClassInfo classInfo, @Nonnull String code) {
				int codeLength = code.length();
				int keyLength = KEY.length();
				int i = codeLength;

				// Get class comments container if it exists.
				ClassPathNode classPath = workspace.findClass(classInfo.getName());
				if (classPath == null)
					return code;
				WorkspaceComments comments = persistMap.get(CommentKey.workspaceInput(workspace));
				if (comments == null)
					return code;
				ClassComments classComments = comments.getClassComments(classPath);
				if (classComments == null)
					return code;

				do {
					int commentIndex = code.lastIndexOf(KEY, i);
					if (commentIndex < 0)
						break;
					i = commentIndex - 1; // Move backwards

					// Extract path key text from annotation name.
					int keyValueStart = commentIndex + keyLength;
					int endWorkspaceKey = Math.min(codeLength, keyValueStart + 8);
					int endPathKey = Math.min(codeLength, keyValueStart + 17);
					String pathKeyText = code.substring(endWorkspaceKey + 1, endPathKey);
					try {
						// The values are integer hashCodes converted to unsigned hex.
						// Casting back will put em back to the expected value.
						//  Initial hash: -1845811502
						//  Unsigned:      2449155794 (out of int bounds)
						//  Casting will return to the original value.
						int pathKey = (int) Long.parseLong(pathKeyText, 16);

						// Lookup what path in the class correlates to the path-key.
						String comment = null;
						int pathHash = CommentKey.hashPath(classPath);
						if (pathHash == pathKey) {
							comment = classComments.getClassComment();
						} else {
							for (FieldMember field : classInfo.getFields()) {
								pathHash = CommentKey.hashPath(classPath.child(field));
								if (pathHash == pathKey) {
									comment = classComments.getFieldComment(field);
									break;
								}
							}
							if (comment == null) {
								for (MethodMember method : classInfo.getMethods()) {
									pathHash = CommentKey.hashPath(classPath.child(method));
									if (pathHash == pathKey) {
										comment = classComments.getMethodComment(method);
										break;
									}
								}
							}
						}

						// Replace the annotation.
						String replacement;
						if (comment == null) {
							replacement = "";
						} else {
							int wordWrapLimit = config.getWordWrappingLimit().getValue();
							if (comment.length() > wordWrapLimit) {
								comment = WordWrap.from(comment)
										.breakWords(false)
										.maxWidth(wordWrapLimit)
										.wrap();
							}

							if (comment.contains("\n")) {
								// The calculated indent includes the '\n' so we can just for-each the comment lines and prefix it.
								String indent = code.substring(Math.max(0, code.lastIndexOf("\n", commentIndex)), commentIndex);
								StringBuilder sb = new StringBuilder("/**");
								comment.lines().forEach(line -> sb.append(indent).append(" * ").append(line));
								sb.append(indent).append(" */");
								replacement = sb.toString();
							} else {
								replacement = "/** " + comment + " */";
							}
						}
						code = StringUtil.replaceRange(code, commentIndex, commentIndex + 31, replacement);
					} catch (NumberFormatException ignored) {
						// Bogus anno
					}
				} while (true);
				return code;
			}
		};
		decompilerManager.addJvmBytecodeFilter(keyInsertingFilter);
		decompilerManager.addOutputTextFilter(keyReplacementFilter);

		// Restore any saved comments from disk.
		loadComments();

		// Register mapping listeners so that when types & members are renamed the comments are migrated.
		mappingListeners.addMappingApplicationListener(new MappingApplicationListener() {
			@Override
			public void onPreApply(@Nonnull MappingResults mappingResults) {
				WorkspaceComments comments = getCurrentWorkspaceComments();
				if (comments == null)
					return;

				// There are comments that may need to be updated.
				Mappings mappings = mappingResults.getMappings();
				BasicMappingsRemapper remapper = new BasicMappingsRemapper(mappings);
				mappingResults.streamPreToPostMappingPaths().forEach(pair -> {
					ClassPathNode preMapped = pair.getLeft();
					ClassPathNode postMapped = pair.getRight();

					// Skip if class has no comments.
					ClassComments classComments = comments.getClassComments(preMapped);
					if (classComments == null)
						return;

					// If the class name changed, we will want to migrate the comment to a new target container.
					ClassInfo preClassInfo = preMapped.getValue();
					String preClassName = preClassInfo.getName();
					ClassComments targetComments;
					boolean isNewClassContainer;
					if (!preClassName.equals(postMapped.getValue().getName())) {
						comments.deleteClassComments(preMapped);
						targetComments = comments.getOrCreateClassComments(postMapped);
						targetComments.setClassComment(classComments.getClassComment());
						isNewClassContainer = true;
					} else {
						targetComments = classComments;
						isNewClassContainer = false;
					}

					// Migrate field comments.
					for (FieldMember field : preClassInfo.getFields()) {
						String fieldName = field.getName();
						String fieldDesc = field.getDescriptor();

						// If the field is mapped, or the class is mapped, migrate to the new definition.
						String targetFieldName = mappings.getMappedFieldName(preClassName, fieldName, fieldDesc);
						if (targetFieldName == null && isNewClassContainer)
							targetFieldName = fieldName;
						if (targetFieldName == null)
							continue;
						String comment = classComments.getFieldComment(fieldName, fieldDesc);
						if (comment != null) {
							// Clear old comment, set in target container with up-to-date field declaration.
							if (!isNewClassContainer) classComments.setFieldComment(fieldName, fieldDesc, null);
							targetComments.setFieldComment(targetFieldName, remapper.mapDesc(fieldDesc), comment);
						}
					}

					// Migrate method comments.
					for (MethodMember method : preClassInfo.getMethods()) {
						String methodName = method.getName();
						String methodDesc = method.getDescriptor();

						// If the method mapped, or the class is mapped, migrate to the new definition.
						String targetMethodName = mappings.getMappedMethodName(preClassName, methodName, methodDesc);
						if (targetMethodName == null && isNewClassContainer)
							targetMethodName = methodName;
						if (targetMethodName == null)
							continue;
						String comment = classComments.getMethodComment(methodName, methodDesc);
						if (comment != null) {
							// Clear old comment, set in target container with up-to-date method declaration.
							if (!isNewClassContainer) classComments.setMethodComment(methodName, methodDesc, null);
							targetComments.setMethodComment(targetMethodName, remapper.mapMethodDesc(methodDesc), comment);
						}
					}
				});
			}

			@Override
			public void onPostApply(@Nonnull MappingResults mappingResults) {
				// no-op
			}
		});
	}

	/**
	 * Loads comments from disk.
	 */
	private void loadComments() {
		// Skip loading in test environment
		if (TestEnvironment.isTestEnv())
			return;

		try {
			// TODO: Its not ideal having all comments across all workspaces loaded at once
			//  - Not a big deal right now since I doubt most users will utilize this feature much
			Gson gson = gsonProvider.getGson();
			Path commentsDirectory = getCommentsDirectory();
			Path store = commentsDirectory.resolve("comments.json");
			if (Files.exists(store)) {
				String json = Files.readString(store);
				var deserialized = gson.fromJson(json, new TypeToken<Map<String, PersistWorkspaceComments>>() {});
				persistMap.putAll(deserialized);
			}
		} catch (Throwable t) {
			logger.error("Failed to load comments", t);
		}
	}

	/**
	 * Persists comments to disk when shutdown is observed.
	 */
	@PreDestroy
	private void onShutdown() {
		// Skip persist in test environment
		if (TestEnvironment.isTestEnv())
			return;

		try {
			Gson gson = gsonProvider.getGson();
			String serialized = gson.toJson(persistMap);
			Path commentsDirectory = getCommentsDirectory();
			if (!Files.isDirectory(commentsDirectory))
				Files.createDirectories(commentsDirectory);
			Path store = commentsDirectory.resolve("comments.json");
			Files.writeString(store, serialized);
		} catch (Throwable t) {
			logger.error("Failed to save comments", t);
		}
	}

	@Override
	public void onClassCommentUpdated(@Nonnull ClassPathNode path, @Nullable String comment) {
		Unchecked.checkedForEach(commentUpdateListeners, listener -> listener.onClassCommentUpdated(path, comment),
				(listener, t) -> logger.error("Exception thrown when updating class comment", t));
	}

	@Override
	public void onFieldCommentUpdated(@Nonnull ClassMemberPathNode path, @Nullable String comment) {
		Unchecked.checkedForEach(commentUpdateListeners, listener -> listener.onFieldCommentUpdated(path, comment),
				(listener, t) -> logger.error("Exception thrown when updating field comment", t));
	}

	@Override
	public void onMethodCommentUpdated(@Nonnull ClassMemberPathNode path, @Nullable String comment) {
		Unchecked.checkedForEach(commentUpdateListeners, listener -> listener.onMethodCommentUpdated(path, comment),
				(listener, t) -> logger.error("Exception thrown when updating method comment", t));
	}

	@Override
	public void onClassContainerCreated(@Nonnull ClassPathNode path, @Nullable ClassComments comments) {
		Unchecked.checkedForEach(commentContainerListeners, listener -> listener.onClassContainerCreated(path, comments),
				(listener, t) -> logger.error("Exception thrown when creating class comment container", t));
	}

	@Override
	public void onClassContainerRemoved(@Nonnull ClassPathNode path, @Nullable ClassComments comments) {
		Unchecked.checkedForEach(commentContainerListeners, listener -> listener.onClassContainerRemoved(path, comments),
				(listener, t) -> logger.error("Exception thrown when removing class comment container", t));
	}

	/**
	 * @param workspace
	 * 		Workspace to check for comments.
	 *
	 * @return Comments container for the given workspace, creating one if none existed.
	 */
	@Nonnull
	public WorkspaceComments getOrCreateWorkspaceComments(@Nonnull Workspace workspace) {
		WorkspaceComments comments = getWorkspaceComments(workspace);
		if (comments == null) {
			// No existing comments found, lets create them.
			// - One entry for persistence
			// - One entry for listener callbacks, delegating to the persist model
			String input = CommentKey.workspaceInput(workspace);
			PersistWorkspaceComments persistComments = persistMap.computeIfAbsent(input, i -> new PersistWorkspaceComments());
			DelegatingWorkspaceComments delegatingComments = newDelegatingWorkspaceComments(workspace, persistComments);
			delegatingMap.put(input, delegatingComments);

			// We want to yield the delegating model for listener support.
			comments = delegatingComments;
		}
		return comments;
	}

	/**
	 * @param workspace
	 * 		Workspace to check for comments.
	 *
	 * @return Comments container for the given workspace, if any comments exist.
	 * If there are no comments, then {@code null}.
	 */
	@Nullable
	public WorkspaceComments getWorkspaceComments(@Nonnull Workspace workspace) {
		String input = CommentKey.workspaceInput(workspace);
		PersistWorkspaceComments persistComments = persistMap.get(input);
		if (persistComments == null)
			return null; // No persist model, so there are no comments.

		// Wrap the persist model with a delegating model for listener support.
		return delegatingMap.computeIfAbsent(input, i -> newDelegatingWorkspaceComments(workspace, persistComments));
	}

	/**
	 * @return Comments container for the current workspace, if any comments exist.
	 * If there are no comments, then {@code null}.
	 * If there is no current workspace, then {@code null}.
	 */
	@Nullable
	public WorkspaceComments getCurrentWorkspaceComments() {
		Workspace current = workspaceManager.getCurrent();
		if (current == null)
			return null;
		return getOrCreateWorkspaceComments(current);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addCommentListener(@Nonnull CommentUpdateListener listener) {
		commentUpdateListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removeCommentListener(@Nonnull CommentUpdateListener listener) {
		commentUpdateListeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addCommentContainerListener(@Nonnull CommentContainerListener listener) {
		commentContainerListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removeCommentContainerListener(@Nonnull CommentContainerListener listener) {
		commentContainerListeners.remove(listener);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public CommentManagerConfig getServiceConfig() {
		return config;
	}

	@Nonnull
	private Path getCommentsDirectory() {
		return directoriesConfig.getBaseDirectory().resolve("comments");
	}

	@Nonnull
	private DelegatingWorkspaceComments newDelegatingWorkspaceComments(@Nonnull Workspace workspace,
	                                                                   @Nonnull PersistWorkspaceComments persistComments) {
		DelegatingWorkspaceComments delegatingComments = new DelegatingWorkspaceComments(this, persistComments);

		// Initialize delegate class comment models for entries in the persist model.
		for (String classKey : persistComments.classKeys()) {
			ClassPathNode classPath = workspace.findClass(classKey);
			if (classPath != null)
				delegatingComments.getClassComments(classPath);
		}

		return delegatingComments;
	}
}
