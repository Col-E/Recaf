package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.data.ClassMapping;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.MethodMapping;
import software.coley.recaf.util.StringUtil;

/**
 * Jadx mappings file implementation.
 * <br>
 * This format type is no longer used as of Jadx 1.4.2.
 * Instead, Jadx now uses Engima & TinyV2.
 *
 * @author Matt Coley
 */
@Dependent
public class JadxMappings extends AbstractMappingFileFormat {
	public static final String NAME = "Jadx (Legacy)";

	/**
	 * New jadx instance.
	 */
	public JadxMappings() {
		super(NAME, true, true);
	}

	@Override
	public IntermediateMappings parse(@Nonnull String mappingText) {
		IntermediateMappings mappings = new IntermediateMappings();
		String[] lines = StringUtil.splitNewline(mappingText);
		// Example:
		// c android.support.a.b.a = C0005a
		// f android.support.a.b.a.a:Ljava/lang/Object; = f3a
		// m android.support.a.a.a.a(Landroid/app/Activity;[Ljava/lang/String;I)V = m0a
		int line = 0;
		for (String lineStr : lines) {
			line++;
			String[] args = lineStr.trim().split("[\\s=:]+");
			String type = args[0];
			try {
				switch (type) {
					case "c":
						// 1: class-name
						// 2: renamed class (does not include package)
						// Replace "." in class name
						String original = args[1].replace('.', '/');
						String packageName = original.substring(0, original.lastIndexOf('/') + 1);
						// The new value is always in the same package.
						// Only the class is renamed, not the package.
						String renamed = packageName + args[2];
						mappings.addClass(original, renamed);
						break;
					case "f":
						// 1: class-name.field-name
						// 2: field-type
						// 3: renamed
						String f1 = args[1].replaceAll("\\.(?=.+\\..+$)", "/");
						String fieldOwner = f1.substring(0, f1.indexOf('.'));
						String fieldName = f1.substring(f1.indexOf('.') + 1);
						String fieldType = args[2];
						String renamedField = args[3];
						// Replace all "." except last one
						mappings.addField(fieldOwner, fieldType, fieldName, renamedField);
						break;
					case "m":
						// 1: class-name.method-name + method-desc
						// 2: renamed
						String m1 = args[1].replaceAll("\\.(?=.+\\..+$)", "/");
						String methodOwner = m1.substring(0, m1.indexOf('.'));
						String methodName = m1.substring(m1.indexOf('.') + 1, m1.indexOf('('));
						String methodType = m1.substring(m1.indexOf('('));
						String renamedMethod = args[2];
						// Replace all "." except last one
						mappings.addMethod(methodOwner, methodType, methodName, renamedMethod);
						break;
					default:
						break;
				}
			} catch (IndexOutOfBoundsException ex) {
				throw new IllegalArgumentException("Invalid jadx mappings, failed parsing line " + line, ex);
			}
		}
		return mappings;
	}

	@Override
	public String exportText(Mappings mappings) {
		StringBuilder sb = new StringBuilder();
		IntermediateMappings intermediate = mappings.exportIntermediate();
		for (String oldClassName : intermediate.getClassesWithMappings()) {
			ClassMapping classMapping = intermediate.getClassMapping(oldClassName);
			if (classMapping != null) {
				String newClassName = classMapping.getNewName();
				// c android.support.a.b.a = C0005a
				sb.append("c ")
						.append(oldClassName.replace('/', '.')).append(" = ")
						.append(newClassName.substring(newClassName.lastIndexOf('/') + 1)).append("\n");
			}
			for (FieldMapping fieldMapping : intermediate.getClassFieldMappings(oldClassName)) {
				String oldFieldName = fieldMapping.getOldName();
				String newFieldName = fieldMapping.getNewName();
				String fieldDesc = fieldMapping.getDesc();
				// f android.support.a.b.a.a:Ljava/lang/Object; = f3a
				sb.append("f ")
						.append(oldClassName.replace('/', '.')).append('.')
						.append(oldFieldName).append(':').append(fieldDesc).append(" = ")
						.append(newFieldName).append("\n");

			}
			for (MethodMapping methodMapping : intermediate.getClassMethodMappings(oldClassName)) {
				String oldMethodName = methodMapping.getOldName();
				String newMethodName = methodMapping.getNewName();
				String methodDesc = methodMapping.getDesc();
				// m android.support.a.a.a.a(Landroid/app/Activity;[Ljava/lang/String;I)V = m0a
				sb.append("m ")
						.append(oldClassName.replace('/', '.')).append('.')
						.append(oldMethodName)
						.append(methodDesc).append(" = ")
						.append(newMethodName).append("\n");
			}
		}
		return sb.toString();
	}
}
