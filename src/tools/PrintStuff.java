package tools;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import corpus.ID;
import corpus.NEtype;
import corpus.Relation;
import corpus.RelationSignature;
import corpus.RelationType;
import corpus.Schema;
import corpus.Sentence;
import corpus.Word;
import representation.Candidate;
import representation.Path;
import weka.classifiers.Evaluation;

public class PrintStuff {
	private PrintStuff() {

	}

	public static void printSentenceAndCandidatesTextAndID(Dumper d,Sentence s) throws IOException {
		int i = 0;
		d.print("\n\nSentence ");
		d.flush();
		printSentence(d,s);
		d.flush();
		d.println("Candidates:");
		for (Candidate candidate : s.candidates) {
			i++;
			System.out.print(i+":");
			printCandidate(d, candidate);
		}

		d.println("-");
		d.flush();
	}

	public static void printSchema(Schema s, ArrayList<String> selectedTypes) throws IOException {
		System.out.println("Schema: ");
		System.out.println("Defined Entity Types");
		for (NEtype netype: s.DefinedTypes) {
			System.out.println("\tNE type:" + netype.type);
		}
		System.out.println("Defined Relation Types");
		if (selectedTypes.size() >0) {
			System.out.println("(showing only those that were selected in the configuration file)");
			for (RelationType retype: s.DefinedRelationTypes) {
				if (selectedTypes.contains(retype.type)) {
					String name = retype.getName();
					System.out.println("\tRelation Type: \n\t\t"+name);
					System.out.println("\tSignatures:");
					for (RelationSignature sig:retype.relationSignatures) {
						System.out.println("\t\t("+sig.arg1.role+")"+sig.arg1.arg.getName()+"   -   ("+sig.arg2.role+")"+sig.arg2.arg.getName());
					}
				}
			}
		}
		else {
			for (RelationType retype: s.DefinedRelationTypes) {
				String name = retype.getName();
				System.out.println("\tRelation Type: \n\t\t"+name);
				System.out.println("\tSignatures:");
				for (RelationSignature sig:retype.relationSignatures) {
					System.out.println("\t\t("+sig.arg1.role+")"+sig.arg1.arg.getName()+"   -   ("+sig.arg2.role+")"+sig.arg2.arg.getName());
				}

			}
		}
		System.out.println("----------------");
	}


	public static void printCandidate(Dumper d, Candidate candidate) throws IOException {
		Sentence s = candidate.sentence;
		if (candidate instanceof Path) {
			Path p = (Path) candidate;
			if (candidate.relation != null)  {
				if (candidate.relation.type.type.equals("none"))  d.print("|-|");
				else d.print("|+|");
			}
			else {
				d.print("|?|");
			}
			d.print("\t[");
			d.print(candidate.candidateRelationType.type+"]: ("+candidate.candidateRelationType.arg1role+") ");
			d.print("["+p.arg1.argument.type.type+":"+p.arg1.argument.getTagValue("surface_form")+"{"+p.arg1.argument.tid.getMixID()+"}"+"] ");
			List<ID> list = p.getPath();
			boolean starting = true;
			for (ID id : list) {
				if (!starting)
					d.print(" - ");
				starting = false;
				switch (id.type) {
				case textbound:
					if (s.getWordByID(id.id) != null){
						d.print(s.getWordByID(id.id).text);
					}
					else if (s.getTermByID(id.id)!= null){
						List<Word> words = s.getTermByID(id.id).findWords(s);
						for (Word w : words) {
							d.print(w.text);
						}
					}
					break;
				case relation:
					d.print("(" + s.getSyntacticRelationByID(id.id).type.type + ")");
					break;
				default:
					d.println(id.type.toString());
				}
				d.print("{"+id.getMixID()+"}");
			}
			d.print("["+p.arg2.argument.type.type+":"+p.arg2.argument.getTagValue("surface_form")+"{"+p.arg2.argument.tid.getMixID()+"}"+"] ");
			d.println(" ("+candidate.candidateRelationType.arg2role+")");
		}

	}

