package me.coley.recaf.assemble.analysis;

import java.util.Objects;

/**
 * Base value type.
 *
 * @author Matt Coley
 */
public class Value {

	// TODO: Track context (Ast) so users know what contributed?
	//  - But format it to look like a consecutive statement if possible
	//    - varName
	//    - varName.method(param)

	/**
	 * Value holding a generic object.
	 */
	public static class ObjectValue extends Value {
		private final String type;

		/**
		 * @param type
		 * 		Internal name of the object type.
		 */
		public ObjectValue(String type) {
			this.type = type;
		}

		/**
		 * @return Internal name of the object type.
		 */
		public String getType() {
			return type;
		}

		@Override
		public String toString() {
			return type;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ObjectValue that = (ObjectValue) o;
			return Objects.equals(type, that.type);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type);
		}
	}
}
