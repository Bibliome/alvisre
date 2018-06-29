package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class PostToBB {
	static String[] addresses = {"dialectiv@gmail.com","dvalsamou@gmail.com","zorana.ratkovic@gmail.com","dialekti.valsamou@jouy.inra.fr","dialekti.valsamou@limsi.fr","dialekti.valsamou@u-psud.fr","dialectivalsamou@gmail.com","alexdialekti@gmail.com"};

	public static String postToBBContest(File in, String filename, String address, int task) throws InterruptedException, IOException {
		if (address == null) {
		int idx = new Random().nextInt(addresses.length);
		address = addresses[idx];
		}
        HttpClient httpclient = new DefaultHttpClient();
        String resultat = "";
        try {
            HttpPost httppost = new HttpPost("http://genome.jouy.inra.fr/~rbossy/cgi-bin/bionlp-eval/BB_fix.cgi");
            FileBody bin = new FileBody(in);

            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("uploadfile", bin);
            reqEntity.addPart("filename", new StringBody(filename));
            reqEntity.addPart("email", new StringBody(address));
            reqEntity.addPart("task", new StringBody(""+task));
            reqEntity.addPart("submit", new StringBody("Send!"));
            
            
            httppost.setEntity(reqEntity);

//            List<NameValuePair> formparams = new ArrayList<NameValuePair>();
//            formparams.add(new BasicNameValuePair("email", randomAddress));
//            formparams.add(new BasicNameValuePair("task", "2"));
//            formparams.add(new BasicNameValuePair("submit", "Send!"));
//            
//            
//
//
//            httppost.setEntity(new UrlEncodedFormEntity(formparams));

//            System.out.println("executing request " + httppost.getRequestLine());
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();

//            System.out.println("----------------------------------------");
//            System.out.println(response.getStatusLine());
            if (resEntity != null) {
//                System.out.println("Response content length: " + resEntity.getContentLength());
            }
            resultat = new String( EntityUtils.toByteArray(resEntity) );
        }catch (IOException e) {
            System.out.println("Erreur durant la connection au serveur : "+ e.getMessage());
        }
        finally {
            try { httpclient.getConnectionManager().shutdown(); } catch (Exception ignore) {System.out.println(ignore.getMessage()+" Http client shutdown");}
        }
        Pattern p = Pattern.compile("Success");
        Matcher m = p.matcher(resultat);
        if (m.find()) {
        	return printPRF(resultat);
        }
        else {
        	Pattern pat = Pattern.compile("submission allowed in ([0-9]+) minutes");
        	Matcher mat = pat.matcher(resultat);
        	if (mat.find()){
        		String minutes = mat.group(1);
        		int mins = 5;
        		if (minutes != null) {
        		mins = Integer.parseInt(minutes);
        		}
        		if (mins < 5){
        			System.out.println("Forced to wait for "+minutes+" minutes");
            		Thread.sleep(60*1000*mins);
            		return printPRF(postToBBContest(in, filename, address, task));
        		}
        		else {
        			System.out.println("Retrying with different email address");
            		return printPRF(postToBBContest(in, filename, task));
        		}
        	}
    		return new String("Problem with the io.output, here's the web response  \n"+resultat);
        }
    }
	
	public static String postToBBContest(File file, String filename, int task) throws InterruptedException, IOException{
		return postToBBContest(file, filename, null, task);
	}
    public static void main(String[] args) throws Exception {
    	String filename = args[0];
    	int task = Integer.parseInt(args[1]);
        File in = new java.io.File(filename);
        String res = postToBBContest(in, filename, null, task);
        System.out.println(res);
    }
    
    private static String printPRF(String response) throws IOException{
    	String result="";
    	BufferedReader bufReader = new BufferedReader(new StringReader(response));
    	String line=null;
    	while( (line=bufReader.readLine()) != null ){
    		if (line.contains("Recall")||line.contains("Precision")||line.contains("F1")){
    			result += line+"\n";
    		}
    	}
    	return result;
    }

}