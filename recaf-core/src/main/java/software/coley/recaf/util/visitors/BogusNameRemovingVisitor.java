package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.WorkspaceClassRemapper;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static software.coley.recaf.util.Keywords.getKeywords;

/**
 * Visitor for renaming bogus names. This is for legibility improvements only and does not ensure the
 * resulting transformed class can be compiled.
 *
 * @author Matt Coley
 */
public class BogusNameRemovingVisitor extends ClassVisitor {
	private final LiveMapper liveMapper;

	private BogusNameRemovingVisitor(@Nonnull Workspace workspace, @Nonnull ClassVisitor cv, @Nonnull LiveMapper liveMapper) {
		super(RecafConstants.getAsmVersion(), new WorkspaceClassRemapper(cv, workspace, liveMapper));
		this.liveMapper = liveMapper;
	}

	/**
	 * @param workspace
	 * 		Workspace to pull classes from. Enhances mapping capabilities.
	 * @param cv
	 * 		Parent visitor.
	 *
	 * @return New bogus name removing visitor.
	 */
	@Nonnull
	public static BogusNameRemovingVisitor create(@Nonnull Workspace workspace, @Nonnull ClassVisitor cv) {
		return new BogusNameRemovingVisitor(workspace, cv, new LiveMapper());
	}

	/**
	 * @return Number of type references renamed.
	 */
	public int getRenamedTypeCount() {
		return liveMapper.mappedTypes;
	}

	/**
	 * @return Number of generic names <i>(field/method names, etc)</i> renamed.
	 */
	public int getRenamedNameCount() {
		return liveMapper.mappedNames;
	}

	@Override
	public void visitEnd() {
		int types = liveMapper.mappedTypes;
		int names = liveMapper.mappedNames;
		if (types > 0 || names > 0) {
			AnnotationVisitor av = visitAnnotation("LRemapped;", true);
			if (types > 0 && names > 0) {
				av.visit("message", "Recaf has remapped " + types + " types, " + names + " names");
			} else if (types > 0 && names == 0) {
				av.visit("message", "Recaf has remapped " + types + " types");
			} else if (types == 0) {
				av.visit("message", "Recaf has remapped " + names + " names");
			}
		}
		super.visitEnd();
	}

	private static class LiveMapper implements Mappings {
		private final Map<String, String> lookup = new HashMap<>();
		private int mappedTypes;
		private int mappedNames;

		@Nonnull
		@Override
		public String getMappedClassName(@Nonnull String internalName) {
			return lookup.computeIfAbsent(internalName, this::className);
		}


		@Nonnull
		@Override
		public String getMappedFieldName(@Nonnull String ownerName, @Nonnull String fieldName,
		                                 @Nonnull String fieldDesc) {
			return lookup.computeIfAbsent(fieldName, this::itemName);
		}

		@Nonnull
		@Override
		public String getMappedMethodName(@Nonnull String ownerName, @Nonnull String methodName,
		                                  @Nonnull String methodDesc) {
			if (methodName.equals("<init>") || methodName.equals("<clinit>")) return methodName;
			return lookup.computeIfAbsent(methodName, this::itemName);
		}

		@Nonnull
		@Override
		public String getMappedVariableName(@Nonnull String className, @Nonnull String methodName,
		                                    @Nonnull String methodDesc, @Nullable String name,
		                                    @Nullable String desc, int index) {
			return lookup.computeIfAbsent(methodName, this::itemName);
		}

		@Nonnull
		@Override
		public IntermediateMappings exportIntermediate() {
			throw new UnsupportedOperationException();
		}

		@Nonnull
		private String className(@Nonnull String name) {
			String original = name;
			name = name.chars()
					.mapToObj(LiveMapper::mapCodePoint)
					.collect(Collectors.joining());
			name = StringUtil.fastSplit(name, true, '/').stream()
					.map(LiveMapper::replaceKeyword)
					.collect(Collectors.joining("/"));
			if (!original.equals(name))
				mappedTypes++;
			return name;
		}


		@Nonnull
		private String itemName(@Nonnull String name) {
			String original = name;
			name = replaceKeyword(name.chars()
					.mapToObj(LiveMapper::mapCodePoint)
					.collect(Collectors.joining()));
			if (!original.equals(name)) mappedNames++;
			return name;
		}

		@Nonnull
		private static String mapCodePoint(int cp) {
			return (Character.isLetterOrDigit(cp) || cp == '/' || cp == '$' || cp == '_') ?
					Character.toString(Character.toChars(cp)[0]) : ("_");
		}

		@Nonnull
		private static String replaceKeyword(@Nonnull String name) {
			if (getKeywords().contains(name))
				return StringUtil.uppercaseFirstChar(name);
			return name;
		}
	}
}
