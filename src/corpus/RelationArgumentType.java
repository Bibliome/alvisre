package corpus;

import java.io.Serializable;

public class RelationArgumentType implements Serializable {
	private static final long serialVersionUID = 1L;
	public String role = "";
	public SchemaType arg = null;
	RelationArgumentType(String r, SchemaType a) {
		role = r;
		arg = a;
	}
}