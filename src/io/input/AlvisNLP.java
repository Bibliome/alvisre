package io.input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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
import tools.PrintStuff;

public class AlvisNLP extends InputModule {

	Double verbose;

	public AlvisNLP(String path, Schema s) {
		super(path);
		this.type = "AlvisNLP";
		this.schema = s;
	}

	//	public Schema importSchema() {
	//		Schema schema = new Schema();
	//		return schema;
	//	}

	public void setSchema(Schema s) {
		schema = s;
	}

	public void importFiles(Double verbose, Boolean threaded, String[] wordTags) throws NumberFormatException, IOException {
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

		ArrayList<ANThread> ants = new ArrayList<ANThread>();
		ArrayList<Thread> threads = new ArrayList<Thread>();
		//		ArrayList<Document> docs = new ArrayList<Document>();
		for (File file : listOfFiles) {
			String filename = file.getName();
			if (!filename.contains("output")){
				if (verbose > 1)
					System.out.println("Reading " + filename);
				String[] nameparts = filename.split("\\.");
				if (nameparts.length < 2) {
					if (verbose > 1)	System.out.println(filename);
				}
				else {
					String id = nameparts[0];
					String suffix = nameparts[1];
					if (suffix.equals("a2")||suffix.equals("a") ) {
						if (threaded) {
							ANThread ant = new ANThread(verbose, this, this.corpus.searchDocumentByID(id), this.corpus, file, id, wordTags);
							ants.add(ant);
							Thread th = new Thread(ant, "Import "+filename);
							th.start();
							threads.add(th);
						}
						else {
							Document d = importSingleFile(verbose, id, this.corpus.searchDocumentByID(id), file, wordTags);
							corpus.addDocument(d);
						}
						//
					}
				}
			}
		}
		for (Thread th:threads){
			try {
				th.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			corpus.addDocument(ants.get(threads.indexOf(th)).getValue());
		}
	}


	public Document importSingleFile(Double verbose,  String id, Document d, File file, String[] wordTags) throws IOException {
		if (d != null) {
			throw (new UnexpectedDatasetFormat("Do not how to react to adding an existing document!"));
		} else {
			d = new Document(id);
		}
		File alvisfile = file;
		if (alvisfile.exists()) {
			FileReader alvisreader = new FileReader(alvisfile);
			BufferedReader alvistextreader = new BufferedReader(alvisreader);
			String line;
			Sentence fakesentence = new Sentence("fake", d);

			while ((line = alvistextreader.readLine()) != null) {
				String[] parts = line.split("\\s");
				Pattern p = Pattern.compile("[0-9];[0-9]");
				Matcher m = p.matcher(line);
				if (m.find()){
					parts = line.split("\\s|;");
					//					System.out.println("klaka");
				}
				if (parts.length < 4) {
					//					alvistextreader.close();
					//					throw new UnexpectedDatasetFormat("Problem with line: "+alvisfile.getName()+" : "+line);
					System.out.println("ERROR HERE, Ignoring for debugging reasons: Problem with line: "+alvisfile.getName()+" : "+line);
					continue;
				}
				if (parts[0].length() > 0 && parts[0].charAt(0) == 'T') {
					if (parts[1].equals("Word")){
						ID wordid = new ID(parts[0]);
						String rest = parts[4];
						for (int i = 5; i< parts.length; i++){
							rest = rest+" "+parts[i];
						}
						String[] wordparts = rest.split("\\|");
						if (wordparts.length < 1) {
							alvistextreader.close();
							throw new UnexpectedDatasetFormat("Problem with line: "+alvisfile.getName()+" : "+line);
							//							System.out.println("Problem with line: "+alvisfile.getName()+" : "+line);
						}
						Word word = new Word(wordparts[0], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), wordid);
						if (wordTags != null) {
							for (int i =0; i < wordTags.length ; i++ ) {
								int j = i+1;
								if (j < wordparts.length) {
									Tag tag = new Tag(wordparts[j],wordTags[i]);
									word.addTag(tag);
								}
							}
						}
						else {
							Tag tag = new Tag(wordparts[1],"canonical_form");
							word.addTag(tag);
							Integer i = 2;
							while (i < wordparts.length) {
								String[] layerparts = wordparts[i].split("=");
								if (layerparts.length < 2) {
									if (i == 2 && layerparts.length == 1){
										String layername = "POS";
										String tagname = layerparts[0];
										Tag tag1 = new Tag(tagname.replace("\"", ""), layername);
										word.addTag(tag1);
										i++;
										continue;
									}
									else {
										alvistextreader.close();
										throw new UnexpectedDatasetFormat("Problem with line: "+alvisfile.getName()+" : "+line);
									}
								}
								String layername = layerparts[0];
								if (layerparts[1]==null){
									alvistextreader.close();
									throw new UnexpectedDatasetFormat("Problem with the tags of word " + parts[0]+ " of file "+alvisfile.getAbsolutePath());
								}
								else {
									String tagname = layerparts[1];

									Tag tag1 = new Tag(tagname.replace("\"", ""), layername);
									word.addTag(tag1);
								}
								i++;
							}
						}
						fakesentence.words.add(word);

					}
					else if (parts[1].equals("Sentence")){
						String sentenceID = parts[0];
						Sentence sentence = d.searchSentenceByID(sentenceID);
						if (sentence == null) {
							sentence = new Sentence(sentenceID,d);				
						}
						int start = Integer.parseInt(parts[2]);
						int end = Integer.parseInt(parts[3]);
						sentence.setBoundaries(start, end);
						int len = parts.length;
						String stext = null;
						if ((parts.length == 4 )&&((Integer.parseInt(parts[3])-Integer.parseInt(parts[2])==1)||(Integer.parseInt(parts[2])==0))){
							stext = " ";
						}
						else {
							stext =parts[4];
						}
						for (int i = 5; i< len; i++){
							stext = stext+" "+parts[i];
						}
						sentence.text = stext;
						d.sentences.add(sentence);
					}
					else{ //We assume this is a term
						ID termid = new ID(parts[0]);
						NEtype existingType = schema.getNETypeByName(parts[1]);
						NEtype termtype = (existingType == null) ? new NEtype(parts[1]) : existingType;
						NamedEntity namedEntity = new NamedEntity(termtype, Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), termid, line);
						int i = 4;
						while (isInteger(parts[i])) {
							if (isInteger(parts[i+1	])) {
								namedEntity.addStartEnd( Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
							}
							else {
								i = 4;
								break;
							}
							i +=2;
						}
						String rest = parts[i];
						for (int j = i+1; j< parts.length; j++){
							rest = rest+" "+parts[j];
						}
						String[] wordparts = rest.split("\\|");
						if (wordparts.length < 2) {
							//							alvistextreader.close();
							//							throw new UnexpectedDatasetFormat("Problem with line: "+alvisfile.getName()+" : "+line);
							//							System.out.println("WARNING: No canonical form when parsing line \""+line+"\"");
							Tag tag1 = new Tag(wordparts[0],"surface_form");
							namedEntity.addTag(tag1);
							namedEntity.addTag(tag1);
						}
						else {
							Tag tag = new Tag(wordparts[1],"canonical_form");
							namedEntity.addTag(tag);
							Tag tag1 = new Tag(wordparts[0],"surface_form");
							namedEntity.addTag(tag1);
						}
						fakesentence.namedEntities.add(namedEntity);
					}
				}


				else if (parts[0].length() > 0 && parts[0].charAt(0) == 'R') {

					if (parts[1].endsWith("Dependency")){
						if (parts.length < 6) {
							alvistextreader.close();
							throw new UnexpectedDatasetFormat("Problem with line: "+alvisfile.getName()+" : "+line);
						}
						//					R1      Dependency dependent:W5 sentence:S1 head:W12 Label:comp_by:V-N
						ID synid = new ID(parts[0]);
						String role_arg1 = "";
						String role_arg2 ="";
						String synTypeComplete = "" ;

						for (int i = 2; i < parts.length;i++){
							String str = parts[i];
							if (str.startsWith("sentence")) {
							}
							else if (str.startsWith("dependent")) {
								role_arg2 = str;
							}
							else if (str.startsWith("head")) {
								role_arg1 = str;
							}
							else if (str.startsWith("label")){
								synTypeComplete = str;
							}
							else {
								System.out.println("Warning unknown dependency declaration format in line: "+line);
							}
						}
						//						String sentenceid;
						//						role_arg1 = parts[2];
						//						if (parts[3].startsWith("sent")) {
						//							role_arg2 = parts[4];
						//							synTypeComplete = parts[5];
						////							sentenceid = parts[3];
						//						}
						//						else {
						//							role_arg2 = parts[3];
						//							synTypeComplete = parts[4];
						////							sentenceid = null;
						//						}
						String[] tmp1 = role_arg1.split(":");
						String role1 = tmp1[0]; 
						if (tmp1.length < 2) {
							alvistextreader.close();
							throw new UnexpectedDatasetFormat("Problem with line: "+alvisfile.getName()+" : "+line);
						}
						ID w1id = new ID(tmp1[1]);
						String[] tmp2 = role_arg2.split(":");
						if (tmp2.length < 2) {
							alvistextreader.close();
							throw new UnexpectedDatasetFormat("Problem with line: "+alvisfile.getName()+" : "+line);
						}
						String role2 = tmp2[0]; 
						ID w2id = new ID(tmp2[1]);
						String[] tmp4 = synTypeComplete.split(":");
						if (tmp4.length < 2) {
							alvistextreader.close();
							throw new UnexpectedDatasetFormat("Problem with line: "+alvisfile.getName()+" : "+line);
						}
						String synTypeString = tmp4[1];
						for (int i = 2; i < tmp4.length; i++){
							synTypeString = synTypeString+":"+tmp4[2];
						}
						Word word1 = fakesentence.getWordByID(w1id.id);
						Word word2 = fakesentence.getWordByID(w2id.id);
						if (word1 != null && word2 != null){ //this would happen if we're given weird io.input
							//							if (sentenceid!=null){
							//								String[] tmp3 = sentenceid.split(":");
							//								if (tmp3.length < 2) {
							//									alvistextreader.close();
							//									throw new UnexpectedDatasetFormat("Problem with line: "+alvisfile.getName()+" : "+line);
							//								}
							//								Sentence sent = d.searchSentenceByID(tmp3[1]);
							//								SyntacticRelationArgument arg1 = (sent.getSyntacticRelationArgument(word1) == null) ? new SyntacticRelationArgument(word1) : sent.getSyntacticRelationArgument(word1) ;
							//								SyntacticRelationArgument arg2 = (sent.getSyntacticRelationArgument(word2) == null) ? new SyntacticRelationArgument(word2) : sent.getSyntacticRelationArgument(word2);
							//								SyntacticRelationType synType = new SyntacticRelationType(synTypeString);
							//								SyntacticRelation synrel = new SyntacticRelation(synType, role1,arg1,role2, arg2, synid);
							//								sent.syntacticRelations.add(synrel);
							//							}
							//							else {
							SyntacticRelationArgument arg1 = (fakesentence.getSyntacticRelationArgument(word1) == null) ? new SyntacticRelationArgument(word1) : fakesentence.getSyntacticRelationArgument(word1) ;
							SyntacticRelationArgument arg2 = (fakesentence.getSyntacticRelationArgument(word2) == null) ? new SyntacticRelationArgument(word2) : fakesentence.getSyntacticRelationArgument(word2);
							SyntacticRelationType synType = new SyntacticRelationType(synTypeString);
							SyntacticRelation synrel = new SyntacticRelation(synType, role1,arg1,role2, arg2, synid);
							fakesentence.syntacticRelations.add(synrel);
							//							}
						}
					}
					//R504	Anaphora Anaphor:3941-3944 Ante:T876

					else if (parts[1].startsWith("Anaphora")){

						String[] anaphor=parts[2].split(":");
						String[] startend = anaphor[1].split("\\-");
						Integer start = Integer.parseInt(startend[0]);
						Integer end = Integer.parseInt(startend[1]);
						ArrayList<Word> anaphorwords = fakesentence.searchWordByStartEnd(start, end);
						String antestr = parts[3].split(":")[1];
						String antesubstr = antestr.substring(1);
						Integer termid = Integer.parseInt(antesubstr);
						NamedEntity ante = getTermByID(fakesentence.namedEntities, termid);
						ante.findWords(fakesentence);
						for (Word w:anaphorwords){
							ante.addWords(w);
						}
					}
					else { //we assume this is a Relation 
						String relType = parts[1];
						String role_arg1 = parts[2];
						String role_arg2 = parts[3];
						ID relID = new ID(parts[0]);
						String[] tmp1 = role_arg1.split(":");
						String role1 = tmp1[0];
						ID term1id = new ID(tmp1[1]);
						String[] tmp2 = role_arg2.split(":");
						String role2 = tmp2[0];
						ID term2id = new ID(tmp2[1]);
						//						Sentence sentence1 = d.searchSentenceByTermId(term1id); //TODO: This right now only returns ONE sentence. what about relations that span multiple sentences?
						//						Sentence sentence2 = d.searchSentenceByTermId(term2id);
						if (fakesentence != null) {
							//							if (!sentence1.equals(sentence2)){
							//								alvistextreader.close();
							//								throw new UnexpectedDatasetFormat("Relations spanning sentences detected in "+d.id+" between "+term1id.getMixID()+" ("+sentence1.id+") and "+term2id.getMixID()+" ("+sentence2.id+"). Not yet supported : Aborting");
							NamedEntity argument1 = fakesentence.getTermByID(term1id.id);
							NamedEntity argument2 = fakesentence.getTermByID(term2id.id);
							if  (argument1 == null || argument2 == null){
								alvistextreader.close();
								throw new UnexpectedDatasetFormat("Problem with "+d.id+" while parsing line: "+line);

							}
							RelationType type = schema.getRelationTypeByName(relType);
							if (type != null) {
								String correct_role1 = type.arg1role;
								if (!correct_role1.equalsIgnoreCase(role1)) {
									if (correct_role1.equalsIgnoreCase(role2)) {
										NamedEntity tmp = argument1;
										argument1 = argument2;
										argument2 = tmp;
										role2 = role1;
										role1 = correct_role1;
									}
									else {
										alvistextreader.close();	
										throw (new UnexpectedDatasetFormat("Incompatibility with schema roles for relation type:"+line));
									}

								}
								RelationArgument arg1 = (fakesentence.getRelationArgument(argument1, role1) == null) ? new RelationArgument(argument1, role1) : fakesentence.getRelationArgument(argument1, role1);
								RelationArgument arg2 = (fakesentence.getRelationArgument(argument2, role2) == null) ? new RelationArgument(argument2,role2) : fakesentence.getRelationArgument(argument2, role2);
								if (arg1 != null && arg2!= null) {
									Relation rel = new Relation(type,  arg1,  arg2, relID);
									fakesentence.relations.add(rel);
								}
								//							}
								//							else {
								//								NamedEntity argument1 = sentence1.getTermByID(term1id.id);
								//								NamedEntity argument2 = sentence1.getTermByID(term2id.id);
								//								if  (argument1 == null || argument2 == null){
								//									alvistextreader.close();
								//									throw new UnexpectedDatasetFormat("Problem with "+d.id+" while parsing line: "+line);
								//
								//								}
								//								RelationType type = schema.getRelationTypeByName(relType);
								//								RelationArgument arg1 = (sentence1.getRelationArgument(argument1, role1) == null) ? new RelationArgument(argument1, role1) : sentence1.getRelationArgument(argument1, role1);
								//								RelationArgument arg2 = (sentence1.getRelationArgument(argument2, role2) == null) ? new RelationArgument(argument2,role2) : sentence1.getRelationArgument(argument2, role2);
								//								Relation rel = new Relation(type, role1, arg1, role2, arg2, relID);
								//								sentence1.relations.add(rel);
								//							}
							}
						}
						else {
							//							alvistextreader.close();
							System.out.println("The term "+term2id.getMixID()+" in document "+d.id+" does not appear to belong to a known sentence, yet it is involved in a relation ("+relID.getMixID()
							+"). Ignoring");

						}
					}

				}
				else if (parts[0].length() > 0 && parts[0].charAt(0) == 'E') {
					//we assume this is a Relation 
					String relType = parts[1];
					String role_arg1 = parts[2];
					String role_arg2 = parts[3];
					ID relID = new ID(parts[0]);
					String[] tmp1 = role_arg1.split(":");
					String role1 = tmp1[0];
					ID term1id = new ID(tmp1[1]);
					String[] tmp2 = role_arg2.split(":");
					String role2 = tmp2[0];
					ID term2id = new ID(tmp2[1]);
					//						Sentence sentence1 = d.searchSentenceByTermId(term1id); //TODO: This right now only returns ONE sentence. what about relations that span multiple sentences?
					//						Sentence sentence2 = d.searchSentenceByTermId(term2id);
					if (fakesentence != null) {
						//							if (!sentence1.equals(sentence2)){
						//								alvistextreader.close();
						//								throw new UnexpectedDatasetFormat("Relations spanning sentences detected in "+d.id+" between "+term1id.getMixID()+" ("+sentence1.id+") and "+term2id.getMixID()+" ("+sentence2.id+"). Not yet supported : Aborting");
						NamedEntity argument1 = fakesentence.getTermByID(term1id.id);
						NamedEntity argument2 = fakesentence.getTermByID(term2id.id);
						if  (argument1 == null || argument2 == null){
							alvistextreader.close();
							throw new UnexpectedDatasetFormat("Problem with "+d.id+" while parsing line: "+line);

						}
						RelationType type = schema.getRelationTypeByName(relType);
						if (type != null) {
							String correct_role1 = type.arg1role;
							//						if (!correct_role1.equalsIgnoreCase(role1)) {
							//							if (correct_role1.equalsIgnoreCase(role2)) {
							//								NamedEntity tmp = argument1;
							//								argument1 = argument2;
							//								argument2 = tmp;
							//								role2 = role1;
							//								role1 = correct_role1;
							//							}
							//							else {
							//								alvistextreader.close();	
							//								throw (new UnexpectedDatasetFormat("Incompatibility with schema roles for relation type:"+line));
							//							}
							//
							//						}
							RelationArgument arg1 = (fakesentence.getRelationArgument(argument1, role1) == null) ? new RelationArgument(argument1, role1) : fakesentence.getRelationArgument(argument1, role1);
							RelationArgument arg2 = (fakesentence.getRelationArgument(argument2, role2) == null) ? new RelationArgument(argument2,role2) : fakesentence.getRelationArgument(argument2, role2);
							if (arg1 != null && arg2!= null) {
								Relation rel = new Relation(type,  arg1,  arg2, relID);
								fakesentence.relations.add(rel);
							}
							//							}
							//							else {
							//								NamedEntity argument1 = sentence1.getTermByID(term1id.id);
							//								NamedEntity argument2 = sentence1.getTermByID(term2id.id);
							//								if  (argument1 == null || argument2 == null){
							//									alvistextreader.close();
							//									throw new UnexpectedDatasetFormat("Problem with "+d.id+" while parsing line: "+line);
							//
							//								}
							//								RelationType type = schema.getRelationTypeByName(relType);
							//								RelationArgument arg1 = (sentence1.getRelationArgument(argument1, role1) == null) ? new RelationArgument(argument1, role1) : sentence1.getRelationArgument(argument1, role1);
							//								RelationArgument arg2 = (sentence1.getRelationArgument(argument2, role2) == null) ? new RelationArgument(argument2,role2) : sentence1.getRelationArgument(argument2, role2);
							//								Relation rel = new Relation(type, role1, arg1, role2, arg2, relID);
							//								sentence1.relations.add(rel);
							//							}
						}
					}
					else {
						//							alvistextreader.close();
						System.out.println("The term "+term2id.getMixID()+" in document "+d.id+" does not appear to belong to a known sentence, yet it is involved in a relation ("+relID.getMixID()
						+"). Ignoring");

					}
				}



			}
			for (Sentence sentence:d.sentences){
				for (NamedEntity namedEntity:fakesentence.namedEntities){
					for (int i=0; i<namedEntity.getStart().size(); i++){
						if (isWithin(sentence.start, sentence.end, namedEntity.getStart(i), namedEntity.getEnd(i))){
							if (sentence.getTermByID(namedEntity.tid.id) == null){
								sentence.namedEntities.add(namedEntity);
							}
						}
					}
				}
				for (Word word:fakesentence.words){
					if (isWithin(sentence.start, sentence.end, word.start, word.end)) {
						if (sentence.getWordByID(word.wid.id) == null){
							sentence.words.add(word);
						}
						SyntacticRelationArgument sra = fakesentence.getSyntacticRelationArgument(word); 
						//new SyntacticRelationArgument(word);
						for (SyntacticRelation sr:fakesentence.getSyntacticRelationsHaving(sra)){
							if ((!sr.type.type.equalsIgnoreCase("wordpath"))||(isWithin(sentence.start, sentence.end, sr.arg1.argw.start, sr.arg1.argw.end) && isWithin(sentence.start, sentence.end, sr.arg2.argw.start, sr.arg2.argw.end))){
								if (!sentence.syntacticRelations.contains(sr)){
									sentence.syntacticRelations.add(sr);
								}
								if (!sentence.words.contains(sr.arg1.argw) && word !=sr.arg1.argw){
									sentence.words.add(sr.arg1.argw);
								}
								if (!sentence.words.contains(sr.arg2.argw) && word !=sr.arg2.argw){
									sentence.words.add(sr.arg2.argw);
								}
								ArrayList<NamedEntity> argumentterms = new ArrayList<>();
								if (!sr.arg1role.equals("anaphor")) {
									argumentterms.addAll(fakesentence.getTermsForWord(sr.arg1.argw));
								}
								argumentterms.addAll(fakesentence.getTermsForWord(sr.arg2.argw));
								for (NamedEntity t:argumentterms) {
									if (sr.type.type.equals("Anaphora")) {
										if (!sentence.hasTermsOfType("Bacteria")) {
											if (!sentence.namedEntities.contains(t)) sentence.namedEntities.add(t);
										}
										//									else {
										//										System.out.println("je debug");
										//									}
									}
									else {
										if (!sentence.namedEntities.contains(t)) sentence.namedEntities.add(t);
									}
								}
							}
						}
					}
				}
				for (NamedEntity namedEntity:sentence.namedEntities){
					namedEntity.findWords(sentence);
					sentence.addWords(namedEntity.words); //make sure to add all term's words to sentence
				}
				//				System.out.println("debug");

			}
			for (Relation relation:fakesentence.relations){
				ArrayList<Sentence> sentences = d.searchSentenceByTermId(relation.arg1.argument.tid);
				sentences.addAll(d.searchSentenceByTermId(relation.arg2.argument.tid));
				for (Sentence s:sentences){
					if (!s.relations.contains(relation)) s.relations.add(relation);
				}
			}

			alvistextreader.close();	
		} else {
			throw new UnexpectedDatasetFormat("What the hell? There was no file " + alvisfile.getAbsolutePath()+ "!");
		}
		if (verbose >4) {
			System.out.println("In document "+d.id+" found: ");
			for (Sentence s:d.sentences) {
				PrintStuff.printSentence(s);
				PrintStuff.printRelations(s, "(Input) Relations: ");
			}
		}
		return d;

	}

	public Boolean isWithin(int sentstart, int sentend, int start, int end){
		if ((start >= sentstart)&& (end <= sentend)) return true;
		return false;

	}

	public Word getWordByID(ArrayList<Word> words, Integer id){
		Word word = null;
		for (Word w: words){
			if (w.wid.id.equals(id)){
				word = w;
			}
		}
		return word;
	}

	public NamedEntity getTermByID(ArrayList<NamedEntity> namedEntities, Integer id){
		NamedEntity namedEntity = null;
		for (NamedEntity t: namedEntities){
			if (t.tid.id.equals(id)){
				namedEntity = t;
			}
		}
		return namedEntity;
	}

	private static boolean isInteger(String s) {
		try { 
			Integer.parseInt(s); 
		} catch(NumberFormatException e) { 
			//			System.out.println("Exception"+e.getMessage()+" "+s+" is not a number");
			return false; 
		}
		// only got here if we didn't return false
		return true;
	}





}
