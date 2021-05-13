import java.util.ArrayList;
import java.util.HashMap;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.Flags;
import java.util.logging.Logger;

import javax.mail.Part;

import java.io.*;
import java.util.*;

public class Task {
    private HashMap<String, String> createdFiles = new HashMap<>();
    private static Logger logger;
    private Flags flags = new Flags();

    public Task(Logger logger) {
        Task.setLogger(logger);
    }

    private static void setLogger(Logger logger) {
        Task.logger = logger;
    }

    public ArrayList<String> getCreatedFiles() {
        ArrayList<String> copy = new ArrayList<>();
        for (String fileName : createdFiles.keySet()) {
            copy.add(fileName);
        }
        return copy;
    }

    public ArrayList<String> getPDFFiles() {
        ArrayList<String> copy = new ArrayList<>();
        Set<Map.Entry<String, String>> entries = createdFiles.entrySet();

        for (Map.Entry<String, String> entry : entries) {
            if (entry.getValue().equalsIgnoreCase("PDF")) {
                copy.add(entry.getKey());
            }
        }

        return copy;

    }

    public ArrayList<String> getTextFiles() {
        ArrayList<String> copy = new ArrayList<>();
        Set<Map.Entry<String, String>> entries = createdFiles.entrySet();

        for (Map.Entry<String, String> entry : entries) {
            if (entry.getValue().equalsIgnoreCase("TEXT")) {
                copy.add(entry.getKey());
            }
        }

        return copy;

    }

    public ArrayList<String> getHTMLFiles() {
        ArrayList<String> copy = new ArrayList<>();
        Set<Map.Entry<String, String>> entries = createdFiles.entrySet();

        for (Map.Entry<String, String> entry : entries) {
            if (entry.getValue().equalsIgnoreCase("HTML")) {
                copy.add(entry.getKey());
            }
        }

        return copy;

    }

    public boolean hasCreatedHTMLFile() {
        return flags.contains("HTML_FILE_CREATED");
    }

    public boolean hasCreatedTextFile() {
        return flags.contains("TEXT_FILE_CREATED");
    }

    public boolean hasCreatedPDFFile() {
        return flags.contains("PDF_FILE_CREATED");
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
    public void saveMessageParts(Part main, String header, String footer, String folderName)
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
                        writeText(part, "", "", folderName);
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
    public String writePDF(MimeBodyPart p, String folderName) throws MessagingException, IOException {
        if (p == null || folderName == null) {
            throw new IllegalArgumentException("No null parameters allowed");
        }
        String fileName = null;
        if (p.isMimeType("application/pdf")) {
            flags.add("PDF_FILE_CREATED");
            fileName = p.getFileName().replace(".pdf", "") + "_m" + new Date().getTime() + ".pdf";
            createdFiles.put(fileName, "PDF");
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
    public String writeHTML(MimeBodyPart p, String folderName) throws MessagingException, IOException {
        if (p == null || folderName == null) {
            throw new IllegalArgumentException("No null parameters allowed");
        }
        if (p.isMimeType("text/html") || p.isMimeType("image/png") || p.isMimeType("image/jpeg")) {
            flags.add("HTML_FILE_CREATED");
            String fileName = (p.getFileName() == null ? "m" + new Date().getTime() : p.getFileName());

            if (p.isMimeType("text/html"))
                fileName += ".htm";

            createdFiles.put(fileName, "HTML");
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
    public String writeText(MimeBodyPart p, String header, String footer, String folderName)
            throws MessagingException, IOException {
        if (p == null || header == null || footer == null || folderName == null) {
            throw new IllegalArgumentException("No null parameters allowed");
        }
        String fileName = null;
        if (p.isMimeType("text/plain")) {
            flags.add("TEXT_FILE_CREATED");
            fileName = "m" + new Date().getTime() + ".txt";
            createdFiles.put(fileName, "TEXT");
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
    public String writeText(String p, String header, String footer, String folderName)
            throws MessagingException, IOException {
        if (p == null || header == null || footer == null || folderName == null) {
            throw new IllegalArgumentException("No null parameters allowed");
        }
        String fileName = null;
        flags.add("TEXT_FILE_CREATED");
        fileName = "m" + new Date().getTime() + ".txt";
        createdFiles.put(fileName, "TEXT");
        File f = new File("./out/completed/" + (!folderName.isEmpty() ? folderName + "/" : "") + fileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        writer.write(header + "\n");
        writer.write(p);
        writer.write("\n" + footer);
        writer.close();
        return fileName;
    }

        /**
     * Write email text to a file. The email text is in the form of a String. 
     * 
     * @param p          The body.
     * @param fileName   The name of the created file. If empty, it will use a default name.
     * @param folderName the save folder location in /out/completed
     * @return the name of the created file, or null if not created
     */
    public String writeText(String p, String fileName, String folderName)
            throws MessagingException, IOException {
        if (p == null || folderName == null) {
            throw new IllegalArgumentException("No null parameters allowed");
        }
        String aFileName = null;
        flags.add("TEXT_FILE_CREATED");
        aFileName = (!fileName.isEmpty() ? fileName : "m" + new Date().getTime()) + ".txt";
        createdFiles.put(aFileName, "TEXT");
        File f = new File("./out/completed/" + (!folderName.isEmpty() ? folderName + "/" : "") + aFileName);
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        writer.write(p);
        writer.close();
        return aFileName;
    }
}
