package io.output;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import corpus.Document;
import corpus.NamedEntity;
import corpus.Relation;
import corpus.Sentence;

public class LLL extends OutputModule {
String postfix;
String test;
String filename = "/LLL_predictions.txt";
	public LLL(String dir) {
		super(dir);
	}
	/**
	 * create new LLL io.output instance
	 * @param dir where to put the file
	 * @param filename the name of the file to write
	 */
	public LLL(String dir, String filename){
		super(dir);
		if (filename != null){
			this.filename = "/"+filename;
		}
	}
	
	/**
	 * write LLL io.output
	 * @param documents the list of documents
	 * @param test "WITH", "WITHOUT" or "WITH and WITHOUT"
	 * @throws IOException
	 */
	public void writeFile(ArrayList<Document> documents, String test) throws IOException{
		BufferedWriter writer;
		writer = new BufferedWriter(new FileWriter(outputDir+filename));
		writer.write("% Participant name: Dialekti VALSAMOU\n% Participant institution: INRA\n% Participant email address: dialekti.valsamou@jouy.inra.fr\n");
		writer.write("% Format checked: YES\n% Basic data: NO\n% Coreference distinction: "+test+" COREFERENCE\n");
		for (Document d: documents){
//			boolean printit = false;
			boolean printit = true; // this way it prints even it has not found a relation -> for the evaluation program
			for (Sentence s : d.sentences){
				for (Relation r: s.relations){
					if (r != null){
						printit = true;
					}
				}
			}
			if (printit){
			writer.write("ID\t"+d.id);
			writer.newLine();
			writer.write("agents\t");
			for (Sentence s : d.sentences){
				HashSet<String> agents = new HashSet<>();
				HashSet<String> targets = new HashSet<>();
				for (Relation r: s.relations){
					if (r != null){
						if (r.arg1.role.equalsIgnoreCase("agent")){
							agents.add(r.arg1.argument.getTagValue("canonical_form"));
							targets.add(r.arg2.argument.getTagValue("canonical_form"));
						}
						else {
							agents.add(r.arg2.argument.getTagValue("canonical_form"));
							targets.add(r.arg1.argument.getTagValue("canonical_form"));
						}

					}
				}
				for (String agent: agents){
					writer.write("agent('"+agent+"')\t");
				}
				writer.newLine();
				writer.write("targets\t");
				for (String target: targets){
					writer.write("target('"+target+"')\t");
				}
				writer.newLine();
				writer.write("genic_interactions\t");
				for (Relation r: s.relations){
					if (r!= null){
						if (r.type.type.equalsIgnoreCase("Interaction")){
							NamedEntity agent;
							NamedEntity target;
							if (r.arg1.role.equalsIgnoreCase("agent")){
								agent = r.arg1.argument;
								target = r.arg2.argument;
							}
							else {
								agent = r.arg2.argument;
								target = r.arg1.argument;
							}
							//tmp solution with tags
							String agentname = agent.getTagValue("canonical_form");
							String targetname = target.getTagValue("canonical_form");
							writer.write("genic_interaction('"+agentname+"','"+targetname+"')\t");
//							writer.write("genic_interaction('"+agent.tags.get(1).label+"','"+target.tags.get(1).label+"')\t");
						}
						else {
							System.out.println("Relation type: "+r.type.type);
						}
					}
				}
				writer.newLine();
			}
		}
		}
		writer.flush();
		writer.close();
	}
	public void writeFile(ArrayList<Document> documents) throws IOException{
		writeFile(documents, "WITH and WITHOUT");
	}


}
