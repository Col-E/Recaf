package software.coley.recaf.services.search.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.search.AndroidClassSearchVisitor;
import software.coley.recaf.services.search.JvmClassSearchVisitor;
import software.coley.recaf.services.search.ResultSink;
import software.coley.recaf.services.search.match.StringPredicate;
import software.coley.recaf.services.search.result.MemberReference;
import software.coley.recaf.util.StringUtil;

/**
 * Declaration search implementation.
 *
 * @author Matt Coley
 */
public class DeclarationQuery implements JvmClassQuery, AndroidClassQuery {
	private final StringPredicate ownerPredicate;
	private final StringPredicate namePredicate;
	private final StringPredicate descriptorPredicate;

	/**
	 * Member declaration query.
	 * <p/>
	 * Do note that each target value is nullable/optional.
	 * Including only the owner and {@code null} for the name/desc will yield declarations to all members in the class.
	 * Including only the desc will yield declarations of all members with that desc in all classes.
	 *
	 * @param ownerPredicate
	 * 		String matching predicate for comparison against declared member owners.
	 *        {@code null} to ignore matching against owner names.
	 * @param namePredicate
	 * 		String matching predicate for comparison against declared member names.
	 *        {@code null} to ignore matching against member names.
	 * @param descriptorPredicate
	 * 		String matching predicate for comparison against declared member descriptors.
	 *        {@code null} to ignore matching against member descriptors.
	 */
	public DeclarationQuery(@Nullable StringPredicate ownerPredicate,
	                        @Nullable StringPredicate namePredicate,
	                        @Nullable StringPredicate descriptorPredicate) {
		this.ownerPredicate = ownerPredicate;
		this.namePredicate = namePredicate;
		this.descriptorPredicate = descriptorPredicate;
	}

	@Nonnull
	@Override
	public AndroidClassSearchVisitor visitor(@Nullable AndroidClassSearchVisitor delegate) {
		return (resultSink, currentLocation, classInfo) -> {
			if (delegate != null)
				delegate.visit(resultSink, currentLocation, classInfo);
			scan(resultSink, currentLocation);
		};
	}

	@Nonnull
	@Override
	public JvmClassSearchVisitor visitor(@Nullable JvmClassSearchVisitor delegate) {
		return (resultSink, currentLocation, classInfo) -> {
			if (delegate != null)
				delegate.visit(resultSink, currentLocation, classInfo);
			scan(resultSink, currentLocation);
		};
	}

	private boolean isMemberRefMatch(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		// If our query predicates are null, that field can skip comparison, and we move on to the next.
		// If all of our non-null query arguments match the given parameters, we have a match.
		if (ownerPredicate == null || StringUtil.isNullOrEmpty(owner) || ownerPredicate.match(owner))
			if (namePredicate == null || StringUtil.isNullOrEmpty(name) || namePredicate.match(name))
				return descriptorPredicate == null || StringUtil.isNullOrEmpty(desc) || descriptorPredicate.match(desc);

		return false;
	}

	private void scan(@Nonnull ResultSink resultSink, @Nonnull ClassPathNode classPath) {
		ClassInfo classInfo = classPath.getValue();
		for (FieldMember field : classInfo.getFields()) {
			String owner = classInfo.getName();
			String name = field.getName();
			String desc = field.getDescriptor();
			if (isMemberRefMatch(owner, name, desc))
				resultSink.accept(classPath.child(field), new MemberReference(owner, name, desc));
		}
		for (MethodMember method : classInfo.getMethods()) {
			String owner = classInfo.getName();
			String name = method.getName();
			String desc = method.getDescriptor();
			if (isMemberRefMatch(owner, name, desc))
				resultSink.accept(classPath.child(method), new MemberReference(owner, name, desc));
		}
	}
}
