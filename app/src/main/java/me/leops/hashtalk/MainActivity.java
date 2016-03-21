package me.leops.hashtalk;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import jp.wasabeef.recyclerview.animators.LandingAnimator;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    protected RecyclerView mMsgView;
    protected MessagesAdapter mAdapter;
    protected LinearLayoutManager mLayoutManager;
    protected Firebase mFirebaseRef;
    protected SearchView mSearchView;
    protected EditText mContentView;
    protected SharedPreferences mPrefs;
    protected boolean isOpen;
    protected View mCompose;
    protected FloatingActionButton mSend;
    protected EditText mAuthor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getSharedPreferences("prefs", MODE_PRIVATE);

        Firebase.setAndroidContext(this);
        mFirebaseRef = new Firebase("http://hashtalk.firebaseio.com/next");

        mMsgView = (RecyclerView) findViewById(R.id.messageList);

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
        final TextView noFound = (TextView) findViewById(R.id.no_found);

        isOpen = false;
        mCompose = findViewById(R.id.compose);

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
        mMsgView.setLayoutManager(mLayoutManager);

        mMsgView.setItemAnimator(new LandingAnimator());

        mAdapter = new MessagesAdapter(mFirebaseRef);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(noFound != null)
                    noFound.setVisibility(mAdapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                if(progressBar != null)
                    progressBar.setVisibility(View.GONE);
            }
        });
        mMsgView.setAdapter(mAdapter);

        mSend = (FloatingActionButton) findViewById(R.id.send);
        mAuthor = (EditText) findViewById(R.id.nickname);
        mContentView = (EditText) findViewById(R.id.message);

        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isOpen) {
                    CharSequence txt = mContentView.getText();

                    String a = mAuthor.getText().toString();
                    String c = txt.toString();
                    long t = System.currentTimeMillis() / 10;

                    List<String> h = new ArrayList<>();
                    h.add(mSearchView.getQuery().toString());

                    Matcher hm = Message.RX_HASHTAG.matcher(txt);
                    while (hm.find()) {
                        h.add(hm.group(1));
                    }

                    send(new Message(a, c, h, t));
                } else {
                    setCompose(true);

                    if(mAuthor.getText().length() == 0) {
                        mAuthor.requestFocus();
                    } else {
                        mContentView.requestFocus();
                    }
                }
            }
        });

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

        mAuthor.addTextChangedListener(new TextWatcher() {
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
                updateFAB();
            }
        });

        mAuthor.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(!b) mPrefs.edit().putString("nickname", mAuthor.getText().toString()).apply();
            }
        });

        if(mPrefs.contains("nickname")) {
            mAuthor.setText(mPrefs.getString("nickname", ""));
        }

        mMsgView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(isOpen) setCompose(false);
            }
        });
        mMsgView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(isOpen) setCompose(false);
                return false;
            }
        });
        mMsgView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                if(isOpen) setCompose(false);
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                // NOOP
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                MenuItemCompat.collapseActionView(searchItem);
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

        return id == R.id.action_search || super.onOptionsItemSelected(item);
    }

    protected void setCompose(final boolean open) {
        int x = (mSend.getLeft() - mCompose.getLeft()) + (mSend.getWidth() / 2),
            y = (mSend.getTop() - mCompose.getTop()) + (mSend.getHeight() / 2);
        float radius = (float) Math.hypot(mCompose.getWidth(), mCompose.getHeight());

        isOpen = open;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animator anim;
            if (isOpen) {
                anim = ViewAnimationUtils.createCircularReveal(mCompose, x, y, 0, radius);
                mCompose.setVisibility(View.VISIBLE);
                updateFAB();
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                    }
                });
            } else {
                anim = ViewAnimationUtils.createCircularReveal(mCompose, x, y, radius, 0);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mCompose.setVisibility(View.GONE);
                        updateFAB();
                    }
                });
            }

            anim.start();
        } else {
            mCompose.setVisibility(isOpen ? View.VISIBLE : View.GONE);
            updateFAB();
        }
    }

    protected void updateFAB() {
        boolean enabled = !isOpen || mAuthor.length() > 0;
        mSend.setEnabled(enabled);
        mSend.setImageResource(isOpen ? R.drawable.send_icon : R.drawable.compose_icon);

        int color;
        int colorId = enabled ? R.color.green : R.color.inactive;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            color = getResources().getColor(colorId, getTheme());
        } else {
            color = getResources().getColor(colorId);
        }
        mSend.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    public void send(Message msg) {
        mFirebaseRef.push().setValue(msg, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if(firebaseError != null) {
                    Log.e(TAG, firebaseError.getDetails());

                    Toast.makeText(MainActivity.this, "Unknown error", Toast.LENGTH_SHORT).show();
                } else {
                    mContentView.setText("");
                    mMsgView.smoothScrollToPosition(mAdapter.getItemCount());
                    setCompose(false);
                }
            }
        });
    }
}
