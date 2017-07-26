package com.udacity.firebase.shoppinglistplusplus.ui.sharing;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

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
    private List<User> mUserList;

    // Firebase
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUsersReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        /**
         * Link layout elements from XML and setup the toolbar
         */
        initializeScreen();

        /**
         * Create Firebase references
         */
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUsersReference = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USERS);

        mUserList = new ArrayList<>();

        LinearLayoutManager manager = new LinearLayoutManager(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(manager);

        try {
            mFriendsAutocompleteAdapter = new AutocompleteFriendAdapter(mUserList, this);
            mRecyclerView.setAdapter(mFriendsAutocompleteAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mFriendsAutocompleteAdapter != null) {
            mFriendsAutocompleteAdapter.notifyDataSetChanged();
        }
        mRecyclerView.setAdapter(mFriendsAutocompleteAdapter);

        /**
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
                Timber.v("TRUE");
            /* Define and set the adapter otherwise. */
            } else {
                Timber.v("FALSE");

                /**
                 * Add ValueEventListeners to Firebase references
                 * to control get data and control behavior and visibility of elements
                 */
                Query userFriends = mUsersReference.getRef().orderByChild(Constants.FIREBASE_PROPERTY_EMAIL)
                        .startAt(mInput).endAt(mInput + "~").limitToFirst(5);
                Timber.v("userFriends.getRef(): " + userFriends.getRef());
                /* Get the user's friends list; use a SingleValueEvent listener for memory efficiency */
                userFriends.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Timber.v("ValueEventListener onDataChange(DataSnapshot dataSnapshot) " + dataSnapshot.getValue());
                        Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                        if (mUserList != null) {
                            mUserList.clear();
                        }
                        Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                        for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                            if (it.hasNext()) {
                                DataSnapshot listSnapshot = it.next();
                                Timber.v("listSnapshot.getKey(): " + listSnapshot.getKey());
                                Timber.v("listSnapshot.getValue(): " + listSnapshot.getValue());
                                mUserList.add(listSnapshot.getValue(User.class));
                                mFriendsAutocompleteAdapter.notifyDataSetChanged();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Timber.v("Error: " + databaseError);
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
        Timber.v("onResume()");

        /**
         * Add ValueEventListeners to Firebase references
         * to control get data and control behavior and visibility of elements
         */
        Query userFriends = mUsersReference.getRef();
        Timber.v("userFriends.getRef(): " + userFriends.getRef());
        /* Get the user's friends list; use a SingleValueEvent listener for memory efficiency */
        userFriends.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("ValueEventListener onDataChange(DataSnapshot dataSnapshot) " + dataSnapshot.getValue());
                Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                if (mUserList != null) {
                    mUserList.clear();
                }
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                    if (it.hasNext()) {
                        DataSnapshot listSnapshot = it.next();
                        Timber.v("listSnapshot.getKey(): " + listSnapshot.getKey());
                        Timber.v("listSnapshot.getValue(): " + listSnapshot.getValue());
                        mUserList.add(listSnapshot.getValue(User.class));
                        mFriendsAutocompleteAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: " + databaseError);
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
        mRecyclerView = (RecyclerView) findViewById(R.id.list_view_friends_autocomplete);
        mEditTextAddFriendEmail = (EditText) findViewById(R.id.edit_text_add_friend_email);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        /* Add back button to the action bar */
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
}
