package software.coley.recaf.info.annotation;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.ClassMember;

import java.util.List;
import java.util.stream.Stream;

/**
 * Outline of an annotated class or member.
 *
 * @author Matt Coley
 * @see ClassInfo
 * @see ClassMember
 */
public interface Annotated {
	/**
	 * @return List of declared annotations.
	 */
	@Nonnull
	List<AnnotationInfo> getAnnotations();

	/**
	 * @return List of type annotations.
	 */
	@Nonnull
	List<TypeAnnotationInfo> getTypeAnnotations();

	/**
	 * @return Stream of declared annotations.
	 */
	@Nonnull
	default Stream<AnnotationInfo> annotationStream() {
		return Stream.of(this).flatMap(self -> self.getAnnotations().stream());
	}

	/**
	 * @return Stream of type annotations.
	 */
	@Nonnull
	default Stream<AnnotationInfo> typeAnnotationStream() {
		return Stream.of(this).flatMap(self -> self.getAnnotations().stream());
	}

	/**
	 * @return Stream of both normal and type anotations.
	 */
	@Nonnull
	default Stream<AnnotationInfo> allAnnotationsStream() {
		return Stream.concat(annotationStream(), typeAnnotationStream());
	}
}
