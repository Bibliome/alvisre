package tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

public class Evaluation {

	private Evaluation() {	}
	public static Double[] evaluateBI(String filename) throws IOException, InterruptedException{
		String script = Evaluation.class.getResource("giec-eval.pl").getFile();
		String dict = Evaluation.class.getResource("dictionary_data.txt").getFile();
		String key = Evaluation.class.getResource("genic_interaction_data.txt").getFile();
		Process process = Runtime.getRuntime().exec("perl "+script+" -w -d "+dict+" -k "+key+" -p "+filename);
		process.waitFor();
		if(process.exitValue() != 0)
		{
			System.out.println("Command Failure: \"perl "+script+" -w -d "+dict+" -k "+key+" -p "+filename+"\"");
			System.out.println("mpe");
		}
		InputStream eval = process.getInputStream();
		File evalfile = new File(filename+".eval");
		evalfile.createNewFile();
		DataOutputStream evalstream = new DataOutputStream(new FileOutputStream(evalfile));
		String evalstats = convertStreamToString(eval);

		byte[] buffer = new byte[100]; 
		int bytesRead;
		while ((bytesRead = eval.read(buffer)) != -1)
		{
			evalstream.write(buffer, 0, bytesRead);
		}
		evalstream.close();
		//			String evalstats = convertStreamToString(eval);
		evalstats = evalstats.replaceAll("[\n\t]", " ");
		Pattern pfm = Pattern.compile("(?:PRE|REC|FM):(1|[0]\\.[0-9]{0,2})");
		Matcher mfm = pfm.matcher(evalstats);
		ArrayList<String> thematches = new ArrayList<String>();
		while (mfm.find()) {
			thematches.add(mfm.group(1));
		}
		Double pre = 0.0, rec = 0.0, fm = 0.0;
		if (thematches.size()!=0){

			pre = Double.parseDouble(thematches.get(0))*100;
			rec = Double.parseDouble(thematches.get(1))*100;
			fm = Double.parseDouble(thematches.get(2))*100;
		}
		Double[] stats = {pre,rec,fm};
		System.out.println("This fold: Precision "+pre+" Recall "+rec+" F-measure "+fm);


		return stats;
	}
	public static Double[] evaluateBI(String filename, String key, String dictionary, String tool) throws IOException, InterruptedException{
		Process process = Runtime.getRuntime().exec("perl "+tool+" -w -d "+dictionary+" -k "+key+" -p "+filename);
		process.waitFor();
		if(process.exitValue() != 0)
		{
			System.out.println("Command Failure: \"perl "+tool+" -w -d "+dictionary+" -k "+key+" -p "+filename+"\"");
			System.out.println("mpe");
		}
		InputStream eval = process.getInputStream();
		File evalfile = new File(filename+".eval");
		evalfile.createNewFile();
		DataOutputStream evalstream = new DataOutputStream(new FileOutputStream(evalfile));
		String evalstats = convertStreamToString(eval);

		byte[] buffer = new byte[100]; 
		int bytesRead;
		while ((bytesRead = eval.read(buffer)) != -1)
		{
			evalstream.write(buffer, 0, bytesRead);
		}
		evalstream.close();
		//			String evalstats = convertStreamToString(eval);
		evalstats = evalstats.replaceAll("[\n\t]", " ");
		Pattern pfm = Pattern.compile("(?:PRE|REC|FM):(1|[0]\\.[0-9]{0,2})");
		Matcher mfm = pfm.matcher(evalstats);
		ArrayList<String> thematches = new ArrayList<String>();
		while (mfm.find()) {
			thematches.add(mfm.group(1));
		}
		Double pre = 0.0, rec = 0.0, fm = 0.0;
		if (thematches.size()!=0){

			pre = Double.parseDouble(thematches.get(0))*100;
			rec = Double.parseDouble(thematches.get(1))*100;
			fm = Double.parseDouble(thematches.get(2))*100;
		}
		Double[] stats = {pre,rec,fm};
		System.out.println("This fold: Precision "+pre+" Recall "+rec+" F-measure "+fm);


		return stats;

	}
	public static String convertStreamToString(java.io.InputStream is) {
		@SuppressWarnings("resource")
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}


