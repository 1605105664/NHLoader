import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Queue;

import javax.imageio.ImageIO;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

public class FileSender {
    private static final String APPLICATION_NAME = "NHLoader";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this app.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    static Drive service;
    public boolean idle; //boolean indicates whether the sender is available
    public Queue<String> todoQue;
    
	public FileSender() throws IOException, GeneralSecurityException
	{ 
		// Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        idle=true;
	}
	
	public String createFolder() throws IOException
	{
		File folder = new File();
        folder.setName("Collection");
        folder.setMimeType("application/vnd.google-apps.folder");
        folder = service.files().create(folder)
            .setFields("id")
            .execute();
        System.out.println("Folder ID: " + folder.getId());
        return folder.getId();
	}
	
	public String createFolder(String parentID) throws IOException
	{
		File folder = new File();
        folder.setName(String.format("%ty%<tm%<td%<tH%<tM", new Date()));
        folder.setMimeType("application/vnd.google-apps.folder");
        folder.setParents(Collections.singletonList(parentID));
        folder = service.files().create(folder)
            .setFields("id")
            .execute();
        System.out.println("Folder ID: " + folder.getId());
        return folder.getId();
	}
	
	public boolean send(Queue<String> consoleQue, String siteID, String FOLDERID, int lim) throws IOException
	{
		idle=false;
		//check connection
		consoleQue.add("start sending "+siteID+"\n");
        final String site="https://i.nhentai.net/galleries/"+siteID;
        HttpURLConnection a=(HttpURLConnection) new URL(site+"/1.jpg").openConnection();
        if(a.getResponseCode()!=200) 
        {
        	consoleQue.add("Connection error. Please try again. ");
        	idle=true;
        	return false;
        }
        consoleQue.add("opened Connection\n");

        //check for limit
        if(lim!=-1 && ((HttpURLConnection) new URL(site+"/"+(lim+1)+".jpg").openConnection()).getResponseCode()==200)
		{
			consoleQue.add("Page number over limit. skipped.\n");
			idle=true;
			return true;
		}
        
        int size=0;
        //If reading too fast, the server would close connection
        while(true)
        {
        	try {
        	ImageIO.write(ImageIO.read(new URL(site+"/"+(size+1)+".jpg")), "jpg", new java.io.File((size+1)+".tmp"));
        	size++;
        	}catch(Exception e)
        	{
        		break;
        	}
        }
        consoleQue.add("size "+size+"\n");
        
        //upload images to drive
        for(int x=1;x<=size;x++)
        {
        	if(x%5==0 || x==size)
        	consoleQue.add("sending: ("+x+"/"+size+")\n");
        	
        	File fileMetadata = new File();
        	fileMetadata.setParents(Arrays.asList(FOLDERID));
        	java.io.File filePath=new java.io.File(x+".tmp");
        	if(!filePath.exists()) continue;
    		fileMetadata.setName(x+".jpg");
    		//ImageIO.write(ImageIO.read(new URL(site+""+x+".jpg")), "jpg", filePath);
    		FileContent mediaContent = new FileContent("image/jpeg", filePath);
    		File file = service.files().create(fileMetadata, mediaContent)
    		.setFields("id")
    		.execute();
    		filePath.delete();
    		System.out.println("File ID: " + file.getId());
        }
        idle=true;
        return true;
	}
	
    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = FileSender.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user"); //bug occurs when user chooses not to authorize the app
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
    	//not main class: for testing purpose only
    	new FileSender();
    }
}