	public static void printRelation(Dumper d, Relation r) throws IOException {
		d.print(": ["+r.type.type+"]: ");
		String role1 = r.arg1.role;
		String role2 = r.arg2.role;
		d.print("("+role1+") ");
		d.print("["+r.arg1.argument.getTagValue("surface_form")+"{"+r.arg1.argument.tid.getMixID()+"}"+"] ");
		for (Word w:r.arg1.argument.words) {
			d.print(w.text+"{"+w.wid.getMixID()+"} ");
		}
		d.print("("+role2+") ");
		d.print("["+r.arg2.argument.getTagValue("surface_form")+"{"+r.arg2.argument.tid.getMixID()+"}"+"] ");
		for (Word w:r.arg2.argument.words) {
			d.print(w.text+"{"+w.wid.getMixID()+"} ");
		}
		d.println();
	}

	public static void printNeighbours(Dumper d, HashMap<Candidate, ArrayList<Candidate>> table) throws IOException{
		for (Candidate cand:table.keySet()){
			System.out.println("CANDIDATE : ");
			printCandidate(d, cand);
			d.print("\t\t	") ;
			printSentence(d, cand.sentence);
			System.out.println("Neighbours : ");
			int i = 1;
			for (Candidate neighbour:table.get(cand)){
				System.out.print(i+":");
				printCandidate(d, neighbour);
				d.print("\t\t") ;
				printSentence(d, neighbour.sentence);

				i++;

			}
			System.out.println("----------------");
		}
		d.println("-");

	}



	/**
	 * Performs a comparison between the predictions and the actual values of test data and prints the stats (replaces weka stats). 
	 * It actually filters out predicted relations which are not possible because of the schema, potentially increasing the quality of the prediction.
	 * @param predictedCandidates
	 * @param truelabelsCandidates
	 * @throws IOException 
	 */
	public static void givePredictionStats(Dumper dumper,ArrayList<Candidate> predictedCandidates, ArrayList<Candidate> truelabelsCandidates, List<RelationType> definedRelationTypes) throws IOException{
		Double tp = 0.0;
		Double fp = 0.0;
		Double fn = 0.0;
		HashMap<String, Double> classTP = new HashMap<>();
		HashMap<String, Double> classFP = new HashMap<>();
		HashMap<String, Double> classFN = new HashMap<>();
		ArrayList<String> classnames = new ArrayList<>();

		HashMap<String, Integer> classNumbers = new HashMap<>();
		for (RelationType relation: definedRelationTypes){
			classnames.add(relation.type);
			classNumbers.put(relation.type,0);
		}
		Integer labeled =0;
		for (int i = 0; i < predictedCandidates.size(); i++){
			Candidate predicted = predictedCandidates.get(i);
			Candidate actual = truelabelsCandidates.get(i);
			dumper.println("DEBUG==================================");
			dumper.println("Actual Candidate:");
			printCandidate(dumper, actual);
			dumper.println("Predicted Candidate");
			printCandidate(dumper, predicted);
			dumper.println("Actual Relation:");
			if (actual.relation != null) printRelation(dumper, actual.relation);
			dumper.println("Predicted Relation");
			if (predicted.relation != null)printRelation(dumper, predicted.relation);
			dumper.println("DEBUG==================================");

			if (isNoneOrNull(predicted.relation)){
				if (actual.relation != null) {
					labeled++;
					classNumbers.put(actual.relation.type.type, classNumbers.get(actual.relation.type.type)+1);
					fn++;
					if (classFN.containsKey(actual.relation.type.type))	classFN.put(actual.relation.type.type, classFN.get(actual.relation.type.type)+1);
					else classFN.put(actual.relation.type.type, 1.0);
				}
				//				else {
				//					System.out.println("true negative...");
				//				}
			}
			else {
				if (isNoneOrNull(actual.relation)){
					fp++;
					if (classFP.containsKey(predicted.relation.type.type))
						classFP.put(predicted.relation.type.type, classFP.get(predicted.relation.type.type)+1);
					else {
						classFP.put(predicted.relation.type.type,1.0);
					}
				}
				else {
					labeled++;
					classNumbers.put(actual.relation.type.type, classNumbers.get(actual.relation.type.type)+1);
					if (predicted.relation.type.type.equals(actual.relation.type.type)){
						tp++;
						if (classTP.containsKey(predicted.relation.type.type))
							classTP.put(predicted.relation.type.type, classTP.get(predicted.relation.type.type)+1);
						else classTP.put(predicted.relation.type.type,1.0);
					}
					else {
						fp++;
						if (classFP.containsKey(predicted.relation.type.type))
							classFP.put(predicted.relation.type.type, classFP.get(predicted.relation.type.type)+1);
						else classFP.put(predicted.relation.type.type,1.0);
						fn++;
						if (classFN.containsKey(actual.relation.type.type))
							classFN.put(actual.relation.type.type, classFN.get(actual.relation.type.type)+1);
						else classFN.put(actual.relation.type.type, 1.0);
					}
				}
			}
		}
		Double precision =0.0;
		Double recall = 0.0;
		Double fmeasure = 0.0 ;
		dumper.println("CANDIDATE LEVEL STATS:");
		for (String classname: classnames){
			dumper.println("Statistics for class: "+classname);
			if (classTP.get(classname) == null) classTP.put(classname, 0.0);
			if (classFP.get(classname) == null) classFP.put(classname, 0.0);
			if (classFN.get(classname) == null) classFN.put(classname, 0.0);
			Double classprecision = 0.0;
			Double classrecall = 0.0;
			Double classfmeasure =0.0;
			if (classTP.get(classname)!=0) {
				classprecision =  (classTP.get(classname)/(classTP.get(classname)+classFP.get(classname)));
				classrecall = (classTP.get(classname)/(classTP.get(classname)+classFN.get(classname)));
				dumper.format(">>>Precision: %.3f%n",classprecision);
				dumper.format(">>>Recall: %.3f%n",classrecall);
				classfmeasure =  2*classprecision*classrecall/(classprecision+classrecall);
				dumper.format(">>>Fmeasure: %.3f%n",classfmeasure);
			}
			else {
				if (classNumbers.get(classname) ==0){
					dumper.println("No instances of this class in the test set.");
				}
				else {
					dumper.format(">>>Precision: %.3f%n",0.0);
					dumper.format(">>>Recall: %.3f%n",0.0);
					dumper.println(">>>Fmeasure: NA. No TP.");
				}
			}
			precision += classprecision*classNumbers.get(classname);
			recall += classrecall*classNumbers.get(classname);
			fmeasure += classfmeasure*classNumbers.get(classname);
		}

		precision /= labeled;
		recall /=labeled;
		fmeasure /= labeled;
		dumper.println("Weighted average statistics");
		dumper.format(">>>Precision: %.3f%n",precision);
		dumper.format(">>>Recall: %.3f%n",recall);
		dumper.format(">>>Fmeasure: %.3f%n",fmeasure);
		dumper.println("_______________________");
		dumper.println("Average statistics");
		Double avgprecision =  (tp/(tp+fp));
		Double avgrecall =  tp/(tp+fn);
		Double avgfmeasure = 2*avgprecision*avgrecall/(avgprecision+avgrecall);
		dumper.format(">>>Precision: %.3f%n",avgprecision);
		dumper.format(">>>Recall: %.3f%n",avgrecall);
		dumper.format(">>>Fmeasure: %.3f%n",avgfmeasure);
		dumper.println("_______________________");




	}

