import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeBodyPart;

import javax.mail.Part;

import java.io.*;
import java.util.*;

public class FetchEmailServer {

    private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger("MyLogger", null);

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

            // create the IMAP store object and connect with the pop server
            Store store = emailSession.getStore("imaps");

            store.connect(host, user, password);

            // create the folder object and open it
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            // retrieve the messages from the folder in an array and save their contents if applicable
            Message[] messages = emailFolder.getMessages();
            logger.info("messages.length---" + messages.length);

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                logger.info("---------------------------------" + "\nEmail Number " + (i + 1) + "\nSubject: "
                        + message.getSubject() + "\nFrom: " + message.getFrom()[0] + "\n");
                String header = "Subject: " + message.getSubject() + "\nFrom: " + message.getFrom()[0] + "\nTo: "
                        + message.getAllRecipients()[0] + "\n------------------------------------------------------";
                String footer = "-----------END OF EMAIL-----------";
                saveMessageParts(message, header, footer);

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
     * Breaks down the main part into its subparts, then saves subparts with the
     * application/pdf or text/plain content type to a file.
     * 
     * @param main   the main part to break down
     * @param header the text that will show at the beginning of a text file.
     * @param footer the text that will show at the end of a text file.
     * 
     */
    public static void saveMessageParts(Part main, String header, String footer)
            throws IOException, MessagingException {
        Object content = main.getContent();
        if (content instanceof MimeMultipart) {
            MimeMultipart parts = (MimeMultipart) content;
            for (int j = 0; j < parts.getCount(); j++) {
                MimeBodyPart part = (MimeBodyPart) parts.getBodyPart(j);
                logger.info("j = " + j + " content type = " + part.getContentType());
                try {
                    String PDFFile = writePDF(part);
                    if (PDFFile != null) {
                        logger.info("---------------------------------" + "\nPDF " + PDFFile
                                + " in ./out/pdfs/ created.\n");
                    }
                    if (part.isMimeType("multipart/alternative")) {
                        saveMessageParts(part, header, footer);
                    }
                    writeText(part, header, footer);
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Write a PDF attachment to a file. The attachment is in the form of a
     * MimeBodyPart.
     * 
     * @param p A MimeBodyPart with the application/pdf content type.
     * @return the name of the created file, or null if not created
     */
    public static String writePDF(MimeBodyPart p) throws MessagingException, IOException {
        if (p == null) {
            throw new IllegalArgumentException("No null parameters allowed");
        }
        String fileName = null;
        if (p.isMimeType("application/pdf")) {
            fileName = "m" + new Date().getTime() + ".pdf";
            File f = new File("./out/pdfs/" + fileName);
            p.saveFile(f);
        }
        return fileName;
    }

    /**
     * Write email text to a file. The email text is in the form of a MimeBodyPart.
     * Also has options to add a header and footer to the text.
     * 
     * @param p      A MimeBodyPart with the text/plain content type. Contains the
     *               body.
     * @param header The text that will be placed before the body.
     * @param footer the text that will be placed after the body.
     * @return the name of the created file, or null if not created
     */
    public static String writeText(MimeBodyPart p, String header, String footer)
            throws MessagingException, IOException {
        if (p == null || header == null || footer == null) {
            throw new IllegalArgumentException("No null parameters allowed");
        }
        String fileName = null;
        if (p.isMimeType("text/plain")) {
            fileName = "m" + new Date().getTime() + ".txt";
            File f = new File("./out/completed/" + fileName);
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.write(header + "\n");
            writer.write(p.getContent().toString());
            writer.write("\n" + footer);
            writer.close();
        }
        return fileName;
    }

    public static void main(String[] args) {

        String host = "imap.gmail.com";// change accordingly
        String mailStoreType = "imap";
        String username = "mrtorres989@gmail.com";// change accordingly
        String password = "@peLucha1989";// change accordingly

        check(host, mailStoreType, username, password);

    }

}