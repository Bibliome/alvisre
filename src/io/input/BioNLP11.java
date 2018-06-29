package io.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import corpus.Corpus;
import corpus.Document;
import corpus.ID;
import corpus.NEtype;
import corpus.NamedEntity;
import corpus.Relation;
import corpus.RelationArgument;
import corpus.RelationType;
import corpus.Schema;
import corpus.Sentence;
import corpus.SyntacticRelation;
import corpus.SyntacticRelationArgument;
import corpus.SyntacticRelationType;
import corpus.Tag;
import corpus.Word;
import exceptions.UnexpectedDatasetFormat;

public class BioNLP11 extends InputModule {
	/**
	 * This is not a module for the 'BioNLP11-type format of the BioNLP ST 11 but
	 * for the GeniaWriter module of AlvisNLP, practically.
	 */
	Double verbose;

	public BioNLP11(String path) {
		super(path);
		this.type = "BioNLP11";
		this.schema = importSchema();
	}

	public Schema importSchema() {
		Schema schema = new Schema();

		return schema;
	}

	public void setSchema(Schema s) {
		schema = s;
	}


	public void importFiles(Double verbose, Boolean threaded, String[] WordTags)
			throws NumberFormatException, IOException {
		importFiles(verbose);		
	}
	public void importFiles(Double verbose) throws NumberFormatException, IOException {
		this.verbose = verbose;
		this.corpus = new Corpus();
		File folder = new File(inputDir);
		File[] listOfFiles = folder.listFiles();
		if (listOfFiles == null)
			throw new UnexpectedDatasetFormat("Error G39: The path is probably wrong, or there are no files");
		if (listOfFiles.length == 0)
			throw new UnexpectedDatasetFormat("Where are the files, mate?");
		Arrays.sort(listOfFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		for (File file : listOfFiles) {
			String filename = file.getName();
			if (!filename.contains("io.output")){
				if (verbose > 1)
					System.out.println("Reading " + filename);
				String[] nameparts = filename.split("\\.");
				if (nameparts.length < 2) {
					if (verbose > 1)	System.out.println(filename);
				}
				else {
					String id = nameparts[0];
					String suffix = nameparts[1];
					if ((suffix.equals("txt") && this.type.equals("BI")) ||suffix.equals("a2")||suffix.equals("a") ) {
						importSingleFile(this.corpus, file, id);
					}
				}
			}
		}
	}

	public void importFiles() throws NumberFormatException, IOException {
		importFiles(new Double(1));
	}

	private void importSingleDocSingleSentFile(Corpus ccorpus, String id, Document d, String text) throws NumberFormatException, IOException {

		if (d != null) {
			// in the BI case we have 1 sentence per
			// document, if the document exists, it has
			// already one sentence, this one
			d.sentences.get(0).text = text;
		} else {
			Document doc = new Document(id);
			Sentence s = new Sentence(id, text,d);
			doc.addSentence(s);
			ccorpus.addDocument(doc);
			d = doc;
		}
		File a1file = new File(inputDir, id + ".a1");
		if (a1file.exists()) {
			FileReader a1reader = new FileReader(a1file);
			BufferedReader a1textreader = new BufferedReader(a1reader);
			String line;
			while ((line = a1textreader.readLine()) != null) {
				String[] parts = line.split("\\s");
				// for (String part:parts){
				// System.out.println("Debug: "+part);
				// }
				// System.out.println("------");
				if (parts[0].length() > 0 && parts[0].charAt(0) == 'T') {
					ID termid = new ID(parts[0]);
					NEtype existingType = schema.getNETypeByName(parts[1]);
					NEtype termtype = (existingType == null) ? new NEtype(parts[1]) : existingType;
					NamedEntity namedEntity = new NamedEntity(termtype, Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), termid, line);
					d.sentences.get(0).namedEntities.add(namedEntity);
				}
				if (parts[0].length() > 0 && parts[0].charAt(0) == 'W') {
					ID wordid = new ID(parts[0]);
					Word word = new Word(parts[4], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), wordid);
					d.sentences.get(0).words.add(word);
				}
				if (parts[0].length() > 0 && parts[0].charAt(0) == 'R') {
					// R1 comp:N-N(of) W5 W7
					// 0 1 2 3
					ID synid = new ID(parts[0]);
					ID w1id = new ID(parts[2]);
					ID w2id = new ID(parts[3]);
					Word word1 = d.sentences.get(0).getWordByID(w1id.id);
					Word word2 = d.sentences.get(0).getWordByID(w2id.id);
					SyntacticRelationArgument arg1 = (d.sentences.get(0).getSyntacticRelationArgument(word1) == null) ? new SyntacticRelationArgument(word1) : d.sentences.get(0)
							.getSyntacticRelationArgument(word1);
					SyntacticRelationArgument arg2 = (d.sentences.get(0).getSyntacticRelationArgument(word2) == null) ? new SyntacticRelationArgument(word2) : d.sentences.get(0)
							.getSyntacticRelationArgument(word2);
					SyntacticRelationType synType = new SyntacticRelationType(parts[1]);
					SyntacticRelation synrel = new SyntacticRelation(synType, arg1, arg2, synid);
					d.sentences.get(0).syntacticRelations.add(synrel);
					Boolean needsPOS = true;
					for (Tag tag: word1.tags){
						if (tag.layer.equals("POS")) {
							needsPOS = false;
						}
					}
					if (needsPOS) {
						Pattern p = Pattern.compile(".*[a-z]+:([A-Z])-([A-Z]).*");
						Matcher m = p.matcher(parts[1]);
						if (m.matches()) {
							String typ1 = m.group(1);
							String typ2 = m.group(2);
							Tag tag1 = new Tag(typ1, "POS");
							Tag tag2 = new Tag(typ2, "POS");
							word1.addTag(tag1);
							word2.addTag(tag2);
						}
					}
				}
			}
			a1textreader.close();
		} else {
			throw new UnexpectedDatasetFormat("What the hell? There was no a1 file: " + a1file.getAbsolutePath() + " !");
		}