	private static Boolean isNoneOrNull(Relation r){
		if (r == null) return true;
		if (r.type.type.equals("none")) return true;
		return false;
	}

	public static void printDepdendencySimilarities(ConcurrentHashMap<String, Double> concurrentHashMap, Dumper d) throws IOException{
		d.println("Writing out the similarities between dependency types as they were used in this algorithm. Format XML, you can include this in the XML configuration and edit it.");
		d.println("<syntacticDependencyDistance>");
		for (String key:concurrentHashMap.keySet()){
			d.println("\t<entry>");
			String key1 = key.split("\\|")[0];
			String key2 = key.split("\\|")[1];
			d.println("\t\t<type>"+key1+"</type>");
			d.println("\t\t<type>"+key2+"</type>");
			d.println("\t\t<similarity>"+concurrentHashMap.get(key)+"</similarity>");
			d.println("\t</entry>");

		}
		d.println("</syntacticDependencyDistance>");
	}

	public static void printRelations(Sentence s, Dumper d, String title) throws IOException {
		d.println(title);
		int i = 0;
		for (Relation r: s.relations){
			i++;
			d.print(i+"("+r.eid.getMixID()+"): ["+r.type.type+"]: ");
			String role1 = r.arg1.role;
			String role2 = r.arg2.role;
			d.print("("+role1+") ");
			d.print("["+r.arg1.argument.getTagValue("surface_form")+"{"+r.arg1.argument.tid.getMixID()+"}"+"] ");
			for (Word w:r.arg1.argument.words) {
				d.print(w.text+"{"+w.wid.getMixID()+"} ");
			}
			d.print("("+role2+") ");
			d.print("["+r.arg2.argument.getTagValue("surface_form")+"{"+r.arg2.argument.tid.getMixID()+"}"+"] ");
			for (Word w:r.arg2.argument.words) {
				d.print(w.text+"{"+w.wid.getMixID()+"} ");
			}
			d.println();
		}	
		d.println("-");
	}

