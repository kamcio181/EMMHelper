package com.kaszubski.kamil.emmhelper.utils;

public interface Constants {
    String DB_NAME = "database";
    String NOTES_TABLE = "NOTES";
    String ID_COL = "_id";
    String MILLIS_COL = "millis";
    String NOTE_TITLE_COL = "noteTitle";
    String NOTE_TEXT_COL = "noteText";
    String FOLDER_ID_COL = "folderId";
    String DELETED_COL = "deleted";
    String ENCRYPTED_COL = "encrypted";
    String SALT_COL = "salt";

    String WIDGETS_TABLE = "WIDGETS";
    String WIDGET_ID_COL = "widgetId";
    String CONNECTED_NOTE_ID_COL = "noteId";
    String CURRENT_WIDGET_MODE_COL = "mode";
    String CURRENT_THEME_MODE_COL = "themeMode";
    String CURRENT_TEXT_SIZE_COL = "textSize";

    String FOLDER_TABLE = "FOLDERS";
    String FOLDER_NAME_COL = "folderName";
    String FOLDER_ICON_COL = "folderIcon";
    String NOTES_COUNT_COL = "notesCount";

    String PREFS_NAME = "prefs";
    String CONFIGURED_KEY = "configured";
    String SORT_BY_DATE_KEY = "sortByDate";
    String WIDGET_THEME_KEY = "widgetTheme";
    String MY_NOTES_ID_KEY = "myNotesId";
    String TRASH_ID_KEY = "trashId";
    String TITLE_KEY = "title";
    String ARRAY_KEY = "array";
    String STRING_KEY = "string";
    String PACKAGE_INFO_KEY = "packageInfo";
    String SOURCE_DIR = "sourceDir";
    String FILE_FORMAT_KEY = "fileFormat";
    String TXT_FILE_EXTENSION = ".txt";
    String CSV_FILE_EXTENSION = ".csv";
    String APK_FILE_EXTENSION = ".apk";
    String SEARCH_IN_TITLE = "titleSearch";
    String SEARCH_IN_CONTENT = "contentSearch";
    String IGNORE_TABS_IN_WIDGETS_KEY = "ignoreTabsInWidget";
    String NOTE_TEXT_SIZE_KEY = "noteTextSize";
    String STARTING_FOLDER_KEY = "startingFolder";
    String SKIP_MULTILEVEL_NOTE_MANUAL_DIALOG_KEY = "skipMultilevelNoteManualDialog";
    String SKIP_WIDGET_MANUAL_DIALOG_KEY = "skipWidgetManualDialog";
    String RELOAD_MAIN_ACTIVITY_AFTER_RESTORE_KEY = "reloadMainActivityAfterRestore";

    String NOTE_UPDATED_FROM_WIDGET = "noteUpdatedFromWidget";
    String NOTE_TEXT_SIZE_UPDATED = "noteTextSizeUpdated";

    String FRAGMENT_LIST = "ListFragment";
    String FRAGMENT_NOTE = "NoteFragment";
    String FRAGMENT_TRASH_NOTE = "TrashNoteFragment";
    String FRAGMENT_SEARCH = "SearchFragment";
    String FRAGMENT_IP_FINDER = "IPFinderFragment";
    String FRAGMENT_SETTINGS_LIST = "SettingsListFragment";
    String FRAGMENT_SETTINGS_WIDGET_CONFIG = "SettingsWidgetConfigFragment";
    String FRAGMENT_SETTINGS_RESTORE_LIST = "SettingsRestoreListFragment";

    int WRITE_PERMISSION = 0;
    int INTERNET_PERMISSION = 1;
}
