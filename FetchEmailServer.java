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

            // retrieve the messages from the folder in an array and save their contents if
            // applicable
            Message[] messages = emailFolder.getMessages();
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
                saveMessageParts(message, header, footer, folderName);

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
    public static void saveMessageParts(Part main, String header, String footer, String folderName)
            throws IOException, MessagingException {
        Object content = main.getContent();

        if (content instanceof MimeMultipart) {
            MimeMultipart parts = (MimeMultipart) content;
            for (int j = 0; j < parts.getCount(); j++) {
                MimeBodyPart part = (MimeBodyPart) parts.getBodyPart(j);
                logger.info("j = " + j + " content type = " + part.getContentType());
                try {
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                        String PDFFile = writePDF(part, folderName);
                        if (PDFFile != null) {
                            logger.info("---------------------------------" + "\nPDF " + PDFFile + " in ./out/pdfs/"
                                    + folderName + " created.\n");
                        }
                    } else if (part.isMimeType("multipart/alternative")) {
                        saveMessageParts(part, header, footer, folderName);
                    } else {
                        writeText(part, header, footer, folderName);
                        writeHTML(part, folderName);
                    }
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        } else if (content instanceof String) {
            String part = (String) content;
            writeText(part, header, footer, folderName);
        }
    }

    /**
     * Write a PDF attachment to a file. The attachment is in the form of a
     * MimeBodyPart.
     * 
     * @param p A MimeBodyPart with the application/pdf content type.
     * @return the name of the created file, or null if not created
     */
    public static String writePDF(MimeBodyPart p, String folderName) throws MessagingException, IOException {
        if (p == null || folderName == null) {
            throw new IllegalArgumentException("No null parameters allowed");
        }
        String fileName = null;
        if (p.isMimeType("application/pdf")) {
            fileName = p.getFileName().replace(".pdf", "") + "_m" + new Date().getTime() + ".pdf";
            File f = new File("./out/pdfs/" + (!folderName.isEmpty() ? folderName + "/" : ""));
            f.mkdirs();
            f = new File("./out/pdfs/" + (!folderName.isEmpty() ? folderName + "/" : "") + fileName);
            p.saveFile(f);
        }
        return fileName;
    }

    /**
     * Write HTML and referenced content to a file. The HTML core and its referenced
     * content are MimeBodyParts.
     * 
     * @param p          A MimeBodyPart with the text/html, text/png, or text/jpeg
     *                   content type
     * @param folderName the save folder location in /out/completed
     * @return The name of the created file.
     * @throws MessagingException
     * @throws IOException
     */
    public static String writeHTML(MimeBodyPart p, String folderName) throws MessagingException, IOException {
        if (p == null || folderName == null) {
            throw new IllegalArgumentException("No null parameters allowed");
        }
        if (p.isMimeType("text/html") || p.isMimeType("image/png") || p.isMimeType("image/jpeg")) {
            String fileName = (p.getFileName() == null ? "m" + new Date().getTime() : p.getFileName());

            if (p.isMimeType("text/html"))
                fileName += ".htm";

            File f = new File("./out/completed/" + (!folderName.isEmpty() ? folderName + "/" : "") + fileName);
            p.saveFile(f);
        }
        return folderName;
    }

    /**
     * Write email text to a file. The email text is a MimeBodyPart. Also has
     * options to add a header and footer to the text.
     * 
     * @param p          A MimeBodyPart with the text/plain content type. Contains
     *                   the body.
     * @param header     The text that will be placed before the body.
     * @param footer     the text that will be placed after the body.
     * @param folderName the save folder location in /out/completed
     * @return the name of the created file, or null if not created
     */
    public static String writeText(MimeBodyPart p, String header, String footer, String folderName)
            throws MessagingException, IOException {
        if (p == null || header == null || footer == null || folderName == null) {
            throw new IllegalArgumentException("No null parameters allowed");
        }
        String fileName = null;
        if (p.isMimeType("text/plain")) {
            fileName = "m" + new Date().getTime() + ".txt";
            File f = new File("./out/completed/" + (!folderName.isEmpty() ? folderName + "/" : "") + fileName);
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.write(header + "\n");
            writer.write(p.getContent().toString());
            writer.write("\n" + footer);
            writer.close();
        }
        return fileName;
    }

    /**
     * Write email text to a file. The email text is in the form of a String. Also
     * has options to add a header and footer to the text.
     * 
     * @param p          The body.
     * @param header     The text that will be placed before the body.
     * @param footer     the text that will be placed after the body.
     * @param folderName the save folder location in /out/completed
     * @return the name of the created file, or null if not created
     */
    public static String writeText(String p, String header, String footer, String folderName)
            throws MessagingException, IOException {
        if (p == null || header == null || footer == null || folderName == null) {
            throw new IllegalArgumentException("No null parameters allowed");
        }
        String fileName = null;
        fileName = "m" + new Date().getTime() + ".txt";
        File f = new File("./out/completed/" + (!folderName.isEmpty() ? folderName + "/" : "") + fileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        writer.write(header + "\n");
        writer.write(p);
        writer.write("\n" + footer);
        writer.close();
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