	public static void printRelations(Sentence s,  String title) throws IOException {
		System.out.println(title);
		int i = 0;
		for (Relation r: s.relations){
			i++;
			System.out.print(i+"("+r.eid.getMixID()+"): ["+r.type.type+"]: ");
			String role1 = r.arg1.role;
			String role2 = r.arg2.role;
			System.out.print("("+role1+") ");
			System.out.print("["+r.arg1.argument.getTagValue("surface_form")+"{"+r.arg1.argument.tid.getMixID()+"}"+"] ");
			for (Word w:r.arg1.argument.words) {
				System.out.print(w.text+"{"+w.wid.getMixID()+"} ");
			}
			System.out.print("("+role2+") ");
			System.out.print("["+r.arg2.argument.getTagValue("surface_form")+"{"+r.arg2.argument.tid.getMixID()+"}"+"] ");
			for (Word w:r.arg2.argument.words) {
				System.out.print(w.text+"{"+w.wid.getMixID()+"} ");
			}
			System.out.println();
		}	
		System.out.println("-");
	}


	public static void printSentence(Dumper d,Sentence s) throws IOException{
		d.print(s.id+": ");
		if (s.text == null) {
			for (Word w:s.words){
				d.print(w.text+" ");
			}
		}
		else d.print(s.text);
		d.println();
		d.flush();
	}

	public static void printSentence(Sentence s) throws IOException{
		System.out.print(s.id+": ");
		if (s.text == null) {
			for (Word w:s.words){
				System.out.print(w.text+" ");
			}
		}
		else System.out.print(s.text);
		System.out.println();
	}

	public static HashMap<String, Integer> printLabeledClassStats(Dumper dumper, 	HashMap<String, String> classes, 		List<RelationType> definedRelationTypes) throws IOException {
		HashMap<String, Integer> result = new HashMap<>();

		dumper.println("Labeled examples distribution");
		for (RelationType type:definedRelationTypes){
			int i = 0;
			String t = type.type;
			for (String laclass:classes.values()){
				if (laclass.equalsIgnoreCase(t)){
					i++;
				}
			}
			dumper.println(t+": "+i);
			result.put(t, i);
		}
		Integer positives = 0;
		for (String laclass:classes.values()){
			if (!laclass.equalsIgnoreCase("none")){
				positives++;
			}
		}
		dumper.println("Total: "+positives);	
		return result;
	}

