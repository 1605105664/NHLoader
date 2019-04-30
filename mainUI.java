import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.UIManager;


public class mainUI {
	
	final int WIDTH=Math.max((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth(), 1920);
	final int HEIGHT=Math.max((int) Toolkit.getDefaultToolkit().getScreenSize().getHeight(), 1080);
	//final int WIDTH=3000;
	//final int HEIGHT=2000;
	final int UNIT_SPACE=Math.max(HEIGHT*5/1080,5);
	Queue<String> consoleQue;
	FileSender sender;
	String folderID;
	int progress;
	int lim;
    boolean close;
    boolean loadtoSubFolder;
    String indexDirectory;
    
    public mainUI() throws IOException, GeneralSecurityException
    {
    	//check if setup is needed
    	if(!new File("userdata.txt").exists() || !new File("indexes").exists()|| !new File("tokens").exists())
    	{
    		System.out.println("not found! Need to setup");
            new setupUI();
            return;
    	}
    	//initialize console display
    	consoleQue=new LinkedList<String>();
    	
    	//load user data
    	Properties user = new Properties();
    	user.load(new FileInputStream("userdata.txt"));
    	folderID=user.getProperty("folderID");
    	indexDirectory=user.getProperty("indexDirectory");
    	lim=Integer.parseInt(user.getProperty("limit"));
    	close=Boolean.parseBoolean(user.getProperty("closeAfterFinish"));
    	loadtoSubFolder=Boolean.parseBoolean(user.getProperty("loadtoSubFolder"));
    	
    	//log into google drive
    	sender= new FileSender();
    	
    	//read index file
    	BufferedReader br= new BufferedReader(new FileReader(indexDirectory));
    	String tag=br.readLine().split("=")[1];
    	int indexSize=Integer.parseInt(br.readLine());
    	progress=Integer.parseInt(user.getProperty(tag));
        for(int x=0;x<progress-1;x++)
        	br.readLine();
        
        //Display UI
        JFrame fr= new JFrame("NHLoader");
        fr.setLayout(null);
        fr.getContentPane().setLayout(new BoxLayout(fr.getContentPane(), BoxLayout.X_AXIS));//left-right organization
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JPanel leftPane = new JPanel();
        leftPane.setLayout(new BoxLayout(leftPane, BoxLayout.Y_AXIS));
        leftPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 0));
        leftPane.setPreferredSize(new Dimension(WIDTH/4,HEIGHT/2));
        
