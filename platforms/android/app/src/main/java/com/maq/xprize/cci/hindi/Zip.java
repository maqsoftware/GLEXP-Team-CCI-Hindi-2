package com.maq.xprize.cci.hindi;

import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.maq.xprize.cci.hindi.SplashScreenActivity.sharedPref;

public class Zip {

    private ZipFile zipFileHandler;
    private TextView percentText;
    private Activity zipActivity;
    private static int count = 0;

    public Zip(ZipFile zipFile, Activity _activity) {
        this.zipFileHandler = zipFile;
        zipActivity = _activity;
    }

    public Zip(String pathToZipFile) throws IOException {
        this.zipFileHandler = new ZipFile(pathToZipFile);
    }

    public void close() throws IOException {
        zipFileHandler.close();
    }

    public void unzip(File targetDir, int totalZipSize, boolean isMain, int fileVersion) throws IOException {
        int percent;
        ProgressBar progressBar = zipActivity.findViewById(R.id.extraction_progress_bar);
        percentText = zipActivity.findViewById(R.id.mPercentText);
        String path;
        ZipEntry zipEntry;
        File outputFile;
        File outputDir;
        BufferedInputStream inputStream;
        BufferedOutputStream outputStream;
        boolean isExtractionSuccessful = false;

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("Unable to create directory");
        }

        if (!targetDir.isDirectory()) {
            throw new IOException("Unable to extract to a non-directory");
        }

        Enumeration<? extends ZipEntry> zipEntries = zipFileHandler.entries();
        progressBar = progressBar.findViewById(R.id.extraction_progress_bar);

        while (zipEntries.hasMoreElements()) {
            ++count;
            // Calculate the percentage of extracted content
            percent = (count * 100) / totalZipSize;
            // Sync the progress bar with percentage value
            progressBar.setProgress(percent);
            final int finalPercent = percent;
            zipActivity.runOnUiThread(new Runnable() {
                public void run() {
                    // Show the percentage value on progress bar
                    percentText.setText(MessageFormat.format("{0} %", finalPercent));
                }
            });

            zipEntry = zipEntries.nextElement();
            path = targetDir.getPath() + "/" + zipEntry.getName();
            if (!zipEntry.isDirectory()) {
                inputStream = new BufferedInputStream(zipFileHandler.getInputStream(zipEntry));

                outputFile = new File(path);
                outputDir = new File(outputFile.getParent());

                if (!outputDir.exists() && !outputDir.mkdirs()) {
                    throw new IOException("unable to make directory for entry " + path);
                }

                if (!outputFile.exists() && !outputFile.createNewFile()) {
                    throw new IOException("Unable to create directory for " + path);
                }

                outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
                try {
                    int currByte;
                    while ((currByte = inputStream.read()) != -1) {
                        outputStream.write(currByte);
                    }
                    isExtractionSuccessful = true;
                } catch (Exception e) {
                    isExtractionSuccessful = false;
                    e.printStackTrace();
                } finally {
                    outputStream.close();
                    inputStream.close();
                }
            }
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        if (isExtractionSuccessful) {
            if (isMain) {
                editor.putInt(zipActivity.getString(R.string.mainFileVersion), fileVersion);
            } else {
                editor.putInt(zipActivity.getString(R.string.patchFileVersion), fileVersion);
            }
            editor.apply();
        }
    }
}