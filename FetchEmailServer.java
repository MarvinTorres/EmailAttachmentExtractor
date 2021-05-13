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

    private static Logger logger = Logger.getLogger("MyLogger", null);

    public static void check(String host, String storeType, String user, String password) {
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

            // retrieve unread messages from the folder in an array and save their contents
            // if
            // applicable
            Message[] messages = emailFolder.search(new FlagTerm(new Flags(Flag.SEEN), false));

            logger.info("messages.length---" + messages.length);

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                logger.info("---------------------------------" + "\nEmail Number " + (i + 1) + "\nSubject: "
                        + message.getSubject() + "\nFrom: " + message.getFrom()[0] + "\n");
                String header = "Subject: " + message.getSubject() + "\nFrom: " + message.getFrom()[0] + "\nTo: "
                        + message.getAllRecipients()[0] + "\n------------------------------------------------------";
                String footer = "-----------END OF EMAIL-----------";
                String folderName = "m" + new Date().getTime();
                f = new File("./out/completed/" + folderName);
                f.mkdirs();
                Task myTask = new Task(logger);
                myTask.saveMessageParts(message, header, footer, folderName);

                // print information relevant to the user to a separate rundown file

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

                // mark email as
                message.setFlag(Flags.Flag.SEEN, true);

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

    public static void main(String[] args) {

        String host = "imap.outlook.com";// change accordingly
        String mailStoreType = "imap";
        String username = "mrtorres989@outlook.com";// change accordingly
        String password = "@peLucha1989";// change accordingly

        check(host, mailStoreType, username, password);

    }

}