        JPanel rightPane=new JPanel();
        rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.Y_AXIS));
        rightPane.setPreferredSize(new Dimension(WIDTH/4,HEIGHT/2));
        
        JPanel[] pane= new JPanel[6];
        for(int x=0;x<pane.length;x++)
        {
        	pane[x]=new JPanel();
        	pane[x].setAlignmentX(Component.LEFT_ALIGNMENT);
            pane[x].setLayout(new BoxLayout(pane[x],BoxLayout.X_AXIS));
        }
        
        JLabel indexlbl=new JLabel(" Current Index: "+new File(indexDirectory).getName());
    	indexlbl.setFont(new Font("Serif",Font.BOLD,WIDTH/96));
    	pane[0].add(indexlbl);
        
    	pane[0].add(Box.createHorizontalGlue());
    	
        JFileChooser jf= new JFileChooser(new File(indexDirectory));
        JButton change =new JButton("Change");
        change.setPreferredSize(new Dimension(95,30));
        change.setMaximumSize(new Dimension(95,30));
        change.addActionListener(new ActionListener(){
    	public void actionPerformed(ActionEvent e)
    	{
    		JOptionPane.showMessageDialog(fr,"The program will need to restart after changing index file.","Warning",JOptionPane.WARNING_MESSAGE);
    		if (jf.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) 
        	{ 
        		System.out.println(jf.getSelectedFile().getAbsolutePath());
        		user.setProperty("indexDirectory", jf.getSelectedFile().getAbsolutePath());
    			try {
					user.store(new FileOutputStream("userdata.txt"), null);
					fr.dispose();
					new mainUI();
				} catch (Exception e1){
					log(e1);
				} 
        	}
    	}});
        pane[0].add(change);
        pane[0].add(Box.createRigidArea(new Dimension(UNIT_SPACE*3,0)));
        
        JButton addbtn =new JButton("Add");
        addbtn.setPreferredSize(new Dimension(95,30));
        addbtn.setMaximumSize(new Dimension(95,30));
        addbtn.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
    	{
        	String favorites=JOptionPane.showInputDialog(fr,"Enter your favorite tags, seperated by space: (Ex: english color touhou project)","",-1);
        	if(favorites==null) return;
        	String toLocation=JOptionPane.showInputDialog(fr,"Name your index file to: (Ex: index.txt)","",-1);
        	if(toLocation==null) return;
        	if(favorites.equals("") || toLocation.equals(""))
			{
				JOptionPane.showMessageDialog(fr, "Input cannot be empty! Try again");
				return;
			}
        	new Thread() {
        		public void run() {
        		consoleQue.add("Loading new indexes: DO NOT close the program!\n");
        		new setupUI(favorites,toLocation);
        		consoleQue.add("Loading complete!\n");
        	}
        	}.start();
        	user.setProperty(favorites.replaceAll(" ", "+"), "1");
        }});
        pane[0].add(addbtn);
        pane[0].add(Box.createRigidArea(new Dimension(UNIT_SPACE*7,0)));
        
        leftPane.add(pane[0]);
        leftPane.add(Box.createRigidArea(new Dimension(0,UNIT_SPACE*3)));
        
        JLabel progresslbl=new JLabel(" Current Progress: "+progress);
    	progresslbl.setFont(new Font("Serif",Font.BOLD,WIDTH/96));
    	pane[1].add(progresslbl);
    	pane[1].add(Box.createHorizontalGlue());
        
        JButton reset =new JButton("Reset email");
        reset.setPreferredSize(new Dimension(125,30));
        reset.setMaximumSize(new Dimension(125,30));
        reset.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e)
    	{
        	if(JOptionPane.showConfirmDialog(fr,
        			"Resetting will delete your previous token (your dirve files will not be affected), \nand you will be prompted to authorize new email. Continue?",
        			"Warning",2)==JOptionPane.YES_OPTION)
        	{
        		(new File("tokens/StoredCredential")).delete();
        		(new File("tokens")).delete();
        		try {
					sender=new FileSender(); //authorize again
					user.setProperty("folderID", sender.createFolder()); //create and reset the parent folder path
				} catch (Exception e1) {
					log(e1);
				}
        	}
    	}});
        pane[1].add(reset);
        pane[1].add(Box.createRigidArea(new Dimension(UNIT_SPACE*7,0)));
        
        leftPane.add(pane[1]);
        leftPane.add(Box.createRigidArea(new Dimension(0,UNIT_SPACE*2)));
    	
    	JCheckBox specific=new JCheckBox(" Or load from specific ID:");
    	JTextField specificer=new JTextField();
    	
    	specific.setFont(new Font("Serif",Font.BOLD,WIDTH/77));
    	specific.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e)
        	{
        		if(specific.isSelected())
        		{
        			//default border & backgrounds
        			specificer.setEnabled(true);
        			specificer.setBorder(BorderFactory.createLineBorder(new Color(171,173,179)));
        			specificer.setBackground(Color.white);
        		}
        		else
        		{
        			specificer.setEnabled(false);
        			specificer.setBackground(null);
        			specificer.setBorder(null);
        		}
        	}});
    	leftPane.add(specific);

    	specificer.setFont(new Font("Serif",0,WIDTH/77));
    	specificer.setPreferredSize(new Dimension(800, 40));
    	specificer.setMaximumSize(specificer.getPreferredSize());
    	specificer.setBackground(null);
		specificer.setBorder(null);
		specificer.setEnabled(false);
    	leftPane.add(specificer);
    	leftPane.add(Box.createRigidArea(new Dimension(0,UNIT_SPACE*3)));
    	
    	JCheckBox loadBox=new JCheckBox(" Load to different sub-folder");
    	if(loadtoSubFolder)
    		loadBox.setSelected(true);
    	loadBox.setFont(new Font("Serif",Font.BOLD,WIDTH/77));
    	leftPane.add(loadBox);
    	leftPane.add(Box.createRigidArea(new Dimension(0,UNIT_SPACE*3)));
    	
    	JLabel limit=new JLabel(" Set limit to ");
    	limit.setFont(new Font("Serif",Font.BOLD,WIDTH/77));
    	pane[2].add(limit);
    	
    	JTextField limitField=new JTextField();
    	if(lim!=-1)
    		limitField.setText(lim+"");
    	limitField.setFont(new Font("Serif",Font.BOLD,WIDTH/77));
    	limitField.setPreferredSize(new Dimension(60, 50));
    	limitField.setMaximumSize(limitField.getPreferredSize());
    	pane[2].add(limitField);
    	
    	JLabel limit2=new JLabel(" pages");
    	limit2.setFont(new Font("Serif",Font.BOLD,WIDTH/77));
    	pane[2].add(limit2);
    	pane[2].add(Box.createHorizontalGlue());
    	
    	JButton resizer= new JButton("Hide");
    	resizer.setPreferredSize(new Dimension(95,30));
        resizer.setMaximumSize(new Dimension(95,30));
    	resizer.addActionListener(new ActionListener(){
    	public void actionPerformed(ActionEvent e)
    	{
    		if(resizer.getText().equals("Hide"))
    		{
    			rightPane.setVisible(false);
    			fr.setSize((int)(fr.getWidth()/2),fr.getHeight());
    			resizer.setText("Expand");
    		}
    		else {
    			rightPane.setVisible(true);
    			fr.setSize(fr.getWidth()*2,fr.getHeight());
    			resizer.setText("Hide");
    		}
    	}});
    	pane[2].add(resizer);
    	pane[2].add(Box.createRigidArea(new Dimension(UNIT_SPACE*7,0)));
    	leftPane.add(pane[2]);
    	
    	JCheckBox closeBox=new JCheckBox(" Close after finish");
    	if(close)
    		closeBox.setSelected(true);
    	closeBox.setFont(new Font("Serif",Font.BOLD,WIDTH/77));
    	leftPane.add(Box.createRigidArea(new Dimension(0,UNIT_SPACE*3)));
    	leftPane.add(closeBox);    	
    	
    	JButton startbtn =new JButton("Start!");
    	startbtn.setFont(new Font("Serif",Font.BOLD,WIDTH/48));
    	startbtn.setPreferredSize(new Dimension((int)(200/960.0*WIDTH/2),(int)(100/540.0*HEIGHT/2)));
    	startbtn.setMaximumSize(startbtn.getPreferredSize());
    	pane[3].setBorder(BorderFactory.createEmptyBorder(0, WIDTH/16, 0, 0));
    	leftPane.add(Box.createVerticalGlue());
    	pane[3].add(startbtn);
    	leftPane.add(pane[3]);
    	leftPane.add(Box.createRigidArea(new Dimension(0,UNIT_SPACE)));
    	
    	JLabel copyWrong=new JLabel("Made by Jianchen Li 2019 @ No rights reserved.");
    	leftPane.add(copyWrong);
    	
    	fr.getContentPane().add(leftPane);
    	
    	JButton consolebtn=new JButton("console: ");
    	consolebtn.setFont(new Font("Serif",Font.BOLD,WIDTH/96));
    	consolebtn.setPreferredSize(new Dimension(240,(int)(50/540.0*HEIGHT/2)));
    	consolebtn.setMaximumSize(new Dimension(Short.MAX_VALUE,(int)(50/540.0*HEIGHT/2)));
    	pane[4].add(consolebtn);
    	
    	JButton previewbtn=new JButton("preview: ");
    	previewbtn.setFont(new Font("Serif",Font.BOLD,WIDTH/96));
    	previewbtn.setPreferredSize(new Dimension(240,(int)(50/540.0*HEIGHT/2)));
    	previewbtn.setMaximumSize(new Dimension(Short.MAX_VALUE,(int)(50/540.0*HEIGHT/2)));
    	previewbtn.setEnabled(false);
    	pane[4].add(previewbtn);
    	rightPane.add(pane[4]);
    	
    	JTextArea console=new JTextArea("Welcome! Click start to begin: \n"+
    			"Options:\n" + 
    			"Change- change index file path; your progress of the current index will be saved. \r\n" + 
    			"Add- create a new index file under indexes folder.  \r\n" + 
    			"Reset email- link to another email address; your loaded files, progresses and index files will not be affected. \r\n" + 
    			"Hide/expand- hide/expand the right side of UI. \r\n" + 
    			"Load by specific ID- load items by specific image ID; complete URLs are not allowed. \r\n" + 
    			"Set limit- set an upper page limit to items; items over the page limit will be skipped. (Use this if your disk space is limited)\r\n" + 
    			"Load to subfolder- load items into a different subfolder(yymmddHHMM) for every run. This option will greatly reduce render time when opening the main folder.\r\n" + 
    			"Close after finish- automatically close the program after all task is completed; your progress will be saved. \r\n" + 
    			"Start- lock in all choices and start loading preview. \n");
    	console.setFont(new Font("Consolas",0,WIDTH/137));
    	console.setEditable(false);
    	console.setLineWrap(true);
    	console.setWrapStyleWord(true);
    	console.setBorder(BorderFactory.createLineBorder(Color.black));
    	JScrollPane scroll = new JScrollPane(console);
    	scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
    	scroll.setPreferredSize(new Dimension(480,(int)(488/540.0*HEIGHT/2)));
    	rightPane.add(scroll);
    	
        JLabel preview= new JLabel();
    	preview.setVisible(false);
    	rightPane.add(preview);
    	rightPane.add(Box.createVerticalGlue());
    	
    	Queue<ImageIcon> previewQue=new LinkedList<ImageIcon>(); //image of preview items
    	Queue<String> siteIDQue=new LinkedList<String>(); //ID of preview items
    	Queue<String> todo=new LinkedList<String>(); //Task waiting to be processed
    	
    	JButton skip=new JButton("skip(X)");
    	skip.setPreferredSize(new Dimension(240,(int)(40/540.0*HEIGHT/2)));
    	skip.setMaximumSize(new Dimension(Short.MAX_VALUE,(int)(40/540.0*HEIGHT/2)));
    	skip.setVisible(false);
    	skip.addActionListener(new ActionListener(){
    	public void actionPerformed(ActionEvent e)
    	{
    		if(progress==indexSize)
    		{
    			consoleQue.add("Congratulations! You finished all items on this index file!\n");
    			return;
    		}
    		consoleQue.add("Item skipped: "+siteIDQue.poll()+"\n");
    		preview.setIcon(previewQue.poll());
    		progress++;
    	}});
    	pane[5].add(skip);
    	JButton load= new JButton("load(Z)");
    	load.setPreferredSize(new Dimension(240,(int)(40/540.0*HEIGHT/2)));
    	load.setMaximumSize(new Dimension(Short.MAX_VALUE,(int)(40/540.0*HEIGHT/2)));
    	load.setVisible(false);
    	load.addActionListener(new ActionListener(){
    	public void actionPerformed(ActionEvent e)
    	{
    		consoleQue.add("New item added to queue: "+siteIDQue.peek()+"\n");
    		todo.add(siteIDQue.poll());//add another task to the queue
    		preview.setIcon(previewQue.poll());
    	}});
    	pane[5].add(load);
    	rightPane.add(pane[5]);
    	
    	consolebtn.addActionListener(new ActionListener(){
    	public void actionPerformed(ActionEvent e)
    	{
    		scroll.setVisible(true);
    		preview.setVisible(false);
    		load.setVisible(false);
    		skip.setVisible(false);
    	}});
    	
    	previewbtn.addActionListener(new ActionListener(){
    	public void actionPerformed(ActionEvent e)
    	{
    		scroll.setVisible(false);
    		preview.setVisible(true);
    		load.setVisible(true);
    		skip.setVisible(true);
    	}});
    	
        startbtn.addActionListener(new ActionListener(){
    	public void actionPerformed(ActionEvent e)
    	{
    		startbtn.setText("Loading...");
        	startbtn.setEnabled(false);
        	startbtn.setFont(new Font("Serif",Font.BOLD,WIDTH/64));
        	if(limitField.getText().equals("")) lim=-1;
        	else lim=Integer.parseInt(limitField.getText());
        	close=closeBox.isSelected();
        	loadtoSubFolder=loadBox.isSelected();
    		consoleQue.add("Start loading preview: \n");
    		
    		//Thread loading preview images
    		new Thread(){public void run(){
    		if(loadtoSubFolder)
    		{
    			try {
   					folderID=sender.createFolder(folderID); //create a sub-folder
   				} catch (IOException e1) {
   					log(e1);
				}
   			}
           	if(specific.isSelected())
           		todo.add(specificer.getText());
    		while(true)
    		{
    			try {
   				Thread.sleep(100);//prevents thread from freezing
   				if(previewQue.size()<5)
				{
					String temp=br.readLine();
					siteIDQue.add(temp);
					//rescale image before adding to queue
					Image img=ImageIO.read(new URL("https://t.nhentai.net/galleries/"+temp+"/cover.jpg"));
					int w=img.getWidth(null);
					int h=img.getHeight(null);
					if(h>=w)
						img=img.getScaledInstance((int)(w*450/540.0*HEIGHT/2/h), (int)(450/540.0*HEIGHT/2), Image.SCALE_DEFAULT);
					else 
						img=img.getScaledInstance(WIDTH/4, (int)(h*WIDTH/4/w), Image.SCALE_DEFAULT);
					MediaTracker tracker = new MediaTracker(new java.awt.Container());
					tracker.addImage(img, 0);
					tracker.waitForAll();
					
					previewQue.add(new ImageIcon(img));
					if(!previewbtn.isEnabled()) 
						consoleQue.add ("Load preview successful. Go to preview panel and select items to load:\n");
					previewbtn.setEnabled(true);
					preview.setIcon(previewQue.peek());
				}
    			}catch(Exception e){
				consoleQue.add("Load preview failed.\n");
				previewQue.add(new ImageIcon());
				log(e);}
    		}
            }}.start();
    	}});
    	
    	//Thread for the sending algorithm
    	new Thread(){public void run(){
			try {
			while(true)
			{
				Thread.sleep(100); //prevent thread from freezing
				if(progress==indexSize)
	    		{
	    			consoleQue.add("Congratulations! You finished all items on this index file!\n");
	    			break;
	    		}
				if(sender.idle && !todo.isEmpty())
				{
					if(sender.send(consoleQue,todo.poll(),folderID,lim))
					{
						consoleQue.add("Successfully sended! "+todo.size()+" items left.\n");
						progress++;
						progresslbl.setText(" Current Progress: "+progress);
					}
					else consoleQue.add("Sending failed. \n");
					if(todo.isEmpty())
						consoleQue.add("All mission accomplished! Waiting for new items...\n");
					if(todo.isEmpty() && close)
						System.exit(0);
				}
			}
			}catch (Exception e) {
				log(e);
			};
        }}.start();
        
        //Timer refreshing console box
        (new Timer(100,new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent ex)
			{
				if(consoleQue.peek()!=null)
				{
					console.append(consoleQue.poll());
    				scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
				}
			}
		})).start();
    	
        //enable hotkeys
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
        	boolean pressed=false;
        	@Override
            public boolean dispatchKeyEvent(KeyEvent e) {
        		if(!previewbtn.isEnabled()) return false;
                if (e.getID() == KeyEvent.KEY_PRESSED && !pressed && e.getKeyCode()==KeyEvent.VK_Z) {
                	pressed=true;
                	consoleQue.add("New item added to queue: "+siteIDQue.peek()+"\n");
    	    		todo.add(siteIDQue.poll());//add another task to the queue
    	    		preview.setIcon(previewQue.poll());
                }
                if(e.getID() == KeyEvent.KEY_PRESSED && !pressed && e.getKeyCode()==KeyEvent.VK_X) {
                	pressed=true;
                	if(progress==indexSize)
    	    		{
    	    			consoleQue.add("Congratulations! You finished all items on this index file!\n");
    	    			return false;
    	    		}
    	    		consoleQue.add("Item skipped: "+siteIDQue.poll()+"\n");
    	    		preview.setIcon(previewQue.poll());
    	    		progress++;
                }
                if (e.getID() == KeyEvent.KEY_RELEASED) {
                    pressed=false;
                }
                return false;
            }
        });
        
        //Save user data while exiting the program
        Runtime.getRuntime().addShutdownHook(new Thread() {
        	public void run() {
        		try {
        			//only update main folderID upon resetting
        			user.setProperty(tag, progress+"");
        			user.setProperty("limit", lim+"");
        			user.setProperty("closeAfterFinish", closeBox.isSelected()+"");
        			user.setProperty("loadtoSubFolder", loadBox.isSelected()+"");
        			user.setProperty("indexDirectory", indexDirectory);
        			user.store(new FileOutputStream("userdata.txt"), null);
        			//clean up all temp files
        			for (File f : new File(System.getProperty("user.dir")).listFiles()) 
        			{
        			    if (f.getName().endsWith(".tmp")) 
        			        f.delete();
        			}
        			System.out.println("terminated successfully!");
        		}catch(Exception e){log(e);}
        	}}); 
        
        fr.getContentPane().add(rightPane);
        
        fr.pack();
        fr.setFocusable(true);
        fr.setResizable(false);
        fr.setLocationRelativeTo(null);
        fr.setVisible(true);
        System.out.println(fr.getSize());
    }
    
    public static void log(Exception e){
		try {
			PrintWriter pw = new PrintWriter (new FileWriter ("err_log.txt", true));
			e.printStackTrace(pw);
			pw.close();
		} catch (IOException ignore) {}
     }
    
    public static void main(String[] args) throws IOException, GeneralSecurityException, InterruptedException{
    	try { 
    		UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } 
        catch (Exception e) {
        	log(e);
            System.out.println("Look and Feel not set");
        } 
    	new mainUI();
    }
}
