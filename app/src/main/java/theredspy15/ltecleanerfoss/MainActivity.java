package theredspy15.ltecleanerfoss;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fxn.stash.Stash;

import java.io.File;
import java.text.DecimalFormat;

import theredspy15.ltecleanerfoss.extensionapps.AppExtensionState;
import theredspy15.ltecleanerfoss.extensionapps.ExtensionContentProvider;
import theredspy15.ltecleanerfoss.extensionapps.WorkType;

import static theredspy15.ltecleanerfoss.extensionapps.ExtensionContentProviderKt.getContentUri;

public class MainActivity extends AppCompatActivity {

    ConstraintSet constraintSet = new ConstraintSet();
    static boolean running = false;
    SharedPreferences prefs;

    LinearLayout fileListView;
    ScrollView fileScrollView;
    ProgressBar scanPBar;
    TextView progressText;
    TextView statusText;
    ConstraintLayout layout;

    @SuppressLint("LogConditional")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Stash.init(getApplicationContext());

        fileListView = findViewById(R.id.fileListView);
        fileScrollView = findViewById(R.id.fileScrollView);
        scanPBar = findViewById(R.id.scanProgress);
        progressText = findViewById(R.id.ScanTextView);
        statusText = findViewById(R.id.statusTextView);
        layout = findViewById(R.id.main_layout);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        constraintSet.clone(layout);