	public static Double[] evaluateBB(String evaluationScript, String inputPath, String txtPath,String predictionPath,String referencePath, Boolean newref) throws IOException, InterruptedException{
		File inputPathFile = new File(inputPath);
		File txtPathFolder = new File(txtPath);
		//		File predictionPathFolder = new File(predictionPath);
		File referencePathFolder  = new File(referencePath);
		if (!referencePathFolder.exists()){
			referencePathFolder.mkdir();
		}
		if (newref){
			File[] inputFiles = inputPathFile.listFiles();
			Pattern depPat = Pattern.compile("Dependency");
			Pattern anaPat = Pattern.compile("Anaphora");
			Pattern wordPat = Pattern.compile("Word");
			Pattern sentPat = Pattern.compile("Sentence");
			for (File afile:inputFiles){
				if (afile.exists()) {
					FileReader areader = new FileReader(afile);
					BufferedReader atextreader = new BufferedReader(areader);
					String line;
					BufferedWriter a2writer = new BufferedWriter(new FileWriter(referencePath+"/"+afile.getName()+"2"));
					BufferedWriter a1writer = new BufferedWriter(new FileWriter(referencePath+"/"+afile.getName()+"1"));
					while ((line = atextreader.readLine()) != null) {
						if (line.length() > 0 && line.charAt(0) == 'R'){
							Matcher depMat = depPat.matcher(line);
							Matcher anaMat = anaPat.matcher(line);
							if (!depMat.find() && !anaMat.find()){
								a2writer.write(line+"\n");
							}
						}
						else if (line.length() > 0 && line.charAt(0) == 'T'){
							Matcher wordMat = wordPat.matcher(line);
							Matcher sentMat = sentPat.matcher(line);
							//							if (Pattern.matches("Word", line)) {
							//								System.out.println("Word");
							//							}
							//							if (Pattern.matches("Sentence",line)){
							//								System.out.println("Sentence");
							//							}
							if (!wordMat.find() && !sentMat.find()){
								String[] notags = line.split("\\|");
								a1writer.write(notags[0]+"\n"	);
							}
						}
					}
					atextreader.close();
					a2writer.close();
					a1writer.close();
				}
			}
			FileFilter txtfilter = new WildcardFileFilter("*txt");
			FileUtils.copyDirectory(txtPathFolder, referencePathFolder, txtfilter);
		}
		FilenameFilter txtfilter = new WildcardFileFilter("*txt");
		
		
		

		//		ArrayList<String> inputfiles = new ArrayList<String>();
		//		for (String filename:referencePathFolder.list(txtfilter)){
		//			inputfiles.add(filename);
		//		}

		String cmd =  "python "+evaluationScript+" --task 2 --a2-dir "+referencePath+" --a1-dir "+referencePath+" --pred-dir "+predictionPath+"  ";
		for (String filename:referencePathFolder.list(txtfilter)){
			String predfilename = predictionPath+"/"+filename.replaceAll("txt","a2");
			File predfile = new File(predfilename);
			predfile.createNewFile();
			cmd = cmd+" "+referencePath+filename;
		}
		Double pre = 0.0, rec = 0.0, fm = 0.0;

		String evalstats="";
		System.out.println("Running: "+cmd);
		try
		{
			Runtime r = Runtime.getRuntime();
			Process p = r.exec(cmd);
			BufferedReader brout = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader brerrr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			p.waitFor();
			String lineout = "";
			Pattern numbers = Pattern.compile("[0-9]");
			while ((lineout=brout.readLine())!=null) {
				Matcher numM = numbers.matcher(lineout);
				if (! numM.find()) {	
					System.out.println(lineout);
				}
				evalstats = evalstats+lineout;
			}
			String linerr = "";
			while ((linerr=brerrr.readLine())!=null) {
				System.out.println(linerr);
			}
		}
		catch (Exception e)
		{
			String cause = e.getMessage();
			if (cause.equals("python: not found"))
				System.out.println("No python interpreter found.");
		}

		Pattern pfm = Pattern.compile("(?:Precision|Recall|F1)\\s*=\\s*(1|[0]\\.?[0-9]{0,5})");
		Matcher mfm = pfm.matcher(evalstats);
		ArrayList<String> thematches = new ArrayList<String>();
		while (mfm.find()) {
			thematches.add(mfm.group(1));
		}
		if (thematches.size()!=0){

			rec = Double.parseDouble(thematches.get(0));
			pre = Double.parseDouble(thematches.get(1));
			fm = Double.parseDouble(thematches.get(2));
		}
		Double[] stats = {pre,rec,fm};
//		System.out.println("Precision "+pre+" Recall "+rec+" F-measure "+fm);

		return stats;
		//		Recall = 0.000000
		//		Precision = 0.000000
		//		F1= 0
	}
	public static Double[] evaluateBB(String evaluationScript, String inputPath, String txtPath, String predictionPath,String referencePath) throws IOException, InterruptedException{
		return evaluateBB(evaluationScript,inputPath,txtPath,predictionPath,referencePath, true);
	}
//	public static Double[] evaluateBB(String evaluationScript, String inputPath, String predictionPath,String referencePath) throws IOException, InterruptedException{
//		return evaluateBB(evaluationScript,inputPath,referencePath,predictionPath,referencePath, true);
//	}


}
