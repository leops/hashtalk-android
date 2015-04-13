package me.leops.hashtalk;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.melnykov.fab.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import jp.wasabeef.recyclerview.animators.LandingAnimator;


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    protected RecyclerView mMsgView;
    protected MessagesAdapter mAdapter;
    protected LinearLayoutManager mLayoutManager;
    protected Firebase mFirebaseRef;
    protected SearchView mSearchView;
    protected EditText mContentView;
    protected SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fresco.initialize(getApplicationContext());

        mPrefs = getSharedPreferences("prefs", MODE_PRIVATE);

        Firebase.setAndroidContext(this);
        mFirebaseRef = new Firebase("https://hashtalk.firebaseio.com/next");

        mMsgView = (RecyclerView) findViewById(R.id.messageList);

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
        final TextView noFound = (TextView) findViewById(R.id.no_found);

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mMsgView.setLayoutManager(mLayoutManager);

        mMsgView.setItemAnimator(new LandingAnimator());

        mAdapter = new MessagesAdapter(mFirebaseRef);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                noFound.setVisibility(mAdapter.getItemCount() > 0 ? View.INVISIBLE : View.VISIBLE);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                progressBar.setVisibility(View.GONE);
            }
        });
        mMsgView.setAdapter(mAdapter);

        final FloatingActionButton btn = (FloatingActionButton) findViewById(R.id.send);
        final EditText author = (EditText) findViewById(R.id.nickname);
        mContentView = (EditText) findViewById(R.id.message);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CharSequence txt = mContentView.getText();

                String a = author.getText().toString();
                String c = txt.toString();
                long t = System.currentTimeMillis() / 10;

                List<String> h = new ArrayList<>();
                h.add(mSearchView.getQuery().toString());

                Matcher hm = MessageHolder.hashtag.matcher(txt);
                while (hm.find()) {
                    h.add(hm.group(1));
                }

                send(new Message(a, c, h, t));
            }
        });

        mContentView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEND) {
                    btn.performClick();
                    return true;
                }
                return false;
            }
        });

        author.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // NOOP
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // NOOP
            }

            @Override
            public void afterTextChanged(Editable editable) {
                boolean enabled = editable.length() > 0;
                btn.setEnabled(enabled);
                btn.setShadow(enabled);
                btn.setColorNormalResId(enabled ? R.color.green : R.color.green_light);
            }
        });

        author.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(!b) mPrefs.edit().putString("nickname", author.getText().toString()).commit();
            }
        });

        if(mPrefs.contains("nickname")) {
            author.setText(mPrefs.getString("nickname", ""));
            mContentView.requestFocus();
        } else {
            author.requestFocus();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) searchItem.getActionView();

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchItem.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                mAdapter.getFilter().filter(s);
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search || super.onOptionsItemSelected(item))
            return true;
        else
            return false;
    }

    public void send(Message msg) {
        mFirebaseRef.push().setValue(msg, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if(firebaseError != null) {
                    Log.e(TAG, firebaseError.getDetails());

                    Toast toast = Toast.makeText(getApplicationContext(), "Unknown error", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    mContentView.setText("");
                    mMsgView.smoothScrollToPosition(mAdapter.getItemCount());
                }
            }
        });
    }
}
