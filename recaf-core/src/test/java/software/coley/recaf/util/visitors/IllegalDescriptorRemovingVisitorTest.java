package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for {@link IllegalDescriptorRemovingVisitor}.
 */
class IllegalDescriptorRemovingVisitorTest {
	@Test
	void removeMembers() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V17, ACC_PUBLIC, "Sample", null, "java/lang/Object", null);

		cw.visitField(ACC_PRIVATE, "count", "I", null, null).visitEnd();
		cw.visitField(ACC_PRIVATE, "broken", "Q", null, null).visitEnd();
		returnVoid(cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null));
		returnVoid(cw.visitMethod(ACC_PUBLIC, ".dots not allowed.", "(/)L/;", null, null));
		cw.visitEnd();

		assertDoesNotThrow(() -> {
			ClassNode node = new ClassNode();
			new ClassReader(cw.toByteArray()).accept(node, 0);
		});

		ClassNode node = strip(cw.toByteArray());
		assertEquals(List.of("count"), node.fields.stream().map(field -> field.name).toList());
		assertEquals(List.of("run"), node.methods.stream().map(method -> method.name).toList());
	}

	@Test
	void removeAnnotations() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V17, ACC_PUBLIC, "Sample", null, "java/lang/Object", null);

		cw.visitAnnotation("Q", true).visitEnd();

		AnnotationVisitor av = cw.visitAnnotation("LValid;", true);
		av.visit("kept", "value");
		av.visit("brokenType", Type.getType("Lbroken"));
		av.visitAnnotation("brokenNested", "Q");
		av.visitEnd();

		int superTypeRef = TypeReference.newSuperTypeReference(-1).getValue();
		cw.visitTypeAnnotation(superTypeRef, null, "Q", true).visitEnd();
		cw.visitTypeAnnotation(superTypeRef, null, "LValidType;", true).visitEnd();
		cw.visitEnd();

		ClassNode node = strip(cw.toByteArray());
		assertEquals(List.of("LValid;"), node.visibleAnnotations.stream().map(annotationNode -> annotationNode.desc).toList());
		assertEquals(List.of("kept", "value"), node.visibleAnnotations.getFirst().values);
		assertEquals(List.of("LValidType;"), node.visibleTypeAnnotations.stream().map(annotationNode -> annotationNode.desc).toList());
	}

	@Test
	void removeRecordComponents() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V17, ACC_PUBLIC | ACC_RECORD, "Sample", null, "java/lang/Record", null);

		RecordComponentVisitor rcv = cw.visitRecordComponent("count", "I", null);
		rcv.visitAnnotation("LValid;", true).visitEnd();
		rcv.visitAnnotation("Q", true).visitEnd();
		rcv.visitEnd();

		cw.visitRecordComponent("broken", "Q", null).visitEnd();
		cw.visitEnd();

		ClassNode node = strip(cw.toByteArray());
		assertEquals(List.of("count"), node.recordComponents.stream().map(component -> component.name).toList());
		assertEquals(List.of("LValid;"), node.recordComponents.getFirst().visibleAnnotations.stream().map(annotationNode -> annotationNode.desc).toList());
	}

	@Test
	void removeLocals() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V17, ACC_PUBLIC, "Sample", null, "java/lang/Object", null);

		Label start = new Label();
		Label end = new Label();
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		mv.visitCode();
		mv.visitLabel(start);
		mv.visitLocalVariable("good", "I", null, start, end, 1);
		mv.visitLocalVariable("bad", "Q", null, start, end, 2);
		mv.visitLabel(end);
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
		cw.visitEnd();

		ClassNode node = strip(cw.toByteArray());
		assertEquals(List.of("good"), node.methods.getFirst().localVariables.stream().map(local -> local.name).toList());
	}

	private static void returnVoid(@Nonnull MethodVisitor mv) {
		mv.visitCode();
		mv.visitInsn(RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	@Nonnull
	private static ClassNode strip(byte[] classFile) {
		ClassWriter stripped = new ClassWriter(0);
		new ClassReader(classFile).accept(new IllegalDescriptorRemovingVisitor(stripped), 0);
		ClassNode node = new ClassNode();
		new ClassReader(stripped.toByteArray()).accept(node, 0);
		return node;
	}
}
