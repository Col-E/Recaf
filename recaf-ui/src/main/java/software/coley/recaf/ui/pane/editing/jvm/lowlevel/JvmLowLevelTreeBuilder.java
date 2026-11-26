package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import jakarta.annotation.Nonnull;
import software.coley.cafedude.classfile.ClassFile;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.ui.config.MemberDisplayFormatConfig;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Builds a low-level class-file tree from explicit model registrations.
 *
 * @author Matt Coley
 */
public final class JvmLowLevelTreeBuilder {
	private final Workspace workspace;
	private final MemberDisplayFormatConfig memberDisplay;
	private final CellConfigurationService configurationService;

	JvmLowLevelTreeBuilder(@Nonnull Workspace workspace,
	                       @Nonnull MemberDisplayFormatConfig memberDisplay,
	                       @Nonnull CellConfigurationService configurationService) {
		this.workspace = workspace;
		this.memberDisplay = memberDisplay;
		this.configurationService = configurationService;
	}

	/**
	 * Builds a class-file tree rooted at the supplied class file.
	 *
	 * @param classFile
	 * 		Class file to display.
	 * @param classPath
	 * 		Path of the class represented by the file.
	 *
	 * @return Hidden-root tree item.
	 */
	@Nonnull
	ClassItem build(@Nonnull ClassFile classFile, @Nonnull ClassPathNode classPath) {
		JvmLowLevelContext context = new JvmLowLevelContext(workspace, memberDisplay, configurationService, classPath);
		ClassItemRegistry registry = new ClassItemRegistry();

		JvmConstantPoolItems.register(registry, context);
		JvmAnnotationItems.register(registry, context);
		JvmInstructionItems.register(registry, context);
		JvmStackMapItems.register(registry, context);
		JvmAttributeItems.register(registry, context);
		JvmClassFileItems.register(registry, context);

		return registry.createRoot("Root", classFile);
	}
}
