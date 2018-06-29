package tools;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class PostToLLL {
	
	public static String postToLLLContest(File in, String filename){
        HttpClient httpclient = new DefaultHttpClient();
        String resultat = "";
        try {
            HttpPost httppost = new HttpPost("http://genome.jouy.inra.fr/texte/LLLchallenge/upload.cgi");
            FileBody bin = new FileBody(in);
            
            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("predictions-file", bin);
            reqEntity.addPart("filename", new StringBody(filename));

            httppost.setEntity(reqEntity);

            System.out.println("executing request " + httppost.getRequestLine());
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity resEntity = response.getEntity();

            System.out.println("----------------------------------------");
            System.out.println(response.getStatusLine());
            if (resEntity != null) {
                System.out.println("Response content length: " + resEntity.getContentLength());
            }
            resultat = new String( EntityUtils.toByteArray(resEntity) );
        }catch (IOException e) {
        	System.out.println("Erreur durant la connection au serveur : "+ e.getMessage());
		}
        finally {
            try { httpclient.getConnectionManager().shutdown(); } catch (Exception ignore) {System.out.println(ignore.getMessage()+" Http client shutdown");}
        }
        return resultat;
	}
	
	public static void main(String[] args) throws Exception {
		File in = new java.io.File("file/LLL_predictions.txt");
		String filename = "LLL_predictions.txt";
		String res = postToLLLContest(in, filename);
		System.out.println(res);
	}
}
