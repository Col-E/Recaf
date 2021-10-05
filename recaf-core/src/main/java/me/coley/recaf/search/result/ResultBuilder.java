package me.coley.recaf.search.result;

import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.function.Consumer;

/**
 * Builder for {@link Result}.
 *
 * @author Matt Coley
 */
public class ResultBuilder {
	private static final Logger logger = Logging.get(Result.class);
	private final ResultFactory factory;
	private CommonClassInfo containingClass;
	private FieldInfo containingField;
	private MethodInfo containingMethod;
	private String containingAnnotation;
	private int opcode = -1;

	private ResultBuilder(ResultFactory factory) {
		this.factory = factory;
	}

	/**
	 * @param text
	 * 		Text matched.
	 *
	 * @return Builder for {@link TextResult}.
	 */
	public static ResultBuilder text(String text) {
		return new ResultBuilder(builder -> new TextResult(builder, text));
	}

	/**
	 * @param number
	 * 		Numeric value matched.
	 *
	 * @return Builder for {@link NumberResult}.
	 */
	public static ResultBuilder number(Number number) {
		return new ResultBuilder(builder -> new NumberResult(builder, number));
	}

	/**
	 * @param owner
	 * 		The class defining the referenced member.
	 * @param name
	 * 		The name of the referenced member.
	 * @param desc
	 * 		The type descriptor of the referenced member.
	 *
	 * @return Builder for {@link ReferenceResult}.
	 */
	public static ResultBuilder reference(String owner, String name, String desc) {
		return new ResultBuilder(builder -> new ReferenceResult(builder, owner, name, desc));
	}

	/**
	 * @param owner
	 * 		The class defining the declared member.
	 * @param name
	 * 		The name of the declared member.
	 * @param desc
	 * 		The type descriptor of the declared member.
	 *
	 * @return Builder for {@link ReferenceResult} for declared members.
	 */
	public static ResultBuilder declaration(String owner, String name, String desc) {
		return new ResultBuilder(builder -> new ReferenceResult(builder, owner, name, desc));
	}

	/**
	 * @param containingClass
	 * 		The class the matched item is contained within.
	 *
	 * @return Builder.
	 */
	public ResultBuilder inClass(CommonClassInfo containingClass) {
		this.containingClass = containingClass;
		return this;
	}

	/**
	 * @param containingField
	 * 		The field the matched item is contained within.
	 *
	 * @return Builder.
	 */
	public ResultBuilder inField(FieldInfo containingField) {
		this.containingField = containingField;
		return this;
	}

	/**
	 * @param containingMethod
	 * 		The method the matched item is contained within.
	 *
	 * @return Builder.
	 */
	public ResultBuilder inMethod(MethodInfo containingMethod) {
		this.containingMethod = containingMethod;
		return this;
	}

	/**
	 * @param annotationType
	 * 		The annotation the matched item is contained within.
	 *
	 * @return Builder.
	 */
	public ResultBuilder inAnnotation(String annotationType) {
		this.containingAnnotation = annotationType;
		return this;
	}

	/**
	 * @param opcode
	 * 		The opcode of the instruction the matched item is contained within.
	 *
	 * @return Builder.
	 */
	public ResultBuilder withOpcode(int opcode) {
		this.opcode = opcode;
		return this;
	}

	/**
	 * Run some action with the generated result.
	 *
	 * @param resultConsumer
	 * 		Consumer that takes in the generated result.
	 */
	public void then(Consumer<Result> resultConsumer) {
		Result result = factory.create(this);
		if (containingClass == null) {
			logger.error("Failed to collect result {} into result collection, because containing class was not set!",
					result);
			return;
		}
		resultConsumer.accept(result);
	}

	/**
	 * @return The class the matched item is contained within.
	 */
	public CommonClassInfo getContainingClass() {
		return containingClass;
	}

	/**
	 * @return The field the matched item is contained within.
	 */
	public FieldInfo getContainingField() {
		return containingField;
	}

	/**
	 * @return The method the matched item is contained within.
	 */
	public MethodInfo getContainingMethod() {
		return containingMethod;
	}

	/**
	 * @return The annotation the matched item is contained within.
	 */
	public String getContainingAnnotation() {
		return containingAnnotation;
	}

	/**
	 * @return The opcode of the instruction the matched item is contained within.
	 */
	public int getOpcode() {
		return opcode;
	}

	private interface ResultFactory {
		Result create(ResultBuilder builder);
	}
}
