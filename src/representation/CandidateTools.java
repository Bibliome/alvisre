package representation;

import java.util.ArrayList;
import java.util.List;

import corpus.NEtype;
import corpus.NamedEntity;
import corpus.RelationType;
import corpus.SchemaType;
import corpus.Sentence;
import exceptions.UnexpectedDatasetFormat;

public class CandidateTools {

	public CandidateTools() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * given the possible relation types of this schema, what are the
	 * relations one could have here, based only the existence of the
	 * necessary namedEntities, and regardless of whether or not there exists a path
	 * between them and what that is.
	 */
	public static List<Couple> findPossibleCandidates(Sentence s,List<NEtype> netypes, List<RelationType> relationtypes, Boolean relations_on_relations) {
		if (!relations_on_relations) {

		List<NEtype> myTermTypes = new ArrayList<NEtype>();
		Integer i = 0;
		for (NamedEntity t : s.namedEntities) {
			for (NEtype type : netypes){
				if (type.equals(t.type)){
					t.type = type;
				}
			}
			myTermTypes.add(t.type);
			i++;
		}
		List<Couple> couples = new ArrayList<Couple>();
		for (RelationType rtype : relationtypes) {
			List<SchemaType> types1 = rtype.arg1types;
			for (SchemaType type1 : types1) {
				if (type1 instanceof NEtype) {
					NEtype netype1 = (NEtype) type1;
					List<SchemaType> types2 = rtype.getPossibleOtherArgumentTypes(type1);
					for (SchemaType type2 : types2) {
						NEtype netype2 = (NEtype) type2;
						if (type2 instanceof NEtype){
							if (myTermTypes.contains(netype1) && myTermTypes.contains(netype2)) {
								for (NamedEntity term1 : s.getTermsByType(netype1)) {
									for (NamedEntity term2 : s.getTermsByType(netype2)) {
										if (!term1.equals(term2)) {
											Couple newcouple = new Couple(term1, term2, rtype);
											if (!contains(couples, newcouple))	{
												couples.add(newcouple);
											}
										}
									}
								}
							}
						}

					}
				}
			}
		}
		return couples;
		}
		else {
			throw (new UnexpectedDatasetFormat("Work in Progress"));
		}
	}


	private static Boolean contains(List<Couple> couples, Couple couple1){
		for (Couple couple2:couples){
			if (couple2.equals(couple1)){
				return true;
			}
		}
		return false;
	}
	




}

