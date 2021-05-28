import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import com.sun.mail.imap.IMAPFolder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Flags.Flag;
import javax.mail.Flags;
import javax.mail.search.FlagTerm;
import java.util.logging.Logger;
import java.io.*;
import java.util.*;

public final class FetchEmailServer {

    private static Logger logger;
    private static FetchEmailServer fetcher;

    private String host;
    private String user;
    private String password;

    private Properties properties = null;
    private Session emailSession = null;
    private Store store = null;
    private IMAPFolder sourceFolder = null;

    public static FetchEmailServer getInstance(String host, String user, String password, Logger aLogger) {
        if (aLogger == null)
            throw new IllegalArgumentException("logger must not be null");
        if (fetcher != null)
            return fetcher;
        fetcher = new FetchEmailServer(aLogger);
        fetcher.host = host;
        fetcher.user = user;
        fetcher.password = password;
        try {
            fetcher.connect();
            fetcher.loadSourceFolder("INBOX");
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not connect to server.\nReason: " + e.getMessage());
        }
        return fetcher;
    }

    private FetchEmailServer(Logger aLogger) {
        logger = aLogger;
    }

    public void setLogger(Logger aLogger) {
        if (aLogger == null)
            throw new IllegalArgumentException("logger must not be null");
        logger = aLogger;
    }

    private void connect() throws MessagingException {
        // create properties field
        properties = new Properties();

        // create the IMAP store object and connect with the imap server
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", "993");
        properties.put("mail.imap.starttls.enable", "true");
        emailSession = Session.getDefaultInstance(properties);

        store = emailSession.getStore("imaps");
        store.connect(host, user, password);
    }

    public void waitForUpdate() {
        try {
            // wait for a folder update
            sourceFolder.idle(true);
        } catch (MessagingException e) {
            throw new IllegalStateException("Email folder encountered a problem. More details\n" + e.getMessage());
        }
    }

    private void loadSourceFolder(String newFolderName) {
        if (newFolderName == null)
            throw new IllegalArgumentException("The input folder name must not be null");
        try {
            // create the source folder object and open it
            IMAPFolder newFolder = (IMAPFolder) store.getFolder(newFolderName);
            sourceFolder = newFolder;
            sourceFolder.open(Folder.READ_WRITE);
        } catch (MessagingException e) {
            throw new IllegalStateException("The folder failed to load. More details:\n" + e.getMessage());
        }
    }

    public void fetch() {
        try {
            File f = new File("./out/pdfs/");
            f.mkdirs();
            f = new File("./out/completed/");
            f.mkdirs();

            // create the destination folder object
            Folder destinationFolder = store.getFolder("COMPLETED");
            if (!destinationFolder.exists()) {
                logger.info("Created destonation folder " + destinationFolder.getFullName() + " on server side.\n");
                destinationFolder.create(Folder.HOLDS_MESSAGES);
            }

            // retrieve unread messages from the folder and save their contents
            Message[] messages = sourceFolder.search(new FlagTerm(new Flags(Flag.SEEN), false));

            // move messages to destination folder in server
            sourceFolder.copyMessages(messages, destinationFolder);

            logger.info("messages.length---" + messages.length);

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];

                // prepare message to be written to its respective folder
                logger.info("---------------------------------" + "\nEmail Number " + (i + 1));
                String folderName = "m" + new Date().getTime();
                f = new File("./out/completed/" + folderName);
                f.mkdirs();
                Task myTask = new Task(logger);

                // print email header to a separate rundown file
                myTask.saveHeaderInformation(message, folderName);

                // save important message parts to their respective files
                myTask.saveMessageParts(message, folderName);

                StringBuilder emailInfo = new StringBuilder();
                String PDFLocationsFileName = "Where_Are_The_PDFs";

                if (myTask.hasCreatedPDFFile()) {
                    emailInfo.append("PDF Files Created:\n");
                    for (String fileName : myTask.getPDFFiles()) {
                        emailInfo.append(fileName + "\n");
                    }
                    emailInfo.append("\nThese files can be found in " + "./out/pdf/" + folderName + "\n");
                } else {
                    emailInfo.append("No PDFs were found in this email.\n");
                    PDFLocationsFileName = "No_PDFs_Here";
                }

                myTask.writeText(emailInfo.toString(), PDFLocationsFileName, folderName);
            }

        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void finalize() throws MessagingException {
        try {
            // close the folder
            sourceFolder.close(false);
            // close the store
            store.close();
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not close the store. More details:\n" + e.getMessage());
        }
    }

}