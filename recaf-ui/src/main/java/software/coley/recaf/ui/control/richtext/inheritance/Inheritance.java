package software.coley.recaf.ui.control.richtext.inheritance;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.ClassMemberPathNode;

/**
 * Outline of an inheritance item for a method declaration.
 * Will be either a child <i>(where the annotated method is overridden)</i>
 * or a parent <i>(where the annotated method is the override)</i>.
 *
 * @author Matt Coley
 */
public sealed interface Inheritance extends Comparable<Inheritance> {
	/**
	 * @return Line number of the inheritance marker.
	 */
	int line();

	/**
	 * @return Path to the class member the inheritance applies to.
	 */
	ClassMemberPathNode path();

	/**
	 * @param newLine
	 * 		New line for inheritance.
	 *
	 * @return Copy of the current inheritance, but with the line number modified.
	 */
	Inheritance withLine(int newLine);

	@Override
	default int compareTo(@Nonnull Inheritance o) {
		int cmp = Integer.compare(line(), o.line());
		if (cmp == 0) {
			if (this instanceof Child && o instanceof Parent)
				cmp = -1;
			else if (this instanceof Parent && o instanceof Child)
				cmp = 1;
		}
		return cmp;
	}

	/**
	 * Inheritance item representing a child override method.
	 */
	record Child(int line, @Nonnull ClassMemberPathNode path) implements Inheritance {
		@Override
		public Inheritance withLine(int newLine) {
			return new Child(newLine, path);
		}

		@Override
		public int compareTo(@Nonnull Inheritance o) {
			int cmp = Inheritance.super.compareTo(o);
			if (cmp == 0 && o instanceof Child otherChild)
				cmp = path.localCompare(otherChild.path);
			return cmp;
		}
	}

	/**
	 * Inheritance item representing an overridden parent method.
	 */
	record Parent(int line, @Nonnull ClassMemberPathNode path) implements Inheritance {
		@Override
		public Inheritance withLine(int newLine) {
			return new Parent(newLine, path);
		}

		@Override
		public int compareTo(@Nonnull Inheritance o) {
			int cmp = Inheritance.super.compareTo(o);
			if (cmp == 0 && o instanceof Child otherChild)
				cmp = path.localCompare(otherChild.path);
			return cmp;
		}
	}
}
