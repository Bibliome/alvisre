package kernels;

import java.io.IOException;
import java.util.Arrays;

import com.wcohen.ss.AffineGap;
import com.wcohen.ss.Jaccard;
import com.wcohen.ss.Jaro;
import com.wcohen.ss.JaroTFIDF;
import com.wcohen.ss.JaroWinkler;
import com.wcohen.ss.JaroWinklerTFIDF;
import com.wcohen.ss.Level2;
import com.wcohen.ss.MongeElkan;
import com.wcohen.ss.NeedlemanWunsch;
import com.wcohen.ss.ScaledLevenstein;
import com.wcohen.ss.SmithWaterman;
import com.wcohen.ss.api.Tokenizer;
import com.wcohen.ss.tokens.NGramTokenizer;
import com.wcohen.ss.tokens.SimpleTokenizer;

import de.linguatools.disco.DISCO;
import edu.cmu.lti.ws4j.WS4J;
import net.ricecode.similarity.DiceCoefficientStrategy;
import net.ricecode.similarity.JaroStrategy;
import net.ricecode.similarity.JaroWinklerStrategy;
public class TextboundSimilarity {

	private TextboundSimilarity() {
	}
	/**
	 * 
	 * @param string1
	 * @param string2
	 * @param method is the library used: it can be "second" for secondString, "wordnet" for semantic via wordnet, "discopub" for DISCO with the PubMed corpus, and "string" for simple surface similarity 
	 * @param submethod: the sub-type for each library.
	 * @return score as a double
	 * @throws IOException when DISCO is used and the disco file is not found
	 */
	public static Double compare(String string1, String string2, String method, String submethod) throws IOException{
		switch (method.toLowerCase()){
		case "second":
			return secondStringScompare(string1, string2, submethod);
		case "wordnet":
			return semanticCompareViaWordnet(string1, string2, submethod);
		case "discopub":
			Boolean first = false;
			if (submethod.toLowerCase().equals("true")) first = true;
			return semanticCompareViaDISCOPubmed(submethod,string1, string2, first,false );
		default:
			return stringCompare(string1, string2, submethod);
		}
	}
	
	/**
	 * 
	 * @param string1
	 * @param string2
	 * @param method is the library used: it can be "second" for secondString, "wordnet" for semantic via wordnet, "discopub" for DISCO with the PubMed corpus, and "string" for simple surface similarity 
	 * @return score as a double
	 * @throws IOException when DISCO is used and the disco file is not found
	 */
	public static Double compare(String string1, String string2, String method) throws IOException{
		switch (method.toLowerCase()){
		case "second":
			return secondStringScompare(string1, string2);
		case "wordnet":
			return semanticCompareViaWordnet(string1, string2, "default");
		case "discopub":
			return semanticCompareViaDISCOPubmed(null,string1, string2,false, false);
		default:
			return stringCompare(string1, string2);
		}
	}
	/**
	 * compares with approximate string matching
	 * @param string1
	 * @param string2
	 * @return score as a double
	 * @throws IOException when DISCO is used and the disco file is not found
	 */
	public static Double compare(String string1, String string2) throws IOException{
		return compare(string1, string2, "secondString", "jaro");
	}
	/**
	 * WS4J (WordNet Similarity for Java): Semantic Relatedness/Similarity Attention runs on LEMMAS
	 * @see  https://code.google.com/p/ws4j/
	 * @see ftp://ftp.cs.utoronto.ca/pub/gh/Budanitsky+Hirst-2001.pdf
	 * @param string1
	 * @param string2
	 * @method: "HSO", "LCH", "LESK", "WUP", "RES", "JCN", "LIN", "PATH", default: PATH
	 * @return score as a double, note that LCH and RES are not normalized between 0.0 and 1.0 and could be unpredictable (max double value for the same word etc)
	 */
	public static Double semanticCompareViaWordnet(String string1, String string2, String method){
		Double score = 0.0;
		switch (method.toLowerCase()){
		case "hso":
			score=  WS4J.runHSO(string1, string2);
			if (score.equals(Double.MAX_VALUE)) return 1.0;
			else return score/16.0;
		case "lch":
			return  WS4J.runLCH(string1, string2);
		case "lesk":
			score=   WS4J.runLESK(string1, string2);
			if (score.equals(Double.MAX_VALUE)) return 1.0;
			else return score;
		case "wup":
			score=   WS4J.runWUP(string1, string2);
			if (score.equals(Double.MAX_VALUE)) return 1.0;
			else return score;
		case "res":
			score=   WS4J.runRES(string1, string2);
		case "jcn":
			score=   WS4J.runJCN(string1, string2);
			if (score.equals(Double.MAX_VALUE)) return 1.0;
			else return score;
		case "lin":
			score=   WS4J.runLIN(string1, string2);
			if (score.equals(Double.MAX_VALUE)) return 1.0;
			else return score;
//		case "path":
//			score=   WS4J.runPATH(string1, string2);
//			if (score == Double.MAX_VALUE) return 1.0;
//			else return score;
		default:
			score=   WS4J.runPATH(string1, string2);
			if (score.equals(Double.MAX_VALUE)) return 1.0;
			else return score;
		}
	}

