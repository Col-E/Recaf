package me.coley.recaf.decompile.jadx.dexcompat;

import com.google.common.collect.Iterables;
import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.data.ISeqConsumer;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.JadxAnnotation;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import me.coley.recaf.android.cf.MutableField;
import me.coley.recaf.android.cf.MutableMethod;
import me.coley.recaf.code.DexClassInfo;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.Adaptors.MethodDefinition;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.baksmali.formatter.BaksmaliWriter;
import org.jf.dexlib2.AnnotationVisibility;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * A JadX class data delegate to DexLib's {@link ClassDef}.
 *
 * @author Matt Coley
 */
public class DexClassData implements IClassData {
	private final ClassDef def;

	public DexClassData(ClassDef def) {
		this.def = def;
	}

	/**
	 * @return DexLib class definition.
	 */
	public ClassDef getDef() {
		return def;
	}

	@Override
	public IClassData copy() {
		// I am far too lazy to bother with a proper copy
		return this;
	}

	@Override
	public String getInputFileName() {
		// This is in-memory, so there is no input file name
		return getType().substring(1, getType().length() - 1);
	}

	@Override
	public String getType() {
		return def.getType();
	}

	@Override
	public int getAccessFlags() {
		return def.getAccessFlags();
	}

	@Override
	public String getSuperType() {
		return def.getSuperclass();
	}

	@Override
	public List<String> getInterfacesTypes() {
		return def.getInterfaces();
	}

	@Override
	public void visitFieldsAndMethods(ISeqConsumer<IFieldData> fieldsConsumer, ISeqConsumer<IMethodData> methodConsumer) {
		fieldsConsumer.init(Iterables.size(def.getFields()));
		for (Field field : def.getFields())
			fieldsConsumer.accept(new DexFieldData(this, field));
		methodConsumer.init(Iterables.size(def.getMethods()));
		for (Method method : def.getMethods())
			methodConsumer.accept(new DexMethodData(this, method));
	}

	@Override
	public List<IJadxAttribute> getAttributes() {
		List<IJadxAttribute> attributes = new ArrayList<>();
		List<JadxAnnotation> runtimeAnnotations = new ArrayList<>();
		List<JadxAnnotation> buildAnnotations = new ArrayList<>();
		for (Annotation annotation : def.getAnnotations()) {
			int vis = annotation.getVisibility();
			if (vis == AnnotationVisibility.SYSTEM) {
				IJadxAttribute attribute = DexCompatUtil.mapAnnotationAttribute(getType(), annotation);
				if (attribute != null)
					attributes.add(attribute);
			} else if (vis == AnnotationVisibility.RUNTIME) {
				runtimeAnnotations.add(DexCompatUtil.mapAnnotation(annotation));
			} else if (vis == AnnotationVisibility.BUILD) {
				buildAnnotations.add(DexCompatUtil.mapAnnotation(annotation));
			}
		}
		// TODO: Where the hell does jadx want me to put these so they show up?
		//  - runtimeAnnotations / buildAnnotations
		return attributes;
	}

	@Override
	public String getDisassembledCode() {
		BaksmaliOptions options = new BaksmaliOptions();
		ClassDefinition classDef = new ClassDefinition(options, def);
		StringWriter stringWriter = new StringWriter();
		try (BaksmaliWriter writer = new BaksmaliWriter(
				stringWriter,
				options.implicitReferences ? def.getType() : null)) {
			classDef.writeTo(writer);
			return stringWriter.toString();
		} catch (IOException ex) {
			return "// Error: " + ex.getMessage();
		}
	}
}
