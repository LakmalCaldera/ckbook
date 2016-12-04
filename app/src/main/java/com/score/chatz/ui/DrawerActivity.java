package com.score.chatz.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.pojo.DrawerItem;

import java.util.ArrayList;

/**
 * Created by eranga on 12/4/16.
 */

public class DrawerActivity extends AppCompatActivity implements View.OnClickListener {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ListView drawerListView;
    private ActionBarDrawerToggle actionBarDrawerToggle;

    private ImageView homeView;
    private TextView titleText;
    private TextView homeUserText;

    // drawer components
    private ArrayList<DrawerItem> drawerItemList;
    private DrawerAdapter drawerAdapter;

    private Typeface typeface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        typeface = Typeface.createFromAsset(getAssets(), "fonts/GeosansLight.ttf");

        setupToolbar();
        setupActionBar();
        setupDrawer();
        initDrawerList();
        loadSensors();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void setupDrawer() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        //drawerLayout.setScrimColor(getResources().getColor(android.R.color.transparent));
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        homeUserText = (TextView) findViewById(R.id.home_user_text);
        homeUserText.setTypeface(typeface);
        homeUserText.setText("@lambda");
    }

    /**
     * Initialize Drawer list
     */
    private void initDrawerList() {
        // initialize drawer content
        // need to determine selected item according to the currently selected sensor type
        drawerItemList = new ArrayList();
        drawerItemList.add(new DrawerItem("Rahas", R.drawable.rahaslogo, R.drawable.rahaslogo, true));
        drawerItemList.add(new DrawerItem("Friends", R.drawable.rahaslogo, R.drawable.rahaslogo, false));

        drawerAdapter = new DrawerAdapter(this, drawerItemList);
        drawerListView = (ListView) findViewById(R.id.drawer);

        if (drawerListView != null)
            drawerListView.setAdapter(drawerAdapter);

        //drawerListView.setOnItemClickListener(new DrawerItemClickListener());
    }


    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.home_toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setCustomView(getLayoutInflater().inflate(R.layout.home_action_bar_layout, null));
        actionBar.setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowCustomEnabled(true);

        homeView = (ImageView) findViewById(R.id.home_view);
        homeView.setOnClickListener(this);

        titleText = (TextView) findViewById(R.id.title_text);
        titleText.setTypeface(typeface);
    }

    @Override
    public void onClick(View v) {
        if (v == homeView) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(Gravity.LEFT); //CLOSE Nav Drawer!
            } else {
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        }
    }

    /**
     * Load my sensor list fragment
     */
    private void loadSensors() {
        RecentChatListFragment fragment = new RecentChatListFragment();

        // fragment transitions
        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main, fragment);
        transaction.commit();
    }

}
