package com.github.heuberg.myconvert;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Business logic for conversions.
 */
public class Converter {

    private static final String FOLDER_NAME_BACKUP = "backup";

    private final Context context;

    private final List<Conversion> conversions = new ArrayList<>();
    private final Set<String> categories = new HashSet<>();
    private final Map<String, Conversion> conversionByName = new HashMap<>();
    private final Map<String, List<Conversion>> conversionByCat = new HashMap<>();

    Converter(Context context) {
        this.context = context;
    }

    /**
     * Loop through the xml-files in the app's external storage directory and read in all
     * conversion definitions.
     * this.conversions gets filled.
     * this.conversionByName gets filled.
     */
    void loadConversionDefinitionsFromXml() {
        this.conversions.clear();
        this.conversionByName.clear();
        this.conversionByCat.clear();
        this.categories.clear();

        File convDefDir = getStorageDirectory(context);
        File convDefFiles[] = convDefDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".xml");
            }
        });
        if (convDefFiles != null && convDefFiles.length > 0) {
            Arrays.sort(convDefFiles); //alphabetically
            for (File convDefFile : convDefFiles) {
                if (!convDefFile.isDirectory()) {
                    List<Conversion> convs = Conversion.multipleFromXml(convDefFile.getAbsolutePath());
                    if (convs != null) this.conversions.addAll(convs);
                }
            }
        }

        //(if convDefDir isEmpty or the xml-files could not be read: read asset file!)
        if (this.conversions.size() == 0) {
            Toast.makeText(context, context.getResources().getString(R.string.loading_nodefs, convDefDir), Toast.LENGTH_LONG).show();
            try {
                List<Conversion> ccc = Conversion.multipleFromXml(context.getAssets().open("test-conversion.xml"));
                this.conversions.addAll(ccc);
            } catch (IOException e) {
                Toast.makeText(context, "FATAL ERROR! Please re-install the app.", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

        for (Conversion c: this.conversions) {
            this.conversionByName.put(c.getName(), c);
            if (!this.conversionByCat.containsKey(c.getCategory())) this.conversionByCat.put(c.getCategory(), new ArrayList<Conversion>());
            this.conversionByCat.get(c.getCategory()).add(c);
            this.categories.add(c.getCategory());
        }
    }

    /**
     * Returns the total number of conversions.
     * @return the total number of conversions.
     */
    int getNumberOfConversions() {
        return this.conversions.size();
    }

    /**
     * Returns the number of conversions within the given category.
     * @param category the category for which to query the number of conversions.
     * @return the number of conversions within the given category.
     */
    int getNumberOfConversions(String category) {
        return categoryExists(category) ? conversionByCat.get(category).size() : 0;
    }

    /**
     * Returns an array of all categories (= category names).
     * @return an array of all categories.
     */
    String[] getCategoriesArray() {
        String[] categoriesArray = categories.toArray(new String[0]); Arrays.sort(categoriesArray);
        return categoriesArray;
    }

    /**
     * Returns whether the given category (name) exists.
     * @param category the category (name)
     * @return true if the given category exists, false otherwise.
     */
    boolean categoryExists(String category) {
        return conversionByCat.containsKey(category);
    }

    /**
     * Returns a list of all conversions within the given category.
     * @param category category for which to query the conversions.
     * @return list of all conversions within the given category.
     */
    List<Conversion> getConversions(String category) {
        return conversionByCat.get(category);
    }

    /**
     * Returns an array of all conversion names within the given category.
     * @param category category for which to query the conversion names.
     * @return array of all conversion names within the given category.
     */
    String[] getConversionNamesArray(String category) {
        List<Conversion> convs = getConversions(category);
        if (convs == null || convs.size() == 0) return new String[0];

        String[] conversionNamesArray = new String[convs.size()];
        for (int i = 0; i < conversionNamesArray.length; i++)
            conversionNamesArray[i] = convs.get(i).getName();

        return conversionNamesArray;
    }

    /**
     * Returns whether the given conversion name exists within the given category. If the given
     * category does not even exist, false is returned.
     * @param category category for which to query whether the conversion name exists.
     * @param conversionName name of the conversion
     * @return true if the given conversion name exists within the given category, false otherwise.
     */
    boolean conversionExists(String category, String conversionName) {
        return conversionByCat.containsKey(category) && conversionByName.containsKey(conversionName);
    }

    /**
     * Returns the conversion object for a given conversion name.
     * @param conversionName the name of the conversion requested
     * @return conversion object for a given conversion name.
     */
    Conversion getConversionByName(String conversionName) {
        return conversionByName.get(conversionName);
    }

    /**
     * Evaluate a given formula using the given variable values.
     * Formulas may contain the variable names v1,v2,...,vN (N being variables.length)
     * @param formula the formula to evaluate.
     * @param variables the variable values to use as substitutes for the variable names v1,...,vN
     * @return the result value. Throws an IllegalArgumentException if something goes wrong with evaluation.
     */
    double evalFormula(String formula, double[] variables) {
        ExpressionBuilder exprBuilder = new ExpressionBuilder(formula);
        for (int i = 1; i <= variables.length; i++) {
            exprBuilder.variable("v"+i);
        }
        Expression expr = exprBuilder.build();
        for (int i = 1; i <= variables.length; i++) {
            expr.setVariable("v"+i, variables[i-1]);
        }
        return expr.evaluate();
    }

    /**
     * Duplicates the conversion given by its name.
     * @param conversionName the name of the conversion to duplicate.
     * @return the new conversion object.
     * @throws IOException in case the conversion's definition file could not be duplicated
     */
    Conversion duplicateConversion(String conversionName) throws IOException {
        Conversion convToDuplicate = conversionByName.get(conversionName);

        File convDefDir = getStorageDirectory(context);
        long now = (new Date()).getTime();
        String newConvFilePath = convDefDir.getAbsolutePath() + File.separator + "conv-" + now + ".xml";

        Conversion newConv = new Conversion(convToDuplicate, convToDuplicate.getName() + "-" + now);
        Conversion.toXml(newConv, newConvFilePath);

        conversions.add(newConv);
        conversionByName.put(newConv.getName(), newConv);
        conversionByCat.get(newConv.getCategory()).add(newConv);

        return newConv;
    }

    /**
     * Create a temporary backup file containing all conversions' definitions.
     * @return the File object pointing to the created backup file.
     * @throws IOException in case the backup file could not be created.
     */
    File createTempBackupFile() throws IOException {
        long now = (new Date()).getTime();
        String pathToBackup = getStorageDirectory(context).getAbsolutePath() + File.separator + FOLDER_NAME_BACKUP + File.separator + now+".xml";
        Conversion.multipleToXml(conversions, pathToBackup);
        return new File(pathToBackup);
    }

    /**
     * Clears the backup directory. All temporary backup files are deleted.
     */
    void deleteTempBackupFiles() {
        File backupDir = new File(getStorageDirectory(context), FOLDER_NAME_BACKUP);
        if (backupDir.isDirectory())
            for (File f : backupDir.listFiles())
                f.delete();
    }

///////////////////////////////////////////////////////////////////////////////
// STATIC HELPER METHODS

    /**
     * Retrieve the absolute path to the directory where conversion definitions are stored.
     * @param context current context.
     * @return absolute path to the conversion definitions' storage directory.
     */
    public static File getStorageDirectory(Context context) {
        return context.getExternalFilesDir(null);
    }

    /**
     *  Checks if external storage is available for read and write.
     */
    public static boolean isStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     *  Checks if external storage is available to at least read.
     */
    public static boolean isStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
