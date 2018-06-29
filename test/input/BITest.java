/**
 * 
 */
package input;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.junit.Test;

import corpus.Corpus;
import exceptions.UnexpectedDatasetFormat;
import io.input.BI;

/**
 * @author dialecti
 *
 */
public class BITest {
	File dataFolder = new File( System.getProperty("user.dir"), "data");
	File corpusFolder = new File(dataFolder, "BioNLP-ST_2011_bacteria_interactions_train_data_rev1" );

	/**
	 * Test method for {@link io.input.BI#importFiles(java.lang.String)}.
	 * @throws ParseException 
	 * @throws IOException 
	 */
	@Test
	public void testImportFiles() throws ParseException, IOException {
		BI bimport = new BI( corpusFolder.getAbsolutePath() );
		bimport.importFiles();
		assertEquals("Protein", bimport.corpus.searchDocumentByID("PMID-10323866-S9").sentences.get(0).getTermByID(5).type.type);
	}

	
	
	@Test
	public void testImportSingleFile() throws ParseException, Exception, IOException{
		Corpus ccorpus = new Corpus();
		File file1 = new File(corpusFolder, "PMID-10075739-S13.txt");
		String id1 = "PMID-10075739-S13";
		BI bimport = new BI( corpusFolder.getAbsolutePath() );
		bimport.importSingleFile(ccorpus, file1, id1);
		assertEquals("Action",ccorpus.documents.get(0).sentences.get(0).namedEntities.get(0).type.type);
		assertEquals("N", ccorpus.documents.get(0).sentences.get(0).words.get(20).tags.get(0).label);
		assertEquals("mod_att:N-ADJ",ccorpus.documents.get(0).sentences.get(0).syntacticRelations.get(15).type.type);
		//E5	ActionTarget Action:T1 Target:T2
		assertEquals("ActionTarget", ccorpus.documents.get(0).sentences.get(0).relations.get(4).type.type);
		assertEquals("Action", ccorpus.documents.get(0).sentences.get(0).relations.get(4).arg1.role);
		assertEquals("Action", ccorpus.documents.get(0).sentences.get(0).relations.get(4).arg1.argument.type.type);
		//W25	Word 159 161	as
		assertEquals(new Integer(159), ccorpus.searchDocumentByID(id1).searchSentenceByID(id1).getWordByID(25).start);
		String text = "These results suggest that a rising level of GerE in sporulating cells may first activate cotD transcription from the upstream site then repress transcription as the downstream site becomes occupied.";
		assertEquals(text,  ccorpus.searchDocumentByID(id1).searchSentenceByID(id1).text);
	}
	
	@Test(expected = FileNotFoundException.class)
	public void testImportSingleNonExistingTxt() throws NumberFormatException, IOException, ParseException{
		Corpus ccorpus = new Corpus();
		File file1 = new File(corpusFolder, "PMID-10075739-S113.txt");
		String id1 = "PMID-10075739-S113";
		BI bimport = new BI( corpusFolder.getAbsolutePath() );
		bimport.importSingleFile(ccorpus, file1, id1);
	}
	@Test(expected = UnexpectedDatasetFormat.class)
	public void testImportSingleNonExistingA1() throws ParseException, NumberFormatException, IOException{
		Corpus ccorpus = new Corpus();
		File file1 = new File(corpusFolder, "KOKO-10075739-S1013.txt");
		String id1 = "PMID-10075739-S1013";
		BI bimport = new BI( corpusFolder.getAbsolutePath() );
		bimport.importSingleFile(ccorpus, file1, id1);
	}
}
