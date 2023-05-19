package io.github.pastthepixels.freepaint;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

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

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private boolean _isHidden = false;

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

    private Menu topMenu;
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
        if(item.getIcon() != null) {
            item.setChecked(!item.isChecked());
            updateMenuItemColor(item);
            // If the item has been checked, uncheck everything else.
            if (item.isChecked() == true) {
                // TODO: Tool buttons are identified based on having an image. Find some better way to do this.
                for(int i = 0; i < topMenu.size(); i++) {
                    if (topMenu.getItem(i).getIcon() != null && topMenu.getItem(i) != item) {
                        topMenu.getItem(i).setChecked(false);
                        updateMenuItemColor(topMenu.getItem(i));
                    }
                }
            }
            // Set the current tool to the selected image button.
            updateTool();
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateTool() {
        DrawCanvas.TOOLS tool = DrawCanvas.TOOLS.none;
        for(int i = 0; i < topMenu.size(); i++) {
            if (topMenu.getItem(i).getIcon() != null && topMenu.getItem(i).isChecked() == true) {
                // Can't use a switch statement because we get an error about R.id.* not being constant
                if (topMenu.getItem(i).getItemId() == R.id.tool_paintbrush) tool = DrawCanvas.TOOLS.paint;
                if (topMenu.getItem(i).getItemId() == R.id.tool_eraser) tool = DrawCanvas.TOOLS.eraser;
                if (topMenu.getItem(i).getItemId() == R.id.tool_pan) tool = DrawCanvas.TOOLS.pan;
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
            DrawableCompat.setTint(drawable, ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_primary));
        } else {
            DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.black));
        }
        item.setIcon(drawable);
        System.out.println(item.isChecked());
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}