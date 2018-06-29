/**
 * 
 */
package kernels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;

import corpus.ID;
import corpus.ID.Type;
import corpus.SyntacticRelation;
import corpus.Word;
import exceptions.UnexpectedDatasetFormat;
import representation.Path;
import semantictools.Word2vecHelper;
import semantictools.WordToSemClassTable;

/**
 * @author dvalsamou
 *
 */
public class PositionScore {
	public SimilarityFunctionConfiguration configuration = new SimilarityFunctionConfiguration();
	public List<PartSimilarityTable> partSimilarityTables = new ArrayList<PartSimilarityTable>();
	Double posIdentical;
	Double sameSyntacticDependencyType;
	Double sameSyntacticDependencyFamily;
	Double sameSemanticClass;
	Double discourageScore;
	Boolean verbose = false;
	ConcurrentHashMap<String, Double> knowndict = new ConcurrentHashMap<>();
	ConcurrentHashMap<String, Double> knowndep = new ConcurrentHashMap<>();
	String discoDir;
	String word2vecVectorFile;
	WordToSemClassTable wordToSemClassTable = new WordToSemClassTable();
	Double canonicalDifferent;
	Double canonicalIdentical;
	String canonicalStringSimilarity;
	Double differentSyntacticDependencyType;
	Double discoWeight;
	Double word2vecWeight;
	Boolean maskTerms;
	Double surfaceDifferent;
	Double surfaceIdentical;
	String surfaceStringSimilarity;
	Double surfaceSimilarityWeight;
	String WordNetAlgorithm;
	Double WordNetWeight;
	Double posDifferent;
	Double canonicalSimilarityWeight;
	Double wordpathSyntaxSimilarity;
	Double POSWeight;
	Double wordpathCost;
	Word2vecHelper w2v;

	/**
	 * @throws Exception 
	 * 
	 */
	public PositionScore(SimilarityFunctionConfiguration conf, List<PartSimilarityTable> tables) throws Exception {
		partSimilarityTables = tables;
		configuration = conf;
		verbose = conf.getBooleanValue("verbose");
		discoDir = conf.getStringValue("discoDir");
		word2vecVectorFile = conf.getStringValue("word2vecVectorFile");
		wordToSemClassTable = conf.wordToSemClassTable;
		posIdentical = conf.getDoubleValue("posIdentical");
		surfaceIdentical = conf.getDoubleValue("surfaceIdentical");
		sameSyntacticDependencyType = conf.getDoubleValue("sameSyntacticDependencyType");
		sameSyntacticDependencyFamily = conf.getDoubleValue("sameSyntacticDependencyFamily");
		sameSemanticClass = conf.getDoubleValue("sameSemanticClass");
		canonicalDifferent = conf.getDoubleValue("canonicalDifferent"); 
		canonicalIdentical = conf.getDoubleValue("canonicalIdentical");
		canonicalStringSimilarity = conf.getStringValue("canonicalStringSimilarity");
		canonicalSimilarityWeight = conf.getDoubleValue("canonicalSimilarityWeight");
		differentSyntacticDependencyType = conf.getDoubleValue("differentSyntacticDependencyType");
		discoWeight = conf.getDoubleValue("discoWeight");
		word2vecWeight = conf.getDoubleValue("word2vecWeight");
		maskTerms = conf.getBooleanValue("maskTerms");
		surfaceDifferent = conf.getDoubleValue("surfaceDifferent");
		surfaceStringSimilarity =  conf.getStringValue("surfaceStringSimilarity");
		surfaceSimilarityWeight = conf.getDoubleValue("surfaceSimilarityWeight");
		WordNetAlgorithm = conf.getStringValue("WordNetAlgorithm");
		WordNetWeight = conf.getDoubleValue("WordNetWeight");
		posDifferent = conf.getDoubleValue("posDifferent");
		POSWeight = conf.getDoubleValue("POSWeight");
		wordpathSyntaxSimilarity = conf.getDoubleValue("wordpathSyntaxSimilarity");
		wordpathCost = conf.getDoubleValue("wordpathCost");
		discourageScore = -100.0;
		if (word2vecWeight != 0.0) w2v = new Word2vecHelper(word2vecVectorFile, true);
	}

	public  Double score( ID id1, Path p1, ID id2, Path p2) { // word to
		return score(id1, p1, id2, p2, false);
	}

