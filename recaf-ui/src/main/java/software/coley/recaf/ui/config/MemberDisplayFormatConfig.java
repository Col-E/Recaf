package software.coley.recaf.ui.config;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.objectweb.asm.Type;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.Types;

/**
 * Config for {@link ClassMember} display.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class MemberDisplayFormatConfig extends BasicConfigContainer {
	public static final String ID = "member-format";
	private final ObservableObject<Display> nameTypeDisplay = new ObservableObject<>(Display.NAME_ONLY);

	@Inject
	public MemberDisplayFormatConfig() {
		super(ConfigGroups.SERVICE_UI, ID + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>("name-type-display", Display.class, nameTypeDisplay));
	}

	@Nonnull
	public ObservableObject<Display> getNameTypeDisplay() {
		return nameTypeDisplay;
	}

	@Nonnull
	public String getDisplay(@Nonnull ClassMember member) {
		if (member instanceof FieldMember field)
			return getDisplay(field);
		else if (member instanceof MethodMember method)
			return getDisplay(method);
		throw new IllegalStateException("Member not field or method: " + member);
	}

	@Nonnull
	public String getDisplay(@Nonnull FieldMember member) {
		return getFieldDisplay(member.getName(), member.getDescriptor());
	}

	@Nonnull
	public String getDisplay(@Nonnull MethodMember member) {
		return getMethodDisplay(member.getName(), member.getDescriptor());
	}

	@Nonnull
	public String getDisplay(@Nonnull String name, @Nonnull String desc) {
		if (desc.charAt(0) == '(')
			return getMethodDisplay(name, desc);
		return getFieldDisplay(name, desc);
	}

	@Nonnull
	public String getFieldDisplay(@Nonnull String name, @Nonnull String desc) {
		return switch (nameTypeDisplay.getValue()) {
			case NAME_ONLY -> name;
			case NAME_AND_RAW_DESCRIPTOR -> name + " " + desc;
			case NAME_AND_PRETTY_DESCRIPTOR -> name + " " + Types.pretty(desc);
		};
	}

	@Nonnull
	public String getMethodDisplay(@Nonnull String name, @Nonnull String desc) {
		return switch (nameTypeDisplay.getValue()) {
			case NAME_ONLY -> name;
			case NAME_AND_RAW_DESCRIPTOR -> name + desc;
			case NAME_AND_PRETTY_DESCRIPTOR -> name + " " + Types.pretty(desc);
		};
	}

	@Nonnull
	public String getDescriptorDisplay(@Nonnull String desc) {
		return switch (nameTypeDisplay.getValue()) {
			case NAME_ONLY, NAME_AND_RAW_DESCRIPTOR ->  desc;
			case NAME_AND_PRETTY_DESCRIPTOR -> Types.pretty(desc);
		};
	}

	public enum Display {
		NAME_ONLY,
		NAME_AND_RAW_DESCRIPTOR,
		NAME_AND_PRETTY_DESCRIPTOR
	}
}
