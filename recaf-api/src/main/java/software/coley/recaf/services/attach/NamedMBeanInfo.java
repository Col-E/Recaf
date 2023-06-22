package software.coley.recaf.services.attach;

import jakarta.annotation.Nonnull;

import javax.management.*;

/**
 * Extension of {@link MBeanInfo} providing access to the associated {@link ObjectName}.
 *
 * @author Matt Coley
 */
public class NamedMBeanInfo extends MBeanInfo {
	private final ObjectName objectName;

	/**
	 * @param objectName
	 * 		Associated name to the bean info.
	 * @param info
	 * 		Wrapped bean info.
	 */
	public NamedMBeanInfo(@Nonnull ObjectName objectName, @Nonnull MBeanInfo info) {
		this(objectName,
				info.getClassName(),
				info.getDescription(),
				info.getAttributes(),
				info.getConstructors(),
				info.getOperations(),
				info.getNotifications(),
				info.getDescriptor());
	}

	private NamedMBeanInfo(@Nonnull ObjectName objectName,
						   String className,
						   String description,
						   MBeanAttributeInfo[] attributes,
						   MBeanConstructorInfo[] constructors,
						   MBeanOperationInfo[] operations,
						   MBeanNotificationInfo[] notifications,
						   Descriptor descriptor) throws IllegalArgumentException {
		super(className, description, attributes, constructors, operations, notifications, descriptor);
		this.objectName = objectName;
	}

	/**
	 * @return Key to use in {@link MBeanServerConnection} operations.
	 */
	@Nonnull
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * @param connection
	 * 		JMX connection.
	 * @param attribute
	 * 		Attribute to get value of.
	 *
	 * @return Value of attribute.
	 *
	 * @throws Exception
	 * 		See: {@link MBeanServerConnection#getAttribute(ObjectName, String)}.
	 */
	public Object getAttributeValue(@Nonnull JmxBeanServerConnection connection,
									@Nonnull MBeanAttributeInfo attribute) throws Exception {
		return getAttributeValue(connection, attribute.getName());
	}

	/**
	 * @param connection
	 * 		JMX connection.
	 * @param attributeName
	 * 		Name of attribute to get value of.
	 *
	 * @return Value of attribute.
	 *
	 * @throws Exception
	 * 		See: {@link MBeanServerConnection#getAttribute(ObjectName, String)}.
	 */
	public Object getAttributeValue(@Nonnull JmxBeanServerConnection connection,
									@Nonnull String attributeName) throws Exception {
		return connection.getConnection().getAttribute(getObjectName(), attributeName);
	}
}
