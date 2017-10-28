package com.nepdeveloper.backgroundcamera.utility;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nepdeveloper.backgroundcamera.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class DirectoryChooserFragment extends DialogFragment {
    static final /* synthetic */ boolean $assertionsDisabled = (!DirectoryChooserFragment.class.desiredAssertionStatus());
    private static final String ARG_INITIAL_DIRECTORY = "INITIAL_DIRECTORY";
    public static final String KEY_CURRENT_DIRECTORY = "CURRENT_DIRECTORY";
    private AppCompatButton mBtnConfirm;
    private FileObserver mFileObserver;
    private ArrayList<String> mFilenames;
    private File[] mFilesInDir;
    private String mInitialDirectory;
    private ArrayAdapter<String> mListDirectoriesAdapter;
    private OnFragmentInteractionListener mListener;
    private File mSelectedDir;
    private TextView mTxtvSelectedFolder;

    public interface OnFragmentInteractionListener {
        void onCancelChooser();

        void onSelectDirectory(@NonNull String str);
    }

    public static DirectoryChooserFragment newInstance(@Nullable String initialDirectory) {
        DirectoryChooserFragment fragment = new DirectoryChooserFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_DIRECTORY, initialDirectory);
        fragment.setArguments(args);
        return fragment;
    }

    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedDir != null) {
            outState.putString(KEY_CURRENT_DIRECTORY, mSelectedDir.getAbsolutePath());
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null) {
            throw new IllegalArgumentException("You must create DirectoryChooserFragment via newInstance().");
        }
        mInitialDirectory = getArguments().getString(ARG_INITIAL_DIRECTORY);
        if (savedInstanceState != null) {
            mInitialDirectory = savedInstanceState.getString(KEY_CURRENT_DIRECTORY);
        }
        if (getShowsDialog()) {
            setStyle(1, 0);
        } else {
            setHasOptionsMenu(true);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if ($assertionsDisabled || getActivity() != null) {
            File initialDir;
            View view = inflater.inflate(R.layout.directory_chooser, container, false);
            mBtnConfirm = view.findViewById(R.id.btnConfirm);
            AppCompatButton mBtnCancel = view.findViewById(R.id.btnCancel);
            ImageButton mBtnCreateFolder = view.findViewById(R.id.btnCreateFolder);
            mTxtvSelectedFolder = view.findViewById(R.id.txtvSelectedFolder);
            ListView mListDirectories = view.findViewById(R.id.directoryList);
            mBtnConfirm.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isValidFile(mSelectedDir)) {
                        returnSelectedFolder();
                    }
                }
            });
            mBtnCancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onCancelChooser();
                }
            });
            mListDirectories.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    Log.i("biky", "Selected index =" + position);
                    if (mFilesInDir != null && position >= 0 && position < mFilesInDir.length) {
                        changeDirectory(mFilesInDir[position]);
                    }
                }
            });
            view.findViewById(R.id.btnNavUp).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    navigateToParentDir();
                }
            });

            view.findViewById(R.id.default_dir).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!Constant.FILE.exists() || !Constant.FILE.isDirectory()) {
                        //noinspection ResultOfMethodCallIgnored
                        Constant.FILE.mkdirs();
                    }
                    changeDirectory(Constant.FILE);
                }
            });

            mBtnCreateFolder.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    openNewFolderDialog();
                }
            });
            if (!getShowsDialog()) {
                mBtnCreateFolder.setVisibility(View.GONE);
            }
            mFilenames = new ArrayList<>();
            mListDirectoriesAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, mFilenames);
            mListDirectories.setAdapter(mListDirectoriesAdapter);
            if (mInitialDirectory == null || !isValidFile(new File(mInitialDirectory))) {
                initialDir = Environment.getExternalStorageDirectory();
            } else {
                initialDir = new File(mInitialDirectory);
            }
            changeDirectory(initialDir);
            return view;
        }
        throw new AssertionError();
    }

    @SuppressWarnings("deprecation")
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    public void navigateToParentDir() {
        if (mSelectedDir != null) {
            File parent = mSelectedDir.getParentFile();
            if (parent != null) {
                changeDirectory(parent);
            }
        }
    }

    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onPause() {
        super.onPause();
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
    }

    public void onResume() {
        super.onResume();
        if (mFileObserver != null) {
            mFileObserver.startWatching();
        }
        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {

                    if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) {
                        if (mSelectedDir != null) {
                            File parent = mSelectedDir.getParentFile();
                            if (parent != null) {
                                changeDirectory(parent);
                            } else {
                                dismiss();
                            }
                        }
                    } else {
                        return false;
                    }

                }
                return false;
            }
        });
    }

    private void openNewFolderDialog() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.create_new_folder_dialog, null);

        new AlertDialog.Builder(getActivity())
                .setTitle("Enter folder name")
                .setView(view)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Toast.makeText(getActivity(), createFolder(((TextView) view.findViewById(R.id.folder_name)).getText().toString()), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void changeDirectory(File dir) {
        if (dir == null) {
            Log.i("biky", "Could not change folder: dir was null");
        } else if (dir.isDirectory()) {
            File[] contents = dir.listFiles();
            if (contents != null) {
                int numDirectories = 0;
                for (File f : contents) {
                    if (f.isDirectory()) {
                        numDirectories++;
                    }
                }
                mFilesInDir = new File[numDirectories];
                mFilenames.clear();

                int i = 0;
                int counter = 0;
                while (i < numDirectories) {
                    if (contents[counter].isDirectory()) {
                        mFilesInDir[i] = contents[counter];
                        mFilenames.add(contents[counter].getName());
                        i++;
                    }
                    counter++;
                }
                Arrays.sort(mFilesInDir);
                Collections.sort(mFilenames);
                mSelectedDir = dir;
                mTxtvSelectedFolder.setText(dir.getAbsolutePath());
                mListDirectoriesAdapter.notifyDataSetChanged();
                mFileObserver = createFileObserver(dir.getAbsolutePath());
                mFileObserver.startWatching();
                Log.i("biky", "Changed directory to " + dir.getAbsolutePath());
            } else {
                Log.i("biky", "Could not change folder: contents of dir were null 2");
            }
        } else {
            Log.i("biky", "Could not change folder: dir is no directory");
        }
        if(isValidFile(dir)){
            Log.i("biky","is valud");
        }else{
            Log.i("biky","is invalid");
        }

        refreshButtonState();
    }

    private void refreshButtonState() {
        if (getActivity() != null && mSelectedDir != null) {
            mBtnConfirm.setEnabled(isValidFile(mSelectedDir));
            getActivity().invalidateOptionsMenu();
        }
    }

    private void refreshDirectory() {
        if (mSelectedDir != null) {
            changeDirectory(mSelectedDir);
        }
    }

    private FileObserver createFileObserver(String path) {
        return new FileObserver(path, 960) {

            public void onEvent(int event, String path) {
                Log.i("biky", "FileObserver received event " + event);
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshDirectory();
                        }
                    });
                }
            }
        };
    }

    private void returnSelectedFolder() {
        if (mSelectedDir != null) {
            Log.i("biky", "Returning " + mSelectedDir.getAbsolutePath() + " as result");
            mListener.onSelectDirectory(mSelectedDir.getAbsolutePath());
            return;
        }
        mListener.onCancelChooser();
    }

    private int createFolder(String mNewDirectoryName) {
        if (mNewDirectoryName != null && mSelectedDir != null && mSelectedDir.canWrite()) {
            if (mNewDirectoryName.isEmpty()) {
                return R.string.folder_name_empty;
            }
            File newDir = new File(mSelectedDir, mNewDirectoryName);
            if (newDir.exists()) {
                return R.string.create_folder_error_already_exists;
            }
            if (newDir.mkdir()) {
                return R.string.create_folder_success;
            }
            return R.string.create_folder_error;
        } else if (mSelectedDir == null || !mSelectedDir.canWrite()) {
            return R.string.create_folder_error;
        } else {
            return R.string.create_folder_error_no_write_access;
        }
    }

    private boolean isValidFile(File file) {
        try {
            return file != null && file.isDirectory() && file.canRead() && file.canWrite();
        } catch (Exception e) {
            return false;
        }
    }
}
