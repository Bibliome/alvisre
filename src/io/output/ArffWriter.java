package io.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import weka.core.Instances;

public class ArffWriter extends OutputModule {

	public ArffWriter(String dir) {
		super(dir);
	}

	public void writeArff(Instances set, String name) throws IOException{
		File checkfile = new File(outputDir);
		if (!checkfile.exists()){
			checkfile.mkdirs();
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir+"/"+name+".arff"));
		writer.write(set.toString());
		writer.newLine();
		writer.flush();
		writer.close();
	}
	
	
}