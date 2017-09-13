package com.github.heuberg.myconvert;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;

/**
 * Activity that can receive xml-files via Intent.ACTION_SEND or Intent.ACTION_SEND_MULTIPLE
 */
public class ImportActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Converter.isStorageWritable()) {
            Toast.makeText(this, R.string.import_noextstorage, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent receivedIntent = getIntent();
        String receivedAction = receivedIntent.getAction();

        if(Intent.ACTION_SEND.equals(receivedAction)) {
            Uri importFileUri = receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            try {
                Importer.importDefinition(importFileUri, this);
                Toast.makeText(this, getResources().getString(R.string.import_done), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, getResources().getString(R.string.import_failed), Toast.LENGTH_LONG).show();
            }
        } else if(Intent.ACTION_SEND_MULTIPLE.equals(receivedAction)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                //SDK>=16: clipData is available, SDK<16: ?? //TODO test on API 15
                ClipData clipData = receivedIntent.getClipData();
                if (clipData != null) {
                    boolean importedAll = true;
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        Uri importFileUri = clipData.getItemAt(i).getUri();
                        try {
                            Importer.importDefinition(importFileUri, this);
                        } catch (IOException e) {
                            importedAll = false;
                        }
                    }
                    if (importedAll)
                        Toast.makeText(this, getResources().getString(R.string.import_done), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(this, getResources().getString(R.string.import_failed), Toast.LENGTH_LONG).show();
                }
            }
        }
        finish();
    }
}
