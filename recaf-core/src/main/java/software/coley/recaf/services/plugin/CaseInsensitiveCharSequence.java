package software.coley.recaf.services.plugin;

final class CaseInsensitiveCharSequence {
	private final CharSequence csq;

	CaseInsensitiveCharSequence(CharSequence csq) {
		this.csq = csq;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CaseInsensitiveCharSequence that))
			return false;
		CharSequence csq = this.csq;
		CharSequence thatCsq = that.csq;
		int length;
		if ((length = csq.length()) != thatCsq.length())
			return false;
		while (length != 0) {
			if (Character.toUpperCase(csq.charAt(--length))
					!= Character.toUpperCase(thatCsq.charAt(length)))
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int h = 1;
		CharSequence csq = this.csq;
		for (int i = csq.length(); i != 0; ) {
			h = h * 31 + Character.toUpperCase(csq.charAt(--i));
		}
		return h;
	}
}
