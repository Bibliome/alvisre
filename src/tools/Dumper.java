package tools;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;


public class Dumper {
	 BufferedWriter writer = null;
	File file = null;


	public Dumper(String name, File xmlfile) throws IOException {
		if (!name.startsWith("/run")) file = new File(name);
		else file = new File(System.getProperty("user.dir"),  name);
		if (!file.getParentFile().exists()){
			file.getParentFile().mkdirs();
		}
		writer = new BufferedWriter(new FileWriter(file));
		FileReader xmlin = new FileReader (xmlfile);
		BufferedReader xmlreader = new BufferedReader(xmlin);
		String line;
		writer.write("XML CONFIGURATION:__________________________________\n");
		while ( (line = xmlreader.readLine()) != null){
			writer.write(line+"\n");
		}
		xmlreader.close();
		writer.write("_______________________________\nEND XML CONFIGURATION_______________________________\nPROGRAM OUTPUT___________________________\n");
	}
	
	public void reopen () throws IOException{
		writer.close();
		writer = new BufferedWriter(new FileWriter(file,true));
	}
	
	public  void print(String text) throws IOException{
		System.out.print(text);
		writer.write(text);
	}
	
	public  void println(String text) throws IOException{
		System.out.println(text);
		writer.write(text+"\n");
	}
	public  void println() throws IOException{
		System.out.println();
		writer.write("\n");
	}
	
	public PrintStream getPrintStream() throws FileNotFoundException{
		return new PrintStream(new FileOutputStream(file.getAbsoluteFile(), true));
	}
	
	public void close(String output) throws IOException{
		writer.write("_______________________________\nEND PROGRAM OUTPUT_______________________________\nWrote files to: "+output+"\n");
		writer.flush();
		writer.close();
		System.out.println("Ended. Wrote dump to: "+file.getPath());
	}

	public void close() throws IOException {
		writer.flush();
		writer.close();
	}
	public File getFile(){
		return file;
	}

	public void flush() throws IOException {
		writer.flush();		
	}

	public void format(String string, Object ... values) throws IOException {
		System.out.format(string,values);
		writer.write(String.format(string, values));
	}
	
}