		File a2file = new File(inputDir, id + ".a2");
		if (a2file.exists()) {
			FileReader a2reader = new FileReader(a2file);
			BufferedReader textreader = new BufferedReader(a2reader);
			String line;
			while ((line = textreader.readLine()) != null) {
				String[] parts = line.split("\\s");
				String relType = parts[1];
				String role_arg1 = parts[2];
				String role_arg2 = parts[3];
				ID relID = new ID(parts[0]);
				String[] tmp1 = role_arg1.split(":");
				String role1 = tmp1[0];
				ID term1id = new ID(tmp1[1]);
				NamedEntity argument1 = d.sentences.get(0).getTermByID(term1id.id);
				String[] tmp2 = role_arg2.split(":");
				String role2 = tmp2[0];
				ID term2id = new ID(tmp2[1]);
				NamedEntity argument2 = d.sentences.get(0).getTermByID(term2id.id);
				RelationType type = schema.getRelationTypeByName(relType);
				if (relType == null || type == null) {
					System.out.println("G160: Problem, this shouldn't have happened!");
				}
				RelationArgument arg1 = (d.sentences.get(0).getRelationArgument(argument1, role1) == null) ? new RelationArgument(argument1, role1) : d.sentences.get(0).getRelationArgument(
						argument1, role1);
				RelationArgument arg2 = (d.sentences.get(0).getRelationArgument(argument2, role2) == null) ? new RelationArgument(argument2, role2) : d.sentences.get(0).getRelationArgument(
						argument2, role2);
				Relation rel = new Relation(type,  arg1,  arg2, relID);
				d.sentences.get(0).relations.add(rel);
			}
			textreader.close();
		}

	}

	private void importSingleDocMultiSentFile(Corpus ccorpus, String id, Document d, File file) throws IOException {

		if (d != null) {
			// TODO: Add the text later on
			// Integer noOfSentences = d.sentences.size();
			// d.sentences.get(0).text = text;
		} else {
			Document doc = new Document(id);
			//			Sentence s = new Sentence(id, text);
			//			doc.addSentence(s);
			ccorpus.addDocument(doc);
			d = doc;
		}
		File geniafile = file;
		if (geniafile.exists()) {
			FileReader geniareader = new FileReader(geniafile);
			BufferedReader geniatextreader = new BufferedReader(geniareader);
			String line;
			Sentence fakesentence = new Sentence("fake",d);

			while ((line = geniatextreader.readLine()) != null) {
				String[] parts = line.split("\\s");
				if (parts[0].length() > 0 && parts[0].charAt(0) == 'T') {
					ID termid = new ID(parts[0]);
					NEtype existingType = schema.getNETypeByName(parts[1]);
					NEtype termtype = (existingType == null) ? new NEtype(parts[1]) : existingType;
					NamedEntity namedEntity = new NamedEntity(termtype, Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), termid, line);
					fakesentence.namedEntities.add(namedEntity);
				}
				if (parts[0].length() > 0 && parts[0].charAt(0) == 'W') {
					ID wordid = new ID(parts[0]);
					Word word = new Word(parts[4], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), wordid);
					Integer i = 5;
					while (i < parts.length) {
						String layername = parts[i];
						i++;
						if (i == parts.length){
							geniatextreader.close();
							throw new UnexpectedDatasetFormat("Problem with the tags of word " + parts[0]+ " of file "+geniafile.getAbsolutePath());
						}
						else {
							String tagname = parts[i];
							Tag tag = new Tag(tagname, layername);
							word.addTag(tag);
						}
						i++;
					}
					fakesentence.words.add(word);
				}
				if (parts[0].length() > 0 && parts[0].charAt(0) == 'R') {
					//					R1      Dependency dependent:W5 sentence:S1 head:W12 Label:comp_by:V-N

					ID synid = new ID(parts[0]);
					String role_arg1 = parts[2];
					String role_arg2;
					String synTypeComplete;
					if (parts[3].startsWith("sent")) {
						//					String sentenceid = parts[3];
						role_arg2 = parts[4];
						synTypeComplete = parts[5];
					}
					else {
						role_arg2 = parts[3];
						synTypeComplete = parts[4];
					}
					String[] tmp1 = role_arg1.split(":");
					//String role1 = tmp1[0]; //not needed if direction is ignored //TODO
					ID w1id = new ID(tmp1[1]);
					String[] tmp2 = role_arg2.split(":");
					//					String role2 = tmp2[0]; //not needed if direction is ignored
					ID w2id = new ID(tmp2[1]);
					//String[] tmp3 = sentenceid.split(":");
					//			ID sentid = new ID(tmp3[1]); //TODO: not used for now
					String[] tmp4 = synTypeComplete.split(":");
					String synTypeString = tmp4[1];
					Word word1 = fakesentence.getWordByID(w1id.id);
					Word word2 = fakesentence.getWordByID(w2id.id);
					SyntacticRelationArgument arg1 = (fakesentence.getSyntacticRelationArgument(word1) == null) ? new SyntacticRelationArgument(word1) : fakesentence.getSyntacticRelationArgument(word1) ;
					SyntacticRelationArgument arg2 = (fakesentence.getSyntacticRelationArgument(word2) == null) ? new SyntacticRelationArgument(word2) : fakesentence.getSyntacticRelationArgument(word2);
					SyntacticRelationType synType = new SyntacticRelationType(synTypeString);
					SyntacticRelation synrel = new SyntacticRelation(synType, arg1, arg2, synid);
					fakesentence.syntacticRelations.add(synrel);
				}
				if (parts[0].length() > 0 && parts[0].charAt(0) == 'E'){
					String relType = parts[1];
					String role_arg1 = parts[2];
					String role_arg2 = parts[3];
					ID relID = new ID(parts[0]);
					String[] tmp1 = role_arg1.split(":");
					String role1 = tmp1[0];
					ID term1id = new ID(tmp1[1]);
					Sentence sentence = d.searchSentenceByTermId(term1id).get(0);
					NamedEntity argument1 = sentence.getTermByID(term1id.id);
					String[] tmp2 = role_arg2.split(":");
					String role2 = tmp2[0];
					ID term2id = new ID(tmp2[1]);
					NamedEntity argument2 = sentence.getTermByID(term2id.id);
					RelationType type = schema.getRelationTypeByName(relType);
					RelationArgument arg1 = (sentence.getRelationArgument(argument1, role1) == null) ? new RelationArgument(argument1, role1) : sentence.getRelationArgument(argument1, role1);
					RelationArgument arg2 = (sentence.getRelationArgument(argument2, role2) == null) ? new RelationArgument(argument2, role2) : sentence.getRelationArgument(argument2, role2);
					Relation rel = new Relation(type,  arg1,  arg2, relID);
					sentence.relations.add(rel);
				}
				if (parts[0].length() > 0 && parts[0].charAt(0) == 'S') {
					String sentenceID = id + parts[0];
					Sentence sentence = d.searchSentenceByID(sentenceID);
					if (sentence == null) {
						sentence = new Sentence(sentenceID,d);				
					}
					int start = Integer.parseInt(parts[2]);
					int end = Integer.parseInt(parts[3]);
					sentence.setBoundaries(start, end);
					int len = parts.length;
					String stext =parts[4];					
					for (int i = 5; i< len; i++){
						stext = stext+" "+parts[i];
					}
					sentence.text = stext;
					d.sentences.add(sentence);
				}
				for (Sentence sentence:d.sentences){
					for (Word word:fakesentence.words){
						if (isWithin(sentence.start, sentence.end, word.start, word.end)) {
							if (sentence.getWordByID(word.wid.id) == null){
								sentence.words.add(word);
							}
							SyntacticRelationArgument sra = fakesentence.getSyntacticRelationArgument(word); 
							//new SyntacticRelationArgument(word);
							for (SyntacticRelation sr:fakesentence.getSyntacticRelationsHaving(sra)){
								if (!sentence.syntacticRelations.contains(sr)){
									sentence.syntacticRelations.add(sr);
								}
							}
						}
					}
					for (NamedEntity namedEntity:fakesentence.namedEntities){
						for (int i=0; i<namedEntity.getStart().size(); i++){
							if (isWithin(sentence.start, sentence.end, namedEntity.getStart(i), namedEntity.getEnd(i))){
								if (sentence.getTermByID(namedEntity.tid.id) == null){
									sentence.namedEntities.add(namedEntity);
								}
							}
						}
					}
				}


			}
			geniatextreader.close();	
		} else {
			throw new UnexpectedDatasetFormat("What the hell? There was no file " + geniafile.getAbsolutePath()+ "!");
		}

		//		String a2filename = inputPath + id + ".a2";
		//		File a2file = new File(a2filename);
		//		if (a2file.exists()) {
		//			FileReader a2reader = new FileReader(a2file);
		//			BufferedReader textreader = new BufferedReader(a2reader);
		//			String line;
		//			while ((line = textreader.readLine()) != null) {
		//				String[] parts = line.split("\\s");
		//				String relType = parts[1];
		//				String role_arg1 = parts[2];
		//				String role_arg2 = parts[3];
		//				ID relID = new ID(parts[0]);
		//				String[] tmp1 = role_arg1.split(":");
		//				String role1 = tmp1[0];
		//				ID term1id = new ID(tmp1[1]);
		//				Sentence sentence = d.searchSentenceByTermId(term1id);
		//				NamedEntity argument1 = sentence.getTermByID(term1id.id);
		//				String[] tmp2 = role_arg2.split(":");
		//				String role2 = tmp2[0];
		//				ID term2id = new ID(tmp2[1]);
		//				NamedEntity argument2 = sentence.getTermByID(term2id.id);
		//				RelationType type = schema.getRelationTypeByName(relType);
		//				RelationArgument arg1 = (sentence.getRelationArgument(argument1) == null) ? new RelationArgument(argument1) : sentence.getRelationArgument(argument1);
		//				RelationArgument arg2 = (sentence.getRelationArgument(argument2) == null) ? new RelationArgument(argument2) : sentence.getRelationArgument(argument2);
		//				Relation rel = new Relation(type, role1, arg1, role2, arg2, relID);
		//				sentence.relations.add(rel);
		//			}
		//		}
	}

	public void importSingleFile(Corpus ccorpus, File file, String id) throws NumberFormatException, IOException {
		Document d = ccorpus.searchDocumentByID(id);
		//test for file
		//		if (!file.exists()){
		//			throw new UnexpectedDatasetFormat("There is no such file!");
		//		}

		if (this.type.equals( "BI")) {
			FileReader reader = new FileReader(file);
			String text;
			BufferedReader txtreader = new BufferedReader(reader);
			text = txtreader.readLine();
			importSingleDocSingleSentFile(ccorpus, id, d, text);
			txtreader.close();
		} else {
			importSingleDocMultiSentFile(ccorpus, id, d, file);
		}

	}

	public Boolean isWithin(int sentstart, int sentend, int start, int end){
		if ((start >= sentstart)&& (end <= sentend)) return true;
		return false;

	}





}
