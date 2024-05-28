package io.github.pastthepixels.freepaint;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.rarepebble.colorpicker.ColorPickerView;
import com.rarepebble.colorpicker.ColorPreference;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.io.OutputStream;
import java.util.Objects;

import io.github.pastthepixels.freepaint.Graphics.DrawCanvas;
import io.github.pastthepixels.freepaint.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private final SettingsBottomSheet settingsBottomSheet = new SettingsBottomSheet();
    private final ToolsBottomSheet toolsBottomSheet = new ToolsBottomSheet();
    private ActivityMainBinding binding;
    private int currentToolId = R.id.select_tool_paintbrush;

    /**
     * Handles file picker actions -- onActivityResult is called after a file path is chosen (see MainActivity.onOptionsItemSelected)
     */
    private final ActivityResultLauncher<Intent> activityResultLauncherSave = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (result) -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    System.out.println(Objects.requireNonNull(uri).getPath());
                    try {
                        binding.drawCanvas.saveFile(uri);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "An error was encountered while saving.", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> activityResultLauncherLoad = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (result) -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    System.out.println(Objects.requireNonNull(uri).getPath());
                    try {
                        binding.drawCanvas.loadFile(uri);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "An error was encountered while loading.", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> activityResultLauncherExport = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (result) -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    System.out.println(Objects.requireNonNull(uri).getPath());
                    try (OutputStream stream = getApplicationContext().getContentResolver().openOutputStream(uri, "wt")) {
                        assert stream != null;
                        binding.drawCanvas.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "An error was encountered while loading.", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
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

        // Styles the info bar
        // Courtesy of https://stackoverflow.com/questions/18781902/rounded-corner-for-textview-in-android/61768682#61768682
        float radius = getResources().getDimension(com.google.android.material.R.dimen.cardview_default_radius);
        ShapeAppearanceModel shapeAppearanceModel = new ShapeAppearanceModel()
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, radius)
                .build();
        MaterialShapeDrawable shapeDrawable = new MaterialShapeDrawable(shapeAppearanceModel);
        shapeDrawable.setAlpha(100);
        ViewCompat.setBackground(binding.infoBar, shapeDrawable);

        // Adjusts views on the edges to always be tappable (above navigation bars)
        ViewGroup.MarginLayoutParams infobarParams = (ViewGroup.MarginLayoutParams) binding.infoBar.getLayoutParams();
        ViewCompat.setOnApplyWindowInsetsListener(binding.infoBar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(infobarParams.leftMargin, insets.top + infobarParams.topMargin, 0, 0);
            binding.infoBar.setLayoutParams(params);
            return WindowInsetsCompat.CONSUMED;
        });
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

        updateBottomBarColors(
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt("fillColor", 0x10000000),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt("strokeColor", 0x10000000)
        );
    }

    /**
     * Updates the fill/stroke colors in the bottom bar.
     * FIXME: uses magic numbers for indices
     */
    public void updateBottomBarColors(int fillColor, int strokeColor) {
        Menu menu = binding.bottomAppBar.getMenu();
        MenuItem fillItem = menu.getItem(3);
        MenuItem strokeItem = menu.getItem(2);
        Drawable fillDrawable = fillItem.getIcon();
        Drawable strokeDrawable = strokeItem.getIcon();
        DrawableCompat.setTint(fillDrawable, fillColor);
        DrawableCompat.setTint(strokeDrawable, strokeColor);
        fillItem.setIcon(fillDrawable);
        strokeItem.setIcon(strokeDrawable);
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
     * Switches the DrawCanvas tool when a tool has been selected from the UI.
     *
     * @param id the ID of the tool button
     */
    public void setTool(int id) {
        currentToolId = id;
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
        if (id == R.id.select_tool_spraypaint) {
            binding.FAB.setImageResource(R.drawable.baseline_spraypaint_24);
            tool = DrawCanvas.TOOLS.spraypaint;
        }
        binding.drawCanvas.setTool(tool);
    }

    /**
     * Handles clicks for ActionBar buttons
     */
    public boolean onBottomBarItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_save || id == R.id.action_load) {
            Intent intent = new Intent(id == R.id.action_save ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/svg+xml");
            intent.putExtra(Intent.EXTRA_TITLE, id == R.id.action_save ? "output.svg" : "input.svg");
            intent = Intent.createChooser(intent, "Save/load file");
            if (id == R.id.action_save) {
                activityResultLauncherSave.launch(intent);
            } else {
                activityResultLauncherLoad.launch(intent);
            }
            return true;
        }

        if (id == R.id.action_export) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_TITLE, "exported.png");
            intent = Intent.createChooser(intent, "Export image");
            activityResultLauncherExport.launch(intent);
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

        if (id == R.id.action_set_fill) {
            (new ColorPickerDialog("fillColor")).show(getSupportFragmentManager(), "COLOR_PICKER_DIALOG");
        }

        if (id == R.id.action_set_stroke) {
            (new ColorPickerDialog("strokeColor")).show(getSupportFragmentManager(), "COLOR_PICKER_DIALOG");
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
                activity.updateBottomBarColors(
                        sharedPreferences.getInt("fillColor", 0x10000000),
                        sharedPreferences.getInt("strokeColor", 0x10000000)
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

        public static final String TAG = "SettingsBottomSheet";

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
    }

    /**
     * Tool selection dialog
     */
    public static class ToolsBottomSheet extends BottomSheetDialogFragment {

        public static final String TAG = "ToolsBottomSheet";

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
                    view.findViewById(R.id.select_tool_spraypaint),
            };
            for (Button button : buttons) {
                button.setOnClickListener(event -> {
                    // Hack but we can do this because the activity is always MainActivity
                    ((MainActivity) requireActivity()).setTool(button.getId());
                    // Dismiss the bottom sheet.
                    dismiss();
                });
            }

            // Set background resource for all buttons
            for (Button button : buttons) {
                if (button.getId() == ((MainActivity) requireActivity()).currentToolId) {
                    button.setBackgroundResource(R.drawable.selected_button);
                    button.setTextColor(getResources().getColor(R.color.white));
                } else {
                    button.setBackgroundResource(R.drawable.not_selected_button);
                    button.setTextColor(getResources().getColor(R.color.md_theme_light_primary));
                }
            }
            // Done
            return view;
        }

        @Override
        public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
        }
    }


    /*
     * Color picker dialog that attempts to recreate the one in the preferences screen
     *
     * FIXME: Button strings in English
     */
    public static class ColorPickerDialog extends DialogFragment {

        String prefName;

        ColorPickerDialog(String prefName) {
            this.prefName = prefName;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction.
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final ColorPickerView picker = new ColorPickerView(getContext());
            picker.setColor(PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt(prefName, Color.BLACK));
            System.out.println(picker.getColor());

            builder
                    .setTitle(null)
                    .setView(picker)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putInt(prefName, picker.getColor());
                        editor.apply();
                        // Then, update MainActivity
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).updateBottomBarColors(
                                    PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt("fillColor", 0x10000000),
                                    PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt("strokeColor", 0x10000000)
                            );
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, id) -> {
                    });
            // Create the AlertDialog object and return it.
            return builder.create();
        }
    }

}