	/**
	 *  SecondString, an open-source Java-based package of approximate string-matching techniques
	 * @see http://secondstring.sourceforge.net/
	 * @param string1
	 * @param string2
	 * @param method Jaro (default), JaroWinkler, Jaccard, JaroTFIDF, JaroWinklerTFIDF, AffineGap, Levenstein, ScaledLevenstein, SmithWaterman, NeedlmanWunsch 
	 * @see http://secondstring.sourceforge.net/javadoc/com/wcohen/ss/package-frame.html
	 * @return score as a double
	 */
	public static Double secondStringScompare(String string1, String string2, String method){
		switch (method.toLowerCase()){
		case "jaro":
			return new Jaro().score(string1, string2);
		case "jaccard":
			return new Jaccard().score(string1, string2);
		case "jarotfidf":
			return new JaroTFIDF().score(string1, string2);
		case "jarowinkler":
			return new JaroWinkler().score(string1, string2);
		case "jarowinklertfidf":
			return new JaroWinklerTFIDF().score(string1, string2);
		case "affinegap":
			return new AffineGap().score(string1, string2) / (double)(string1.length()+string2.length());
		case "levenstein":
			return new ScaledLevenstein().score(string1, string2);
		case "mongeelkan":
			return new MongeElkan().score(string1, string2);
		case "needlemanwunsch":
			return new NeedlemanWunsch().score(string1, string2);
		case "scaledlevenstein":
			return new ScaledLevenstein().score(string1, string2);
		case "smithwaterman":
			return new SmithWaterman().score(string1, string2) / (double)(string1.length()+string2.length());

		default:
			return new Jaro().score(string1, string2);	
		}

	}

	/**
	 * Similarity for multi-word segments, using a simple tokenizer	with ignorePunctuation and ignoreCase both FALSE
	 * @see http://secondstring.sourceforge.net/
	 * @param string1
	 * @param string2
	 * @param method Jaro (default), JaroWinkler, Jaccard, JaroTFIDF, JaroWinklerTFIDF, AffineGap, Levenstein, ScaledLevenstein, SmithWaterman, NeedlmanWunsch 
	 * @see http://secondstring.sourceforge.net/javadoc/com/wcohen/ss/package-frame.html
	 * @return
	 */
	public static Double tokenizedSecondStringScompare(String string1, String string2, String method){
		//TODO verify this works - correct it
		Tokenizer tokenizer = new SimpleTokenizer(false, false);
		switch (method.toLowerCase()){
		case "jaro":
			return new Level2( tokenizer, new Jaro()).score(string1, string2);
		case "jaccard":
			return new Level2( tokenizer, new Jaccard()).score(string1, string2);
		case "jarotfidf":
			return new Level2( tokenizer, new JaroTFIDF()).score(string1, string2);
		case "jarowinkler":
			return new Level2( tokenizer, new JaroWinkler()).score(string1, string2);
		case "jarowinklertfidf":
			return new Level2( tokenizer, new JaroWinklerTFIDF()).score(string1, string2);
		case "affinegap":
			return new Level2( tokenizer, new AffineGap()).score(string1, string2) / (double)(string1.length()+string2.length());
		case "levenstein":
			return new Level2( tokenizer, new ScaledLevenstein()).score(string1, string2);
		case "mongeelkan":
			return new Level2( tokenizer, new MongeElkan()).score(string1, string2);
		case "needlemanwunsch":
			return new Level2( tokenizer, new NeedlemanWunsch()).score(string1, string2);
		case "scaledlevenstein":
			return new Level2( tokenizer, new ScaledLevenstein()).score(string1, string2);
		case "smithwaterman":
			return new Level2( tokenizer, new SmithWaterman()).score(string1, string2) / (double)(string1.length()+string2.length());
		default:
			return new Level2( tokenizer, new Jaro()).score(string1, string2);	
		}
	}

