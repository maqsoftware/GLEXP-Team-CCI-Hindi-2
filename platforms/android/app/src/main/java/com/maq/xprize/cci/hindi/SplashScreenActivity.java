package com.maq.xprize.cci.hindi;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import java.util.zip.ZipFile;

import static com.cci.DownloadExpansionFile.xAPKs;


public class SplashScreenActivity extends Activity {
    public static SharedPreferences sharedPref;
    Intent intent = null;
    String obbFilePath;
    File obbFile;
    ZipFile obbZipFile;
    Zip zipFileHandler;
    String dataFilePath;
    File packageDir;
    int mainFileVersion;
    int patchFileVersion;

    public static String getDataFilePath() {
        return "/storage/emulated/0/Android/data/com.maq.xprize.cci.hindi/files/";
    }

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
        String flagFilePath = "/storage/emulated/0/Android/data/" + getPackageName() + "/files/.success.txt";
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
        if (Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else {
            //for new api versions.
            View decorView = this.getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
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
    public void toCallApplication() {
        intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void unzipFile() {
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
                    zipFileHandler = new Zip(obbZipFile, this);
                    dataFilePath = getDataFilePath();
                    packageDir = new File(dataFilePath);
                    if (xf.mIsMain && packageDir.exists()) {
                        DownloadExpansionFile.deleteDir(packageDir);
                        packageDir.mkdir();
                    }
                    zipFileHandler.unzip(dataFilePath, totalSize, xf.mIsMain, xf.mFileVersion);
                    zipFileHandler.close();
                }
            }
            toCallApplication();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public int getTotalSize() {
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

    public String getObbFilePath(boolean isMain, int fileVersion) {
        return Environment.getExternalStorageDirectory().toString() + "/Android/obb/" + Helpers.getPackageName(this) + File.separator +
                Helpers.getExpansionAPKFileName(this, isMain, fileVersion);
    }

    private class DownloadFile extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... sUrl) {
            unzipFile();
            return null;
        }
    }
}
