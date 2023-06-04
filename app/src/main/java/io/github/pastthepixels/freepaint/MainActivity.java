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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.rarepebble.colorpicker.ColorPreference;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import io.github.pastthepixels.freepaint.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private BottomSheetBehavior sheetBehavior;

    private Menu topMenu;

    private String intentAction;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        System.out.println(uri.getPath());
                        try {
                            if (intentAction == Intent.ACTION_CREATE_DOCUMENT)
                                binding.drawCanvas.saveFile(uri);
                            if (intentAction == Intent.ACTION_OPEN_DOCUMENT)
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

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);


        // The "more options" dialog
        LinearLayout moreOptions = findViewById(R.id.more_options);
        int initialOptionsPaddingTop = moreOptions.getPaddingTop();
        int initialOptionsPaddingBottom = moreOptions.getPaddingBottom();
        sheetBehavior = BottomSheetBehavior.from(moreOptions);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);


        // Adjusts the FAB to always be tappable (above navigation bars)
        ViewGroup.MarginLayoutParams initialMarginLayoutParams = (ViewGroup.MarginLayoutParams) binding.ExpandToolbar.getLayoutParams();
        int bottomMargin = initialMarginLayoutParams.bottomMargin;
        ViewCompat.setOnApplyWindowInsetsListener(binding.ExpandToolbar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Adjusts the FAB's position
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = bottomMargin + insets.bottom;
            v.setLayoutParams(mlp);
            // Adjusts margins for moreOptions
            moreOptions.setPadding(
                    moreOptions.getPaddingLeft(),
                    initialOptionsPaddingTop + insets.top,
                    moreOptions.getPaddingRight(),
                    initialOptionsPaddingBottom + insets.bottom
            );

            return WindowInsetsCompat.CONSUMED;
        });

        //
        int initialSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
        binding.ExpandToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float deg = 0;
                if (getSupportActionBar().isShowing()) {
                    // See https://stackoverflow.com/questions/30075827/android-statusbar-icons-color
                    getWindow().getDecorView().setSystemUiVisibility(initialSystemUiVisibility | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    getSupportActionBar().hide();
                    deg = 180F;
                } else {
                    getWindow().getDecorView().setSystemUiVisibility(initialSystemUiVisibility);
                    getSupportActionBar().show();
                }
                binding.ExpandToolbar.animate().rotation(deg).setInterpolator(new AccelerateDecelerateInterpolator());
            }
        });

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.moreOptionsPreferences, new PreferencesFragment())
                .commit();

    }

    public void setCanvasSize(float x, float y) {
        binding.drawCanvas.documentSize.set(x, y);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        topMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        for (int i = 0; i < topMenu.size(); i++) {
            if (topMenu.getItem(i).getIcon() != null) {
                updateMenuItemColor(topMenu.getItem(i));
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        // If the item has an icon, it is a tool which you can toggle
        if (item.getIcon() != null) {
            item.setChecked(!item.isChecked());
            updateMenuItemColor(item);
            // If the item has been checked, uncheck everything else.
            if (item.isChecked()) {
                // TODO: Tool buttons are identified based on having an image. Find some better way to do this.
                for (int i = 0; i < topMenu.size(); i++) {
                    if (topMenu.getItem(i).getIcon() != null && topMenu.getItem(i) != item) {
                        topMenu.getItem(i).setChecked(false);
                        updateMenuItemColor(topMenu.getItem(i));
                    }
                }
            }
            // Set the current tool to the selected image button.
            updateTool();
        }

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

        if (id == R.id.action_settings) {
            if (sheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NonConstantResourceId")
    public void updateTool() {
        DrawCanvas.TOOLS tool = DrawCanvas.TOOLS.none;
        for (int i = 0; i < topMenu.size(); i++) {
            if (topMenu.getItem(i).getIcon() != null && topMenu.getItem(i).isChecked()) {
                // Can't use a switch statement because we get an error about R.id.* not being constant
                if (topMenu.getItem(i).getItemId() == R.id.tool_paintbrush)
                    tool = DrawCanvas.TOOLS.paint;
                if (topMenu.getItem(i).getItemId() == R.id.tool_eraser)
                    tool = DrawCanvas.TOOLS.eraser;
                if (topMenu.getItem(i).getItemId() == R.id.tool_pan) tool = DrawCanvas.TOOLS.pan;
                if (topMenu.getItem(i).getItemId() == R.id.tool_select)
                    tool = DrawCanvas.TOOLS.select;
            }
        }
        binding.drawCanvas.setTool(tool);
    }

    /*
     * https://developer.android.com/develop/ui/views/components/menus#checkable
     * says we can't make the top icons checkable through some clean, already existing means. :(
     */
    private void updateMenuItemColor(MenuItem item) {
        Drawable drawable = item.getIcon();
        drawable = DrawableCompat.wrap(drawable);

        // See https://stackoverflow.com/questions/75943818/how-can-i-access-theme-color-attributes-via-r-attr-colorprimary
        if (item.isChecked()) {
            TypedValue typedValue = new TypedValue();
            this.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
            DrawableCompat.setTint(drawable, typedValue.data);
        } else {
            TypedValue typedValue = new TypedValue();
            this.getTheme().resolveAttribute(com.google.android.material.R.attr.colorControlNormal, typedValue, true);
            DrawableCompat.setTint(drawable, typedValue.data);
            //DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.textColorPrimary));
        }
        item.setIcon(drawable);
    }

    // Updates top bar icons when the user switches to dark mode
    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, resid, first);
        if (topMenu != null) {
            for (int i = 0; i < topMenu.size(); i++) {
                if (topMenu.getItem(i).getIcon() != null) {
                    updateMenuItemColor(topMenu.getItem(i));
                }
            }
        }
    }

    static public class PreferencesFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private boolean paused = true;

        @Override
        public void onCreatePreferencesFix(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
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
}