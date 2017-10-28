package com.nepdeveloper.backgroundcamera.utility;

import android.os.Environment;

import java.io.File;

public class Constant {
    //preferences
    public static final String PREFERENCE_NAME = "preference";
    public static final String PREFERENCE_VERSION = "PREFERENCE_VERSION";
    public static final int PREFERENCE_VERSION_CODE = 6;
    public static final int HOLD_DOWN_TIME = 1000;
    public static final String CAPTURE_PHOTO_BACK_CAM = "CAPTURE_PHOTO_BACK_CAM";
    public static final String CAPTURE_PHOTO_FRONT_CAM = "CAPTURE_PHOTO_FRONT_CAM";
    public static final String RECORD_VIDEO = "RECORD_VIDEO";
    public static final String RECORD_AUDIO = "RECORD_AUDIO";
    public static final String CAPTURE_PHOTO = "CAPTURE_PHOTO";
    public static final String STORAGE_LOCATION = "STORAGE_LOCATION";
    public static final String STORAGE_LOCATION_OLD = "STORAGE_LOCATION_OLD";
    public static final String START_TIME = "START_TIME";
    public static final String END_TIME = "END_TIME";
    public static final String SERVICE_ALWAYS_ACTIVE = "SERVICE_ALWAYS_ACTIVE";

    //settings
    public static final String SHUTTER_SOUND = "SHUTTER_SOUND";
    public static final String WHEN_TO_STOP_RECORDING = "WHEN_TO_STOP_RECORDING";
    public static final String SCREEN_IS_TOGGLED_TO_STOP = "SCREEN_IS_TOGGLED_TO_STOP";
    public static final String MANUALLY_STOP = "MANUALLY_STOP";
    public static final String SHOW_NOTIFICATION = "SHOW_NOTIFICATION";
    public static final String LEAST_VOLUME_IS_ONE = "LEAST_VOLUME_IS_ONE";

    //other
    public static final String IMAGE_FILE_EXTENSION = "I";
    public static final String VIDEO_FILE_EXTENSION = "V";
    public static final String AUDIO_FILE_EXTENSION = "A";
    public static final String NEW_FILE_CREATED = "NEW_FILE_CREATED";
    public static final String CURRENT_FILE_SELECTION = "CURRENT_FILE_SELECTION";
    public static final String DELETED_POSITIONS = "DELETED_POSITIONS";
    public static final int APP_PERMISSIONS = 43;
    public static final String PERMISSION_ASKED_BEFORE = "PERMISSION_ASKED_BEFORE";

    public static final String RECORDING_VIDEO = "RECORDING_VIDEO";
    public static final String RECORDING_AUDIO = "RECORDING_AUDIO";
    public static final String SERVICE_ACTIVE = "SERVICE_ACTIVE";
    public static final String QUALITY_HIGH = "QUALITY_HIGH";
    public static final String QUALITY_MEDIUM = "QUALITY_MEDIUM";
    public static final String QUALITY_LOW = "QUALITY_LOW";
    public static final String RECORDING_QUALITY = "RECORDING_QUALITY";


    public static final int REQUEST_DELETED_POSITIONS = 5;
    public static final long[] VIBRATE_PATTERN = {0, 20, 100, 20};

    public static final String ACTION_STOP_SELF = "ACTION_STOP_SELF";
    public static final String FROM_NOTIFICATION = "FROM_NOTIFICATION";

    //public static File FILE = new File(FILE_PATH);
    public static final String FILE_PATH_NAME = "FILE_PATH_NAME";
    public static final String FILE_PREFIX = "BGC";
    public static final String AGREEMENT_ACCEPTED = "AGREEMENT_ACCEPTED";
    public static final File FILE = new File(Environment.getExternalStorageDirectory(), FILE_PREFIX);

    public static final int NOTIFICATION_ID_VIDEO_RECORD = 100;
    public static final int NOTIFICATION_ID_AUDIO_RECORD = 200;
    public static final int NOTIFICATION_ID_TRANSFERRING_FILES = 300;
    public static final String DEFAULT_START_TIME = "5:00";         // 5 AM
    public static final String DEFAULT_END_TIME = "00:00";   // 12 aM
    public static final String TIME_PICKER = "TIME_PICKER";



//    public static int DOCUMENT_TREE = 25234;
}
