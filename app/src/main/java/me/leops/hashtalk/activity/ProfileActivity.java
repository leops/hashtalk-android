package me.leops.hashtalk.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import me.leops.hashtalk.R;

public class ProfileActivity extends AppCompatAuthenticatorActivity {
    private static final String TAG = "ProfileActivity";

    private TextInputEditText nickname;

    private Firebase user;
    private String currentNickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        nickname = (TextInputEditText) findViewById(R.id.nickname);

        Intent intent = getIntent();
        Account account = intent.getParcelableExtra("account");
        String pass = intent.getStringExtra("password");

        if(account == null || pass == null) {
            Log.wtf(TAG, "No account received");
            finish();
            return;
        }

        Firebase.setAndroidContext(this);
        final Firebase fbRef = new Firebase("http://hashtalk.firebaseio.com/users");
        fbRef.authWithPassword(account.name, pass, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                user = fbRef.child(authData.getUid());

                Bundle res = new Bundle();
                res.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
                setAccountAuthenticatorResult(res);

                final ImageView avatar = (ImageView) findViewById(R.id.avatar);

                user.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())
                            return;

                        if (nickname != null) {
                            if(currentNickname == null)
                                nickname.setEnabled(true);

                            currentNickname = dataSnapshot.child("displayName").getValue(String.class);
                            nickname.setText(currentNickname);
                        }

                        if(avatar != null) {
                            Glide.with(ProfileActivity.this)
                                .load(
                                    dataSnapshot.child("avatar")
                                        .getValue(String.class)
                                )
                                .fitCenter()
                                .crossFade(100)
                                .into(avatar);
                        }
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        Log.e(TAG, "onCancelled", firebaseError.toException());
                    }
                });
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                Log.e(TAG, "onAuthenticationError", firebaseError.toException());
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        String txt = nickname.getText().toString();
        if(user != null && !currentNickname.equals(txt)) {
            currentNickname = txt;
            user.child("displayName").setValue(currentNickname);
        }
    }

    public void editProfilePic(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://en.gravatar.com/")));
    }
}
