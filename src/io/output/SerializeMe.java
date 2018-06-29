package io.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import org.apache.commons.io.FileUtils;

import exceptions.UnexpectedDatasetFormat;
import representation.Candidate;
import representation.Path;

public class SerializeMe {
	public String outputDir;
	
	public SerializeMe(String dir) {
		if (dir.contains("objects")) {
			outputDir = dir;
		}
		else {
			outputDir = dir+"/objects/";
		}
		File checkfile = new File(outputDir);
		if (!checkfile.exists()){
			checkfile.mkdirs();
		}
	}
	
	public void writePath(Path path) throws IOException{
		FileOutputStream fstr = new FileOutputStream(outputDir+"/"+"path"+path.index);
		ObjectOutputStream ostr = new ObjectOutputStream(fstr);
		ostr.writeObject(path);
		ostr.close();
		fstr.close();
	}
	
	public Path readPath(String filename) throws IOException, ClassNotFoundException{
		FileInputStream fstr = new FileInputStream(new File(outputDir+"/"+filename));
		ObjectInputStream ostr = new ObjectInputStream(fstr);
		Path path = (Path) ostr.readObject();
		ostr.close();
		fstr.close();
		return path;
		
	}
	public void writeCandidate(Candidate candidate ) throws IOException{
		FileOutputStream fstr = new FileOutputStream(outputDir+"/"+"candidate"+candidate.index);
		ObjectOutputStream ostr = new ObjectOutputStream(fstr);
		ostr.writeObject(candidate);
		ostr.close();
		fstr.close();
	}
	
	public Candidate readCandidate(String filename) throws IOException, ClassNotFoundException{
		FileInputStream fstr = new FileInputStream(new File(outputDir+"/"+filename));
		ObjectInputStream ostr = new ObjectInputStream(fstr);
		Candidate candidate = (Candidate) ostr.readObject();
		ostr.close();
		fstr.close();
		return candidate;
		
	}
	
	public void writePaths(ArrayList<Path> paths) throws IOException{
		for (Path path:paths){
			writePath(path);
		}
	}
	
	public ArrayList<Path> readPaths() throws ClassNotFoundException, IOException{
		ArrayList<Path> paths = new ArrayList<Path>();
		File folder = new File(outputDir);
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
			if (filename.contains("path")){
				paths.add(readPath(filename));
			}
		}
		return paths;
	}
	
	public ArrayList<Candidate> readCandidates()  throws ClassNotFoundException, IOException{
		ArrayList<Candidate> candidates = new ArrayList<Candidate>();
		File folder = new File(outputDir);
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
			if (filename.contains("path")){
				candidates.add(readCandidate(filename));
			}
		}
		return candidates;
	}

	public void writeCandidates(ArrayList<Candidate> candidates, Boolean delete) throws IOException {
		File checkfile = new File(outputDir);
		if(checkfile.isDirectory()){
			if((checkfile.list().length>0) && delete){
				System.out.println(new Date().toString()+" Deleting previous serialized objects!");
				FileUtils.cleanDirectory(checkfile);
				System.out.println(new Date().toString()+" Deleted");
			}
		}
		for (Candidate c:candidates){
			if ( c instanceof Path ){
				writePath((Path) c);
			}
			else writeCandidate(c);
		}
	}
	
	public void writeCandidates(ArrayList<Candidate> candidates) throws IOException {
		writeCandidates(candidates, true);
		
	}

}
