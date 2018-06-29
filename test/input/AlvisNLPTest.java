package input;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import corpus.Corpus;
import corpus.Document;
import corpus.NEtype;
import corpus.RelationType;
import corpus.Schema;
import io.input.AlvisNLP;

public class AlvisNLPTest {

	@Test
	public void testImportSingleFile() throws IOException {
		NEtype agent = new NEtype("Agent");
		NEtype  target = new NEtype("Target");
		ArrayList<NEtype> agentterms =  new ArrayList<NEtype>(Arrays.asList(agent));
		ArrayList<NEtype> targetterms =  new ArrayList<NEtype>(Arrays.asList(target));
		ArrayList<NEtype> terms =  new ArrayList<NEtype>();
		terms.addAll(targetterms);
		terms.addAll(agentterms);
		RelationType inhibition = new RelationType("Interaction", "Target", targetterms, "Agent", agentterms);
		ArrayList<RelationType> rtypes = new ArrayList<RelationType>(Arrays.asList(inhibition));
		Schema schema = new Schema( terms, rtypes, terms);
		AlvisNLP alvistest = new AlvisNLP("data/", schema);
		alvistest.setSchema(schema);
		Corpus corpus = new Corpus();
		String fileID = "data/zorana/output_lll_ccg/10075739-11.a2";
		File data = new File(fileID); 
		File file = data;
		Document d = new Document(fileID);
		alvistest.importSingleFile(new Double(10), fileID,d, file, null);
	}
	
	@Test
	public void testImportFiles() throws IOException {
		NEtype agent = new NEtype("Agent");
		NEtype  target = new NEtype("Target");
		ArrayList<NEtype> agentterms =  new ArrayList<NEtype>(Arrays.asList(agent));
		ArrayList<NEtype> targetterms =  new ArrayList<NEtype>(Arrays.asList(target));
		ArrayList<NEtype> terms =  new ArrayList<NEtype>();
		terms.addAll(targetterms);
		terms.addAll(agentterms);
		RelationType inhibition = new RelationType("Interaction", "Target", targetterms, "Agent", agentterms);
		ArrayList<RelationType> rtypes = new ArrayList<RelationType>(Arrays.asList(inhibition));
		Schema schema = new Schema( terms, rtypes, terms);
		AlvisNLP alvistest = new AlvisNLP("data/zorana/output_lll_ccg/", schema);
//		Corpus corpus  = new Corpus();
		alvistest.importFiles(new Double(1), new Boolean(false), null);
		System.out.println("test");

	}

}
