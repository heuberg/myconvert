package com.github.heuberg.myconvert;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main app activity.
 */
public class MainActivity extends Activity {

    private static final String PREF_KEY_LATEST_CATEGORY = "latest_category";
    private static final String PREF_KEY_LATEST_CONVERSION = "latest_conversion";
    private static final int INTENT_CODE_SENDBACKUP = 24357;
    private static final int INTENT_CODE_EDITACTIVITY = 1237;

    private Context context;
    private Converter converter;
    private boolean isFormulaRewritten = false;

///////////////////////////////////////////////////////////////////////////////
//  UI elements (views)
    private Spinner convSpinner;
    private Spinner catSpinner;
    private Button convertBtn;
    private Button rewriteBtn;
    private LinearLayout layoutVars;
    private final List<EditText> variableInputViews = new ArrayList<>();
    private LinearLayout layoutRess;
    private final List<TextView> resultOutputViews = new ArrayList<>();

///////////////////////////////////////////////////////////////////////////////
//  LISTENERS
    private final AdapterView.OnItemSelectedListener convSpinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            String selectedCategory = getSelectedCategory();
            String selectedConversion = getSelectedConversion();
            createUIforConversion(selectedConversion);
            setStringPreferenceValue(PREF_KEY_LATEST_CATEGORY, selectedCategory);     //remember the selected category
            setStringPreferenceValue(PREF_KEY_LATEST_CONVERSION, selectedConversion); //remember the selected conversion
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) { /*nothing to do*/ }
    };
    private final AdapterView.OnItemSelectedListener catSpinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            setConvSpinnerItems();
            String selectedConversion = getSelectedConversion();
            createUIforConversion(selectedConversion);
        }
        @Override
        public void onNothingSelected(AdapterView<?> adapterView) { /*nothing to do*/}
    };

///////////////////////////////////////////////////////////////////////////////
//  OVERRIDDEN METHODS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        converter = new Converter(context);
        convSpinner = this.findViewById(R.id.spinner_conv);
        catSpinner = this.findViewById(R.id.spinner_cat);
        convertBtn = this.findViewById(R.id.button_convert);
        rewriteBtn = this.findViewById(R.id.button_rewrite);
        layoutVars = this.findViewById(R.id.layout_vars);
        layoutRess = this.findViewById(R.id.layout_ress);

        if (!Converter.isStorageReadable()) {
            Toast.makeText(context, R.string.loading_noextstorage, Toast.LENGTH_LONG).show();
            return;
        }

        converter.loadConversionDefinitionsFromXml();
        initSpinners();
        initButtons();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_duplicate:
                duplicateSelectedConversion(this.getSelectedConversion());
                return true;
            case R.id.menu_editmode:
                Intent intent = new Intent(this, EditActivity.class);
                startActivityForResult(intent, INTENT_CODE_EDITACTIVITY);
                return true;
            case R.id.menu_import:
                //TODO show import dialog
                return true;
            case R.id.menu_export:
                sendBackup();
                return true;
            case R.id.menu_help:
                showHelpDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //called when focus is back on this activity (from EditActivity or SendBackup)
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_CODE_EDITACTIVITY) {
            //back from EditActivity
            converter.loadConversionDefinitionsFromXml();
            initSpinners();
        } else if (requestCode == INTENT_CODE_SENDBACKUP) {
            //back from sending a backup -> delete backup file
            converter.deleteTempBackupFiles();
        }
    }

