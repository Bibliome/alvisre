package io.output;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import corpus.Relation;
import corpus.Sentence;

public class BIo extends OutputModule {

	public BIo(String dir) {
		super(dir);
	}

	public void writeFiles(ArrayList<Sentence> sentences) throws IOException{
		for (Sentence s : sentences){
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir+"/"+s.id+".a2"));
			for (Relation r: s.relations){
				if (r!= null){
				writer.write("E"+r.eid.id+"\t"+r.type.type+" "+r.type.arg1role+":"+r.arg1.argument.tid.getMixID()+" "+r.type.arg2role+":"+r.arg2.argument.tid.getMixID());
				writer.newLine();
				}
			}
			writer.flush();
			writer.close();
		}
	}
	
	
}
