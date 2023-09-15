package software.coley.recaf.test.dummy;

/**
 * Dummy class to test method overrides with varing degrees of access.
 */
@SuppressWarnings("all")
public class AccessibleMethodsChild extends AccessibleMethods{
	@Override
	public void publicMethod() {
		super.publicMethod();
	}

	@Override
	protected void protectedMethod() {
		super.protectedMethod();
	}

	@Override
	void packageMethod() {
		super.packageMethod();
	}
}