	/**
	 * Similarity for multi-word segments, using ngrams	with ignorePunctuation and ignoreCase both FALSE
	 * @see http://secondstring.sourceforge.net/	 
	 * @param string1
	 * @param string2
	 * @param method Jaro (default), JaroWinkler, Jaccard, JaroTFIDF, JaroWinklerTFIDF, AffineGap, Levenstein, ScaledLevenstein, SmithWaterman, NeedlmanWunsch 
	 * @see http://secondstring.sourceforge.net/javadoc/com/wcohen/ss/package-frame.html
	 * @param minNGramSize
	 * @param maxNGramSize
	 * @param keepOldTokens
	 * @return
	 */
	public static Double ngramSecondStringScompare(String string1, String string2, String method, int minNGramSize, int maxNGramSize, boolean keepOldTokens){
		//TODO verify this works - correct it
		Tokenizer simple = new SimpleTokenizer(false, false);
		Tokenizer tokenizer = new NGramTokenizer(minNGramSize, maxNGramSize, false, simple);
		switch (method.toLowerCase()){
		case "jaro":
//			Token[] str1 = tokenizer.tokenize(string1);
//			Token[] str2 = tokenizer.to
//					StringWrapper str = null;
			return new Level2( tokenizer, new Jaro()).score(string1, string2);
		case "jaccard":
			return new Level2( tokenizer, new Jaccard()).score(string1, string2);
		case "jarotfidf":
			return new Level2( tokenizer, new JaroTFIDF()).score(string1, string2);
		case "jarowinkler":
			return new Level2( tokenizer, new JaroWinkler()).score(string1, string2);
		case "jarowinklertfidf":
			return new Level2( tokenizer, new JaroWinklerTFIDF()).score(string1, string2);
		case "affinegap":
			return new Level2( tokenizer, new AffineGap()).score(string1, string2) / (double)(string1.length()+string2.length());
		case "levenstein":
			return new Level2( tokenizer, new ScaledLevenstein()).score(string1, string2);
		case "mongeelkan":
			return new Level2( tokenizer, new MongeElkan()).score(string1, string2);
		case "needlemanwunsch":
			return new Level2( tokenizer, new NeedlemanWunsch()).score(string1, string2);
		case "scaledlevenstein":
			return new Level2( tokenizer, new ScaledLevenstein()).score(string1, string2);
		case "smithwaterman":
			return new Level2( tokenizer, new SmithWaterman()).score(string1, string2) / (double)(string1.length()+string2.length());
		default:
			return new Level2( tokenizer, new Jaro()).score(string1, string2);	
		}
	}
	
//	private Double tokenizedScore(String s, String t,Tokenizer tokenizer, StringDistance distance){
//		Token[] sBag = tokenizer.tokenize(s);
//		Token[] tBag = tokenizer.tokenize(t);
//			double sumOverI = 0;
//			for (Iterator i = sBag.tokenIterator(); i.hasNext(); ) {
//		    Token tokenI = (Token)i.next();
//				double maxOverJ = -Double.MAX_VALUE;
//				for (Iterator j = tBag.tokenIterator(); j.hasNext(); ) {
//					Token tokenJ = (Token)j.next();
//					double scoreItoJ = tokenDistance.score( tokenI.getValue(), tokenJ.getValue() );
//					maxOverJ = Math.max( maxOverJ, scoreItoJ);
//				}
//				sumOverI += maxOverJ;
//			}
//			return  sumOverI / sBag.size();
//		}
//	}

