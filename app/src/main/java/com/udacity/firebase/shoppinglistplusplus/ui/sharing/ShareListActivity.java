package com.udacity.firebase.shoppinglistplusplus.ui.sharing;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.ui.BaseActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;

import java.util.HashMap;
import java.util.Iterator;

import timber.log.Timber;

/**
 * Allows for you to check and un-check friends that you share the current list with
 */
public class ShareListActivity extends BaseActivity {
    private static final String LOG_TAG = ShareListActivity.class.getSimpleName();
    private FriendAdapter mFriendAdapter;
    private String mListId;
    private String mEncodedEmail;
    private HashMap<String, User> mSharedWithUsers;
    private RecyclerView mRecyclerView;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mCurrentUserFriendsReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_list);

        /* Get the push ID from the extra passed by ActiveListDetailsActivity */
        Intent intent = this.getIntent();
        mListId = intent.getStringExtra(Constants.KEY_LIST_ID);
        if (mListId == null) {
            /* No point in continuing without a valid ID. */
            finish();
            return;
        }
        mEncodedEmail = intent.getStringExtra(Constants.KEY_ENCODED_EMAIL);
        mSharedWithUsers = new HashMap<>();

        /**
         * Link layout elements from XML and setup the toolbar
         */
        initializeScreen();

        LinearLayoutManager manager = new LinearLayoutManager(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(manager);

        try {
            mFriendAdapter = new FriendAdapter(mSharedWithUsers, mEncodedEmail);
            mRecyclerView.setAdapter(mFriendAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mFriendAdapter != null) {
            mFriendAdapter.notifyDataSetChanged();
        }
        mRecyclerView.setAdapter(mFriendAdapter);

        /**
         * Set interactive bits, such as click events/adapters
         */

    }

    @Override
    public void onResume() {
        super.onResume();
        // Timber.v("onResume()");

        /**
         * Create Firebase references
         */
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mCurrentUserFriendsReference = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USER_FRIENDS);


        /**
         * Add ValueEventListeners to Firebase references
         * to control get data and control behavior and visibility of elements
         */
        Query userFriends = mCurrentUserFriendsReference.child(mEncodedEmail);
        Timber.v("userFriends.getRef(): " + userFriends.getRef());
        /* Get the user's friends list; use a SingleValueEvent listener for memory efficiency */
        userFriends.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("ValueEventListener onDataChange(DataSnapshot dataSnapshot) " + dataSnapshot.getValue());
                Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                mSharedWithUsers.clear();
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                    if (it.hasNext()) {
                        DataSnapshot listSnapshot = it.next();
                        Timber.v("listSnapshot.getKey(): " + listSnapshot.getKey());
                        Timber.v("listSnapshot.getValue(): " + listSnapshot.getValue());
                        mSharedWithUsers.put(listSnapshot.getKey(), listSnapshot.getValue(User.class));
                        mFriendAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: " + databaseError);
            }
        });
    }

    /**
     * Cleanup the adapter when activity is destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Link layout elements from XML and setup the toolbar
     */
    public void initializeScreen() {
        mRecyclerView = (RecyclerView) findViewById(R.id.list_view_friends_share);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        /* Add back button to the action bar */
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Launch AddFriendActivity to find and add user to current user's friends list
     * when the button AddFriend is pressed
     */
    public void onAddFriendPressed(View view) {
        Intent intent = new Intent(ShareListActivity.this, AddFriendActivity.class);
        startActivity(intent);
    }
}
