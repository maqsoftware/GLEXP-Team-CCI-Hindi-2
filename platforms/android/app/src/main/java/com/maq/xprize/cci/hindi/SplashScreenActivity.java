package com.maq.xprize.cci.hindi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.cci.DownloadExpansionFile;
import com.google.android.vending.expansion.downloader.Helpers;


import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipFile;

import static com.cci.DownloadExpansionFile.xAPKs;


public class SplashScreenActivity extends Activity {
    public static SharedPreferences sharedPref;
    private String obbFilePath;
    private File obbFile;
    private ZipFile obbZipFile;
    private int mainFileVersion;
    private int patchFileVersion;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new DownloadFile().execute(null, null, null);
                } else {
                    Toast.makeText(this, "Permission required!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            break;
            case 2: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Permission required!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = getSharedPreferences("ExpansionFile", MODE_PRIVATE);
        String flagFilePath;
        flagFilePath = Objects.requireNonNull(this.getExternalFilesDir(null)).getPath()+ "/.success.txt";
        int defaultFileVersion = 0;
        File flagFile = new File(flagFilePath);
        boolean extractionRequired = false;
        if (!flagFile.exists()) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.mainFileVersion), defaultFileVersion);
            editor.putInt(getString(R.string.patchFileVersion), defaultFileVersion);
            editor.apply();
            extractionRequired = !flagFile.exists();
        } else {
            int mainFileVersion = sharedPref.getInt(getString(R.string.mainFileVersion), defaultFileVersion);
            int patchFileVersion = sharedPref.getInt(getString(R.string.patchFileVersion), defaultFileVersion);
            for (DownloadExpansionFile.XAPKFile xf : xAPKs) {
                if ((xf.mIsMain && xf.mFileVersion != mainFileVersion) || (!xf.mIsMain && xf.mFileVersion != patchFileVersion)) {
                    extractionRequired = true;
                    break;
                }
            }
        }

        // call main activity if extraction done
        if (!extractionRequired) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //for new api versions.
        View decorView = this.getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        setContentView(R.layout.activity_splash_screen);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            new DownloadFile().execute(null, null, null);
        }
    }

    /* function to call the main application after extraction */
    private void toCallApplication() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void unzipFile() {
        int totalSize = getTotalSize();
        sharedPref = getSharedPreferences("ExpansionFile", MODE_PRIVATE);
        mainFileVersion = sharedPref.getInt(getString(R.string.mainFileVersion), 0);
        patchFileVersion = sharedPref.getInt(getString(R.string.patchFileVersion), 0);
        try {
            for (DownloadExpansionFile.XAPKFile xf : xAPKs) {
                if (xf.mIsMain && xf.mFileVersion != mainFileVersion || !xf.mIsMain && xf.mFileVersion != patchFileVersion) {
                    obbFilePath = getObbFilePath(xf.mIsMain, xf.mFileVersion);
                    obbFile = new File(obbFilePath);
                    obbZipFile = new ZipFile(obbFile);
                    Zip zipFileHandler = new Zip(obbZipFile, this);
                    File packageDir = this.getExternalFilesDir(null);
                    assert packageDir != null;
                    if (xf.mIsMain && !packageDir.exists()) packageDir.mkdir();
                    zipFileHandler.unzip(packageDir, totalSize, xf.mIsMain, xf.mFileVersion);
                    zipFileHandler.close();
                }
            }
            toCallApplication();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private int getTotalSize() {
        int totalSize = 0;
        try {
            for (DownloadExpansionFile.XAPKFile xf : xAPKs) {
                if (!xf.mIsMain && (xf.mFileVersion != patchFileVersion) || xf.mIsMain && (xf.mFileVersion != mainFileVersion)) {
                    obbFilePath = getObbFilePath(xf.mIsMain, xf.mFileVersion);
                    obbFile = new File(obbFilePath);
                    obbZipFile = new ZipFile(obbFile);
                    totalSize += obbZipFile.size();

                }
            }
        } catch (IOException ie) {
            System.out.println(ie);
        }
        return totalSize;
    }

    private String getObbFilePath(boolean isMain, int fileVersion) {
        return getObbDir() + File.separator + Helpers.getExpansionAPKFileName(this, isMain, fileVersion);
    }

    private class DownloadFile extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... sUrl) {
            unzipFile();
            return null;
        }
    }
}
