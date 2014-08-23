package com.leops.hashtalk;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.HashMap;
import java.util.Map;

public class MessagesFragment extends ListFragment {

    private static final String FIREBASE_URL = "https://hashtalk.firebaseio.com";
    private Firebase ref;
    private MessageListAdapter msgListAdapter;
    private LayoutInflater inflater;
    OnMessageClickListener mCallback;
    //private View spinner;

    public interface OnMessageClickListener {
        public void onMessageClicked(String link);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnMessageClickListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnMessageClickListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ref = new Firebase(FIREBASE_URL).child("next");
        this.inflater = inflater;
        View content = inflater.inflate(R.layout.fragment_messages, container, false);
        //this.spinner = content.findViewById(R.id.spinner);
        return content;
    }

    @Override
    public void onStart() {
        super.onStart();

        final ListView listView = getListView();
        msgListAdapter = new MessageListAdapter(ref, inflater, R.layout.template_message);
        listView.setAdapter(msgListAdapter);
        msgListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(msgListAdapter.getCount() - 1);
            }
        });
        msgListAdapter.setMsgClickListener(new LinkClickListener() {
            @Override
            public void onTextLinkClick(View textView, String clickedString) {
                mCallback.onMessageClicked(clickedString);
            }
        });
    }

    public void sendMessage(Message msg) {
        ref.push().setValue(msg);
    }

    public void setQuery(String query) {
        msgListAdapter.getFilter().filter(query);
    }
}

