package com.github.heuberg.myconvert;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;

public class EditActivity extends Activity {

    private Context context;
    private Editor editor;

    private Spinner spinner;
    private Button deleteBtn;
    private Button renameBtn;
    private Button saveBtn;
    private EditText xmlContentTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        this.context = this;
        this.editor = new Editor(context);

        this.spinner = this.findViewById(R.id.spinner_edit);
        this.deleteBtn = this.findViewById(R.id.button_delete);
        this.deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ("".equals(getSelectedFilename())) {
                    Toast.makeText(context, context.getResources().getString(R.string.error_noconversion), Toast.LENGTH_LONG).show();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getResources().getString(R.string.edit_dlg_delete))
                        .setMessage(context.getResources().getString(R.string.edit_dlg_deleteTxt))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                deleteConversion(getSelectedFilename());
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
        this.renameBtn = this.findViewById(R.id.button_rename);
        this.renameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ("".equals(getSelectedFilename())) {
                    Toast.makeText(context, context.getResources().getString(R.string.error_noconversion), Toast.LENGTH_LONG).show();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                final EditText newFilenameInput = new EditText(EditActivity.this);
                newFilenameInput.setInputType(InputType.TYPE_CLASS_TEXT);
                newFilenameInput.setText(getSelectedFilename());
                newFilenameInput.setSelection(0, Math.max(0,newFilenameInput.getText().length()-4));
                AlertDialog dlg = builder.setTitle(context.getResources().getString(R.string.edit_dlg_rename))
                        .setMessage(context.getResources().getString(R.string.edit_dlg_renameTxt))
                        .setPositiveButton(R.string.btn_dorename, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                renameConversion(getSelectedFilename(), newFilenameInput.getText().toString());
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setView(newFilenameInput)
                        .create();
                try { dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                } catch (Exception e) { /*ok*/ }
                dlg.show();
            }
        });
        this.saveBtn = this.findViewById(R.id.button_save);
        this.saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ("".equals(getSelectedFilename())) {
                    Toast.makeText(context, context.getResources().getString(R.string.error_noconversion), Toast.LENGTH_LONG).show();
                    return;
                }
                saveConversion(getSelectedFilename());
            }
        });
        this.xmlContentTxt = this.findViewById(R.id.text_xmlcontent);

        if (!Converter.isStorageWritable()) {
            Toast.makeText(context, R.string.edit_noextstorage, Toast.LENGTH_LONG).show();
            return;
        }

        editor.loadConversions();
        initSpinner();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_mainmode:
                //return to main activity
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Initialize the spinner showing the possible conversions.
     * The spinner gets filled with elements, i.e. conversion names.
     */
    private void initSpinner() {
        if (editor.getNumberOfFiles() > 0) {

            loadSpinnerItems();
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    String selectedConversion = getSelectedFilename();
                    setXMLContents(selectedConversion);
                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView) { /*nothing to do*/ }
            });
        } else {
            if (Utils.DEBUG) Log.d("EditActivity", "Huh! No XML files!?");
        }
    }

    private void loadSpinnerItems() {
        String[] spinnerArray = editor.getFilenamesArray();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /**
     * For the given (selected) conversion set the xml contents to the edittext.
     * @param selectedConversion the selected conversion (filename).
     */
    private void setXMLContents(String selectedConversion) {
        xmlContentTxt.setText(editor.getXmlContent(selectedConversion));
    }

    /**
     * Save the contents of the edittext to the given (selected) conversion's xml file.
     * @param selectedFilename the selected conversion filename.
     */
    private void saveConversion(String selectedFilename) {
        try {
            editor.saveConversion(selectedFilename, xmlContentTxt.getText().toString());
            Toast.makeText(context, R.string.edit_save_done, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(context, R.string.edit_save_failed, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Rename the given (selected) conversion's xml file to the given new filename.
     * @param selectedFilename the selected conversion filename.
     * @param newFilename the new filename.
     */
    private void renameConversion(String selectedFilename, String newFilename) {
        try {
            editor.renameConversion(selectedFilename, newFilename);
            loadSpinnerItems();
            Toast.makeText(context, R.string.edit_rename_done, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(context, R.string.edit_rename_failed, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Delete the given (selected) conversion's xml file.
     * @param selectedFilename the selected conversion filename.
     */
    private void deleteConversion(String selectedFilename) {
        try {
            editor.deleteConversion(selectedFilename);
            xmlContentTxt.setText("");
            loadSpinnerItems();
            Toast.makeText(context, R.string.edit_delete_done, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(context, R.string.edit_delete_failed, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Returns the selected spinner item or "" if no item is selected.
     * @return the selected spinner item or "" if no item is selected.
     */
    private String getSelectedFilename() {
        Object selItem = spinner.getSelectedItem();
        return selItem == null ? "" : selItem.toString();
    }
}
