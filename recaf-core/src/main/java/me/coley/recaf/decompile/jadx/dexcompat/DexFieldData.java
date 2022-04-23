package me.coley.recaf.decompile.jadx.dexcompat;

import jadx.api.plugins.input.data.IFieldData;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.JadxAnnotation;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import org.jf.dexlib2.AnnotationVisibility;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Field;

import java.util.ArrayList;
import java.util.List;

/**
 * A JadX field data delegate to DexLib's {@link Field}.
 *
 * @author Matt Coley
 */
public class DexFieldData implements IFieldData {
	private final DexClassData parent;
	private final Field info;

	public DexFieldData(DexClassData parent, Field info) {
		this.info = info;
		this.parent = parent;
	}

	@Override
	public int getAccessFlags() {
		return info.getAccessFlags();
	}

	@Override
	public List<IJadxAttribute> getAttributes() {
		List<IJadxAttribute> attributes = new ArrayList<>();
		if (info.getInitialValue() != null) {
			org.jf.dexlib2.iface.value.EncodedValue value = info.getInitialValue();
			EncodedValue jadxValue = DexCompatUtil.mapValue(value);
			attributes.add(jadxValue);
		}
		List<JadxAnnotation> runtimeAnnotations = new ArrayList<>();
		List<JadxAnnotation> buildAnnotations = new ArrayList<>();
		for (Annotation annotation : info.getAnnotations()) {
			int vis = annotation.getVisibility();
			if (vis == AnnotationVisibility.SYSTEM) {
				IJadxAttribute attribute = DexCompatUtil.mapAnnotationAttribute(parent.getType(), annotation);
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
	public String getParentClassType() {
		return parent.getType();
	}

	@Override
	public String getName() {
		return info.getName();
	}

	@Override
	public String getType() {
		return info.getType();
	}
}
