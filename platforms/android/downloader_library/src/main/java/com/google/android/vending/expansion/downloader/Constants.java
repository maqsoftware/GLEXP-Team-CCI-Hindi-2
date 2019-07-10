/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.vending.expansion.downloader;

import java.io.File;


/**
 * Contains the internal constants that are used in the download manager.
 * As a general rule, modifying these constants should be done with care.
 */
public class Constants {
    /**
     * Tag used for debugging/logging
     */
    public static final String TAG = "LVLDL";
    /**
     * The intent that gets sent when the service must wake up for a retry
     */
    public static final String ACTION_RETRY = "android.intent.action.DOWNLOAD_WAKEUP";
    /**
     * The buffer size used to stream the data
     */
    public static final int BUFFER_SIZE = 4096;
    /**
     * The minimum amount of progress that has to be done before the progress bar gets updated
     */
    public static final int MIN_PROGRESS_STEP = 4096;
    /**
     * The minimum amount of time that has to elapse before the progress bar gets updated, in ms
     */
    public static final long MIN_PROGRESS_TIME = 1000;
    /**
     * The number of times that the download manager will retry its network
     * operations when no progress is happening before it gives up.
     */
    public static final int MAX_RETRIES = 5;
    /**
     * The minimum amount of time that the download manager accepts for
     * a Retry-After response header with a parameter in delta-seconds.
     */
    public static final int MIN_RETRY_AFTER = 30; // 30s
    /**
     * The maximum amount of time that the download manager accepts for
     * a Retry-After response header with a parameter in delta-seconds.
     */
    public static final int MAX_RETRY_AFTER = 24 * 60 * 60; // 24h
    /**
     * Enable separate connectivity logging
     */
    public static final boolean LOGX = true;
    /**
     * Enable verbose logging
     */
    public static final boolean LOGV = false;
    public static final long WATCHDOG_WAKE_TIMER = 60 * 1000;
    /**
     * The wake duration to check to see if the process was killed.
     */
    public static final long ACTIVE_THREAD_WATCHDOG = 5 * 1000;
    /**
     * Expansion path where we store obb files
     */
    static final String EXP_PATH = File.separator + "Android"
            + File.separator + "obb" + File.separator;
    /**
     * Enable super-verbose logging
     */
    private static final boolean LOCAL_LOGVV = false;
    public static final boolean LOGVV = LOCAL_LOGVV && LOGV;

}