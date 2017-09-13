package com.github.heuberg.myconvert;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

/**
 * Class for importing an XML conversion definition.
 */
public class Importer {

    /**
     * Import a conversion definition from a URL.
     * @param urlString the URL pointing to an xml file (http://..../....xml)
     * @param context the current context to retrieve the storage directory
     * @throws IOException in case of any read/write error
     */
    public static void importDefinition(String urlString, Context context) throws IOException {
        URL url = new URL(urlString);
        URLConnection urlConnection = url.openConnection();
        urlConnection.connect();
        InputStream inputStream = new BufferedInputStream(url.openStream(), 8192);
        long now = (new Date()).getTime();
        OutputStream outputStream = new FileOutputStream(Converter.getStorageDirectory(context) + File.separator + "import-"+now+".xml");
        copy(inputStream, outputStream);
    }

    /**
     * Import a conversion definition from a URL.
     * @param importFileUri the Uri pointing to an xml file (file:///..../....xml)
     * @param context the current context to retrieve the storage directory
     * @throws IOException in case of any read/write error
     */
    public static void importDefinition(Uri importFileUri, Context context) throws IOException {
        File importFile = new File(importFileUri.getPath());
        if (importFile.exists()) {
            long now = (new Date()).getTime();
            File outputFile = new File(Converter.getStorageDirectory(context) + File.separator + "import-"+now+".xml");
            copy(importFile, outputFile);
        }
    }

    /**
     * Copy the given file src to the given file dst
     * @param src the source file
     * @param dst the destination file
     * @throws IOException in case of any read/write error
     */
    public static void copy(File src, File dst) throws IOException {
        FileInputStream in = new FileInputStream(src);
        try {
            FileOutputStream out = new FileOutputStream(dst);
            try {
                copy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
    /**
     * Copy the given InputStream to the given OutputStream
     * @param in the source file
     * @param out the destination file
     * @throws IOException in case of any read/write error
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
    }

}