	public  Double score(ID id1, Path p1, ID id2, Path p2, Boolean threadedMaps){
			// word
			// similarity
			// score
			// TODO: Terms --- There will never be namedEntities here because this is taken care of at the path level, RIGHT?
		ConcurrentHashMap<String, Double> knowndictLOCAL = knowndict;
		ConcurrentHashMap<String, Double> knowndepLOCAL = knowndep;
//			if (threadedMaps)  {
//				knowndictLOCAL = new HashMap<String,Double>();
//				knowndepLOCAL = new HashMap<String,Double>();
//			}
			Double posScore = 0.0;
			Double canonicalScore= 0.0;
			Double surfaceScore= 0.0;
			if (id1.type.equals(Type.textbound)) {
				if (id2.type.equals(Type.textbound)) {
					Word w1 = p1.sentence.getWordByID(id1.id);
					Word w2 = p2.sentence.getWordByID(id2.id);
					if (w1 == null || w2 == null) {
						throw new UnexpectedDatasetFormat("Error GA81: There was no word(s) given to the word similarity calculator, not nice!");
					}
					if (w1.equals(w2)) { 
						return 1.0;
					}
					Double score = 0.0;
					Double maxscore = 0.0;
					if (w1.tags.size() != 0 && w2.tags.size() != 0) {
						String pos1 = w1.getTagValue("POS");
						String pos2 = w2.getTagValue("POS");
						if ((pos1 != null) && (pos2 != null) ){
							String pos1start = pos1.substring(0, 1);
							if (pos1.equals(pos2)){
								posScore= posIdentical;
							}
							else if (pos2.startsWith(pos1start)){
								posScore = 0.9 * posIdentical ;
							}
							else {
								posScore = posDifferent;
							}
						}
						String surface1 = w1.getTagValue("surface_form");
						String surface2 = w2.getTagValue("surface_form");
						if ((surface1 == null) || (surface2 == null) ){
							surface1 = w1.text;
							surface2 = w2.text;
						}
						if (surface1.equals(surface2)){
							surfaceScore = surfaceIdentical;
						}
						else {
							Double srfcscore =  TextboundSimilarity.stringCompare(surface1, surface2, surfaceStringSimilarity);
	//						if (srfcscore > 0.5) surfaceScore = srfcscore;
							if(srfcscore < 1.0) surfaceScore = srfcscore;
						}
						String canonical1 = w1.getTagValue("canonical_form");
						String canonical2 = w2.getTagValue("canonical_form");
						if (canonical1 == null) canonical1 = surface1;
						if (canonical2 == null) canonical2 = surface2;
						if ((canonical1 != null) && (canonical2 != null) ){
							if (canonical1.equals(canonical2)){
								canonicalScore =  canonicalIdentical;
							}
							else {
								if (canonical1.toLowerCase().equals(canonical2.toLowerCase())){
									canonicalScore =  0.9*canonicalIdentical;
								}
								else {
									String concat;
									if (canonical1.compareTo(canonical2) < 0){
										concat = canonical1+"|"+canonical2;
									}
									else {
										concat = canonical2+"|"+canonical1;
									}
									if (knowndictLOCAL.containsKey(concat)){
										canonicalScore = knowndictLOCAL.get(concat);
									}
									else {
										ArrayList<Double> scores = new ArrayList<>();
										scores.add(canonicalDifferent);
										if (wordToSemClassTable.inTheSameClass(canonical1, canonical2)){
											scores.add(sameSemanticClass);
										}
										scores.add(TextboundSimilarity.stringCompare(canonical1, canonical2, canonicalStringSimilarity));
										if (discoWeight != 0.0){
											if (StringUtils.isAlphanumeric(canonical1) && StringUtils.isAlphanumeric(canonical2)) {
												try {
													scores.add(discoWeight*TextboundSimilarity.semanticCompareViaDISCOPubmed(discoDir,canonical1, canonical2, false, false));
												} catch (IOException e) {
													e.printStackTrace();
												}
											}
										}
										if (word2vecWeight != 0.0){
											if (StringUtils.isAlphanumeric(canonical1) && StringUtils.isAlphanumeric(canonical2)) {
												Double wscore =word2vecWeight* w2v.dotProduct(canonical1, canonical2);
												if (wscore != -100.0) {
													if (wscore > 1.0) {
														scores.add(1.0); 
													}
													else {
														scores.add(wscore);
													}
												}
											}
										}
										if (WordNetWeight != 0.0) {
											scores.add(WordNetWeight*TextboundSimilarity.semanticCompareViaWordnet(canonical1, canonical2, WordNetAlgorithm));
										}
										canonicalScore = Collections.max(scores);
	
									}
									if (canonicalScore <= 0.0) {
										knowndictLOCAL.put(concat, 0.0);
										canonicalScore = 0.0;
									}
									else {
										knowndictLOCAL.put(concat, canonicalScore);
									}
								}
							}
						}
					}
	
	
					score = POSWeight * posScore +  surfaceSimilarityWeight * surfaceScore +  canonicalSimilarityWeight*canonicalScore;
					maxscore += (POSWeight + surfaceSimilarityWeight + canonicalSimilarityWeight);
					if (score == 0.0){
						return score;
					}
					score = score / maxscore;
					return score ;
				} else
					return discourageScore; // no word-syntax alignment
			}
			if (id1.type.equals(Type.relation)) {
				if (id2.type.equals(Type.relation)) {
					SyntacticRelation sr1 = p1.sentence.getSyntacticRelationByID(id1.id);
					SyntacticRelation sr2 = p2.sentence.getSyntacticRelationByID(id2.id);
					if (sr1 == null || sr2 == null) {
						throw new UnexpectedDatasetFormat("Error GA101: There was no relation(s) given to the relation similarity calculator, not nice!");
						//
					}
					String depconcat;
	
	
	
	
					if (sr1.type.type.compareTo(sr2.type.type) < 0){
	
						depconcat = sr1.type.type+"|"+sr2.type.type;
					}
					else {
						depconcat = sr2.type.type+"|"+sr1.type.type;
					}
					//				//cut here for wordpath
					if (sr1.type.type.equalsIgnoreCase("wordpath")||sr2.type.type.equalsIgnoreCase("wordpath")){
						knowndepLOCAL.put(depconcat,wordpathSyntaxSimilarity);
						return wordpathSyntaxSimilarity; 
					}
					//				//cut here
	
	
					//  cut here for anaphora hack
					if (sr1.type.type.equalsIgnoreCase("anaphora")||sr2.type.type.equalsIgnoreCase("anaphora")){
						knowndepLOCAL.put(depconcat,discourageScore);
						return discourageScore; 
					}
	
					// cut here
	
					if (sr1.type.type.equals(sr2.type.type)) {
						knowndepLOCAL.put(depconcat,sameSyntacticDependencyType);
						return sameSyntacticDependencyType;
					}
	
					if (knowndepLOCAL.containsKey(depconcat)){
						return knowndepLOCAL.get(depconcat);
					}
	
					if (getSimilarityTableByLayerName("syntacticDependencies") == null) {
						String sr1s = sr1.type.type.substring(0, 3);
						String sr2s = sr2.type.type.substring(0, 3);
						if (sr1s.equals(sr2s)) { 
							knowndepLOCAL.put(depconcat, sameSyntacticDependencyFamily);
							return sameSyntacticDependencyFamily;
						}
						else {
							knowndepLOCAL.put(depconcat, differentSyntacticDependencyType);
							return differentSyntacticDependencyType;
						}
	
					} else {
						PartSimilarityTable syntactic = getSimilarityTableByLayerName("syntacticDependencies");
						if (syntactic.getSimilarity(sr1.type.type, sr2.type.type) == null) {
							if ( (sr1.type.type.length() > 3 && sr2.type.type.length() >3) && sr2.type.type.startsWith(sr1.type.type.substring(0,3))){
								if (sameSyntacticDependencyFamily!= 0.0) { 
									knowndepLOCAL.put(depconcat, sameSyntacticDependencyFamily);
									return sameSyntacticDependencyFamily;
								}
							}
							knowndepLOCAL.put(depconcat, differentSyntacticDependencyType);
							return differentSyntacticDependencyType;
						}
						knowndepLOCAL.put(depconcat, syntactic.getSimilarity(sr1.type.type, sr2.type.type));
						return syntactic.getSimilarity(sr1.type.type, sr2.type.type);
					}
				} else
					return discourageScore; // no word-syntax alignment
			}
			return 0.0;
	
		}

	


	public PartSimilarityTable getSimilarityTableByLayerName(String layer) {
		for (PartSimilarityTable r : partSimilarityTables) {
			if (r.layer.equals(layer)) {
				return r;
			}
		}
		return null;
	}

}
