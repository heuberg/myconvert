package com.github.heuberg.myconvert;

import android.util.Log;
import android.util.Xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Class representing the definition of a conversion.
 * Version 1: name, category, variables (names and default values), results (names and formulas).
 */
public class Conversion {

    static final String VERSION_1 = "1";
    static final String DEFAULT_CATEGORY = "Default";
    static final double DEFAULT_VARIABLE_VALUE = 0;

    private static final String XML_NODE_ROOT = "myconvert";        //<myconvert version="1">
    private static final String XML_ATTR_VERSION = "version";

    private static final String XML_NODE_DEFINITION = "def";        //<def name="C->K" cat="Temp">
    private static final String XML_ATTR_DEF_NAME = "name";
    private static final String XML_ATTR_DEF_CATEGORY = "cat";

    private static final String XML_NODE_VARIABLE = "var";          //<var name="°C" def="20.0" />
    private static final String XML_ATTR_VAR_NAME = "name";
    private static final String XML_ATTR_VAR_DEFAULT = "def";

    private static final String XML_NODE_RESULT = "res";            //<res name="K" formula="v1+273.15" />
    private static final String XML_ATTR_RES_NAME = "name";
    private static final String XML_ATTR_RES_FORMULA = "formula";

    private static final String XML_NODE_REVARIABLE = "revar";      //<revar name="K" def="293.15" />
    private static final String XML_ATTR_REVAR_NAME = "name";       //  rewritten formula's variable
    private static final String XML_ATTR_REVAR_DEFAULT = "def";

    private static final String XML_NODE_RERESULT = "reres";        //<reres name="°C" formula="v1-273.15" />
    private static final String XML_ATTR_RERES_NAME = "name";       //  rewritten formula's result
    private static final String XML_ATTR_RERES_FORMULA = "formula";

    private final String version;
    private final String name; //UNIQUE !!!
    private final String category;
    private final List<ConversionVar> variables;
    private final List<ConversionRes> results;
    private final List<ConversionVar> reVariables;
    private final List<ConversionRes> reResults;

    public Conversion(String name) {
        this(VERSION_1, name, DEFAULT_CATEGORY);
    }
    public Conversion(String name, String category) {
        this(VERSION_1, name, category);
    }
    public Conversion(String version, String name, String category) {
        this.version = version;
        this.name = name;
        this.category = category;
        this.variables = new ArrayList<>();
        this.results = new ArrayList<>();
        this.reVariables = new ArrayList<>();
        this.reResults = new ArrayList<>();
    }

    public Conversion(Conversion c, String new_name) {
        this.version = c.version;
        this.name = new_name;
        this.category = c.category;
        this.variables = new ArrayList<>(c.variables);
        this.results = new ArrayList<>(c.results);
        this.reVariables = new ArrayList<>(c.reVariables);
        this.reResults = new ArrayList<>(c.reResults);
    }

    public String getVersion() {
        return version;
    }
    public String getName() {
        return name;
    }
    public String getCategory() {
        return category;
    }
    public List<ConversionVar> getVariables() {
        return variables;
    }
    public List<ConversionRes> getResults() {
        return results;
    }
    public boolean hasRewrittenForm() {
        return reVariables.size() > 0 && reResults.size() > 0;
    }
    public List<ConversionVar> getRewrittenVariables() {
        return reVariables;
    }
    public List<ConversionRes> getRewrittenResults() {
        return reResults;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        //equal if names are equal
        return obj != null &&
                obj instanceof Conversion &&
                this.name.equals(((Conversion)obj).name);
    }

    ///////////////////////////////////////////////////////////////////////////////
//  STATIC METHODS

    /**
     * Creates a List of Conversion objects from an xml file, given by its path. If the file cannot be
     * read or is not in the right format an empty List is returned.
     * @param absolutePathToXml the absolute path to the xml file containing the conversion definition(s)
     * @return a List of Conversion objects or an empty List in case of an error.
     */
    public static List<Conversion> multipleFromXml(String absolutePathToXml) {
        List<Conversion> createdObjects = new ArrayList<>();
        try {
            createdObjects = multipleFromXml(new FileInputStream(new File(absolutePathToXml)));
        } catch (FileNotFoundException e) {
            if (Utils.DEBUG) Log.d("Conversion", "Error while reading xml files. The file " + absolutePathToXml + " does not exist.");
            e.printStackTrace();
        }
        return createdObjects;
    }

