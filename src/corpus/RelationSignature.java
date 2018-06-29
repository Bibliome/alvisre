package corpus;

import java.io.Serializable;


public class RelationSignature implements Serializable {
	private static final long serialVersionUID = -8786796965698495012L;
	public RelationArgumentType arg1 = null;
	public RelationArgumentType arg2 = null;
	
	RelationSignature(RelationArgumentType a1,   RelationArgumentType a2){
		arg1 = a1;
		arg2 = a2;
	}
			
	
}