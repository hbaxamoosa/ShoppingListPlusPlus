package com.udacity.firebase.shoppinglistplusplus.ui.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.ui.BaseActivity;
import com.udacity.firebase.shoppinglistplusplus.ui.MainActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.util.HashMap;

import timber.log.Timber;

/**
 * Represents Sign in screen and functionality of the app
 */
public class LoginActivity extends BaseActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    /* Request code used to invoke sign in user interactions for Google+ */
    public static final int RC_GOOGLE_LOGIN = 1;
    public static final String ANONYMOUS = "anonymous";
    private static final String LOG_TAG = LoginActivity.class.getSimpleName();
    // Firebase Analytics
    public static FirebaseAnalytics mFirebaseAnalytics;
    protected String mProvider, mEncodedEmail;
    /* Client used to interact with Google APIs. */
    protected GoogleApiClient mGoogleApiClient;
    /* A dialog that is presented until the Firebase authentication finished. */
    private ProgressDialog mAuthProgressDialog;
    private EditText mEditTextEmailInput, mEditTextPasswordInput;
    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedPrefEditor;
    // Firebase Authentication
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private String mUsername;
    // Firebase Realtime Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mShoppingListDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPrefEditor = mSharedPref.edit();

        /**
         * Initialize Firebase components
         */
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(LoginActivity.this);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mShoppingListDatabaseReference = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USERS);

        /* Setup the Google API object to allow Google logins */
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        /**
         * Build a GoogleApiClient with access to the Google Sign-In API and the
         * options specified by gso.
         */

        /* Setup the Google API object to allow Google+ logins */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        /**
         * Getting mProvider and mEncodedEmail from SharedPreferences
         */
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        /* Get mEncodedEmail and mProvider from SharedPreferences, use null as default value */
        mEncodedEmail = sp.getString(Constants.KEY_ENCODED_EMAIL, null);
        mProvider = sp.getString(Constants.KEY_PROVIDER, null);

        /**
         * Link layout elements from XML and setup progress dialog
         */
        initializeScreen();

        /**
         * Call signInPassword() when user taps "Done" keyboard action
         */
        mEditTextPasswordInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {

                if (actionId == EditorInfo.IME_ACTION_DONE || keyEvent.getAction() == KeyEvent.ACTION_DOWN) {

                }
                return true;
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_GOOGLE_LOGIN) {
            Timber.v("requestCode == RC_GOOGLE_LOGIN");
            if (resultCode == RESULT_OK) {
                Timber.v("resultCode == RESULT_OK");
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result != null) {
                    if (result.isSuccess()) {
                        Timber.v("result.isSuccess()");
                        // Google Sign In was successful, authenticate with Firebase
                        GoogleSignInAccount account = result.getSignInAccount();
                        firebaseAuthWithGoogle(account);
                    } else if (resultCode == RESULT_CANCELED) {
                        // Sign in was canceled by the user, finish the activity
                        Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Status status = result.getStatus();
                    Timber.v("status: " + status);
                }
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Timber.v("firebaseAuthWithGoogle: " + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Timber.v("signInWithCredential:success");
                            FirebaseUser user = mFirebaseAuth.getCurrentUser();
                            Timber.v("user.getDisplayName(): " + user.getDisplayName());
                            Timber.v("user.getEmail(): " + user.getEmail());

                            mUsername = user.getDisplayName();
                            mEncodedEmail = Utils.encodeEmail(user.getEmail());

                            mSharedPrefEditor = mSharedPref.edit();
                            /* Save provider name and encodedEmail for later use and start MainActivity */
                            mSharedPrefEditor.putString(Constants.KEY_ENCODED_EMAIL, mEncodedEmail).apply();
                            mSharedPrefEditor.putString(Constants.KEY_PROVIDER, null);

                            onSignedInInitialize(mUsername);
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra(Constants.KEY_ENCODED_EMAIL, mEncodedEmail);
                            startActivity(intent);
                        } else {
                            // If sign in fails, display a message to the user.
                            Timber.v("signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mFirebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Sign in success, update UI with the signed-in user's information
            Timber.v("signInWithCredential:success");
            FirebaseUser user = mFirebaseAuth.getCurrentUser();
            Timber.v("user.getDisplayName(): " + user.getDisplayName());
            Timber.v("user.getEmail(): " + user.getEmail());

            mUsername = user.getDisplayName();
            mEncodedEmail = Utils.encodeEmail(user.getEmail());

            mSharedPrefEditor = mSharedPref.edit();
            /* Save provider name and encodedEmail for later use and start MainActivity */
            mSharedPrefEditor.putString(Constants.KEY_ENCODED_EMAIL, mEncodedEmail).apply();
            mSharedPrefEditor.putString(Constants.KEY_PROVIDER, null);

            onSignedInInitialize(mUsername);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra(Constants.KEY_ENCODED_EMAIL, mEncodedEmail);
            startActivity(intent);
        } else {
            Timber.v("user does not yet exist");
        }
    }

    private void onSignedInInitialize(String username) {
        mUsername = username;

        /**
         * Set raw version of date to the ServerValue.TIMESTAMP value and save into
         * timestampCreatedMap
         */
        HashMap<String, Object> timestampCreated = new HashMap<>();
        timestampCreated.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CAMPAIGN, "add new list");
        LoginActivity.mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        /* Build the user */
        User user = new User(mUsername, mEncodedEmail, timestampCreated);

        mShoppingListDatabaseReference.child(mEncodedEmail).setValue(user).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Timber.v(e.getLocalizedMessage());
            }
        });
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
    }

    /**
     * Override onCreateOptionsMenu to inflate nothing
     *
     * @param menu The menu with which nothing will happen
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /**
     * Link layout elements from XML and setup the progress dialog
     */
    public void initializeScreen() {
        mEditTextEmailInput = findViewById(R.id.edit_text_email);
        mEditTextPasswordInput = findViewById(R.id.edit_text_password);
        LinearLayout linearLayoutLoginActivity = findViewById(R.id.linear_layout_login_activity);
        initializeBackground(linearLayoutLoginActivity);
        /* Setup the progress dialog that is displayed later when authenticating with Firebase */
        mAuthProgressDialog = new ProgressDialog(this);
        mAuthProgressDialog.setTitle(getString(R.string.progress_dialog_loading));
        mAuthProgressDialog.setMessage(getString(R.string.progress_dialog_authenticating_with_firebase));
        mAuthProgressDialog.setCancelable(false);
        /* Setup Google Sign In */
        setupGoogleSignIn();
    }

    /* Sets up the Google Sign In Button : https://developers.google.com/android/reference/com/google/android/gms/common/SignInButton */
    private void setupGoogleSignIn() {
        SignInButton signInButton = findViewById(R.id.login_with_google);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.v("onSignInGooglePressed(v)");
                onSignInGooglePressed(v);
            }
        });
    }

    /**
     * Sign in with Google plus when user clicks "Sign in with Google" textView (button)
     */
    public void onSignInGooglePressed(View view) {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_GOOGLE_LOGIN);
        mAuthProgressDialog.show();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not be available.
        Timber.v("onConnectionFailed:" + connectionResult);
    }
}