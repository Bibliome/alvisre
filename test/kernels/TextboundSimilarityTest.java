package kernels;

import java.io.IOException;

import org.junit.Test;


public class TextboundSimilarityTest {
	String string1 = "cow";
	String string2 = "cowboy";
	String string3 = "parrot";
	Double score1 = 0.0;
	Double score2 = 0.0;

	@Test
	public void testCompareStringStringStringString() throws IOException {
		TextboundSimilarity.compare(string1, string2, "wordnet", "hco");
	}
	@Test
	public void testCompareStringStringString() throws IOException {
		TextboundSimilarity.compare(string1, string2, "secondstring");
	}

	@Test
	public void testCompareStringString() throws IOException {
		TextboundSimilarity.compare(string1, string2);
	}

	@Test
	public void testSemanticCompareViaWordnet() {
		// 
		System.out.println("testSemanticCompareViaWordnet");
		String[] methods ={"HSO", "LCH", "LESK", "WUP", "RES", "JCN", "LIN", "CACA"};
		for (String method:methods) {
			score1 = TextboundSimilarity.semanticCompareViaWordnet(string1, string2, method);
			score2 = TextboundSimilarity.semanticCompareViaWordnet(string1, string3, method);
			System.out.println(method+"\t"+string1+"\t"+string2+": "+score1);
			System.out.println(method+"\t"+string1+"\t"+string3+": "+score2);
		}
	}

	@Test
	public void testSecondStringCompare() {
		System.out.println("testSecondStringCompare");
		String[] methods ={"Jaro", "JaroWinkler", "Jaccard", "JaroTFIDF", "JaroWinklerTFIDF", "AffineGap", "Levenstein", "ScaledLevenstein", "SmithWaterman", "NeedlmanWunsch"};
		for (String method:methods) {
			score1 = TextboundSimilarity.secondStringScompare(string1, string2, method);
			score2 = TextboundSimilarity.secondStringScompare(string1, string3, method);
			System.out.println(method+"\t"+string1+"\t"+string2+": "+score1);
			System.out.println(method+"\t"+string1+"\t"+string3+": "+score2);
		}
	}

	@Test
	public void testNgramSecondStringCompare() {
		string1 = "Two red chickens go to the market";
		string2 = "Two blue chickens go to the market.";
		string3 = "I don't know what I'm talking about.";
		System.out.println("ngramSecondStringScompare n=2");
		String[] methods ={"Jaro", "JaroWinkler", "Jaccard", "JaroTFIDF", "JaroWinklerTFIDF", "AffineGap", "Levenstein", "ScaledLevenstein", "SmithWaterman", "NeedlmanWunsch"};
		for (String method:methods) {
			score1 = TextboundSimilarity.ngramSecondStringScompare(string1, string2, method,1,10, true);
			score2 = TextboundSimilarity.ngramSecondStringScompare(string1, string3, method,2,2, true);
			System.out.println(method+"\t"+string1+"\t"+string2+": "+score1);
			System.out.println(method+"\t"+string1+"\t"+string3+": "+score2);
		}	
		}

	@Test
	public void testTokenizedSecondStringCompare() {
		string1 = "Two red chickens go to the market";
		string2 = "Two blue chickens go to the market.";
		string3 = "I don't know what I'm talking about.";
		System.out.println("testTokenizedSecondStringCompare");
		String[] methods ={"Jaro", "JaroWinkler", "Jaccard", "JaroTFIDF", "JaroWinklerTFIDF", "AffineGap", "Levenstein", "ScaledLevenstein", "SmithWaterman", "NeedlmanWunsch"};
		for (String method:methods) {
			score1 = TextboundSimilarity.tokenizedSecondStringScompare(string1, string2, method);
			score2 = TextboundSimilarity.tokenizedSecondStringScompare(string1, string3, method);
			System.out.println(method+"\t"+string1+"\t"+string2+": "+score1);
			System.out.println(method+"\t"+string1+"\t"+string3+": "+score2);
		}	}

	@Test
	public void testSemanticCompareViaDISCOPubmed() throws IOException {
		string1 = "GerE";
		string2 = "SpoIIID";
		score1 = TextboundSimilarity.semanticCompareViaDISCOPubmed("data/en-PubMedOA-20070501/", string2, string1, true, false);
		System.out.println("DISCO 1st order \t"+string1+"\t"+string2+": "+score1);
		score1 = TextboundSimilarity.semanticCompareViaDISCOPubmed("data/en-PubMedOA-20070501/",string1, string2, false, false);
		System.out.println("DISCO 2nd order \t"+string1+"\t"+string2+": "+score1);


	}

	@Test
	public void testStringCompareStringStringString() {
		System.out.println("testStringCompareStringStringString");
		String[] methods ={"j","jw","sd"};
		for (String method:methods) {
			score1 = TextboundSimilarity.stringCompare(string1, string2, method);
			score2 = TextboundSimilarity.stringCompare(string1, string3, method);
			System.out.println(method+"\t"+string1+"\t"+string2+": "+score1);
			System.out.println(method+"\t"+string1+"\t"+string3+": "+score2);
		}
	}

	@Test
	public void testStringCompareStringString() {
		System.out.println("testStringCompareStringString");
		score1 = TextboundSimilarity.stringCompare(string1, string2);
		score2 = TextboundSimilarity.stringCompare(string1, string3);
		System.out.println("default\t"+string1+"\t"+string2+": "+score1);
		System.out.println("default\t"+string1+"\t"+string3+": "+score2);	}

}
