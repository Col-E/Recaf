package software.coley.recaf.services.analysis.structure;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;

import java.util.List;

/**
 * Group of entry points associated with a class.
 *
 * @param classPath
 * 		Path to the class defining the entry points.
 * @param source
 * 		Source of the entry point discovery.
 * @param memberEntryPoints
 * 		List of entry points discovered in the class.
 * 		Will be empty for {@link EntryPointSource#ANDROID_ACTIVITY}.
 *
 * @author Matt Coley
 */
public record EntryPointGroup(@Nonnull ClassPathNode classPath,
                              @Nonnull EntryPointSource source,
                              @Nonnull List<ClassMemberPathNode> memberEntryPoints) {}
