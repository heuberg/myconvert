package com.github.heuberg.myconvert;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


/**
 * Business logic for conversion editing.
 */
public class Editor {

    final Context context;

    final Map<String, String> contentsByFilename = new HashMap<>();

    Editor(Context context) {
        this.context = context;
    }

    /**
     * Loop through the xml-files in the app's external storage directory and read in their
     * contents.
     * this.contentsByFilename gets filled.
     */
    void loadConversions() {
        File convDefDir = Converter.getStorageDirectory(context);
        File convDefFiles[] = convDefDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".xml");
            }
        });
        if (convDefFiles != null && convDefFiles.length > 0) {
            Arrays.sort(convDefFiles); //alphabetically
            for (File convDefFile : convDefFiles) {
                if (!convDefFile.isDirectory()) {
                    try {
                        String contents = (new Scanner(convDefFile)).useDelimiter("\\Z").next();
                        this.contentsByFilename.put(convDefFile.getName(), contents);
                    } catch (IOException e) {
                        //This single file could not be read.
                        if (Utils.DEBUG) Log.d("Editor", "Error while reading contents of file " + convDefFile.getAbsolutePath() + "!");
                    }
                }
            }
        }
    }

    /**
     * Returns the number of conversion definition files.
     * @return the number of files.
     */
    int getNumberOfFiles() {
        return contentsByFilename.size();
    }
    /**
     * Returns an array of all conversion definition files' names.
     * @return an array of all filenames sorted by filename.
     */
    String[] getFilenamesArray() {
        String[] filenamesArray = contentsByFilename.keySet().toArray(new String[0]); Arrays.sort(filenamesArray);
        return filenamesArray;
    }

    /**
     * Returns the content of the conversion definition file.
     * @param convFilename the filename of the conversion definition file to query.
     * @return the content of the conversion definition file.
     */
    String getXmlContent(String convFilename) {
        return this.contentsByFilename.get(convFilename);
    }

    /**
     * Save the given XML contents to the given conversion's xml file.
     * @param convFilename the selected conversion filename.
     * @param xmlContent the XML contents to save.
     */
    void saveConversion(String convFilename, String xmlContent) throws IOException {
        String path = Converter.getStorageDirectory(context).getAbsolutePath();
        File xmlFile = new File(path + File.separator + convFilename);
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(xmlFile);
            writer.print(xmlContent);
            writer.close();
            contentsByFilename.remove(convFilename);
            contentsByFilename.put(convFilename, xmlContent);
        } catch (IOException e) {
            if (Utils.DEBUG) Log.d("EditActivity", "Save failed: " + xmlFile.getAbsolutePath());
            throw e;
        } finally {
            if (writer != null) writer.close();
        }
    }

    /**
     * Rename the given conversion's xml file to the given new filename.
     * @param convFilename the selected conversion filename.
     * @param newFilename the new filename.
     * @throws IOException in case renaming failed.
     */
    void renameConversion(String convFilename, String newFilename) throws IOException {
        String path = Converter.getStorageDirectory(context).getAbsolutePath();
        File xmlFile = new File(path + File.separator + convFilename);
        File newFile = new File(path + File.separator + newFilename);
        if (newFile.exists() || !xmlFile.renameTo(newFile)) {
            if (Utils.DEBUG) Log.d("EditActivity", "Rename failed: " + xmlFile.getAbsolutePath() + " -> " + newFile.getAbsolutePath());
            throw new IOException("Rename failed: " + xmlFile.getAbsolutePath() + " -> " + newFile.getAbsolutePath());
        } else {
            String contents = contentsByFilename.remove(convFilename);
            contentsByFilename.put(newFilename, contents); //all changes within the edittext are lost!!
        }
    }

    /**
     * Delete the given conversion's xml file.
     * @param convFilename the conversion filename.
     * @throws IOException in case deletion failed.
     */
    void deleteConversion(String convFilename) throws IOException {
        String path = Converter.getStorageDirectory(context).getAbsolutePath();
        File xmlFile = new File(path + File.separator + convFilename);
        if (!xmlFile.exists() || !xmlFile.delete()) {
            if (Utils.DEBUG) Log.d("EditActivity", "Delete failed: " + xmlFile.getAbsolutePath());
            throw new IOException("Delete failed: " + xmlFile.getAbsolutePath());
        } else {
            contentsByFilename.remove(convFilename);
        }
    }
}
