package io.output;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.*;


import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import corpus.Document;
import corpus.NamedEntity;
import corpus.Relation;
import corpus.Sentence;

public class AlvisNLPo extends OutputModule {
	public String tgz = outputDir+"/pred.tgz";
	public AlvisNLPo(String dir) {
		super(dir);
	}
	public void writeFiles(ArrayList<Document> documents, boolean WriteEntities, String prefix) throws IOException{
		for (Document d : documents){
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputDir+"/"+prefix+d.id+".a2"));
			if (WriteEntities){
				for (NamedEntity t: d.getTerms()){
					writer.write(t.forOutput);
					writer.newLine();
				}
			}

			for (Sentence s: d.sentences){
				for (Relation r: s.relations){
					if (r!= null){
						writer.write("R"+r.eid.id+"\t"+r.type.type+" "+r.type.arg1role+":"+r.arg1.argument.tid.getMixID()+" "+r.type.arg2role+":"+r.arg2.argument.tid.getMixID());
						writer.newLine();
						System.out.println("wrote relation "+r.eid.id+" of document "+d.id);
					}
				}
			}
			writer.flush();
			writer.close();
			System.out.println("Wrote "+d.id+" to "+outputDir+"/"+prefix+d.id+".a2");
		}
	}
	public void a2TarGZip() throws IOException{
		final int BUFFER = 2048;
		FileOutputStream fOut = new FileOutputStream(tgz);
		BufferedOutputStream bOut = new BufferedOutputStream(fOut);
		GzipCompressorOutputStream gzipOut =  new GzipCompressorOutputStream(bOut);
		TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut);
		File folder = new File(outputDir);
		File[] listOfFiles = folder.listFiles();
		Arrays.sort(listOfFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		for (File file:listOfFiles){
			String filename = file.getName();
			if (filename.endsWith("a2")) {
				TarArchiveEntry entry = new TarArchiveEntry(file, folder.toURI().relativize(file.toURI()).getPath());


				tarOut.putArchiveEntry(entry);
				FileInputStream fi = new FileInputStream(file);
				BufferedInputStream sourceStream = new BufferedInputStream(fi, BUFFER);

				int count;
				byte data[] = new byte[BUFFER];
				while ((count = sourceStream.read(data, 0, BUFFER)) != -1) {
					tarOut.write(data, 0, count);
				}
				sourceStream.close();
				fi.close();
				tarOut.closeArchiveEntry();
			}
		}
		tarOut.close();
	}
	public void a2Zip() throws IOException {
		final int BUFFER = 2048;
		FileOutputStream dest = new  FileOutputStream(outputDir+"/prediction.zip");
		ZipOutputStream zipOut = new   ZipOutputStream(new BufferedOutputStream(dest));
		File folder = new File(outputDir);
		File[] listOfFiles = folder.listFiles();
		Arrays.sort(listOfFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		for (File file:listOfFiles){
			String filename = file.getName();
			if (filename.endsWith("a2")) {
				ZipEntry entry = new ZipEntry( folder.toURI().relativize(file.toURI()).getPath());
				zipOut.putNextEntry(entry);
				FileInputStream fi = new FileInputStream(file);
				BufferedInputStream sourceStream = new BufferedInputStream(fi, BUFFER);
				int count;
				byte data[] = new byte[BUFFER];
				while ((count = sourceStream.read(data, 0, BUFFER)) != -1) {
					zipOut.write(data, 0, count);
				}
				sourceStream.close();
				fi.close();
				zipOut.closeEntry();

			}
		}
		zipOut.close();

	}

}