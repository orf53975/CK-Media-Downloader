/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package downloaderProject;

import ChrisPackage.ClipboardListener;
import Queryer.QueryManager;
import Share.Actions;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.events.JFXDialogEvent;
import downloader.DataStructures.Device;
import downloader.DataStructures.GenericQuery;
import downloader.DataStructures.video;
import downloader.DownloadManager;
import downloader.DownloaderItem;
import downloader.Site;
import java.awt.SplashScreen;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

/**
 *
 * @author christopher
 */
public class MainApp extends Application {
    
    public enum OsType {Windows, Linux, Apple}; // Apple /Users/username/
    public static OsType OS;
    public static String username;
    public static File pageCache, imageCache, saveDir, progressCache;
    public static DownloadManager dm;
    public static QueryManager query;
    public static ListView<Pane> downloads;
    public static Scene scene;
    public static boolean active = true;
    public static Vector<Device> devices;
    public static ComboBox<Device> deviceBox;
    private static AnchorPane actionPaneHolder;
    public static ProgressBar progress;
    public static TextArea log;
    public static Actions act;
    private static final String TITLE = "Video Downloader Prototype build 25";
    public static DownloadHistory downloadHistoryList;
    public static StackPane root;
    public static DataCollection habits;
    private static boolean dontLoad;
    
    public static final int WIDTH = 895, HEIGHT = 550, XS = 100;
    public static final int SUPPORTEDSITES = 36, PANES = 6;
    public static Pane[] actionPanes = new Pane[PANES];
    public static final int DOWNLOADPANE = 0, BROWSERPANE = 1, SETTINGSPANE = 2, SHAREPANE = 3, DOWNLOADHISTORYPANE = 4, ACCOUNTPANE = 5;
    
    public static ManageSettings settings;
    private ClipboardListener clippy;
    public static mainLayoutController mainController;
    public static Stage window;

    private void getUserName() {
        username = System.getProperty("user.name");
    }
    
    private void setOSicon(ImageView icon) {
        InputStream in;
        switch(OS) {
            case Windows:
                in = System.class.getResourceAsStream("/icons/icons8-windows-xp-48.png");
                break;
            case Linux:
                in = System.class.getResourceAsStream("/icons/icons8-linux-48.png");
                break;
            case Apple:
                in = System.class.getResourceAsStream("/icons/icons8-mac-client-30.png");
                break;
            default: 
                in = System.class.getResourceAsStream("/icons/icons8-linux-48.png");
                break;
        }
        Image image = new Image(in);
        icon.setImage(image);
    }
    
    private void determineOS() {
        String Os = System.getProperty("os.name");
        if((Os.contains("win") || Os.contains("Win")))
            OS = OsType.Windows;
        else if (Os.contains("Linux"))
            OS = OsType.Linux;
        else OS = OsType.Apple;
    }
    
    private void setCacheDir() {
        String home = System.getProperty("user.home");
        if (null == OS) { //assume unix system
            imageCache = new File(home+File.separator+".downloaderCache/images");
            pageCache = new File(home+File.separator+".downloaderCache/pages");
            progressCache = new File(home+File.separator+".downloaderCache/progress");
            saveDir = new File(home+File.separator+".downloaderSettings");
            if (!imageCache.exists()) imageCache.mkdirs();
            if (!pageCache.exists()) pageCache.mkdirs();
            if (!saveDir.exists()) saveDir.mkdirs();
            if (!progressCache.exists()) progressCache.mkdirs();
        } else switch (OS) {
            case Windows:
                String homeFolder = System.getProperty("user.home");
                imageCache = new File(homeFolder+"\\downloaderCache\\images");
                pageCache = new File(homeFolder+"\\downloaderCache\\pages");
                progressCache = new File(homeFolder+"\\downloaderCache\\progress");
                saveDir = new File(homeFolder+"\\downloaderSettings");
                if (!imageCache.exists()) imageCache.mkdirs();
                if (!pageCache.exists()) pageCache.mkdirs();
                if (!saveDir.exists()) saveDir.mkdirs();
                if (!progressCache.exists()) progressCache.mkdirs();
                try {
                    Runtime.getRuntime().exec("attrib +h "+imageCache.getAbsolutePath());
                    Runtime.getRuntime().exec("attrib +h "+pageCache.getAbsolutePath());
                    Runtime.getRuntime().exec("attrib +h "+progressCache.getAbsolutePath());
                    Runtime.getRuntime().exec("attrib +h "+saveDir.getAbsolutePath());
                    Runtime.getRuntime().exec("attrib +h "+homeFolder+"\\downloaderCache");
                } catch (IOException e) {}   
                break;
            case Linux:
            case Apple:
            default:
                imageCache = new File(home+File.separator+".downloaderCache/images");
                pageCache = new File(home+File.separator+"/.downloaderCache/pages");
                progressCache = new File(home+File.separator+"/.downloaderCache/progress");
                saveDir = new File(home+File.separator+"/.downloaderSettings");
                if (!imageCache.exists()) imageCache.mkdirs();
                if (!pageCache.exists()) pageCache.mkdirs();
                if (!saveDir.exists()) saveDir.mkdirs();
                if (!progressCache.exists()) progressCache.mkdirs();
        }
    }
    
