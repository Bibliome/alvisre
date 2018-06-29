package corpus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RelationType extends SchemaType implements Serializable {
	private static final long serialVersionUID = -1589682063236553823L;
	public String type;
	public List<SchemaType> arg1types = new ArrayList<>();
	public String arg1role;
	public List<SchemaType> arg2types = new ArrayList<>();
	public String arg2role;
	public Boolean relation_on_relations = false;
	public Boolean directional = true;
	
	public ArrayList<RelationSignature> relationSignatures = new ArrayList<>();
	
	@SuppressWarnings("unchecked")
	public RelationType(String rtype, String a1r, List<?>  arg1, String a2r, List<?>  arg2){
		type = rtype;
		arg1types =  (List<SchemaType>) arg1;
		arg2types = (List<SchemaType>) arg2;
		arg1role = a1r;
		arg2role = a2r;
		if ((!arg1.isEmpty()) && (!arg2.isEmpty())) {

			if ((arg1.get(0) instanceof NEtype) && arg2.get(0) instanceof NEtype ) {
				relation_on_relations = false;
			}
			else if ((arg1.get(0) instanceof RelationType) || (arg2.get(0) instanceof RelationType)) {
				relation_on_relations = true;
			}
		}
		for (SchemaType t1:arg1types) {
			for (SchemaType t2:arg2types) {
				addSignature(t1, t2);
			}
		}
	}

	public RelationType(String rtype) {
		type = rtype;
	}

	@SuppressWarnings("unchecked")
	public void updateRelation( String a1r, List<?>  arg1, String a2r, List<?>  arg2){
		arg1types =  (List<SchemaType>) arg1;
		arg2types = (List<SchemaType>) arg2;
		arg1role = a1r;
		arg2role = a2r;
		
		for (SchemaType type1:arg1types) {
			for (SchemaType type2:arg2types){
				addSignature(type1, type2);
			}
		}
		if ((!arg1.isEmpty()) && (!arg2.isEmpty())) {
			if ((arg1.get(0) instanceof NEtype) && arg2.get(0) instanceof NEtype ) {
				relation_on_relations = false;
			}
			else if ((arg1.get(0) instanceof RelationType) || (arg2.get(0) instanceof RelationType)) {
				relation_on_relations = true;
			}
		}	
	}

	public String getName(){
		return type;
	}
	
	public void addSignature(SchemaType arg1, SchemaType arg2) {
		relationSignatures.add(new RelationSignature(new RelationArgumentType(arg1role, arg1), new RelationArgumentType(arg2role, arg2)));
		if (!arg1types.contains(arg1)) arg1types.add(arg1);
		if (!arg2types.contains(arg2)) arg2types.add(arg2);
	}
	public void addSignature(String role1, SchemaType arg1, String role2, SchemaType arg2) {
		relationSignatures.add(new RelationSignature(new RelationArgumentType(role1, arg1), new RelationArgumentType(role2, arg2)));
		if (!arg1types.contains(arg1)) arg1types.add(arg1);
		if (!arg2types.contains(arg2)) arg2types.add(arg2);
	}
	
	public void setRoles(String role1, String role2) {
		arg1role = role1;
		arg2role = role2;
	}
	
	public List<SchemaType> getPossibleOtherArgumentTypes(SchemaType arg){
		List<SchemaType> result = new ArrayList<SchemaType>();
		for (RelationSignature sig:relationSignatures) {
			if (sig.arg1.arg.equals(arg)) {
				result.add(sig.arg2.arg);
			}
		}
		return result;
	}
	
}