        if (!isWriteExternalPermissionGranted()) {
            requestWriteExternalPermission();
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String workStatus = extras.getString(ExtensionContentProvider.KEY_WORK_STATUS);
            String workType = extras.getString(ExtensionContentProvider.KEY_WORK_TYPE);
            if (workStatus != null) {
                analyzeOrDelete(workType);
            }
        }
    }

    /**
     * Starts the settings activity
     *
     * @param view the view that is clickedprefs = getSharedPreferences("Settings",0);
     */
    public final void settings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Runs search and delete on background thread
     */
    public final void clean(View view) {
        clean();
    }

    public void animateBtn() {
        TransitionManager.beginDelayedTransition(layout);
        constraintSet.clear(R.id.cleanButton, ConstraintSet.TOP);
        constraintSet.clear(R.id.statusTextView, ConstraintSet.BOTTOM);
        constraintSet.setMargin(R.id.statusTextView, ConstraintSet.TOP, 50);
        constraintSet.applyTo(layout);
    }

    private void clean() {
        if (!running) {
            if (!prefs.getBoolean("one_click", false)) // one-click disabled
                new AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
                        .setTitle(R.string.select_task)
                        .setMessage(R.string.do_you_want_to)
                        .setPositiveButton(R.string.clean, (dialog, whichButton) -> { // clean
                            new Thread(() -> scan(true)).start();
                        })
                        .setNegativeButton(R.string.analyze, (dialog, whichButton) -> { // analyze
                            new Thread(() -> scan(false)).start();
                        }).show();
            else new Thread(() -> scan(true)).start(); // one-click enabled
        }
    }

    private void analyzeOrDelete(String workType) {
        if (!running) {
            if (WorkType.Clean.INSTANCE.getId().equals(workType)) {
                new Thread(() -> scan(true)).start();
            } else {
                new Thread(() -> scan(false)).start();
            }
        }
    }

    /**
     * Searches entire device, adds all files to a list, then a for each loop filters
     * out files for deletion. Repeats the process as long as it keeps finding files to clean,
     * unless nothing is found to begin with
     */
    @SuppressLint("SetTextI18n")
    private void scan(boolean delete) {
        Looper.prepare();
        running = true;
        reset();

        File path = Environment.getExternalStorageDirectory();

        // scanner setup
        FileScanner fs = new FileScanner(path);
        fs.setEmptyDir(prefs.getBoolean("empty", false));
        fs.setAutoWhite(prefs.getBoolean("auto_white", true));
        fs.setDelete(delete);
        fs.setCorpse(prefs.getBoolean("corpse", false));
        fs.setGUI(this);

        // filters
        fs.setUpFilters(prefs.getBoolean("generic", true),
                prefs.getBoolean("aggressive", false),
                prefs.getBoolean("apk", false));

        // failed scan
        if (path.listFiles() == null) { // is this needed? yes.
            TextView textView = printTextView("Scan failed.", Color.RED);
            runOnUiThread(() -> fileListView.addView(textView));
        }

        runOnUiThread(() -> {
            animateBtn();
            resolveWorkState(AppExtensionState.Start.INSTANCE.getId());
            statusText.setText(getString(R.string.status_running));
        });

        // start scanning
        long kilobytesTotal = fs.startScan();

        // crappy but working fix for percentage never reaching 100
        runOnUiThread(() -> {
            resolveWorkState(AppExtensionState.Start.INSTANCE.getId());
            scanPBar.setProgress(scanPBar.getMax());
            progressText.setText("100%");
        });

        // kilobytes found/freed text
        runOnUiThread(() -> {
            resolveWorkState(AppExtensionState.Stop.INSTANCE.getId());
            if (delete) {
                statusText.setText(getString(R.string.freed) + " " + convertSize(kilobytesTotal));
            } else {
                statusText.setText(getString(R.string.found) + " " + convertSize(kilobytesTotal));
            }
        });
        fileScrollView.post(() -> fileScrollView.fullScroll(ScrollView.FOCUS_DOWN));

        running = false;
        Looper.loop();
    }

    private void resolveWorkState(String state) {
        Uri uri = getContentUri(state);
        getApplicationContext().getContentResolver().insert(uri, null);
    }

    /**
     * Convenience method to quickly create a textview
     *
     * @param text - text of textview
     * @return - created textview
     */
    private synchronized TextView printTextView(String text, int color) {
        TextView textView = new TextView(MainActivity.this);
        textView.setTextColor(color);
        textView.setText(text);
        textView.setPadding(3, 3, 3, 3);
        return textView;
    }

    private String convertSize(long length) {
        final DecimalFormat format = new DecimalFormat("#.##");
        final long MiB = 1024 * 1024;
        final long KiB = 1024;

        if (length > MiB) {
            return format.format(length / MiB) + " MB";
        }
        if (length > KiB) {
            return format.format(length / KiB) + " KB";
        }
        return format.format(length) + " B";
    }

    /**
     * Increments amount removed, then creates a text view to add to the scroll view.
     * If there is any error while deleting, turns text view of path red
     *
     * @param file file to delete
     */
    synchronized TextView displayPath(File file) {
        // creating and adding a text view to the scroll view with path to file
        TextView textView = printTextView(file.getAbsolutePath(), getResources().getColor(R.color.colorAccent));

        // adding to scroll view
        runOnUiThread(() -> fileListView.addView(textView));

        // scroll to bottom
        fileScrollView.post(() -> fileScrollView.fullScroll(ScrollView.FOCUS_DOWN));

        return textView;
    }


    /**
     * Removes all views present in fileListView (linear view), and sets found and removed
     * files to 0
     */
    private synchronized void reset() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        runOnUiThread(() -> {
            fileListView.removeAllViews();
            scanPBar.setProgress(0);
            scanPBar.setMax(1);
        });
    }

    private boolean isWriteExternalPermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
//            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Request write permission
     */
    public synchronized void requestWriteExternalPermission() {
//        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                startActivityForResult(intent, 2296);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2296);
            }
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2296) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    prompt();
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Handles the whether the user grants permission. Launches new fragment asking the user to give file permission.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 &&
                grantResults.length > 0 &&
                grantResults[0] != PackageManager.PERMISSION_GRANTED)
            prompt();
    }

    /**
     * Launches the prompt activity
     */
    public final void prompt() {
        Intent intent = new Intent(this, PromptActivity.class);
        startActivity(intent);
    }
}