	/**
	 * SecondString, an open-source Java-based package of approximate string-matching techniques
	 * @see http://secondstring.sourceforge.net/
	 * @param string1
	 * @param string2
	 * @return score as a double
	 */
	public static Double secondStringScompare(String string1, String string2){
		return secondStringScompare(string1, string2, "jaro");
	}

//
//	/**
//	 * DISCO (extracting DIstributionally related words using CO-occurrences): semantic similarity between arbitrary words, using PubMed
//	 * @see http://www.linguatools.de/disco/disco_en.html
//	 * @param string1
//	 * @param string2
//	 * @return score as a double
//	 * @throws IOException if the disco language file is not found
//	 */
//	public static Double semanticCompareViaDISCOPubmed(String string1, String string2, Boolean LoadToRAM) throws IOException{
//		if (LoadToRAM) System.out.println("Loading DISCO corpus to RAM");
//		else System.out.println("NOT Loading DISCO corpus to RAM, reading directly from disk");
//    	long startTime = System.currentTimeMillis();
//		String discoDir = "data/en-PubMedOA-20070501/";
//        DISCO disco = new DISCO(discoDir, LoadToRAM);
//        Double score = (double) disco.secondOrderSimilarity(string1, string2);
//        Double score1 = (double) disco.firstOrderSimilarity(string1, string2);
//    	long endTime = System.currentTimeMillis();
//        long elapsedTime = endTime - startTime;
//    	System.out.println("OK. Computation took "+elapsedTime+" ms.");
//    	return score1;
//	}

	/**
	 * DISCO (extracting DIstributionally related words using CO-occurrences): semantic similarity between arbitrary words, using PubMed
	 * @see http://www.linguatools.de/disco/disco_en.html
	 * @param string1
	 * @param string2
	 * @return score as a double
	 * @throws IOException if the disco language file is not found
	 */
	public static Double semanticCompareViaDISCOPubmed(String discoDir,String string1, String string2, Boolean FirstOrder, Boolean LoadToRAM) throws IOException{
//		if (LoadToRAM) System.out.println("Loading DISCO corpus to RAM");
//		else System.out.println("NOT Loading DISCO corpus to RAM, reading directly from disk");
//    	long startTime = System.currentTimeMillis();
		if (discoDir == null) discoDir = "en-PubMedOA-20070501/";
        DISCO disco = new DISCO(discoDir, LoadToRAM);
        Double score = 0.0;
        if (FirstOrder){
         score = (double) disco.firstOrderSimilarity(string1, string2);
        }
        else {
            score = (double) disco.secondOrderSimilarity(string1, string2);

        }
//    	long endTime = System.currentTimeMillis();
//        long elapsedTime = endTime - startTime;
//    	System.out.println("OK. Computation took "+elapsedTime+" ms.");
    	return score;
	}
	//	/**
	//	 * DISCO (extracting DIstributionally related words using CO-occurrences): semantic similarity between arbitrary words, using wikipedia
	//	 * @param string1
	//	 * @param string2
	//	 * @return score as a double
	//	 */
	//	public static Double semanticCompareViaDISCOWikipedia(String string1, String string2){
	//		return null;
	//	}

	/**
	 * java-string-similarity with algorithms Jaro distance, Jaro-Winkler distance, Sørensen–Dice coefficient
	 * @param string1
	 * @param string2
	 * @param method can be j for Jaro, jw for Jaro-Winkler, or sd for Sorensen-Dice
	 * @return score as a double
	 */
	public static Double stringCompare(String string1, String string2, String method){
		switch(method.toLowerCase()) {
		case "j":
			return new JaroStrategy().score(string1, string2);
		case "jw":
			return new JaroWinklerStrategy().score(string1, string2);
		case "sd":
			return new DiceCoefficientStrategy().score(string1, string2);
		default:
			System.out.println("Unknown string similarity algorithm given, using Jaro");
			return new JaroStrategy().score(string1, string2);	
		}
	}

