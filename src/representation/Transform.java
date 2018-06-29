/**
 * 
 */
package representation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import corpus.Document;
import corpus.ID;
import corpus.NEtype;
import corpus.NamedEntity;
import corpus.RelationArgument;
import corpus.RelationType;
import corpus.Schema;
import corpus.Sentence;
import experiments.Experiment;

/**
 * @author dialecti
 *
 */
public abstract class Transform {

	Sentence sentence;
	List<Candidate> candidate;
	Document document;
	public Integer couplesize = 0;
	public Integer candidatesize = 0;
	public Integer positiveCoupleSize = 0;
	public Integer positiveDisconnectedCoupleSize = 0;
	public Double positiveUniqueCoupleSize = 0.0;
	public Double positiveUniqueDisconnectedCoupleSize = 0.0;



	public Transform (Sentence s, Document d) {
		sentence = s;
		document = d;
		candidate = new ArrayList<Candidate>();
	}

	public Transform(Sentence s){
		this.sentence = s;
		this.document = null;
		candidate = new ArrayList<Candidate>();
	}




	public List<Candidate> CalculateTransformation(Experiment exp, Boolean relations_on_relations)
			throws IOException {
		Schema sc = exp.getSchema();
		Boolean verbose = (exp.verbose >0);
		return findAllConnectedCandidates(exp,this.sentence, sc.DefinedTypes, sc.DefinedRelationTypes, verbose, relations_on_relations);		
	}

	public List<Candidate> findAllConnectedCandidates(Experiment exp,Sentence s,List<NEtype> netypes, List<RelationType> relationtypes, Boolean verbose, Boolean relations_on_relations) throws IOException {
		List<String> printed = new ArrayList<>();
		List<Candidate> candidates = new ArrayList<Candidate>();
		List<Couple> couples = CandidateTools.findPossibleCandidates(s,netypes, relationtypes, relations_on_relations);
		couplesize += couples.size();
		for (Couple couple : couples) {
			NamedEntity t1 = couple.arg1;
			NamedEntity t2 = couple.arg2;
			List<ID> path = findPathBetweenTerms(exp,s, t1, t2, verbose);
			if (!path.isEmpty()) {
				RelationArgument r1 = s.getRelationArgument(t1,couple.type.arg1role);
				RelationArgument r2 = s.getRelationArgument(t2,couple.type.arg2role);
				String idx = UUID.randomUUID().toString();
				Candidate candidate = new Path(idx, r1, r2, path, s, couple.type);
				if (!candidates.contains(candidate))candidates.add(candidate);
				if (s.inRelation(t1, t2, couple.type)){
					positiveCoupleSize++;
					if (couple.type.directional) {
						positiveUniqueCoupleSize ++;
					}
					else {
						positiveUniqueCoupleSize +=0.5;
					}
				}
			}
			else {
				String att = "";
				if(t1.tid.id < t2.tid.id) att = t1.tid.id+":"+t2.tid.id;
				else att = t2.tid.id+":"+t1.tid.id;
				if (!printed.contains(att)){
					if (verbose) exp.getDumper().print("Attention! No path found in sentence "+s.id+" between "+t1.tid.getMixID()+" and "+t2.tid.getMixID()+".");
					if (s.inRelation(t1, t2, couple.type)){
						if (verbose) 	exp.getDumper().println(" POSITIVE");
						positiveDisconnectedCoupleSize++;
						if (couple.type.directional) {
							positiveUniqueDisconnectedCoupleSize ++;
						}
						else {
							positiveUniqueDisconnectedCoupleSize +=0.5;
						}
					}
					else {
						if (verbose) 	exp.getDumper().println(" NEGATIVE");
					}
					printed.add(att);
				}
			}
		}
		candidatesize += candidates.size();
		return candidates;
	}

	public abstract List<ID> findPathBetweenTerms(Experiment exp, Sentence s,NamedEntity t1, NamedEntity t2, Boolean verbose) throws IOException;

	public List<Candidate> specialCalculateTransformation(Experiment exp, ArrayList<Candidate> oldcandidates) throws IOException {
		Boolean verbose = (exp.verbose >0);

		return specialFindAllConnectedCandidates(oldcandidates,exp,this.sentence, verbose);
	}

	private List<Candidate> specialFindAllConnectedCandidates(ArrayList<Candidate> oldcandidates,Experiment exp,Sentence s, Boolean verbose) throws IOException {
		List<String> printed = new ArrayList<>();
		List<Candidate> candidates = new ArrayList<Candidate>();
		//			List<Couple> couples = findPossibleCandidates(s,netypes, relationtypes);
		List<Couple> couples = candidates2couples(oldcandidates,s);
		couplesize += couples.size();
		for (Couple couple : couples) {
			NamedEntity t1 = couple.arg1;
			NamedEntity t2 = couple.arg2;
			List<ID> path = findPathBetweenTerms(exp,s, t1, t2, verbose);
			if (!path.isEmpty()) {
				RelationArgument r1 = s.getRelationArgument(t1,couple.type.arg1role);
				RelationArgument r2 = s.getRelationArgument(t2,couple.type.arg2role);
				//				if (r1.name == null){
				//					r1.name = couple.type.arg1role;
				//				}
				//				if (r2.name == null) {
				//					r2.name = couple.type.arg2role;
				//				}
				String idx = UUID.randomUUID().toString();
				Candidate candidate = new Path(idx, r1, r2, path, s, couple.type);
				if (!candidates.contains(candidate))candidates.add(candidate);
			}
			else {
				String att = "";
				if(t1.tid.id < t2.tid.id) att = t1.tid.id+":"+t2.tid.id;
				else att = t2.tid.id+":"+t1.tid.id;
				if (!printed.contains(att)){
					if (verbose)exp.getDumper().print("Attention! No path found in sentence "+s.id+" between "+t1.tid.getMixID()+" and "+t2.tid.getMixID()+".");
					if (s.inRelation(t1, t2, couple.type)){
						if (verbose)	exp.getDumper().println(" POSITIVE");
					}
					else {
						if (verbose)	exp.getDumper().println(" NEGATIVE");
					}
					printed.add(att);
				}
			}
		}
		candidatesize += candidates.size();
		return candidates;
	}

	private ArrayList<Couple> candidates2couples(ArrayList<Candidate> candidates, Sentence s){
		ArrayList<Couple> couples = new ArrayList<Couple>();
		for (Candidate cand:candidates){
			if(cand.sentence.id.equals(s.id)){
				NamedEntity t1 = s.getTermByID(cand.arg1.argument.tid.id);
				NamedEntity t2 = s.getTermByID(cand.arg2.argument.tid.id);
				couples.add(new Couple(t1,t2, cand.candidateRelationType));
			}
		}

		return couples;

	}


//	public void matchRelationsToCandidates(Sentence s) {
//		for (Relation r : s.relations) {
//			for (Candidate c : s.candidates) {
//				if (c instanceof Path) {
//					Path p = (Path) c;
//
//					if (r.arg1.argument.tid.equals(p.arg1.argument.tid) && r.arg2.argument.tid.equals(p.arg2.argument.tid)) {
//						c.relation = r;
//					}
//				}
//			}
//		}
//	}



}