/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gambleblocker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.*;
import javax.swing.JOptionPane;

/**
 *
 * @author sascha
 */
public class GambleBlocker {

    private static final String HOSTS_FILE = System.getenv("WINDIR") + "\\System32\\drivers\\etc\\hosts";
    
    private static final String HOSTS_NEW = "C:\\Program Files\\GambleBlocker\\hosts.new";
    
    private static final String HOSTS_BACKUP = "C:\\Program Files\\GambleBlocker\\hosts.backup";
    
    public static void main(String[] args) throws IOException, InterruptedException{
        createApplicationDirectoryAndSaveDefaultHost();
        getAndWriteUpdatedGambleIPs();
        watchHostsFileForChange();
    }
    
    private static void createApplicationDirectoryAndSaveDefaultHost() throws IOException{
       
        File applicationDirectory = new File("C:\\Program Files\\GambleBlocker\\");
        File sourceHostFile = new File(HOSTS_FILE);
        
        File backupHost = new File(HOSTS_BACKUP);
        
        if (!applicationDirectory.exists()){
            applicationDirectory.mkdirs();
            if (!applicationDirectory.exists()){
                JOptionPane.showMessageDialog(null, "Programmverzeichnis konnte nicht erstellt werden. Programm wird beendet!");
                System.exit(0);
            }
        }
        
        if (!backupHost.exists()){
            try {
                Files.copy(sourceHostFile.toPath(), backupHost.toPath());  
            } catch (Exception e) {
                System.out.println("Couldn't create backup of Hosts");
            }
        }
        
    }
    
    private static void getAndWriteUpdatedGambleIPs() throws IOException{
        
        String hostNameQuery, targetIpQuery, targetAdress = null;
        String usernameSQL = "xxx", passwordSQL = "xxx";
        Statement stmtGetHost, stmtGetTarget;
        ResultSet rsHost, rsTarget;
        
        try {  
            Class.forName("com.mysql.jdbc.Driver");
            String connectionUrl = "jdbc:mysql://xxx";
            Connection con = DriverManager.getConnection(connectionUrl, usernameSQL, passwordSQL);
            
            hostNameQuery = "SELECT HostName FROM Hosts;";
            targetIpQuery = "SELECT Adress FROM TargetIP;";
            stmtGetHost = con.createStatement();
            stmtGetTarget = con.createStatement();
            
            Path path = Paths.get(HOSTS_NEW);
            Files.deleteIfExists(path);
            
            rsTarget = stmtGetTarget.executeQuery(targetIpQuery);
            while (rsTarget.next()){
             targetAdress = rsTarget.getString("Adress");           
            }
            rsTarget.close();
            
            rsHost = stmtGetHost.executeQuery(hostNameQuery);
            while (rsHost.next()) {
                try(PrintWriter writeHosts = new PrintWriter(new BufferedWriter(new FileWriter(HOSTS_NEW, true)))) {
                    writeHosts.println(targetAdress + " " + rsHost.getString("HostName"));
                }catch (IOException e) {
                 JOptionPane.showMessageDialog(null, "GambleBlocker hat ein Problem festgetellt. Programm wird beendet");
                 System.exit(0);
                }
            }
            rsHost.close();
        } catch (SQLException | ClassNotFoundException e) {
            return;
        }
        
        File sourceHostFile = new File(HOSTS_NEW);
        File targetHostFile = new File(HOSTS_FILE);
        
        try {
          Files.copy(sourceHostFile.toPath(), targetHostFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "GambleBlocker hat ein Problem festgestellt. Programm wird beendet");
            System.exit(0); 
        }
        
    }
    
    private static void watchHostsFileForChange() throws IOException, InterruptedException{
        final Path hostsPath = Paths.get(System.getenv("WINDIR") + "\\System32\\drivers\\etc\\");
        
        try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
            final WatchKey watchKey = hostsPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            
            while (true) {
                final WatchKey wk = watchService.take();
                wk.pollEvents().stream().map((event) -> (Path) event.context()).map((changed) -> {
                    return changed;
                }).filter((changed) -> (changed.endsWith("hosts"))).forEach((Path _item) -> {
                    try {
                        Thread.sleep(5000);                 //wait 5 seconds to rewrite Hosts File -> prevent crash by writing File before actualy closed
                    } catch(InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    
                    File sourceHostFile = new File(HOSTS_NEW);
                    File targetHostFile = new File(HOSTS_FILE);
                    
                    try {
                        Files.copy(sourceHostFile.toPath(), targetHostFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "GambleBlocker hat ein Problem festgestellt. Programm wird beendet");
                        System.exit(0);
                    }
                });
            boolean valid = wk.reset(); //need to be resetted, else won't work again 
            if (!valid) {
                watchHostsFileForChange();
                }
            }
        }
    }
    
}
