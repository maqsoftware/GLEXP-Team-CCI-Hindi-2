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
import android.os.Handler;
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
    File packageDir;
    int mainFileVersion;
    int patchFileVersion;

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
                // Check user permission if the permission is explicitly removed by the user
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
        int defaultFileVersion = 0;

        // Retrieve the stored values of main and patch file version
        mainFileVersion = sharedPref.getInt(getString(R.string.mainFileVersion), defaultFileVersion);
        patchFileVersion = sharedPref.getInt(getString(R.string.patchFileVersion), defaultFileVersion);
        boolean isExtractionRequired = isExpansionExtractionRequired(mainFileVersion, patchFileVersion);
        if (mainFileVersion == 0 && patchFileVersion == 0) {
            // set the default file version if the extraction across for the first time
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(getString(R.string.mainFileVersion), defaultFileVersion);
            editor.putInt(getString(R.string.patchFileVersion), defaultFileVersion);
            editor.apply();
        } else if (!isExtractionRequired) {
            // if extraction done start the WebView
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }
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
        try {
            for (DownloadExpansionFile.XAPKFile xf : xAPKs) {
                if (xf.mIsMain && xf.mFileVersion != mainFileVersion || !xf.mIsMain && xf.mFileVersion != patchFileVersion) {
                    obbFilePath = getObbFilePath(xf.mIsMain, xf.mFileVersion);
                    obbFile = new File(obbFilePath);
                    obbZipFile = new ZipFile(obbFile);
                    zipFileHandler = new Zip(obbZipFile, this);
                    packageDir = this.getExternalFilesDir(null);
                    if (xf.mIsMain && !packageDir.exists()) {
                        packageDir.mkdir();
                    }
                    zipFileHandler.unzip(packageDir, totalSize, xf.mIsMain, xf.mFileVersion);
                    zipFileHandler.close();
                }
            }
            toCallApplication();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public boolean isStorageSpaceAvailable() {
        long totalExpansionFileSize = 0;
        File internalStorageDir = Environment.getDataDirectory();
        for (DownloadExpansionFile.XAPKFile xf : xAPKs) {
            if (xf.mIsMain && xf.mFileVersion != mainFileVersion || !xf.mIsMain && xf.mFileVersion != patchFileVersion) {
                totalExpansionFileSize = xf.mFileSize;
            }
        }
        return totalExpansionFileSize < internalStorageDir.getFreeSpace();
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

    private boolean isExpansionExtractionRequired(int storedMainFileVersion, int storedPatchFileVersion) {
        for (DownloadExpansionFile.XAPKFile xf : xAPKs) {
            // If main or patch file is updated set isExtractionRequired to true
            if (xf.mIsMain && xf.mFileVersion != storedMainFileVersion || !xf.mIsMain && xf.mFileVersion != storedPatchFileVersion) {
                return true;
            }
        }
        return false;
    }

    public String getObbFilePath(boolean isMain, int fileVersion) {
        return getObbDir() + File.separator + Helpers.getExpansionAPKFileName(this, isMain, fileVersion);
    }

    private class DownloadFile extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... sUrl) {
            // unzip if storage space available
            if (isStorageSpaceAvailable()) {
                unzipFile();
            } else {
                SplashScreenActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(SplashScreenActivity.this, "Insufficient storage space! Please free up your storage to use this application.", Toast.LENGTH_LONG).show();
                        // Call finish after the toast message disappears
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SplashScreenActivity.this.finish();
                            }
                        }, Toast.LENGTH_LONG);
                    }
                });
            }
            return null;
        }
    }
}