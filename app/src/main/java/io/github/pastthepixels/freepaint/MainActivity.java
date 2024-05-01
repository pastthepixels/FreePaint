package io.github.pastthepixels.freepaint;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rarepebble.colorpicker.ColorPreference;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.util.Objects;

import io.github.pastthepixels.freepaint.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private final SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();

    private final ToolsBottomSheet toolsBottomSheet = new ToolsBottomSheet();

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
        binding.FAB.setOnClickListener(view -> {
            toolsBottomSheet.show(getSupportFragmentManager(), ToolsBottomSheet.TAG);
        });

        // On click action for the bottom bar
        binding.bottomAppBar.setOnMenuItemClickListener(this::onBottomBarItemSelected);

        // Sets default tool
        setTool(R.id.select_tool_paintbrush);
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
     * @param id the ID of the tool button
     */
    public void setTool(int id) {
        DrawCanvas.TOOLS tool = DrawCanvas.TOOLS.none;
        if (id == R.id.select_tool_paintbrush) {
            binding.FAB.setImageResource(R.drawable.baseline_brush_24);
            tool = DrawCanvas.TOOLS.paint;
        }
        if (id == R.id.select_tool_eraser) {
            binding.FAB.setImageResource(R.drawable.baseline_eraser_24);
            tool = DrawCanvas.TOOLS.eraser;
        }
        if (id == R.id.select_tool_pan) {
            binding.FAB.setImageResource(R.drawable.baseline_pan_tool_24);
            tool = DrawCanvas.TOOLS.pan;
        }
        if (id == R.id.select_tool_select) {
            binding.FAB.setImageResource(R.drawable.baseline_select_all_24);
            tool = DrawCanvas.TOOLS.select;
        }
        binding.drawCanvas.setTool(tool);
    }

    /**
     * Handles clicks for ActionBar buttons
     */
    public boolean onBottomBarItemSelected(MenuItem item) {
        int id = item.getItemId();

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
            settingsBottomSheet.show(getSupportFragmentManager(), SettingsBottomSheet.TAG);
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
    public static class SettingsBottomSheet extends BottomSheetDialogFragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        public static final String TAG = "SettingsBottomSheet";
    }

    /**
     * Tool selection dialog
     */
    public static class ToolsBottomSheet extends BottomSheetDialogFragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.tool_popup, container, false);
            // Manually connect each button...
            Button[] buttons = {
                    view.findViewById(R.id.select_tool_paintbrush),
                    view.findViewById(R.id.select_tool_eraser),
                    view.findViewById(R.id.select_tool_pan),
                    view.findViewById(R.id.select_tool_select),
            };
            for (Button button : buttons) {
                button.setOnClickListener(event -> {
                    // Hack but we can do this because the activity is always MainActivity
                    ((MainActivity) requireActivity()).setTool(button.getId());
                    // Dismiss the bottom sheet.
                    dismiss();
                });
            }
            // Done
            return view;
        }

        @Override
        public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
        }

        public static final String TAG = "ToolsBottomSheet";
    }
}