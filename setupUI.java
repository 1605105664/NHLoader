import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class setupUI {
        
	static int step=1;
	public setupUI() throws IOException
	{
		//set default user properties
		Properties user=new Properties();
		user.setProperty("limit", -1+"");
		user.setProperty("closeAfterFinish", "false");
		user.setProperty("loadtoSubFolder","false");
		
		JFrame fr= new JFrame("Setup Guide");
        fr.setSize(680,420);
        fr.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel steplbl=new JLabel("Setting up: Step "+step+" of 3");
        steplbl.setFont(new Font("Serif",Font.BOLD,25));
        fr.add(steplbl,BorderLayout.PAGE_START);
        
        JTextArea infoField=new JTextArea("");
        infoField.setPreferredSize(new Dimension(620,260));
        infoField.setFont(new Font("Consolas",0,14));
        infoField.setEditable(false);
        infoField.setLineWrap(true);
        infoField.setWrapStyleWord(true);
        infoField.setBorder(BorderFactory.createLineBorder(Color.black));
        BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("info.txt")));
        String line = "";
        while((line=in.readLine()) != null){
          infoField.append(line + "\n");
        }
        fr.add(infoField,BorderLayout.CENTER);
        
        JPanel pane= new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        JLabel title=new JLabel("Enter your favorite tags, seperated by space: (Ex: english color touhou project)");
        JLabel title2=new JLabel("Name your index file to: (Ex: index.txt)");
        title.setFont(new Font("Serif",0,19)); title2.setFont(new Font("Serif",0,20));
        title.setVisible(false); title2.setVisible(false);
        
        JTextField input=new JTextField();
        JTextField input2=new JTextField();
        input.setPreferredSize(new Dimension(620,50)); input2.setPreferredSize(new Dimension(620,50));
        input.setMaximumSize(new Dimension(Short.MAX_VALUE,50)); input2.setMaximumSize(new Dimension(Short.MAX_VALUE,50));
        input.setFont(new Font("Serif",0,20)); input2.setFont(new Font("Serif",0,20));
        input.setVisible(false); input2.setVisible(false);
        pane.add(Box.createRigidArea(new Dimension(0,20)));
        pane.add(title); 
        pane.add(input); 
        pane.add(Box.createRigidArea(new Dimension(0,20)));
        pane.add(title2);
        pane.add(input2);
        pane.add(Box.createVerticalGlue());
        
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(620,30));
        progressBar.setMaximumSize(new Dimension(Short.MAX_VALUE,30));
        pane.add(progressBar);
        
        JButton contbtn=new JButton("Continue");
        contbtn.setFont(new Font("Serif",Font.BOLD,20));
        contbtn.setPreferredSize(new Dimension(150,40));
        contbtn.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e)
        	{
        		switch(step)
        		{
        			case 1:
        				infoField.setVisible(false);
        				title.setVisible(true); title2.setVisible(true);
        				input.setVisible(true); input2.setVisible(true);
        				step++;
        				steplbl.setText("Setting up: Step "+step+" of 3");
        				fr.add(pane);
        		        fr.revalidate();
        				break;
        			case 2:
        				if(input.getText().equals("") || input2.getText().equals(""))
        				{
            				JOptionPane.showMessageDialog(fr, "Input cannot be empty! Try again");
            				break;
        				}
        				contbtn.setText("Loading...");
        				contbtn.setEnabled(false);
        				new Thread(){public void run(){
        					progressBar.setVisible(true);
        					user.setProperty(input.getText().replaceAll(" ", "+"), "1");
        					if(setup(progressBar,input.getText(),input2.getText()))
        					{
        						user.setProperty("indexDirectory", "indexes/"+input2.getText());
        						contbtn.setText("Authorize");
            					title.setVisible(false); title2.setVisible(false);
            					input.setVisible(false); input2.setVisible(false);
            					
            					infoField.setText("");
            					BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("AuthorizationInfo.txt")));
            					String line = "";
            			        try {
									while((line=br.readLine()) != null){
									  infoField.append(line + "\n");
									}
								} catch (IOException ignore) {}
            					
            					step++;
            					steplbl.setText("Setting up: Step "+step+" of 3");
            					infoField.setVisible(true);
            					progressBar.setVisible(false);
        					}
            				else
            				{
            					JOptionPane.showMessageDialog(fr, "Encountered problem while loading indexes.");
            					contbtn.setText("Continue");
            				}
        					contbtn.setEnabled(true);
        				}}.start();
        				break;
        			case 3:
        				try {
        				contbtn.setText("Authorizing...");
        				contbtn.setEnabled(false);
						user.setProperty("folderID",authorize());
						user.store(new FileOutputStream("userdata.txt"), null);
        				} catch (Exception ex) {
        					JOptionPane.showMessageDialog(fr, "Encountered problem while authorizing.");
        					contbtn.setEnabled(true);
        					contbtn.setText("Authorize");
        					break;
        				}
        				JOptionPane.showMessageDialog(fr, "Setup Complete! Go ahead and enjoy!");
        				fr.setVisible(false);
        				fr.dispose();
        				try{new mainUI();}catch(Exception ex){}
        				break;
        		}
        	}});
        JPanel pane2= new JPanel();
        pane2.add(contbtn);
        pane2.setBorder(BorderFactory.createEmptyBorder(10, 460, 0, 0));
        fr.add(pane2,BorderLayout.PAGE_END);
        
        fr.setResizable(false);
        fr.setLocationRelativeTo(null);
        fr.setVisible(true);
        fr.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	
	//setup without UI
	public setupUI(String favorites, String toLocation)
	{
		setup(new JProgressBar(),favorites,toLocation);
	}
	
	//setup index file according to the favorite tags provided
	public boolean setup(JProgressBar p, String favorites, String toLocation)
	{
    	String site="https://nhentai.net/search/?q="; 
    	String tags=favorites.replaceAll(" ", "+");
    	site+=tags;
    	System.out.println (site);
    	try {
    	//find how many pages for the certain keyword
    	HttpURLConnection a=(HttpURLConnection) new URL(site).openConnection();
    	a.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
    	if(a.getResponseCode()!=200) 
		{
			System.out.println(a.getResponseCode());
			return false;
		}
    	BufferedReader in= new BufferedReader(new InputStreamReader(a.getInputStream()));
    	String line="";
    	int page=0;
    	while((line=in.readLine())!=null)
    	{
    		line=line.replaceAll(",","");
    	    Pattern results= Pattern.compile("<h2>\\d* Results</h2>");
    	    Matcher m1= results.matcher(line);
    	    if(m1.find())
    	    {
    	    	System.out.println(page);
    	    	page=(int)Math.ceil(Double.parseDouble(line.substring(m1.start()+4,m1.end()-13))/25);
    	    	//System.out.println(line.substring(m1.start()+4,m1.end()-13));
    	    }
    	}
    	in.close();
    	System.out.println(page);
    	if(page==0) return false;
    	if(page>100) page=100; //prevent being stucked for too long time, load 100 pages only
    	
    	//creating the index folder
    	new File("indexes").mkdir();
    	PrintWriter pr = new PrintWriter(new File("indexes/"+toLocation));
    	pr.println("TAG="+tags);
    	String site1=site;
    	ArrayList<String> indexArr=new ArrayList<String>();
    	for(int x=1;x<=page;x++)
    	{
    		System.out.println ("page "+x);
    		p.setValue((int)Math.ceil(x*100.0/page));
    		if(x!=1) site=site1+"&page="+x;
	    	
    		a=(HttpURLConnection) new URL(site).openConnection();
    		a.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
    		in= new BufferedReader(new InputStreamReader(a.getInputStream()));
        	while((line=in.readLine())!=null)
        	{
        		Pattern word= Pattern.compile("//t.nhentai.net/galleries/\\d*/thumb.jpg");//only load .jpg files for now, .png files skipped
        	    Matcher m= word.matcher(line);
       		    if(m.find())
        	   	{
       		    	indexArr.add(line.substring(m.start()+26,line.indexOf("\"",m.start()+31)-10));
        	   		System.out.println (line.substring(m.start()+26,line.indexOf("\"",m.start()+31)-10));
        	   	}
        	}
        	in.close();
        	Thread.sleep(1000);
    	}
    	pr.println(indexArr.size());
    	for(int x=0;x<indexArr.size();x++)
    		pr.println(indexArr.get(x));
   		pr.close();
    	}catch (Exception ex) {
    		JOptionPane.showMessageDialog(null, "Encountered problem while loading indexes.");
    		ex.printStackTrace();
    		return false;}
    	return true;
	}
	
	//Authorize google credentials and create folder
	public String authorize() throws IOException, GeneralSecurityException
	{
		return new FileSender().createFolder();
	}
	
    public static void main(String[] args) throws IOException{
    	//not main class: for testing purpose only
        new setupUI();
    }
}
