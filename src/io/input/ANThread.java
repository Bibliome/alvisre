package io.input;

import java.io.File;
import java.io.IOException;

import corpus.Corpus;
import corpus.Document;

public class ANThread implements Runnable{
	AlvisNLP io= null;
	Document d = null;
	Corpus corpus = null;
	String id = null;
	File file = null;
	Document doc= null;
	Double verbose = 0.0;
	String[] wordTags = null;
	
	public ANThread(Double verbose, AlvisNLP io, Document doc, Corpus corpus, File file, String id, String[] wordTags ) {
		this.io = io;
		this.doc = doc;
		this.corpus = corpus;
		this.file = file;
		this.id = id;
		this.verbose = verbose;
		this.wordTags = wordTags;
	}

	@Override
	public void run() {
		try {
			d= io.importSingleFile(verbose,  id, doc, file, wordTags);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public Document getValue(){
		return d;
	}
}
