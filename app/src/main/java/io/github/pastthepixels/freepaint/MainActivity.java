package io.github.pastthepixels.freepaint;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.rarepebble.colorpicker.ColorPreference;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.util.Objects;

import io.github.pastthepixels.freepaint.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private final ModalBottomSheet settingsBottomSheet = new ModalBottomSheet();

    private Menu topMenu;

    /**
     * Records the last used intent action -- used in <code>activityResultLauncher<code> to see if we should load the selected path or save to it.
     */
    private String intentAction;

    /**
     * Handles file picker actions -- onActivityResult is called after a file path is chosen (see MainActivity.onOptionsItemSelected)
     */
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        System.out.println(Objects.requireNonNull(uri).getPath());
                        try {
                            if (Objects.equals(intentAction, Intent.ACTION_CREATE_DOCUMENT))
                                binding.drawCanvas.saveFile(uri);
                            if (Objects.equals(intentAction, Intent.ACTION_OPEN_DOCUMENT))
                                binding.drawCanvas.loadFile(uri);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Resets settings if the setting is enabled to do that
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("savePrefsOnExit", true)) {
            @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.clear();
            editor.putBoolean("savePrefsOnExit", false);
            editor.apply();
            PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        }

        // Android things (including setting/adjusting layout)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Sets default settings values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Adjusts the FAB to always be tappable (above navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomAppBar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.setOnApplyWindowInsetsListener(binding.FAB, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Adjusts the FAB's position
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = insets.bottom / 2;
            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });

        // On click action for the FAB
        // TODO: see if commented SystemUI darkening should be removed or not
        int initialSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
        binding.FAB.setOnClickListener(view -> {
            settingsBottomSheet.show(getSupportFragmentManager(), ModalBottomSheet.TAG);
        });
        // Right click/press and hold for the FAB
        registerForContextMenu(binding.FAB);

        // On click action for the bottom bar
        /*BottomNavigationView bottomNavigationView;
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(this::onToolSelected);
        binding.drawCanvas.setTool(DrawCanvas.TOOLS.paint);*/
    }

    /**
     * Sets the document size of a drawCanvas.
     *
     * @param x The new width of the document
     * @param y The new height of the document
     */
    public void setCanvasSize(float x, float y) {
        binding.drawCanvas.documentSize.set(x, y);
        binding.drawCanvas.invalidate();
    }


    /**
     * Whenever something with a context menu is activated, that opens the context menu
     *
     * @param menu The context menu that is being built
     * @param v The view for which the context menu is being built
     * @param menuInfo Extra information about the item for which the
     *            context menu should be shown. This information will vary
     *            depending on the class of v.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }


    /**
     * Switches the DrawCanvas tool when a tool has been selected from the UI.
     * @param item MenuItem for the selected tool
     * @return some boolean, I don't know and didn't look up why
     */
    public boolean onToolSelected(MenuItem item) {
        DrawCanvas.TOOLS tool = DrawCanvas.TOOLS.none;
        int id = item.getItemId();
        System.out.print(id);
        // Can't use a switch statement because we get an error about R.id.* not being constant
        if (id == R.id.tool_paintbrush)
            tool = DrawCanvas.TOOLS.paint;
        if (id == R.id.tool_eraser)
            tool = DrawCanvas.TOOLS.eraser;
        if (id == R.id.tool_pan) tool = DrawCanvas.TOOLS.pan;
        if (id == R.id.tool_select)
            tool = DrawCanvas.TOOLS.select;
        binding.drawCanvas.setTool(tool);
        return true;
    }

    /**
     * Handles clicks for ActionBar buttons
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        // If the item has an icon, it is a tool which you can toggle
        if (item.getIcon() != null) {
            item.setChecked(!item.isChecked());
            // If the item has been checked, uncheck everything else.
            if (item.isChecked()) {
                // TODO: Tool buttons are identified based on having an image. Find some better way to do this.
                for (int i = 0; i < topMenu.size(); i++) {
                    if (topMenu.getItem(i).getIcon() != null && topMenu.getItem(i) != item) {
                        topMenu.getItem(i).setChecked(false);
                    }
                }
            }
        }

        // TODO: switch statement???

        if (id == R.id.action_save || id == R.id.action_load) {
            intentAction = id == R.id.action_save ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT;
            Intent intent = new Intent(intentAction);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/svg+xml");
            intent.putExtra(Intent.EXTRA_TITLE, id == R.id.action_save ? "output.svg" : "input.svg");
            intent = Intent.createChooser(intent, "Save/load file");
            activityResultLauncher.launch(intent);
            return true;
        }

        if (id == R.id.action_undo) {
            binding.drawCanvas.undo();
        }

        if (id == R.id.action_redo) {
            binding.drawCanvas.redo();
        }

        if (id == R.id.action_settings) {
            settingsBottomSheet.show(getSupportFragmentManager(), ModalBottomSheet.TAG);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates the info bar with current scaling and position information.
     */
    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    public void updateInfoBar() {
        binding.infoBar.setText(String.format("%.2fx, (%.1f, %.1f)",
                binding.drawCanvas.getScaleFactor(),
                binding.drawCanvas.getPosition().x,
                binding.drawCanvas.getPosition().y
        ));
    }

    /**
     * SETTINGS FRAGMENT (uses a separate module to use a numeric keyboard for int prefs)
     */
    public static class PreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private boolean paused = true;

        @Override
        public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            Objects.requireNonNull(getPreferenceScreen().getSharedPreferences()).registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // TODO: First line: When model sheets are hidden, getContext() just becomes null and the sheets aren't destroyed
            if (getContext() == null) return;
            if (paused) {
                // Either updates the fragment if preferences have been externally updated
                setPreferencesFromResource(R.xml.preferences, getPreferenceScreen().getKey());
            } else if (getActivity() instanceof MainActivity) {
                // or in some cases like document size, runs code after some settings are set
                MainActivity activity = (MainActivity) getActivity();
                activity.setCanvasSize(
                        Float.parseFloat(sharedPreferences.getString("documentWidth", "816")),
                        Float.parseFloat(sharedPreferences.getString("documentHeight", "1056"))
                );
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            paused = false;
        }

        @Override
        public void onPause() {
            super.onPause();
            paused = true;
        }

        @Override
        public void onDisplayPreferenceDialog(@NonNull androidx.preference.Preference preference) {
            if (preference instanceof ColorPreference) {
                ((ColorPreference) preference).showDialog(this, 0);
            } else super.onDisplayPreferenceDialog(preference);
        }
    }

    /**
     * SETTINGS DIALOG
     */
    public static class ModalBottomSheet extends BottomSheetDialogFragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            System.out.println(true);
            // Inflates settings XML
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.moreOptionsPreferences, new PreferencesFragment())
                    .commit();
            return inflater.inflate(R.layout.settings_popup, container, false);
        }

        @Override
        public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
        }

        public static final String TAG = "ModalBottomSheet";
    }
}