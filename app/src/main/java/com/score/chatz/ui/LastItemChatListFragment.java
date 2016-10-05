package com.score.chatz.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.db.SenzorsDbSource;
import com.score.chatz.pojo.Secret;
import com.score.senzc.enums.SenzTypeEnum;
import com.score.senzc.pojos.Senz;

import java.util.ArrayList;

/**
 * Created by lakmalcaldera on 8/19/16.
 */
public class LastItemChatListFragment extends ListFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = LastItemChatListFragment.class.getName();

    private ArrayList<Secret> allSecretsList;
    private LastItemChatListAdapter adapter;
    private SenzorsDbSource dbSource;

    private BroadcastReceiver senzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Got new user from Senz service");

            Senz senz = intent.getExtras().getParcelable("SENZ");
            if (senz.getSenzType() == SenzTypeEnum.SHARE) {
                displayUserList();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        dbSource = new SenzorsDbSource(getContext());
        return inflater.inflate(R.layout.last_item_chat_list_fragment, container, false);
    }

    private void setupEmptyTextFont() {
        ((TextView) getActivity().findViewById(R.id.empty_view_chat)).setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/GeosansLight.ttf"));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupEmptyTextFont();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        displayUserList();
        setupActionBar(false);

        //getActivity().registerReceiver(senzReceiver, IntentProvider.getIntentFilter(IntentProvider.INTENT_TYPE.SENZ));
    }

    @Override
    public void onPause() {
        super.onPause();

        //getActivity().unregisterReceiver(senzReceiver);
    }

    /**
     * Display sensor list
     * Basically setup list adapter if have items to display otherwise display empty view
     */
    private void displayUserList() {
        allSecretsList = dbSource.getLatestChatMessages();
        adapter = new LastItemChatListAdapter(getContext(), allSecretsList);
        getListView().setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Secret secret = allSecretsList.get(position);
        if (secret.isViewed()) {
            secret.setViewed(false);
            adapter.notifyDataSetChanged();
            setupActionBar(false);
        } else {
            Intent intent = new Intent(this.getActivity(), ChatActivity.class);
            intent.putExtra("SENDER", allSecretsList.get(position).getUser().getUsername());
            startActivity(intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Secret secret = allSecretsList.get(position);
        secret.setViewed(true);
        adapter.notifyDataSetChanged();
        setupActionBar(true);

        return true;
    }

    private void setupActionBar(boolean enableDelete) {
        ImageView delete = (ImageView) ((AppCompatActivity) getActivity()).getSupportActionBar().getCustomView().findViewById(R.id.delete);
        TextView name = (TextView) ((AppCompatActivity) getActivity()).getSupportActionBar().getCustomView().findViewById(R.id.user_name);

        if (enableDelete) {
            delete.setVisibility(View.VISIBLE);
            name.setVisibility(View.GONE);
        } else {
            delete.setVisibility(View.GONE);
            name.setVisibility(View.VISIBLE);
        }
    }
}
