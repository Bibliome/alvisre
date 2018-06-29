package representation;

import corpus.NamedEntity;
import corpus.RelationType;


public class Couple {
	public NamedEntity arg1;
	public NamedEntity arg2;
	public RelationType type;

	public Couple(NamedEntity t1, NamedEntity t2, RelationType t) {
		arg1 = t1;
		arg2 = t2;
		type = t;
	}
	
	public Boolean equals(Couple other){
//		if (type.equals(other.type)) {
////			if (type.directional) {
//				boolean result =(arg1.equals(other.arg1) && arg2.equals(other.arg2)); 
//				return result;
////			}
////			else {
////				boolean result = ((arg1.equals(other.arg1) && arg2.equals(other.arg2)) || (arg2.equals(other.arg1) && arg1.equals(other.arg2)));
////				return result;
////			}
//			
//		}
//		return false;
//		
		
		return (arg1.equals(other.arg1) && arg2.equals(other.arg2) && type.equals(other.type));
	}
}

