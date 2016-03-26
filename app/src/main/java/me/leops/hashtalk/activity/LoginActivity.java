package me.leops.hashtalk.activity;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.HashMap;
import java.util.Map;

import fr.tkeunebr.gravatar.Gravatar;
import me.leops.hashtalk.R;

public class LoginActivity extends AppCompatAuthenticatorActivity {
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;

    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void attemptLogin() {
        mEmailView.setError(null);
        mPasswordView.setError(null);

        final String email = mEmailView.getText().toString();
        final String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);

            final AccountManager manager = AccountManager.get(LoginActivity.this);
            final Account account = new Account(email, "me.leops.hashtalk");

            Firebase.setAndroidContext(this);
            final Firebase usersRef = new Firebase("http://hashtalk.firebaseio.com/users");
            usersRef.authWithPassword(email, password, new Firebase.AuthResultHandler() {
                @Override
                public void onAuthenticated(AuthData authData) {
                    finishLogin(manager, account, password);
                }

                @Override
                public void onAuthenticationError(FirebaseError authError) {
                    if(authError.getCode() == FirebaseError.USER_DOES_NOT_EXIST) {
                        usersRef.createUser(email, password, new Firebase.ResultHandler() {
                            @Override
                            public void onSuccess() {
                                usersRef.authWithPassword(email, password, new Firebase.AuthResultHandler() {
                                    @Override
                                    public void onAuthenticated(AuthData authData) {
                                        Map<String, Object> uData = new HashMap<>();
                                        uData.put("displayName", email);
                                        uData.put("avatar",
                                            Gravatar.init()
                                                .with(email)
                                                .build()
                                        );

                                        usersRef.child(authData.getUid()).setValue(uData, new Firebase.CompletionListener() {
                                            @Override
                                            public void onComplete(final FirebaseError dataError, Firebase firebase) {
                                                if(dataError == null) {
                                                    finishLogin(manager, account, password);
                                                } else {
                                                    usersRef.removeUser(email, password, new Firebase.ResultHandler() {
                                                        @Override
                                                        public void onSuccess() {
                                                            showError(dataError.getMessage());
                                                        }

                                                        @Override
                                                        public void onError(FirebaseError rmError) {
                                                            showError(rmError.getMessage());
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onAuthenticationError(FirebaseError authError2) {
                                        showError(authError2.getMessage());
                                    }
                                });
                            }

                            @Override
                            public void onError(FirebaseError createError) {
                                showError(createError.getMessage());
                            }
                        });
                    } else {
                        showError(authError.getMessage());
                    }
                }
            });
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showError(String err) {
        showProgress(false);
        mPasswordView.setError(err);
        mPasswordView.requestFocus();
    }

    private void finishLogin(AccountManager manager, Account account, String password) {
        if (manager.addAccountExplicitly(account, password, null)) {
            Toast.makeText(LoginActivity.this, getString(R.string.account_registered), Toast.LENGTH_SHORT).show();

            Bundle res = new Bundle();
            res.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            res.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            res.putString(AccountManager.KEY_PASSWORD, password);

            setAccountAuthenticatorResult(res);
            finish();
        } else {
            showError(getString(R.string.error_unknown));
        }
    }
}

