package com.udacity.firebase.shoppinglistplusplus.ui.sharing;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.ui.BaseActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;

/**
 * Represents the Add Friend screen and functionality
 */
public class AddFriendActivity extends BaseActivity {
    private EditText mEditTextAddFriendEmail;
    private AutocompleteFriendAdapter mFriendsAutocompleteAdapter;
    private String mInput;
    private RecyclerView mRecyclerView;
    private List<User> mFriendList;

    // Firebase
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUsersReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        Timber.v("onCreate()");

        /*
         * Link layout elements from XML and setup the toolbar
         */
        initializeScreen();

        /*
         * Create Firebase references
         */
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUsersReference = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USERS);

        mFriendList = new ArrayList<>();

        LinearLayoutManager manager = new LinearLayoutManager(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(manager);

        try {
            mFriendsAutocompleteAdapter = new AutocompleteFriendAdapter(mFriendList, this);
            mRecyclerView.setAdapter(mFriendsAutocompleteAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mFriendsAutocompleteAdapter != null) {
            mFriendsAutocompleteAdapter.notifyDataSetChanged();
        }
        mRecyclerView.setAdapter(mFriendsAutocompleteAdapter);

        /*
         * Set interactive bits, such as click events/adapters
         */

        mEditTextAddFriendEmail.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            /* Get the input after every textChanged event and transform it to lowercase */
            mInput = mEditTextAddFriendEmail.getText().toString().toLowerCase();

            /* Clean up the old adapter */
            if (mFriendsAutocompleteAdapter != null) mRecyclerView.setAdapter(null);

            /* Nullify the adapter data if the input length is less than 2 characters */
            if (mInput.equals("") || mInput.length() < 2) {
                mRecyclerView.setAdapter(null);
            /* Define and set the adapter otherwise. */
            } else {
                /*
                 * Add ValueEventListeners to Firebase references
                 * to control get data and control behavior and visibility of elements
                 */
                Query userFriends = mUsersReference.getRef().orderByChild(Constants.FIREBASE_PROPERTY_EMAIL)
                        .startAt(mInput).endAt(mInput + "~").limitToFirst(5);
                // Timber.v("userFriends.getRef(): %s", userFriends.getRef());
                /* Get the user's friends list; use a SingleValueEvent listener for memory efficiency */
                userFriends.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Timber.v("ValueEventListener onDataChange(DataSnapshot dataSnapshot) %s", dataSnapshot.getValue());
                        // Timber.v("dataSnapshot.getValue(): %s", dataSnapshot.getValue());
                        if (mFriendList != null) {
                            mFriendList.clear();
                        }
                        Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                        for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                            if (it.hasNext()) {
                                DataSnapshot listSnapshot = it.next();
                                // Timber.v("listSnapshot.getKey(): %s", listSnapshot.getKey());
                                // Timber.v("listSnapshot.getValue(): %s", listSnapshot.getValue());
                                mFriendList.add(listSnapshot.getValue(User.class));
                                mFriendsAutocompleteAdapter.notifyDataSetChanged();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Timber.v("Error: %s", databaseError);
                    }
                });
                mRecyclerView.setAdapter(mFriendsAutocompleteAdapter);
            }
        }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Timber.v("onResume()");

        /*
         * Add ValueEventListeners to Firebase references
         * to control get data and control behavior and visibility of elements
         */
        Query userFriends = mUsersReference.getRef();
        // Timber.v("userFriends.getRef(): %s", userFriends.getRef());
        /* Get the user's friends list; use a SingleValueEvent listener for memory efficiency */
        userFriends.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Timber.v("ValueEventListener onDataChange(DataSnapshot dataSnapshot) %s", dataSnapshot.getValue());
                // Timber.v("dataSnapshot.getValue(): %s", dataSnapshot.getValue());
                if (mFriendList != null) {
                    mFriendList.clear();
                }
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                    if (it.hasNext()) {
                        DataSnapshot listSnapshot = it.next();
                        // Timber.v("listSnapshot.getKey(): %s", listSnapshot.getKey());
                        // Timber.v("listSnapshot.getValue(): %s", listSnapshot.getValue());
                        mFriendList.add(listSnapshot.getValue(User.class));
                        mFriendsAutocompleteAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: %s", databaseError);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Link layout elements from XML and setup the toolbar
     */
    public void initializeScreen() {
        mRecyclerView = findViewById(R.id.list_view_friends_autocomplete);
        mEditTextAddFriendEmail = findViewById(R.id.edit_text_add_friend_email);
        Toolbar toolbar = findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        /* Add back button to the action bar */
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
}
