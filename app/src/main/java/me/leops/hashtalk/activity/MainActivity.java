package me.leops.hashtalk.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ServerValue;

import java.util.HashMap;
import java.util.Map;

import jp.wasabeef.recyclerview.animators.LandingAnimator;
import me.leops.hashtalk.R;
import me.leops.hashtalk.messages.MessagesAdapter;
import me.leops.hashtalk.messages.ScrollBehavior;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView mInfoText;
    private ProgressBar mProgress;
    private SearchView mSearchView;

    private Firebase mFirebaseRef;
    private AccountManager mManager;

    private RecyclerView mMsgView;
    private Account mAccount;
    private String mUser;
    private MessagesAdapter mAdapter;
    private EditText mContentView;
    private FloatingActionButton mSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupSearch();
        setupLayout();
        setupFirebase();
        setupForm();
    }

    private void setupSearch() {
        mInfoText = (TextView) findViewById(R.id.no_found);
        mProgress = (ProgressBar) findViewById(R.id.login_progress);
        mSearchView = (SearchView) findViewById(R.id.hashtag);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(mAdapter != null)
                    mAdapter.setFilter(newText);

                if(newText.length() > 0) {
                    onStartLoading();
                } else {
                    mInfoText.setText(R.string.info_empty);
                    mInfoText.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });
    }

    private void setupLayout() {
        View sView = findViewById(R.id.scrollView);
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) sView.getLayoutParams();
        ScrollBehavior behavior = (ScrollBehavior) lp.getBehavior();
        behavior.setSearchView(mSearchView);
    }

    private void setupFirebase() {
        Firebase.setAndroidContext(this);
        mFirebaseRef = new Firebase("http://hashtalk.firebaseio.com/");

        mManager = AccountManager.get(this);
        chooseAccount();
    }

    private void chooseAccount() {
        final Account[] accounts = mManager.getAccountsByType("me.leops.hashtalk");
        if (accounts.length == 1) {
            fetch(accounts[0]);
        } else if (accounts.length > 1) {
            String[] names = new String[accounts.length];
            for (int i = 0; i < accounts.length; i++) {
                names[i] = accounts[i].name;
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.pick_account)
                    .setItems(names, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            fetch(accounts[which]);
                        }
                    })
                    .create().show();

        } else {
            mManager.addAccount("me.leops.hashtalk", null, null, null, this, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> future) {
                    try {
                        future.getResult();
                        chooseAccount();
                    } catch (Exception e) {
                        Log.e(TAG, "AddAccount error", e);
                        finish();
                    }
                }
            }, null);
        }
    }

    private void fetch(@NonNull Account account) {
        mAccount = account;

        String pass = mManager.getPassword(account);
        mFirebaseRef.authWithPassword(account.name, pass, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                mUser = authData.getUid();
                setupList();
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Log.e(TAG, "Authentication error", firebaseError.toException());
                finish();
            }
        });
    }

    private void setupList() {
        mMsgView = (RecyclerView) findViewById(R.id.messageList);
        mMsgView.setLayoutManager(new LinearLayoutManager(this));

        mMsgView.setItemAnimator(new LandingAnimator());

        mAdapter = new MessagesAdapter(this, getFirebase("messages"));
        mMsgView.setAdapter(mAdapter);

        mProgress.setVisibility(View.GONE);
        mInfoText.setVisibility(View.VISIBLE);
    }

    private void setupForm() {
        mSend = (FloatingActionButton) findViewById(R.id.send);
        mContentView = (EditText) findViewById(R.id.message);

        mContentView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEND) {
                    mSend.performClick();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.profile:
                if(mUser != null && mAccount != null) {
                    Intent intent = new Intent(this, ProfileActivity.class);
                    intent.putExtra("account", mAccount);
                    intent.putExtra("password", mManager.getPassword(mAccount));

                    startActivity(intent);
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void send(View view) {
        String hashtag = mSearchView.getQuery().toString();
        if(hashtag.length() == 0)
            return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("author", mUser);
        msg.put("content", mContentView.getText().toString());
        msg.put("time", ServerValue.TIMESTAMP);

        getFirebase("messages")
            .child(hashtag)
            .push().setValue(msg, new Firebase.CompletionListener() {
                @Override
                public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                    if(firebaseError != null) {
                        Log.e(TAG, "Send error: " + firebaseError.getDetails(), firebaseError.toException());
                        Snackbar.make(mMsgView, getString(R.string.error_unknown), Snackbar.LENGTH_SHORT).show();
                    } else {
                        mContentView.setText("");
                        mMsgView.smoothScrollToPosition(mAdapter.getItemCount());
                    }
                }
            });
    }

    private void onStartLoading() {
        mSend.setEnabled(false);
        mInfoText.setVisibility(View.GONE);
        mProgress.setVisibility(View.VISIBLE);
    }

    public void onDoneLoading(boolean hasContent) {
        mInfoText.setText(R.string.info_no_found);
        mInfoText.setVisibility(hasContent ? View.GONE : View.VISIBLE);
        mProgress.setVisibility(View.GONE);
        mSend.setEnabled(true);
    }

    public void setQuery(String query) {
        mSearchView.setQuery(query, false);
    }

    public void addMention(String name) {
        Editable current = mContentView.getText();
        if(current.length() > 0)
            current.append(' ');

        mContentView.setText(current.append('@').append(name));
    }

    public Firebase getFirebase(@Nullable String child) {
        if(child == null)
            return mFirebaseRef;
        return mFirebaseRef.child(child);
    }
}
