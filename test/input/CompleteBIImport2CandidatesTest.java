package input;

import java.io.File;

public class CompleteBIImport2CandidatesTest {
	
	File dataFolder = new File( System.getProperty("user.dir"), "data");
	File corpusFolder = new File(dataFolder, "BioNLP-ST_2011_bacteria_interactions_train_data_rev1" );
	String fileId = "PMID-10503549-S4";
	
//	@Test
//	public void singleFileTotal() throws ParseException, IOException{
//		BI bimport = new BI( corpusFolder.getAbsolutePath() );
//		Corpus ccorpus = new Corpus();
//		File file1 = new File(corpusFolder, fileId + ".txt");
//		bimport.importSingleFile(ccorpus, file1 ,fileId);
////		Corpus corpus = bimport.getCorpus();
//		Schema schema = bimport.schema;
//		Document d = ccorpus.documents.get(0);
//		Sentence s = d.sentences.get(0);
//		ShortestDependencyPath sp = new ShortestDependencyPath(s, d);
//		List<ID> list = sp.findPathBetweenTerms(null, s, s.getTermByID(1), s.getTermByID(4), false);
//		assertFalse(list.isEmpty());
//		PrintStuff.printSentenceAndPathID(s, list);
//		List<representation.ShortestDependencyPath.Couple> cp  = sp.findPossibleCandidates(s,schema.DefinedTypes,schema.DefinedRelationTypes);
//		assertFalse(cp.isEmpty());
//		//This (cp) is the list of ALL POSSIBLE CONNECTED CANDIDATES = 
//		//valid relation types possibly expressed in this sentence. 
//		System.out.println("===================");
//		List<Candidate> cands = sp.CalculateTransformation(schema);
//		//This (cands) is the list of all EXISTING connected candidates = relations,
//		// it consists of PATHS. Practically all the paths in this sentence that could be relations.
//		PrintStuff.printSentenceAndCandidatesID(cands);
//		assertFalse(cands.isEmpty());
//		s.candidates = new ArrayList<Candidate>(cands);
//		System.out.println("----------------------");
//		PrintStuff.printSentenceAndCandidatesID(s);
//	}
//
//	@Test
//	public void testAll() throws ParseException, IOException {
//		BI bimport = new BI( corpusFolder.getAbsolutePath() );
//		bimport.importFiles();
//		Corpus corpus = bimport.getCorpus();
//		Schema schema = bimport.schema;
//		assertNotNull(schema);
//		assertNotNull(corpus);
//		assertFalse(corpus.documents.isEmpty());
//		for (Document d : corpus.documents) {
//			assertNotNull(d);
//			for (Sentence s : d.sentences) {
//				assertNotNull(s);
//				ShortestDependencyPath sp = new ShortestDependencyPath(s, d);
//				s.candidates = new ArrayList<Candidate>( sp.CalculateTransformation() );
//				System.out.println(s.id);
//				assertFalse(s.candidates.isEmpty() && !s.relations.isEmpty());
////				PrintStuff.printSentenceAndCandidatesID(s);
//			}
//		}
//	}
}