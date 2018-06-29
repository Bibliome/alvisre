package input;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.junit.Test;

import corpus.Corpus;
import io.input.BioNLP11;

public class AlvisInputTest {
	File dataFolder = new File(System.getProperty("user.dir"), "data");
	File zoranaFiles = new File(dataFolder, "zorana");
	File folderInput = new File(zoranaFiles, "20130302");
	
	/**
	 * Test method for {@link io.input.AlvisTest#importFiles(java.lang.String)}.
	 * @throws ParseException 
	 * @throws IOException 
	 */
	@Test
	public void testImportFiles() throws ParseException, IOException {
		BioNLP11 alvisimport = new BioNLP11(folderInput.getAbsolutePath());
		alvisimport.importFiles();
		assertEquals("NamedEntity", alvisimport.corpus.searchDocumentByID("PMID-10075739-S9").sentences.get(0).getTermByID(1).type.type);
	}

	@Test
	public void testImportSingleFile() throws ParseException, Exception, IOException{
		Corpus ccorpus = new Corpus();
		File file1 = new File(folderInput, "PMID-10075739-S9.a");
		String id = "PMID-10075739-S9";
		BioNLP11 alvisimport = new BioNLP11(folderInput.getAbsolutePath());
		alvisimport.importSingleFile(ccorpus, file1, id);
		assertEquals("NamedEntity", ccorpus.documents.get(0).sentences.get(0).namedEntities.get(0).type.type);
	}
	
}
