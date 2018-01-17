package com.udacity.firebase.shoppinglistplusplus.ui.sharing;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.ui.BaseActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;

import java.util.ArrayList;
import java.util.Iterator;

import timber.log.Timber;

/**
 * Allows for you to check and un-check friends that you share the current list with
 */
public class ShareListActivity extends BaseActivity {
    private static ArrayList<ValueEventListener> mSharedWithListener;
    FirebaseDatabase mFirebaseDatabase;
    private FriendAdapter mFriendAdapter;
    private ArrayList<User> mUsersFriends;
    private DatabaseReference mCurrentUserFriendsReference, mActiveListRef, mSharedFriendInShoppingListRef;
    private ValueEventListener mActiveListRefListener;
    private ValueEventListener mFriendsListener;
    private ShoppingList mShoppingList;
    private RecyclerView mRecyclerView;

    /**
     * Public method that is used to pass ShoppingList object when it is loaded in
     * ValueEventListener
     */
    public static void setShareWithListener(ValueEventListener valueEventListener) {
        mSharedWithListener.add(valueEventListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_list);

        /* Get the push ID from the extra passed by ActiveListDetailsActivity */
        Intent intent = this.getIntent();
        String mListId = intent.getStringExtra(Constants.KEY_LIST_ID);
        if (mListId == null) {
            /* No point in continuing without a valid ID. */
            finish();
            return;
        }
        String mEncodedEmail = intent.getStringExtra(Constants.KEY_ENCODED_EMAIL);
        mUsersFriends = new ArrayList<>();
        mSharedWithListener = new ArrayList<>();

        /*
         * Create Firebase references
         */
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mCurrentUserFriendsReference = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USER_FRIENDS).child(mEncodedEmail);
        mActiveListRef = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USER_LISTS).child(mEncodedEmail).child(mListId);
        mSharedFriendInShoppingListRef = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_LISTS_SHARED_WITH).child(mListId);

        /*
         * Link layout elements from XML and setup the toolbar
         */
        initializeScreen();

        LinearLayoutManager manager = new LinearLayoutManager(this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(manager);

        try {
            // Timber.v("mListId: %s", mListId);
            // Timber.v("mEncodedEmail: %s", mEncodedEmail);
            mFriendAdapter = new FriendAdapter(mUsersFriends, mListId, mEncodedEmail, mSharedFriendInShoppingListRef);
            mRecyclerView.setAdapter(mFriendAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mFriendAdapter != null) {
            mFriendAdapter.notifyDataSetChanged();
        }
        mRecyclerView.setAdapter(mFriendAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Timber.v("onResume()");

        /*
         * Add ValueEventListeners to Firebase references
         * to control get data and control behavior and visibility of elements
         */

        /*
         * Returns datasnapshot from /userLists/mEncodedEmail/mLisrId node to provide the active list
         */
        mActiveListRefListener = mActiveListRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Timber.v("ValueEventListener onDataChange(DataSnapshot dataSnapshot) %s", dataSnapshot.getValue());
                // Timber.v("dataSnapshot.getValue(): %s", dataSnapshot.getValue());

                ShoppingList shoppingList = dataSnapshot.getValue(ShoppingList.class);

                /*
                 * Saving the most recent version of current shopping list into mShoppingList
                 * and pass it to setShoppingList() if present
                 * finish() the activity otherwise
                 */
                if (shoppingList != null) {
                    mShoppingList = shoppingList;
                    if (mFriendAdapter != null) {
                        mFriendAdapter.setShoppingList(mShoppingList);
                    } else {
                        Timber.v("mFriendAdapter is NULL 01");
                    }
                } else {
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("%s%s", getString(R.string.log_error_the_read_failed), databaseError);
            }
        });

        /*
         * Returns datasnapshot from /userFriends/mEncodedEmail node
         */
        mFriendsListener = mCurrentUserFriendsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Timber.v("dataSnapshot.getValue(): %s", dataSnapshot.getValue());
                mUsersFriends.clear();
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                    if (it.hasNext()) {
                        DataSnapshot listSnapshot = it.next();
                        // Timber.v("listSnapshot.getKey(): %s", listSnapshot.getKey());
                        // Timber.v("listSnapshot.getValue(): %s", listSnapshot.getValue());
                        mUsersFriends.add(listSnapshot.getValue(User.class));
                        // Timber.v("mSharedWithUsers.get(i).getEmail(): %s", mUsersFriends.get(i).getEmail());
                        if (mFriendAdapter != null) {
                            mFriendAdapter.setSharedWithUsers(mUsersFriends);
                            mFriendAdapter.notifyDataSetChanged();
                        } else {
                            Timber.v("mFriendAdapter is NULL 02");
                        }
                    }
                    if (mFriendAdapter != null) {
                        Timber.v("mFriendAdapter.getItemCount(): %s", mFriendAdapter.getItemCount());
                    } else {
                        Timber.v("mFriendAdapter is NULL 03");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("%s%s", getString(R.string.log_error_the_read_failed), databaseError);
            }
        });
    }

    /**
     * Cleanup the adapter when activity is destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFriendsListener != null) {
            Timber.v("mCurrentUserFriendsReference.removeEventListener(mFriendsListener)");
            mCurrentUserFriendsReference.removeEventListener(mFriendsListener);
        }
        if (mActiveListRefListener != null) {
            Timber.v("mActiveListRef.removeEventListener(mActiveListRefListener)");
            mActiveListRef.removeEventListener(mActiveListRefListener);
        }
        if (mSharedWithListener != null) {
            Timber.v("mSharedFriendInShoppingListRef.removeEventListener(mSharedWithListener)");
            for (int i = 0; i < mSharedWithListener.size(); i++) {
                Timber.v("mListeners.get(i): %s", mSharedWithListener.get(i));
                mSharedFriendInShoppingListRef.removeEventListener(mSharedWithListener.get(i));
            }
        }

    }

    /**
     * Link layout elements from XML and setup the toolbar
     */
    public void initializeScreen() {
        mRecyclerView = findViewById(R.id.list_view_friends_share);
        Toolbar toolbar = findViewById(R.id.app_bar);
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
