package me.coley.recaf.kotlin;

import kotlin.reflect.jvm.internal.impl.BitEncoding;
import kotlin.reflect.jvm.internal.impl.MetadataNameResolver;
import kotlin.reflect.jvm.internal.impl.metadata.ProtoBuf;
import kotlin.reflect.jvm.internal.impl.metadata.jvm.JvmProtoBuf;
import kotlin.reflect.jvm.internal.impl.protobuf.ExtensionRegistryLite;
import me.coley.recaf.RecafConstants;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;

/**
 * Container for information extracted from Kotlin's {@code @Metadata} annotation.
 *
 * @author Matt Coley
 */
public class KotlinMetadata {
	private static final Logger logger = Logging.get(KotlinMetadata.class);
	private static final ExtensionRegistryLite EXTENSIONS = ExtensionRegistryLite.newInstance();
	private static final String METADATA_DESC = "Lkotlin/Metadata;";
	private final ProtoBuf.Class cls;
	private final MetadataNameResolver resolver;

	static {
		JvmProtoBuf.registerAllExtensions(EXTENSIONS);
	}

	private KotlinMetadata(ProtoBuf.Class cls, MetadataNameResolver resolver) {
		// TODO: Instead of storing these, convert to a friendly model (outlined by this class)
		//  - Need to track additional info pulled from 'KotlinMetadataVisitor'
		this.cls = cls;
		this.resolver = resolver;
	}

	public ProtoBuf.Class getCls() {
		return cls;
	}

	public MetadataNameResolver getResolver() {
		return resolver;
	}

	/**
	 * @param reader
	 * 		Reader wrapper of class to extract Metadata from.
	 *
	 * @return Data extracted from visitor.
	 */
	@Nullable
	public static KotlinMetadata from(byte[] reader) {
		return from(new ClassReader(reader));
	}

	/**
	 * @param reader
	 * 		Reader wrapper of class to extract Metadata from.
	 *
	 * @return Data extracted from visitor.
	 */
	@Nullable
	public static KotlinMetadata from(ClassReader reader) {
		KotlinMetadata[] wrapper = new KotlinMetadata[1];
		reader.accept(new ClassVisitor(RecafConstants.getAsmVersion()) {
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				AnnotationVisitor visitor = super.visitAnnotation(descriptor, visible);
				if (METADATA_DESC.equals(descriptor))
					visitor = new KotlinMetadataVisitor(visitor, v -> wrapper[0] = from(v));
				return visitor;
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
		return wrapper[0];
	}

	/**
	 * @param visitor
	 * 		Metadata visitor which has visited a class file.
	 *
	 * @return Data extracted from visitor.
	 */
	@Nullable
	public static KotlinMetadata from(KotlinMetadataVisitor visitor) {
		try {
			// Skip if visitor does not have data populated.
			String[] data1 = visitor.getData1();
			if (data1 == null)
				return null;
			// Parse "d1"
			byte[] data1Decoded = BitEncoding.decodeBytes(data1);
			ByteArrayInputStream data1Stream = new ByteArrayInputStream(data1Decoded);
			JvmProtoBuf.StringTableTypes types = JvmProtoBuf.StringTableTypes.parseDelimitedFrom(data1Stream, EXTENSIONS);
			ProtoBuf.Class cls = ProtoBuf.Class.parseFrom(data1Stream, EXTENSIONS);
			// Wrap with "d2" so the human-readable names can be extracted
			String[] data2 = visitor.getData2();
			MetadataNameResolver resolver = new MetadataNameResolver(types, data2);
			return new KotlinMetadata(cls, resolver);
		} catch (Exception ex) {
			logger.warn("Kotlin @Metadata 'd1' format failed to parse", ex);
			return null;
		}
	}
}
