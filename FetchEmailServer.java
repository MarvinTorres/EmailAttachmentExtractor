import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;

import javax.mail.BodyPart;
import javax.mail.Part;

import java.io.*;
import java.util.*;

public class FetchEmailServer {

    public static void check(String host, String storeType, String user, String password) {
        try {

            // create properties field
            Properties properties = new Properties();

            properties.put("mail.imap.host", host);
            properties.put("mail.imap.port", "993");
            properties.put("mail.imap.starttls.enable", "true");
            Session emailSession = Session.getDefaultInstance(properties);

            // create the IMAP store object and connect with the pop server
            Store store = emailSession.getStore("imaps");

            store.connect(host, user, password);

            // create the folder object and open it
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            // retrieve the messages from the folder in an array and print it
            Message[] messages = emailFolder.getMessages();
            System.out.println("messages.length---" + messages.length);

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                System.out.println("---------------------------------");
                System.out.println("Email Number " + (i + 1));
                System.out.println("Subject: " + message.getSubject());
                System.out.println("From: " + message.getFrom()[0]);
                Object content = message.getContent();
                if (content instanceof MimeMultipart) {
                    MimeMultipart parts = (MimeMultipart) content;
                    for (int j = 0; j < parts.getCount(); j++) {
                        BodyPart part = parts.getBodyPart(j);
                        try {
                            String nameSuffix = getFileFriendlyName(message.getFrom()[0] + " - " + message.getSubject()); 
                            writePDF(part, nameSuffix);
                            writeText(part, nameSuffix);
                        } catch (MessagingException e) {
                                e.printStackTrace();
                        }
                    }
                }
                System.out.println();
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

    /**
     * Write a PDF attachment to a file.
     * @param p
     * @param nameSuffix text that will be added to the end of the filename.
     */
    public static void writePDF(Part p, String nameSuffix) throws MessagingException, IOException {
        if (p.isMimeType("application/pdf")) {
            String usedNameSuffix = nameSuffix != null ? nameSuffix : "";
            /*
            * Code from https://www.tutorialspoint.com/javamail_api/javamail_api_quick_guide.htm
            */
            File f = new File("m" + new Date().getTime() + "_" + usedNameSuffix + ".pdf");
            DataOutputStream output = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(f)));
            com.sun.mail.util.BASE64DecoderStream test = 
                (com.sun.mail.util.BASE64DecoderStream) p.getContent();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = test.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.close();
        }
    }

    /**
     * Write email text to a file.
     * @param p
     * @param nameSuffix text that will be added to the end of the filename.
     */
    public static void writeText(Part p, String nameSuffix) throws MessagingException, IOException {
        if (p.isMimeType("text/plain")) {
            String usedNameSuffix = nameSuffix != null ? nameSuffix : "";
            File f = new File("m" + new Date().getTime() + "_" + usedNameSuffix + ".txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.write(p.getContent().toString());
            writer.close();
        } else {
            System.out.println("NOT TEXT!");
        }
    }

    /**
     * Removes special characters from a filename that can not be used in Windows filenames.
     * 
     * @param toSanitize
     * @return
     */
    public static String getFileFriendlyName(String toSanitize) {
        String sanitized = toSanitize;
        
        sanitized = sanitized.replaceAll("<.*>", "").replace(".", "(dot)").replace("@", "(at)").replace(" ", "_").replaceAll("^[a-zA-Z0-9]", "");
        return sanitized;
    }

    public static void main(String[] args) {

        String host = "imap.gmail.com";// change accordingly
        String mailStoreType = "imap";
        String username = "mrtorres989@gmail.com";// change accordingly
        String password = "@peLucha1989";// change accordingly

        check(host, mailStoreType, username, password);

    }

}