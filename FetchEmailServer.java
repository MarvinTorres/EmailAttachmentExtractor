import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Flags.Flag;
import javax.mail.Flags;
import javax.mail.search.FlagTerm;
import java.util.logging.Logger;
import java.io.*;
import java.util.*;

public class FetchEmailServer {

    private static Logger logger;

    public FetchEmailServer(Logger aLogger) {
        logger = aLogger;
    }

    public void check(String host, String user, String password) {
        try {
            File f = new File("./out/pdfs/");
            f.mkdirs();
            f = new File("./out/completed/");
            f.mkdirs();

            // create properties field
            Properties properties = new Properties();

            properties.put("mail.imap.host", host);
            properties.put("mail.imap.port", "993");
            properties.put("mail.imap.starttls.enable", "true");
            Session emailSession = Session.getDefaultInstance(properties);

            // create the IMAP store object and connect with the imap server
            Store store = emailSession.getStore("imaps");

            store.connect(host, user, password);

            // create the folder object and open it
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_WRITE);

            // create the destination folder object
            Folder destination = store.getFolder("COMPLETED");
            if (!destination.exists()) {
                logger.info("Created destination folder " + destination.getFullName() + " on server side.\n");
                destination.create(Folder.HOLDS_MESSAGES);
            }

            // retrieve unread messages from the folder and save their contents
            Message[] messages = emailFolder.search(new FlagTerm(new Flags(Flag.SEEN), true));

            // move messages to destination folder in server
            emailFolder.copyMessages(messages, destination);

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

            // close the store and folder objects
            emailFolder.close(false);
            store.close();

        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}