	/**
	 * java-string-similarity with algorithms Jaro distance, Jaro-Winkler distance, Sørensen–Dice coefficient
	 * more info : https://github.com/rrice/java-string-similarity
	 * @param string1
	 * @param string2
	 * with no method defined it uses Jaro's algorithm
	 * @return score as a double
	 */
	public static Double stringCompare(String string1, String string2){
		return stringCompare(string1, string2, "j");
	}
	//	/**
	//	 * 
	//	 * @see http://www.semantic-measures-library.org/sml/
	//	 * @param string1
	//	 * @param string2
	//	 * @return score as a double
	//	 */
	//	public static Double semanticNetworkCompare(String string1, String string2){
	//		//TODO
	//		return null;
	//	}
	//	
	public static double diceCoefficientOptimized(String s, String t)
	{
	        // Verifying the io.input:
	        if (s == null || t == null)
	                return 0;
	        // Quick check to catch identical objects:
	        if (s.equals(t))
	                return 1;
	        // avoid exception for single character searches
	        if (s.length() < 2 || t.length() < 2)
	            return 0;
	 
	        // Create the bigrams for string s:
	        final int n = s.length()-1;
	        final int[] sPairs = new int[n];
	        for (int i = 0; i <= n; i++)
	                if (i == 0)
	                        sPairs[i] = s.charAt(i) << 16;
	                else if (i == n)
	                        sPairs[i-1] |= s.charAt(i);
	                else
	                        sPairs[i] = (sPairs[i-1] |= s.charAt(i)) << 16;
	 
	        // Create the bigrams for string t:
	        final int m = t.length()-1;
	        final int[] tPairs = new int[m];
	        for (int i = 0; i <= m; i++)
	                if (i == 0)
	                        tPairs[i] = t.charAt(i) << 16;
	                else if (i == m)
	                        tPairs[i-1] |= t.charAt(i);
	                else
	                        tPairs[i] = (tPairs[i-1] |= t.charAt(i)) << 16;
	 
	        // Sort the bigram lists:
	        Arrays.sort(sPairs);
	        Arrays.sort(tPairs);
	 
	        // Count the matches:
	        int matches = 0, i = 0, j = 0;
	        while (i < n && j < m)
	        {
	                if (sPairs[i] == tPairs[j])
	                {
	                        matches += 2;
	                        i++;
	                        j++;
	                }
	                else if (sPairs[i] < tPairs[j])
	                        i++;
	                else
	                        j++;
	        }
	        return (double)matches/(n+m);
	}
	
	public static double customSimilarity(String s1, String s2){
		Double score = 0.0;
		Double maxscore = 1.0;
//		if (s1.length() == s2.length()){
//			score += 1;
//		}
//		if (toCapitals(s1).equals(toCapitals(s2))){
//			score +=3;
//		}
		if (((s1.length() + s2.length())/(s1.length()*2) < 1.3)&&((s1.length() + s2.length())/(s1.length()*2) > 0.7) && Math.max(s1.length(), s2.length())< 7){
			score += 1* secondStringScompare(toCapitals(s1), toCapitals(s2), "SmithWaterman");
			maxscore = 2.0;

		}
//		score += 3* secondStringScompare(toCapitals(s1), toCapitals(s2), "NeedlmanWunsch");
		if (s1.startsWith(s2.substring(0, Math.min(2,s2.length())))){
			score +=1;
		}
		
		return score/maxscore;
	}
	
	
	private static String toCapitals(String s){
		String result = "";
		for (char c:s.toCharArray()){
			if (Character.isUpperCase(c)){
				result = result+"X";
			}
			else {
				result = result+"x";
			}
		}
		return result;
	}
}
