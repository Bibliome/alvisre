package corpus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Schema  implements Serializable  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6855992526195472797L;
	public List<NEtype> DefinedTypes = new ArrayList<NEtype>();
	public List<RelationType> DefinedRelationTypes = new ArrayList<RelationType>();
	public List<NEtype> DefinedGroups = new ArrayList<NEtype>();
//	public Boolean SyntacticRelationsBetweenTerms = false;
	
	public Schema() {
	}
//	public Schema(List<NEtype> types, List<RelationType> rtypes,List<NEtype> groups){
//		this(types, rtypes, groups);
//	}
//	
	public Schema(List<NEtype> types, List<RelationType> rtypes, List<NEtype> groups){
		DefinedTypes = types;
		DefinedGroups = groups;
		DefinedRelationTypes = rtypes;
//		SyntacticRelationsBetweenTerms = synrelterms;
	}
	
	public void mergeWith(Schema otherSchema) {
		DefinedGroups.addAll(otherSchema.DefinedGroups);
		DefinedTypes.addAll(otherSchema.DefinedTypes);
		DefinedRelationTypes.addAll(otherSchema.DefinedRelationTypes);
	}
	
	public void addGroup(NEtype group){
		DefinedGroups.add(group);
	}
	public void addType(NEtype type){
		DefinedTypes.add(type);
	}
	public void addRelationType(RelationType type){
		DefinedRelationTypes.add(type);
	}
	
	public RelationType getRelationTypeByName (String name){
		for (RelationType r:DefinedRelationTypes){
			if(r.type.equalsIgnoreCase(name)){
				return r;
			}
		}
		return null;
	}
	
	public ArrayList<RelationType> getRelationTypesByName(String name) {
		ArrayList<RelationType> res = new ArrayList<RelationType>();
		for (RelationType r:DefinedRelationTypes){
			if(r.type.equalsIgnoreCase(name)){
				res.add(r);
			}
		}
		return res;
	}

	public NEtype getNETypeByName (String name){
		for (NEtype n:DefinedTypes){
			if(n.type.equalsIgnoreCase(name)){
				return n;
			}
		}
		return null;
	}
	
	public Boolean hasRelationsOnRelations(){
		for (RelationType rtype:DefinedRelationTypes) {
			if (rtype.relation_on_relations) {
				return true;
			}
		}
		return false;
	}
	
	
}
