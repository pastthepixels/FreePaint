package io.github.pastthepixels.freepaint;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.DocumentsContract;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import io.github.pastthepixels.freepaint.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import com.google.android.material.color.DynamicColors;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private boolean _isHidden = false;

    private Menu topMenu;

    private enum FILE_ACTIONS {save, load};

    private String intentAction;

    private MainActivity self = this;
    private ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        System.out.println(uri.getPath());
                        try {
                            if (intentAction == Intent.ACTION_CREATE_DOCUMENT) binding.drawCanvas.saveFile(uri);
                            if (intentAction == Intent.ACTION_OPEN_DOCUMENT) binding.drawCanvas.loadFile(uri);
                        } catch(Exception e) {
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

        //NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        //appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        //
        binding.ExpandToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float deg = 0;
                if (getSupportActionBar().isShowing()) {
                    getSupportActionBar().hide();
                    deg = 180F;
                } else {
                    getSupportActionBar().show();
                }
                binding.ExpandToolbar.animate().rotation(deg).setInterpolator(new AccelerateDecelerateInterpolator());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        topMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            if (item.isChecked() == true) {
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

        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_save || id == R.id.action_load) {
            intentAction = id == R.id.action_save? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT;
            Intent intent = new Intent(intentAction);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/svg+xml");
            intent.putExtra(Intent.EXTRA_TITLE, id == R.id.action_save? "output.svg" : "input.svg");
            intent = Intent.createChooser(intent, "Save/load file");
            activityResultLauncher.launch(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NonConstantResourceId")
    public void updateTool() {
        DrawCanvas.TOOLS tool = DrawCanvas.TOOLS.none;
        for(int i = 0; i < topMenu.size(); i++) {
            if (topMenu.getItem(i).getIcon() != null && topMenu.getItem(i).isChecked() == true) {
                // Can't use a switch statement because we get an error about R.id.* not being constant
                if (topMenu.getItem(i).getItemId() == R.id.tool_paintbrush) tool = DrawCanvas.TOOLS.paint;
                if (topMenu.getItem(i).getItemId() == R.id.tool_eraser) tool = DrawCanvas.TOOLS.eraser;
                if (topMenu.getItem(i).getItemId() == R.id.tool_pan) tool = DrawCanvas.TOOLS.pan;
                if (topMenu.getItem(i).getItemId() == R.id.tool_select) tool = DrawCanvas.TOOLS.select;
            }
        }
        binding.drawCanvas.setTool(tool);
    }

    /*
     * https://developer.android.com/develop/ui/views/components/menus#checkable
     * says we can't make the top icons checkable through some clean, already existing means. :(
     * TODO: Support dark mode
     */
    private void updateMenuItemColor(MenuItem item) {
        Drawable drawable = item.getIcon();
        drawable = DrawableCompat.wrap(drawable);
        if(item.isChecked() == true) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(androidx.appcompat.R.attr.colorAccent, typedValue, true);
            DrawableCompat.setTint(drawable, typedValue.data);
        } else {
            DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.black));
        }
        item.setIcon(drawable);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}