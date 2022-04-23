package me.coley.recaf.decompile.jadx.dexcompat;

import com.google.common.collect.Iterables;
import jadx.api.plugins.input.data.*;
import jadx.api.plugins.input.data.annotations.JadxAnnotation;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.input.data.impl.CatchData;
import jadx.api.plugins.input.data.impl.DebugInfo;
import jadx.api.plugins.input.data.impl.TryData;
import jadx.api.plugins.input.insns.InsnData;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.Adaptors.MethodDefinition;
import org.jf.baksmali.BaksmaliOptions;
import org.jf.baksmali.formatter.BaksmaliWriter;
import org.jf.dexlib2.AnnotationVisibility;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A JadX method data delegate to DexLib's {@link Method}.
 *
 * @author Matt Coley
 */
public class DexMethodData implements IMethodData {
	private final DexClassData parent;
	private final Method info;
	private final IMethodRef ref;

	public DexMethodData(DexClassData parent, Method info) {
		this.parent = parent;
		this.info = info;
		this.ref = new IMethodRef() {

			@Override
			public String getReturnType() {
				return info.getReturnType();
			}

			@Override
			public List<String> getArgTypes() {
				return info.getParameterTypes()
						.stream().map(CharSequence::toString)
						.collect(Collectors.toList());
			}

			@Override
			public int getUniqId() {
				return 0;
			}

			@Override
			public void load() {
				// no-op
			}

			@Override
			public String getParentClassType() {
				return parent.getType();
			}

			@Override
			public String getName() {
				return info.getName();
			}
		};
	}

	@Override
	public IMethodRef getMethodRef() {
		return ref;
	}

	@Override
	public int getAccessFlags() {
		return info.getAccessFlags();
	}

	@Override
	public List<IJadxAttribute> getAttributes() {
		List<IJadxAttribute> attributes = new ArrayList<>();
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
	public ICodeReader getCodeReader() {
		// TODO: what?
		return new ICodeReader() {
			private int units;

			@Override
			public ICodeReader copy() {
				return this;
			}

			@Override
			public void visitInstructions(Consumer<InsnData> insnConsumer) {
				MethodImplementation impl = info.getImplementation();
				if (impl == null)
					return;
				for (Instruction instruction : impl.getInstructions()) {
					// TODO: Map instructions
					units += instruction.getCodeUnits();
				}
			}

			@Override
			public int getRegistersCount() {
				MethodImplementation impl = info.getImplementation();
				if (impl != null)
					return impl.getRegisterCount();
				return 0;
			}

			@Override
			public int getArgsStartReg() {
				return 0;
			}

			@Override
			public int getUnitsCount() {
				return units;
			}

			@Override
			public IDebugInfo getDebugInfo() {
				return new DebugInfo(Collections.emptyMap(), Collections.emptyList());
			}

			@Override
			public int getCodeOffset() {
				return 0;
			}

			@Override
			public List<ITry> getTries() {
				MethodImplementation impl = info.getImplementation();
				if (impl == null)
					return Collections.emptyList();
				List<ITry> tries = new ArrayList<>();
				info.getImplementation().getTryBlocks().forEach(t -> {
					int start = t.getStartCodeAddress();
					int end = start + t.getCodeUnitCount();
					int count = Iterables.size(t.getExceptionHandlers());
					int[] handlers = new int[count];
					String[] types = new String[count];
					int i = 0;
					for (ExceptionHandler e : t.getExceptionHandlers()) {
						handlers[i] = e.getHandlerCodeAddress();
						types[i] = e.getExceptionType();
					}
					ICatch handler = new CatchData(handlers, types, -1);
					tries.add(new TryData(start, end, handler));
				});
				return tries;
			}
		};
	}

	@Override
	public String disassembleMethod() {
		BaksmaliOptions options = new BaksmaliOptions();
		ClassDef def = parent.getDef();
		ClassDefinition classDef = new ClassDefinition(options, def);
		MethodDefinition methodDefinition = new MethodDefinition(classDef, info, info.getImplementation());
		StringWriter stringWriter = new StringWriter();
		try (BaksmaliWriter writer = new BaksmaliWriter(
				stringWriter,
				options.implicitReferences ? def.getType() : null)) {
			methodDefinition.writeTo(writer);
			return stringWriter.toString();
		} catch (IOException ex) {
			return "// Error: " + ex.getMessage();
		}
	}
}
