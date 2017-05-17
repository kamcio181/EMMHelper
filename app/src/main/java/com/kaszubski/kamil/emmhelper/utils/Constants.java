package com.kaszubski.kamil.emmhelper.utils;

public interface Constants {
    String ARRAY_KEY = "array";
    String ERROR_CODE = "errorCode";
    String PACKAGE_INFO_KEY = "packageInfo";
    String APK_PATH = "apkPath";
    String SOURCE_DIR = "sourceDir";
    String FILE_FORMAT_KEY = "fileFormat";
    String TXT_FILE_EXTENSION = ".txt";
    String CSV_FILE_EXTENSION = ".csv";
    String APK_FILE_EXTENSION = ".apk";
    String TEMP_FILE = "tempFile";

    String FRAGMENT_SEARCH = "SearchFragment";
    String FRAGMENT_IP_FIND = "IPFindFragment";
    String FRAGMENT_EXPORT = "ExportFragment";
    String FRAGMENT_LICENSE_CHECK = "LicenseCheckFragment";
    String FRAGMENT_SIM_INFO = "SimInfoFragment";
    String FRAGMENT_GETPROP = "GetPropFragment";

    String SUCCESS_ACTION = "com.kaszubski.kamil.emmhelper.ACTION_SUCCESS";
    String FAILURE_ACTION = "com.kaszubski.kamil.emmhelper.ACTION_FAILURE";

    int WRITE_EXTERNAL_STORAGE_PERMISSION = 0;
    int INTERNET_PERMISSION = 1;
    int READ_PHONE_STATE_PERMISSION = 2;

    int DEFAULT_ERROR = -1;
}