    /**
     * Creates a List of Conversion objects from an xml input stream. If the resource/file cannot be
     * read or is not in the right format an empty List is returned.
     * @param inputStream input stream to the ressource (e.g. xml file) containing the conversion definition(s)
     * @return a List of Conversion objects or an empty List in case of an error.
     */
    public static List<Conversion> multipleFromXml(InputStream inputStream) {
        List<Conversion> createdObjects = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            Element root = document.getDocumentElement();
            String version = root.getAttribute(XML_ATTR_VERSION);
            if ("".equals(version) || VERSION_1.equals(version)) {
                version = VERSION_1;
                NodeList defs = root.getElementsByTagName(XML_NODE_DEFINITION);
                for (int i = 0; i < defs.getLength(); i++) {
                    Element def = (Element) defs.item(i);

                    String name = def.getAttribute(XML_ATTR_DEF_NAME);
                    if ("".equals(name)) continue; //there has to be a name for every conversion!

                    String category = def.getAttribute(XML_ATTR_DEF_CATEGORY);
                    if ("".equals(category)) category = DEFAULT_CATEGORY; //category is optional

                    Conversion c = new Conversion(version, name, category);
                    //VARIABLES:
                    NodeList vars = def.getElementsByTagName(XML_NODE_VARIABLE);
                    for (int v = 0; v < vars.getLength(); v++) {
                        Element var = (Element) vars.item(v);
                        String varName = var.getAttribute(XML_ATTR_VAR_NAME);
                        if ("".equals(varName)) continue; //there has to be a name for every variable!
                        String varDefaultStr = var.getAttribute(XML_ATTR_VAR_DEFAULT);
                        double varDefault = DEFAULT_VARIABLE_VALUE;
                        try {
                            varDefault = Double.parseDouble(varDefaultStr);
                        } catch (NumberFormatException e) { /*error? ignore value.*/ }
                        c.getVariables().add(new ConversionVar(varName, varDefault));
                    }
                    //RESULTS:
                    NodeList ress = def.getElementsByTagName(XML_NODE_RESULT);
                    for (int r = 0; r < ress.getLength(); r++) {
                        Element res = (Element) ress.item(r);
                        String resName = res.getAttribute(XML_ATTR_RES_NAME);
                        if ("".equals(resName)) continue; //there has to be a name for every result!
                        String resFormula = res.getAttribute(XML_ATTR_RES_FORMULA);
                        if ("".equals(resFormula)) continue; //there has to be a formula for every result!
                        c.getResults().add(new ConversionRes(resName, resFormula));
                    }

                    //(OPTIONAL) REWRITTEN FORMULA (variables:)
                    NodeList reVars = def.getElementsByTagName(XML_NODE_REVARIABLE);
                    for (int v = 0; v < reVars.getLength(); v++) {
                        Element reVar = (Element) reVars.item(v);
                        String reVarName = reVar.getAttribute(XML_ATTR_REVAR_NAME);
                        if ("".equals(reVarName)) continue; //there has to be a name for every rewritten variable!
                        String reVarDefaultStr = reVar.getAttribute(XML_ATTR_REVAR_DEFAULT);
                        double reVarDefault = DEFAULT_VARIABLE_VALUE;
                        try {
                            reVarDefault = Double.parseDouble(reVarDefaultStr);
                        } catch (NumberFormatException e) { /*error? ignore value.*/ }
                        c.getRewrittenVariables().add(new ConversionVar(reVarName, reVarDefault));
                    }
                    //(OPTIONAL) REWRITTEN FORMULA (results:)
                    NodeList reRess = def.getElementsByTagName(XML_NODE_RERESULT);
                    for (int r = 0; r < reRess.getLength(); r++) {
                        Element reRes = (Element) reRess.item(r);
                        String reResName = reRes.getAttribute(XML_ATTR_RERES_NAME);
                        if ("".equals(reResName)) continue; //there has to be a name for every rewritten result!
                        String reResFormula = reRes.getAttribute(XML_ATTR_RERES_FORMULA);
                        if ("".equals(reResFormula)) continue; //there has to be a formula for every rewritten result!
                        c.getRewrittenResults().add(new ConversionRes(reResName, reResFormula));
                    }

                    if (c.getRewrittenVariables().size() == 0 || c.getRewrittenResults().size() == 0) {
                        c.getRewrittenVariables().clear();
                        c.getRewrittenResults().clear();
                    }

                    if (c.getVariables().size() > 0 && c.getResults().size() > 0) {
                        //there has to be at least one variable and one result
                        createdObjects.add(c);
                    }
                }
            } //else if (VERSION_2.equals(version) ...

        } catch (ParserConfigurationException | SAXException | IOException e) {
            if (Utils.DEBUG) Log.d("Conversion", "Error while reading at least 1 xml file..." + e.toString());
        }
        return createdObjects;
    }

    /**
     * Store the given conversion object as an xml-file within the app's external storage directory.
     * @param conversion The conversion object to store as XML.
     * @param absolutePathToXml The absolute path to the xml-file.
     */
    public static void toXml(Conversion conversion, String absolutePathToXml) throws IOException {
        if (conversion == null) {
            if (Utils.DEBUG) Log.d("Conversion", "The given conversion object is NULL!");
            return;
        }
        List<Conversion> cList = new ArrayList<>();
        cList.add(conversion);
        multipleToXml(cList, absolutePathToXml);
    }

    /**
     * Store the given conversion objects as one xml-file within the app's external storage directory.
     * @param conversions The conversion objects to store as XML.
     * @param absolutePathToXml The absolute path to the xml-file.
     */
    public static void multipleToXml(List<Conversion> conversions, String absolutePathToXml) throws IOException {
        if (conversions == null) {
            if (Utils.DEBUG) Log.d("Conversion", "The given conversions list is NULL!");
            return;
        }
        if (conversions.size() < 1) {
            if (Utils.DEBUG) Log.d("Conversion", "Warning: The given conversions list is empty!");
        }

        XmlSerializer serializer = Xml.newSerializer();
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

        File xmlFile = new File(absolutePathToXml);
        if (!xmlFile.getParentFile().exists() && !xmlFile.getParentFile().mkdirs())
            throw new IOException("Could not create directory structure to " + absolutePathToXml + "!");

        FileOutputStream fos = new FileOutputStream(xmlFile);
        serializer.setOutput(fos, "UTF-8");
        serializer.startDocument("UTF-8", true);

        serializer.startTag("", XML_NODE_ROOT);
        serializer.attribute("", XML_ATTR_VERSION, VERSION_1);

        for (Conversion c : conversions) {
            serializer.startTag("", XML_NODE_DEFINITION);
            serializer.attribute("", XML_ATTR_DEF_NAME, c.getName());
            serializer.attribute("", XML_ATTR_DEF_CATEGORY, c.getCategory());
            for (ConversionVar v : c.getVariables()) {
                serializer.startTag("", XML_NODE_VARIABLE);
                serializer.attribute("", XML_ATTR_VAR_NAME, v.getName());
                serializer.attribute("", XML_ATTR_VAR_DEFAULT, String.valueOf(v.getDefaultVal()));
                serializer.endTag("", XML_NODE_VARIABLE);
            }
            for (ConversionRes r : c.getResults()) {
                serializer.startTag("", XML_NODE_RESULT);
                serializer.attribute("", XML_ATTR_RES_NAME, r.getName());
                serializer.attribute("", XML_ATTR_RES_FORMULA, r.getFormula());
                serializer.endTag("", XML_NODE_RESULT);
            }

            if (c.hasRewrittenForm()) {
                for (ConversionVar v : c.getRewrittenVariables()) {
                    serializer.startTag("", XML_NODE_REVARIABLE);
                    serializer.attribute("", XML_ATTR_REVAR_NAME, v.getName());
                    serializer.attribute("", XML_ATTR_REVAR_DEFAULT, String.valueOf(v.getDefaultVal()));
                    serializer.endTag("", XML_NODE_REVARIABLE);
                }
                for (ConversionRes r : c.getRewrittenResults()) {
                    serializer.startTag("", XML_NODE_RERESULT);
                    serializer.attribute("", XML_ATTR_RERES_NAME, r.getName());
                    serializer.attribute("", XML_ATTR_RERES_FORMULA, r.getFormula());
                    serializer.endTag("", XML_NODE_RERESULT);
                }
            }

            serializer.endTag("", XML_NODE_DEFINITION);
        }

        serializer.endTag("", XML_NODE_ROOT);
        serializer.endDocument();
        serializer.flush();
        fos.close();
    }
}
