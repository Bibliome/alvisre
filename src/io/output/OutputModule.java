package io.output;

import java.io.File;

public abstract class OutputModule {
	public String outputDir;
	public OutputModule(String dir) {
		outputDir = dir;
		File checkfile = new File(dir);
		if (!checkfile.exists()){
			checkfile.mkdirs();
		}
	}

}
