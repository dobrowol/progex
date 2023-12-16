/** * In The Name of Allah ***/
package ghaffarian.progex.graphs.cfg;

/**
 * Class type of Control Flow (CF) edges.
 *
 * @author Seyed Mohammad Ghaffarian
 */
public class CFEdge {

	public final Type type;

	public CFEdge(Type type) {
		this.type = type;
	}
    
    @Override
    public String toString() {
        return type.toString();
    }
	

	/**
	 * Enumeration of different types for CF edges.
	 */
	public enum Type {
		EPSILON (""),
		TRUE    ("True"),
		FALSE   ("False"),
		THROWS  ("Throws"),
		CALLS   ("Call"),
		RETURN  ("Return");

		public final String label;

		private Type(String lbl) {
			label = lbl;
		}

		@Override
		public String toString() {
			return label;
		}
		// Custom method to create a Type from a string
        public static Type fromString(String str) {
            for (Type type : Type.values()) {
                if (type.label.equalsIgnoreCase(str)) {
                    return type;
                }
            }
            // Handle the case where the string does not match any Type
            throw new IllegalArgumentException("Unknown Type: " + str);
        }
	}	
}