///////////////////////////////////////////////////////////////////////////////
//  PRIVATE/PROTECTED METHODS

    /**
     * Initialize the convSpinner and catSpinner showing the possible conversions/categories.
     * The convSpinner gets filled with elements, i.e. conversion names.
     * The catSpinner gets filled with elements, i.e. categories.
     */
    private void initSpinners() {
        if (converter.getNumberOfConversions() > 0) {
            setCatSpinnerItems();
            catSpinner.setOnItemSelectedListener(catSpinnerListener);

            setConvSpinnerItems();
            convSpinner.setOnItemSelectedListener(convSpinnerListener);
        } else {
            if (Utils.DEBUG) Log.d("MainActivity", "Huh! No conversions by name found!?");
        }
    }

    private void setCatSpinnerItems() {
        //TODO see https://android-er.blogspot.co.at/2010/12/custom-arrayadapter-for-spinner-with.html for custom convSpinner design
        String[] categoriesArray = converter.getCategoriesArray();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item, categoriesArray);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        catSpinner.setAdapter(adapter);

        String lastUsedCategory = getStringPreferenceValue(PREF_KEY_LATEST_CATEGORY, "");
        if (!"".equals(lastUsedCategory) && converter.categoryExists(lastUsedCategory))
            catSpinner.setSelection(((ArrayAdapter)catSpinner.getAdapter()).getPosition(lastUsedCategory));
    }

    private void setConvSpinnerItems() {
        String selectedCategory = getSelectedCategory();
        if (!converter.categoryExists(selectedCategory)) return;

        String lastUsedConversion = getStringPreferenceValue(PREF_KEY_LATEST_CONVERSION, "");

        String[] conversionNamesArray = converter.getConversionNamesArray(selectedCategory);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item, conversionNamesArray);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        convSpinner.setAdapter(adapter);

        if (!"".equals(lastUsedConversion) && converter.conversionExists(selectedCategory, lastUsedConversion))
            convSpinner.setSelection(((ArrayAdapter)convSpinner.getAdapter()).getPosition(lastUsedConversion));
    }

    /**
     * Initialize the convert button and the formular rewrite button.
     * Sets the OnClickListeners.
     */
    private void initButtons() {
        convertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doConversion();
            }
        });
        rewriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rewriteFormula();
            }
        });
    }

    /**
     * Remove the current conversion's UI elements and create new UI elements for the given
     * (selected) conversion (given by its name). Creates all variables' and results' views.
     * Also clears and refills this.variableInputViews and this.resultOutputViews.
     * @param selectedConversion the conversion (name) to create UI elements for
     */
    private void createUIforConversion(String selectedConversion) {
        //remove old conversion's UI elements and create new ones
        layoutVars.removeAllViews();
        variableInputViews.clear();
        layoutRess.removeAllViews();
        resultOutputViews.clear();

        Conversion conv = converter.getConversionByName(selectedConversion);
        if (conv == null) {
            Toast.makeText(context, getResources().getString(R.string.error_convnotexisting), Toast.LENGTH_SHORT).show();
            return;
        }

        if (conv.hasRewrittenForm())
            rewriteBtn.setVisibility(View.VISIBLE);
        else
            rewriteBtn.setVisibility(View.GONE);

        List<ConversionVar> variables = conv.getVariables();
        List<ConversionRes> results = conv.getResults();
        if (isFormulaRewritten && conv.hasRewrittenForm()) {
            variables = conv.getRewrittenVariables();
            results = conv.getRewrittenResults();
        }

        for (ConversionVar var : variables) {
            LinearLayout layoutVar = new LinearLayout(context);
            layoutVar.setOrientation(LinearLayout.HORIZONTAL);
            layoutVar.setBackgroundDrawable(getResources().getDrawable(R.drawable.border_background));
            TextView varNameView = new TextView(context);
            varNameView.setText(var.getName());
            varNameView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            layoutVar.addView(varNameView);

            EditText varValueView = new EditText(context);
            varValueView.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            varValueView.setText(String.valueOf(var.getDefaultVal())); //TODO format [?]
            varValueView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            varValueView.setGravity(Gravity.END);
            variableInputViews.add(varValueView);
            layoutVar.addView(varValueView);
            layoutVars.addView(layoutVar);
        }

        for (ConversionRes res : results) {
            LinearLayout layoutRes = new LinearLayout(context);
            layoutRes.setOrientation(LinearLayout.HORIZONTAL);
            layoutRes.setBackgroundDrawable(getResources().getDrawable(R.drawable.border_background));
            TextView resNameView = new TextView(context);
            resNameView.setText(res.getName());
            resNameView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            layoutRes.addView(resNameView);

            TextView resValueView = new TextView(context);
            resValueView.setText(res.getFormula());
            resValueView.setTag(res.getFormula());  //set the formula as tag object to be used by doConversion()
            resValueView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            resValueView.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
            resValueView.setGravity(Gravity.END);
            resValueView.setPadding(5, 7, 5, 7);
            resultOutputViews.add(resValueView);
            layoutRes.addView(resValueView);
            layoutRess.addView(layoutRes);
        }

        doConversion(); //fill the result fields
    }

    /**
     * Do the conversion using the current values of the views stored in this.variableInputViews
     * and sets the result values to the views stored in this.resultOutputViews.
     */
    private void doConversion() {
        double[] varValues = new double[variableInputViews.size()];
        for (int i = 0; i < varValues.length; i++) {
            double v;
            try {
                v = Double.parseDouble(variableInputViews.get(i).getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(context, getResources().getString(R.string.error_parseinput), Toast.LENGTH_SHORT).show();
                v = Double.NaN;
            }
            varValues[i] = v;
        }
        for (int i = 0; i < resultOutputViews.size(); i++) {
            TextView res = resultOutputViews.get(i);
            try {
                double result = converter.evalFormula((String)res.getTag(), varValues);
                res.setText(String.format("%1$,.2f", result)); //TODO other format?
            } catch (Exception e) {
                res.setText(getResources().getString(R.string.na));
                Toast.makeText(context, getResources().getString(R.string.error_convfailed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Rewrite the formula.
     */
    private void rewriteFormula() {
        isFormulaRewritten = !isFormulaRewritten;
        String selectedConversion = getSelectedConversion();
        createUIforConversion(selectedConversion);
    }

    /**
     * Returns the selected spinner item or "" if no item is selected.
     * @return the selected spinner item or "" if no item is selected.
     */
    private String getSelectedConversion() {
        Object selItem = convSpinner.getSelectedItem();
        return selItem == null ? "" : selItem.toString();
    }
    /**
     * Returns the selected spinner item or "" if no item is selected.
     * @return the selected spinner item or "" if no item is selected.
     */
    private String getSelectedCategory() {
        Object selItem = catSpinner.getSelectedItem();
        return selItem == null ? "" : selItem.toString();
    }

///////////////////////////////////////////////////////////////////////////////
//  MENU METHODS

    /**
     * Duplicate the conversion (given by its name). A new xml-file is created containing the
     * conversion's definition.
     * @param selectedConversion the conversion (name) to duplicate.
     */
    private void duplicateSelectedConversion(String selectedConversion) {
        if (!Converter.isStorageWritable()) {
            Toast.makeText(context, R.string.saving_noextstorage, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            converter.duplicateConversion(selectedConversion);
            setConvSpinnerItems();
            Toast.makeText(context, R.string.duplicate_done, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(context, getResources().getString(R.string.error_duplfailed), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Create a temporary backup file containing all conversions' definitions. Send this backup file
     * via an intent to apps which are able to send mails (type: message/rfc822).
     */
    private void sendBackup() {
        try {
            File backupFile = converter.createTempBackupFile();
            Intent sendFile = new Intent(Intent.ACTION_SEND);
            sendFile.setType("message/rfc822");
            sendFile.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.backup_send_subject));
            sendFile.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.backup_send_msgbody));
            sendFile.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(backupFile));
            startActivityForResult(Intent.createChooser(sendFile, context.getResources().getString(R.string.backup_send_intenttitle)), INTENT_CODE_SENDBACKUP);
        } catch (IOException e) {
            Toast.makeText(context, getResources().getString(R.string.error_backupsendfailed), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Build and show the help dialog.
     */
    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.dialog_help, (ViewGroup) findViewById(R.id.help_layout));
        builder.setTitle(R.string.help_title)
                .setView(layout)
                .setNeutralButton(R.string.about_title, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.showAboutDialog();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    /**
     * Build and show the about dialog.
     */
    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.dialog_about,
                (ViewGroup) findViewById(R.id.about_layout));

        AlertDialog dlg = builder.setTitle(R.string.about_title)
                .setView(layout)
                .setNeutralButton(R.string.help_title, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.showHelpDialog();
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .show();
        //set correct version text
        TextView versionText = dlg.findViewById(R.id.about_version_text);
        versionText.setText(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);
    }

///////////////////////////////////////////////////////////////////////////////
// STATIC HELPER METHODS

    /**
     * Set the given value to the given key in the shared preferences.
     * @param key shared preferences key
     * @param value value to store
     */
    private void setStringPreferenceValue(String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(key, value);
        prefsEditor.apply();
    }

    /**
     * Retrieve the String value of a given shared preferences key.
     * @param key shared preferences key
     * @param defaultValue default value to be returned if no value is stored for the given key.
     * @return the value for the given key.
     */
    private String getStringPreferenceValue(String key, String defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
        return prefs.getString(key, defaultValue);
    }
}