    private void setProfileFolder() {
        if (null == OS) { //assume unix system
            settings.preferences.setProfileFolder(new File(MainApp.saveDir.getAbsolutePath()+"/Stars"));
            if (!settings.preferences.getProfileFolder().exists()) settings.preferences.getProfileFolder().mkdirs();
        } else switch (OS) {
            case Windows:                
                settings.preferences.setProfileFolder(new File(MainApp.saveDir.getAbsolutePath()+"\\Stars"));
                if (!settings.preferences.getProfileFolder().exists()) settings.preferences.getProfileFolder().mkdirs();
                break;
            case Linux:
            case Apple:
            default:
                settings.preferences.setProfileFolder(new File(MainApp.saveDir.getAbsolutePath()+"/Stars"));
                if (!settings.preferences.getProfileFolder().exists()) settings.preferences.getProfileFolder().mkdirs();
        }       
    }
    
    public static void displayPane(int tab) {
        actionPaneHolder.getChildren().clear();
        actionPaneHolder.getChildren().add(actionPanes[tab]);
    }
    
    public static void setDarkTheme(boolean enable) {
        for(int i = 0; i < PANES; i++) {
            if (actionPanes[i].getStylesheets() != null) 
                    actionPanes[i].getStylesheets().clear();
            if (enable)
                actionPanes[i].getStylesheets().add(MainApp.class.getResource("layouts/darkPane.css").toExternalForm());
            else actionPanes[i].getStylesheets().add(MainApp.class.getResource("layouts/normal.css").toExternalForm());
        }
        
        for(Node n:((AnchorPane)root.getChildren().get(0)).getChildren()) {
            if(n instanceof Pane) {
                if (((Pane)n).getStylesheets() != null) ((Pane)n).getStylesheets().clear();
                if (enable)
                    ((Pane)n).getStylesheets().add(MainApp.class.getResource("layouts/darkPane.css").toExternalForm());
                else ((Pane)n).getStylesheets().add(MainApp.class.getResource("layouts/normal.css").toExternalForm());
            }
        } 
        if (query != null)
        	query.switchTheme(enable);
        if (downloadHistoryList != null)
            downloadHistoryList.switchTheme(enable);
        if(dm != null) 
            dm.changeTheme(enable);
        if (downloads != null) {
            if (downloads.getStylesheets() != null) downloads.getStylesheets().clear();
            if (enable)
                downloads.getStylesheets().add(MainApp.class.getResource("layouts/darkPane.css").toExternalForm());
            else downloads.getStylesheets().add(MainApp.class.getResource("layouts/normal.css").toExternalForm());
        }
    }
    
    public static void loadQuery(GenericQuery q) {
        if(query == null) query = new QueryManager(actionPanes[BROWSERPANE]);
            query.loadSearch(q);
        displayPane(BROWSERPANE);
    }
    