	/**
	 * @param dumper
	 * @param eval is a Weka Evaluation object
	 * @param classes is an array of <candidate id, classname>
	 * @param definedRelationTypes
	 * @return Precision, Recall, Fmeasure in that order as an array of doubles 
	 * @throws IOException
	 */
	public static HashMap<String, Double[]> printWeightedAvgClassFromWeka(Dumper dumper, Evaluation eval, HashMap<String, String> classes, 	List<RelationType> definedRelationTypes) throws IOException {
		Double[] result = {new Double(0),new Double(0),new Double(0)};
		HashMap<String, Double[]> results = new HashMap<String, Double[]>();
		int[] classNumbers = new int[definedRelationTypes.size()];
		Double WPrecision =0.0;
		Double WRecall = 0.0;
		Double WFmeasure=0.0;
		Integer positives=0;
		for (String laclass:classes.values()){
			if (!laclass.equalsIgnoreCase("none")){
				positives++;
			}
		}
		for (int i =0; i<definedRelationTypes.size(); i++){
			RelationType type = definedRelationTypes.get(i);
			classNumbers[i] = 0;
			String t = type.type;
			for (String laclass:classes.values()){
				if (laclass.equalsIgnoreCase(t)){
					classNumbers[i]++;
				}
			}
			if (!t.equals("none")){
				WPrecision += eval.precision(i)*classNumbers[i];
				WRecall += eval.recall(i)*classNumbers[i];
				WFmeasure += eval.fMeasure(i)*classNumbers[i];
			}
			result[0] = eval.precision(i);
			result[1] = eval.recall(i);
			result[2] = eval.fMeasure(i);		
			results.put(t, result);
			}
		WPrecision = WPrecision/positives;
		WRecall = WRecall/positives;
		WFmeasure = WFmeasure/positives;
		result[0] = WPrecision;
		result[1] = WRecall;
		result[2] = WFmeasure;
		dumper.println("_______________________");
		dumper.println("Weighted Average Positive Only Measures");
		dumper.format("Precision: %.3f%n",WPrecision);
		dumper.format("Recall:    %.3f%n",WRecall);
		dumper.format("F-Measure: %.3f%n",WFmeasure);
		dumper.println("_______________________");
		results.put("Weighted Average", result);
		return results;
	}
	/**
	 * @param dumper
	 * @param evals is an array of Weka Evaluation objects, which correspond to the number of runs for the CV
	 * @param classes is an array of <candidate id, classname>
	 * @param definedRelationTypes
	 * @param recap if it is for the recap at the end, be a bit more brief
	 * @return Precision, Recall, Fmeasure in that order as an array of doubles 
	 * @throws IOException
	 */
	public static HashMap<String, Double[]> printWeightedAvgClassFromWeka(Dumper dumper, ArrayList<Evaluation> evals, HashMap<String, String> classes, 	List<RelationType> definedRelationTypes) throws IOException {
		HashMap<String, Double[]> results = new HashMap<String, Double[]>();
		Double[] result = {new Double(0),new Double(0),new Double(0)};
		Double APrecision =0.0;
		Double ARecall = 0.0;
		Double AFmeasure=0.0;
		int run = 1;
		for (Evaluation eval:evals){
			int[] classNumbers = new int[definedRelationTypes.size()];
			Double WPrecision =0.0;
			Double WRecall = 0.0;
			Double WFmeasure=0.0;
			Integer positives=0;
			for (String laclass:classes.values()){
				if (!laclass.equalsIgnoreCase("none")){
					positives++;
				}
			}
			for (int i =0; i<definedRelationTypes.size(); i++){
				RelationType type = definedRelationTypes.get(i);
				classNumbers[i] = 0;
				String t = type.type;
				for (String laclass:classes.values()){
					if (laclass.equalsIgnoreCase(t)){
						classNumbers[i]++;
					}
				}
				if (!t.equals("none")){
					WPrecision += eval.precision(i)*classNumbers[i];
					WRecall += eval.recall(i)*classNumbers[i];
					WFmeasure += eval.fMeasure(i)*classNumbers[i];
				}
				results.put(t+" run "+run,  new Double[]{eval.precision(i),eval.recall(i),eval.fMeasure(i)});
			}
			run++;
			WPrecision = WPrecision/positives;
			WRecall = WRecall/positives;
			WFmeasure = WFmeasure/positives;
			APrecision += WPrecision;
			ARecall +=WRecall;
			AFmeasure += WFmeasure;
		}
		APrecision /= evals.size();
		ARecall /= evals.size();
		AFmeasure /= evals.size();
		dumper.println("___________________________________________________");
		dumper.println("AVERAGE OF "+evals.size()+" runs \nWeighted Average Positive Only Measures");
		dumper.format("Precision: %.3f%n",APrecision);
		dumper.format("Recall:    %.3f%n",ARecall);
		dumper.format("F-Measure: %.3f%n",AFmeasure);
		dumper.println("___________________________________________________");
		result[0] = APrecision;
		result[1] = ARecall;
		result[2] = AFmeasure;
		results.put("Weighted Average", result);
		return results;

	}

	public static void printRecap(AbstractMap<String, Double[]> allStats, String desc, Dumper dumper) throws IOException {
		dumper.reopen();
		dumper.println("\n---------------------------------------------------------------");
		dumper.println("_RECAP "+desc+"__________________________________________________________");
		dumper.println("|_TYPE_______________________________________________|_PREC__|_REC___|_F-M___|");
		for (String type:allStats.keySet()) {
			dumper.format("| %-50s | %5s | %5s | %5s |%n", type, format(allStats.get(type)[0]), format(allStats.get(type)[1]), format(allStats.get(type)[2]));
		}
		dumper.println("________________________________________________________________");
	}

	static String format(Double value){
		if (Double.isNaN(value)) return String.format(" %3s ", value);
		else return String.format("%.3f", value);
	}

}
