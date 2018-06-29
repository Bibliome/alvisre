package io.input;
import java.io.IOException;

import corpus.Corpus;
import corpus.Schema;

public abstract class InputModule {
	public Schema schema;
	public String type;
	public Corpus corpus;
	public String inputDir;

	
	public InputModule (String path){
		schema = new Schema();
		inputDir = path;
	}
	
	public InputModule(InputModule input){
		schema = input.schema;
		inputDir = input.inputDir;
	}
	public Corpus getCorpus (){
		return corpus;
	}

	public abstract void importFiles(Double verbose, Boolean threaded, String[] WordTags) throws NumberFormatException, IOException;
	public abstract void setSchema(Schema sc);
	


}