    private void loadActionPanes() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("layouts/downloads.fxml"));
            actionPanes[DOWNLOADPANE] = loader.load();
            loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("layouts/browser.fxml"));
            actionPanes[BROWSERPANE] = loader.load();
            loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("layouts/settings.fxml"));
            actionPanes[SETTINGSPANE] = loader.load();
            loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("layouts/share.fxml"));
            actionPanes[SHAREPANE] = loader.load();
            loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("layouts/downloadHistory.fxml"));
            actionPanes[DOWNLOADHISTORYPANE] = loader.load();
            loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("layouts/accounts.fxml"));
            actionPanes[ACCOUNTPANE] = loader.load();
        } catch (IOException ex) {
            System.out.println("Action Panes failed");
        }
    }
    
    private void loadView() { //load main scene
         try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("layouts/mainLayout.fxml"));
            root = loader.load();
            AnchorPane main = (AnchorPane) root.getChildren().get(0);
            main.setPrefHeight(HEIGHT);
            main.setPrefWidth(WIDTH);
            //main.getChildren().add(setupMenu());
            
            loadActionPanes();
            
            scene = new Scene(root, WIDTH, HEIGHT);
            scene.getStylesheets().add(MainApp.class.getResource("mainStyleSheet.css").toExternalForm());
            root.scaleXProperty().bind(scene.widthProperty().divide(WIDTH));
            root.scaleYProperty().bind(scene.heightProperty().divide(HEIGHT));
        } catch (IOException e) {
            createMessageDialog("Failed: "+e.getMessage());
        }
    }

    private void moveFiles(File directory, File destination) {
        File[] files = directory.listFiles();
        
        if(files != null)
            for(File f: files)
                DataIO.moveFile(f, destination);
        directory.delete();
    }
    
    private void moveFile(File file, File destination) {
        if(file.exists()) 
            DataIO.moveFile(file, destination);
    }
    
    private void cleanUp() {
        if(OS == OsType.Windows) {
            //move files from old dirs to new locations
            if(new File("C:\\"+username+"\\downloadCache\\images").exists())
                moveFiles(new File("C:\\"+username+"\\downloadCache\\images"),imageCache);
            if(new File("C:\\"+username+"\\downloadCache\\pages").exists())
                moveFiles(new File("C:\\"+username+"\\downloadCache\\pages"),pageCache);
            if(new File("C:\\"+username+"\\downloadCache\\progress").exists())
                moveFiles(new File("C:\\"+username+"\\downloadCache\\progress"),progressCache);
            if(new File("C:\\"+username).exists())
                moveFiles(new File("C:\\"+username),saveDir);
            Vector<video> videos = DataIO.loadVideos();
            if(videos != null) {
                for(video v:videos) {
                    v.adjustThumbnail(imageCache.getAbsolutePath());
                    v.adjustPreview(imageCache.getAbsolutePath());
                }
            }
        } else {
            String home = System.getProperty("user.home");
            moveFile(new File(home+"/settings.dat"),saveDir);
            moveFile(new File(home+"/laterVideos.dat"),saveDir);
            moveFile(new File(home+"/devices.dat"),saveDir);
        }
    }
    
    public static String getSizeText(long size) {
        //ik 1024 is the right unit rather than 1000
        //but dividing by 1000 gave a more accurate result on linux
        int divide = OS == OsType.Windows ? 1024 : 1000;
        if (size < 0) 
            return "----";
        else if (size < 1024)
            return size + " b";
        else if((size >= 1024) && (size < 1048576)) //1024b * 1024b = ?kb you do the math
            return String.format("%.2f",(double)size / divide) + " kb"; 
        else if ((size >= 1048576) && (size < 1073741824)) //1024kb * 1024kb = ?mb you do the math
            return String.format("%.2f",(double)size / divide / divide) + " mb";
        else return String.format("%.2f",(double)size / divide / divide / divide) + " gb"; //1024mb * 1024mb = ?gb you do the math
    }
    
    private String getlocalDeviceName() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            return ip.getHostName();
        } catch (UnknownHostException ex) {
            return "Undetermined";
        }
    }
    
    public static void updateDevices() {
        devices = DataIO.loadDevices();
        if(devices == null) devices = new Vector<Device>();
        
        deviceBox.getItems().clear();
        settings.setDeviceCount(devices.size()+" devices");
        
        for(Device d : devices)
            deviceBox.getItems().add(d);
    }
    
    private void setupSharePane() {
        deviceBox = (ComboBox<Device>)actionPanes[SHAREPANE].lookup("#devices");
        updateDevices();
        
        progress = (ProgressBar)actionPanes[SHAREPANE].lookup("#progressBar");
        log = (TextArea)actionPanes[SHAREPANE].lookup("#log");
        log.setEditable(false);
        Label deviceName = (Label)actionPanes[SHAREPANE].lookup("#deviceName");
        deviceName.setText("This Device Name: "+getlocalDeviceName());
        act = new Actions();
    }
    
    private void setupDownloadHistoryPane() {
        downloadHistoryList = new DownloadHistory((ListView<Pane>)actionPanes[DOWNLOADHISTORYPANE].lookup("#downloaded"));
        downloadHistoryList.display();
        if (downloadHistoryList != null)
            downloadHistoryList.switchTheme(settings.preferences.dark());
    }

    public static void createNotification(final String title, final String msg) {
        Platform.runLater(new Runnable() {
            @Override public void run() {
                String[] tokens = msg.split(" ");
                StringBuilder m = new StringBuilder();
                for(int i = 0; i < tokens.length; i++) {
                    if (i % 6 == 0) { m.append("\n"); m.append(tokens[i]); continue;}
                    if (i == 0) m.append(tokens[i]);
                    else m.append(" "+tokens[i]);
                }
                Notifications.create().title(title).text(m.toString()).darkStyle().position(Pos.BOTTOM_RIGHT).showConfirm();
            }
        });
    }
    
    public static void createMessageDialog(String msg) {
        Platform.runLater(new Runnable() {
            Pane pane; BoxBlur blur = new BoxBlur(5,5,5);
            @Override public void run() {
                try {
                    pane = FXMLLoader.load(new MainApp().getClass().getResource("layouts/messageDialog.fxml"));
                    pane.getStylesheets().clear();
                    if (settings == null) System.out.println(msg); else{
                    if(settings.preferences.dark())
                        pane.getStylesheets().add(MainApp.class.getResource("layouts/darkPane.css").toExternalForm());
                    else pane.getStylesheets().add(MainApp.class.getResource("layouts/normal.css").toExternalForm());
                    AnchorPane a = (AnchorPane)root.getChildren().get(0);
                    JFXDialog dialog = new JFXDialog(root,pane,JFXDialog.DialogTransition.CENTER);
                    Label text = (Label)pane.lookup("#messageLabel");
                    Button ok = (Button)pane.lookup("#okay");
                    text.setText(msg);
                    ok.setOnAction(new EventHandler<ActionEvent>() {
                        @Override public void handle(ActionEvent event) {
                            dialog.close();
                        }
                    });
                    dialog.setOnDialogClosed(new EventHandler<JFXDialogEvent>() {
                        @Override public void handle(JFXDialogEvent event) {
                            a.setEffect(null);
                        }
                    });
                    dialog.show(); a.setEffect(blur);
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    @Override public void start(Stage primaryStage) {
    	window = primaryStage;
        determineOS(); //determine what OS is running
        getUserName(); //get the username
        setCacheDir(); //set up cache (create it if it doesnt exist yet)
        cleanUp(); //correct old version data
        
       dm = new DownloadManager(); //create the download manager
        
       loadView();
       settings = new ManageSettings(actionPanes[SETTINGSPANE],DataIO.loadSettings());
       setProfileFolder();
       setDarkTheme(settings.preferences.dark());

       Pane topPane = (Pane)scene.lookup("#topPane");
       setOSicon((ImageView)topPane.lookup("#OSicon"));
       actionPaneHolder = (AnchorPane)scene.lookup("#currentView");
       displayPane(DOWNLOADPANE);
       
       downloads = (ListView<Pane>) actionPanes[DOWNLOADPANE].lookup("#downloadList"); //listview of downloads
       //downloads.getStyleClass().add("list-view");
       Label userLabel = (Label) scene.lookup("#username");
       
      userLabel.setText(username);
      
       settings.init(); setupDownloadHistoryPane();
       downloadHistoryList.setSettings(settings);
       
       setupSharePane();
       
       clippy = new ClipboardListener(mainController);
       
       window.setTitle(TITLE);
       window.setOnCloseRequest(event -> {
           if (query != null)
               query.release();
            dm.release();
            active = false;
            clippy.stop();
            System.out.println("Exiting");
            //System.exit(0);
        });
       window.setScene(scene);
       window.setMinHeight(HEIGHT);
       window.setMinWidth(WIDTH);
       window.setMaxHeight(HEIGHT+XS);
       window.setMaxWidth(WIDTH+XS);
       window.setHeight(HEIGHT);
       window.setWidth(WIDTH);
       try {
            if(splash != null)
                 splash.close();
       } catch (IllegalStateException e) {
           System.out.println("Splash screen error");
       }
       window.show();
       
       if (!dontLoad)
           loadSuggestions();
       ExecutorService x = Executors.newSingleThreadExecutor();
       x.execute(clippy); x.shutdown();
    }
    
    public static void log(String mediaName, String site) {
        if (habits != null) habits.add(mediaName, site);
        try {DataIO.saveCollectedData(habits);} catch(IOException e) {System.out.println("Failed to save habits");}
    }
    
    public static void log (video v) {
        if (habits != null) habits.addSuggestion(v);
        try {DataIO.saveCollectedData(habits);} catch(IOException e) {System.out.println("Failed to save habits");}
    }
    
    private void loadSuggestions() {
        habits = DataIO.loadCollectedData();
        if (habits != null) {
            int pull = 1;
            if (habits.suggestions() > 12 && habits.suggestions() <= 20) pull = 2;
            else if (habits.suggestions() > 20 && habits.suggestions() <= 35) pull = 3;
            else if (habits.suggestions() > 35) pull = 4;
            for(int i = 0; i < pull; i++) {
                video temp = habits.next(); 
                if (temp != null)
                    determineSite(temp.getLink(),temp);
                else System.out.println("no suggestions");
            }
            try {DataIO.saveCollectedData(habits);} catch(IOException e) {System.out.println("Failed to save habits");}
        } else {habits = new DataCollection(true);}
    }
    
    public void determineSite(String link, video v) {
        DownloaderItem download = new DownloaderItem();
        download.setLink(link); download.setType(Site.getUrlSite(link)); download.setVideo(v);
        //add item to downloadManager for display
        dm.addDownload(download);
    }

    /**
     * @param args the command line arguments
     */
    
    static SplashScreen splash;
    public static void main(String[] args) {
        dontLoad = false;
        if (args != null)
            if (args.length > 0)
                if (args[0].toLowerCase().equals("suppress"))
                    dontLoad = true;
        splash = SplashScreen.getSplashScreen();
        launch(args); 
    }
}