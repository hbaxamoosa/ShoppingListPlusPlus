package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingListItem;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.ui.BaseActivity;
import com.udacity.firebase.shoppinglistplusplus.ui.sharing.ShareListActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import timber.log.Timber;

/**
 * Represents the details screen for the selected shopping list
 */
public class ActiveListDetailsActivity extends BaseActivity {
    private static final String LOG_TAG = ActiveListDetailsActivity.class.getSimpleName();
    public String mKey;
    private Button mButtonShopping;
    private TextView mTextViewPeopleShopping;
    private String mListId;
    private User mCurrentUser;
    private String mEncodedEmail;
    /* Stores whether the current user is shopping */
    private boolean mShopping = false;
    /* Stores whether the current user is the owner */
    private boolean mCurrentUserIsOwner = false;
    private ShoppingList mShoppingList;
    private ArrayList<ShoppingList> mShoppingListArray = new ArrayList<>();
    private ArrayList<ShoppingListItem> mShoppingListItemsArray = new ArrayList<>();
    private HashMap<String, User> mSharedWithUsers = new HashMap<>();
    private ActiveListItemAdapter mActiveListItemAdapter;

    private DatabaseReference mCurrentUserRef, mCurrentListRef, mListItemsRef, mSharedWithRef;
    private ValueEventListener mCurrentUserRefListener, mCurrentListRefListener;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_list_details);

        /* Get the push ID from the extra passed by ShoppingListFragment */
        Intent intent = this.getIntent();
        mListId = intent.getStringExtra(Constants.KEY_LIST_NAME);
        if (mListId == null) {
            /* No point in continuing without a valid ID. */
            finish();
            return;
        }
        int mPosition = intent.getIntExtra(Constants.KEY_LIST_ITEM_ID, 999);
        mKey = intent.getStringExtra("listKey");

        // get SharedPrefs
        SharedPreferences mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mEncodedEmail = mSharedPref.getString(Constants.KEY_ENCODED_EMAIL, null);

        // Initialize Firebase components
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();

        /*
         * Create Firebase references
         */
        mCurrentListRef = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USER_LISTS).child(mEncodedEmail).child(mKey);
        mCurrentUserRef = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USERS).child(mEncodedEmail);
        mListItemsRef = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS).child(mKey);
        mSharedWithRef = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_LISTS_SHARED_WITH);

        // Timber.v("mListItemsRef.toString(): " + mListItemsRef.toString());
        // use this call to Firebase rtdb to grab the correct Shopping List
        mCurrentListRefListener = mCurrentListRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Timber.v("dataSnapshot: " + dataSnapshot.getValue().toString());
                mShoppingList = dataSnapshot.getValue(ShoppingList.class);
                // Timber.v("mShoppingList.getListName() : " + mShoppingList.getListName());
                // Timber.v("mShoppingList.getOwner() : " + mShoppingList.getOwner());

                /* Check if the current user is owner */
                if (mShoppingList != null) {
                    mCurrentUserIsOwner = Utils.checkIfOwner(mShoppingList, mEncodedEmail);
                    // Timber.v("addListenerForSingleValueEvent mCurrentUserIsOwner: " + mCurrentUserIsOwner);

                    /* Calling invalidateOptionsMenu causes onCreateOptionsMenu to be called */
                    invalidateOptionsMenu();

                    /* Set title appropriately. */
                    setTitle(mShoppingList.getListName());

                    /*
                     * Handle the Start/Stop Shopping button
                     */
                    HashMap usersShopping = mShoppingList.getUsersShopping();
                    if (mShoppingList.getUsersShopping() != null) {
                        Timber.v("mShoppingList.getUsersShopping(): %s", mShoppingList.getUsersShopping().toString());
                    } else {
                        Timber.v("mShoppingList.getUsersShopping(): NULL");
                    }
                    if (usersShopping != null && usersShopping.size() != 0 && usersShopping.containsKey(mEncodedEmail)) {
                        // Timber.v("mShopping is TRUE");
                        mShopping = true;
                        mButtonShopping.setText(getString(R.string.button_stop_shopping));
                        mButtonShopping.setBackgroundColor(ContextCompat.getColor(ActiveListDetailsActivity.this, R.color.dark_grey));
                    } else {
                        // Timber.v("mShopping is FALSE");
                        mShopping = false;
                        mButtonShopping.setText(getString(R.string.button_start_shopping));
                        mButtonShopping.setBackgroundColor(ContextCompat.getColor(ActiveListDetailsActivity.this, R.color.primary_dark));
                    }

                    /*
                     * Display list of users that are currently shopping
                     */
                    setWhosShoppingText(mShoppingList.getUsersShopping());

                    /*
                     * Pass the current shopping list to the ActiveListItemAdapter
                     */
                    mActiveListItemAdapter.setShoppingList(mShoppingList);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("error: %s", databaseError.toString());
            }
        });

        /* Save the most up-to-date version of current user in mCurrentUser */
        mCurrentUserRefListener = mCurrentUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                User currentUser = dataSnapshot.getValue(User.class);
                if (currentUser != null) mCurrentUser = currentUser;
                else finish();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("error: %s", databaseError.toString());
            }
        });

        /*
         * Link layout elements from XML and setup the toolbar
         */
        initializeScreen();

        /* Set title appropriately. */
        setTitle(mListId);

        /* Save the most up-to-date version of current user in mCurrentUser */

        final Activity thisActivity = this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Inflate the menu; this adds items to the action bar if it is present. */
        getMenuInflater().inflate(R.menu.menu_list_details, menu);

        /*
         * Get menu items
         */
        MenuItem remove = menu.findItem(R.id.action_remove_list);
        MenuItem edit = menu.findItem(R.id.action_edit_list_name);
        MenuItem share = menu.findItem(R.id.action_share_list);
        MenuItem archive = menu.findItem(R.id.action_archive);

        /* Only the edit and remove options are implemented */
        remove.setVisible(mCurrentUserIsOwner);
        edit.setVisible(mCurrentUserIsOwner);
        share.setVisible(mCurrentUserIsOwner);
        archive.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        /*
         * Show edit list dialog when the edit action is selected
         */
        if (id == R.id.action_edit_list_name) {
            showEditListNameDialog();
            return true;
        }

        /*
         * removeList() when the remove action is selected
         */
        if (id == R.id.action_remove_list) {
            removeList();
            return true;
        }

        /*
         * Eventually we'll add this
         */
        if (id == R.id.action_share_list) {
            Intent intent = new Intent(ActiveListDetailsActivity.this, ShareListActivity.class);
            intent.putExtra(Constants.KEY_LIST_ID, mKey);
            intent.putExtra(Constants.KEY_ENCODED_EMAIL, mEncodedEmail);
            /* Starts an active showing the details for the selected list */
            startActivity(intent);
            return true;
        }

        /*
         * archiveList() when the archive action is selected
         */
        if (id == R.id.action_archive) {
            archiveList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.v("onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Timber.v("onResume()");

        Query orderedListItems = mListItemsRef.orderByChild(Constants.FIREBASE_PROPERTY_BOUGHT);
        orderedListItems.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Timber.v("onChildAdded in mShoppingListItemsReference");
                mShoppingListItemsArray.clear();
                // Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                // Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                // Timber.v("mKey: " + mKey);
                if (mKey.equals(dataSnapshot.getKey())) {
                    // Timber.v("dataSnapshot.getChildrenCount(): " + dataSnapshot.getChildrenCount());
                    // Timber.v("my value is: " + dataSnapshot.getValue());
                    for (DataSnapshot ShoppingListItemsSnapshot : dataSnapshot.getChildren()) {
                        // Timber.v("ShoppingListItemsSnapshot.getKey(): " + ShoppingListItemsSnapshot.getKey());
                        // Timber.v("ShoppingListItemsSnapshot.getValue(): " + ShoppingListItemsSnapshot.getValue());
                        ShoppingListItem shoppingListItems = ShoppingListItemsSnapshot.getValue(ShoppingListItem.class);
                        mShoppingListItemsArray.add(shoppingListItems);
                    }
                    mActiveListItemAdapter.notifyDataSetChanged();
                } else {
                    Timber.v("this list does not contain any items!");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: %s", databaseError.toString());
            }
        });

        Query query = mSharedWithRef.child(mKey);
        // Timber.v("query.getRef(): %s", query.getRef());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Timber.v("dataSnapshot: %s", dataSnapshot);
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                    if (it.hasNext()) {
                        DataSnapshot listSnapshot = it.next();
                        // Timber.v("listSnapshot.getValue(): %s", listSnapshot.getValue());
                        User user = listSnapshot.getValue(User.class);
                        // Timber.v("listSnapshot.getKey(): %s", listSnapshot.getKey());
                        // Timber.v("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + listSnapshot.getKey() + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_LIST_NAME);
                        mSharedWithUsers.put(listSnapshot.getKey(), user);
                        // Timber.v("mSharedWithUsers.size(): %s", mSharedWithUsers.size());
                    }
                }
                /*
                 * Pass the sharedWith users for the current shopping list to the ActiveListItemAdapter
                */
                mActiveListItemAdapter.setSharedWithUsers(mSharedWithUsers);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: %s", databaseError);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Timber.v("onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Timber.v("onStop()");

        if (mCurrentUserRefListener != null) {
            mCurrentUserRef.removeEventListener(mCurrentUserRefListener);
        }
    }

    /**
     * Cleanup when the activity is destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Timber.v("onDestroy()");

        mShoppingListArray.clear();
        mShoppingListItemsArray.clear();
        mSharedWithUsers.clear();

        if (mCurrentUserRefListener != null) {
            mCurrentUserRef.removeEventListener(mCurrentUserRefListener);
        }

        if (mCurrentListRefListener != null) {
            mCurrentListRef.removeEventListener(mCurrentListRefListener);
        }
    }

    /**
     * Link layout elements from XML and setup the toolbar
     */
    private void initializeScreen() {
        RecyclerView mRecyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager manager = new LinearLayoutManager(ActiveListDetailsActivity.this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(manager);

        try {
            mActiveListItemAdapter = new ActiveListItemAdapter(ActiveListDetailsActivity.this, mShoppingListItemsArray, mEncodedEmail, mKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mActiveListItemAdapter != null) {
            mActiveListItemAdapter.notifyDataSetChanged();
        }
        mRecyclerView.setAdapter(mActiveListItemAdapter);

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this, mRecyclerView, new ClickListener() {

            /* Perform buy/return action on listView item click event if current user is shopping. */
            @Override
            public void onClick(View view, final int position) {
                //Values are passing to activity & to fragment as well
                Toast.makeText(ActiveListDetailsActivity.this, "Single click on position: " + position,
                        Toast.LENGTH_SHORT).show();

                /* Check that the view is not the empty footer item */
                if (view.getId() != R.id.list_view_footer_empty) {
                    final ShoppingListItem selectedListItem = mShoppingListItemsArray.get(position);
                    // Timber.v("selectedListItem.isBought(): " + selectedListItem.isBought());
                    // Timber.v("selectedListItem.getItemName(): " + selectedListItem.getItemName());

                    /* If current user is shopping */
                    if (mShopping) {
                        /* Create map and fill it in with deep path multi write operations list */
                        final HashMap<String, Object> updatedItemBoughtData = new HashMap<String, Object>();

                        /* Buy selected item if it is NOT already bought */
                        if (!selectedListItem.isBought()) {
                            updatedItemBoughtData.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/" + mKey + "/" + selectedListItem.getItemName() + "/" + Constants.FIREBASE_PROPERTY_BOUGHT, true);
                            updatedItemBoughtData.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/" + mKey + "/" + selectedListItem.getItemName() + "/" + Constants.FIREBASE_PROPERTY_BOUGHT_BY, mEncodedEmail);
                        } else {
                            /* Return selected item only if it was bought by current user */
                            if (selectedListItem.getBoughtBy().equals(mEncodedEmail)) {
                                updatedItemBoughtData.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/" + mKey + "/" + selectedListItem.getItemName() + "/" + Constants.FIREBASE_PROPERTY_BOUGHT, false);
                                updatedItemBoughtData.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/" + mKey + "/" + selectedListItem.getItemName() + "/" + Constants.FIREBASE_PROPERTY_BOUGHT_BY, null);
                            }
                        }

                        final HashMap<String, Object> timestampNowHash = new HashMap<>();
                        timestampNowHash.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

                        /*
                         * Set the timestampLastChange for the list owner
                         */
                        updatedItemBoughtData.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mEncodedEmail + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, timestampNowHash);

                        /*
                         * Set the timestampLastChange for the remaining user that the list is shared with
                         */
                        Iterator<User> it = mSharedWithUsers.values().iterator();
                        for (int i = 0; i < mSharedWithUsers.size(); i++) {
                            if (it.hasNext()) {
                                User user = it.next();
                                String email = user.getEmail();
                                updatedItemBoughtData.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, timestampNowHash);
                                // Timber.v("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mListKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED);
                            }
                        }

                        Query query = mListItemsRef.child(selectedListItem.getItemName());
                        // Timber.v("query.getRef(): " + query.getRef());
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                                // Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                                // Timber.v("updatedItemBoughtData.toString(): " + updatedItemBoughtData.toString());

                                /* Do the update */
                                mListItemsRef.getRoot().updateChildren(updatedItemBoughtData, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {
                                        if (databaseError != null) {
                                            Timber.v("datebaseError: %s", databaseError.toString());
                                        } else {
                                            /*
                                             * Set the timestampLastChangeReverse for the list owner
                                             */
                                            databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mEncodedEmail).child(mKey).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {

                                                    ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                                    // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                                    if (list != null) {
                                                        long timeReverse = -(list.getTimestampLastChangedLong());
                                                        String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                                                + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                                        // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mOwner).child(mListKey).child(timeReverseLocation).toString());
                                                        databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mEncodedEmail).child(mKey).child(timeReverseLocation).setValue(timeReverse);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {
                                                    Timber.v("%s %s", "Error updating data: ", databaseError.getMessage());
                                                }
                                            });

                                            /*
                                             * Set the timestampLastChangeReverse for the remaining user that the list is shared with
                                             */
                                            Iterator<User> it = mSharedWithUsers.values().iterator();
                                            for (int i = 0; i < mSharedWithUsers.size(); i++) {
                                                if (it.hasNext()) {
                                                    User user = it.next();
                                                    final String email = user.getEmail();
                                                    databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mKey).addListenerForSingleValueEvent(new ValueEventListener() {

                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                                            ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                                            // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                                            if (list != null) {
                                                                long timeReverse = -(list.getTimestampLastChangedLong());
                                                                String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                                                        + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                                                // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListKey).child(timeReverseLocation));
                                                                databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mKey).child(timeReverseLocation).setValue(timeReverse);
                                                            }
                                                        }

                                                        @Override
                                                        public void onCancelled(DatabaseError databaseError) {
                                                            Timber.v("%s %s", "Error updating data: ", databaseError.getMessage());
                                                        }
                                                    });
                                                } else {
                                                    Timber.v("this list is not shared with anyone");
                                                }
                                            }
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Timber.v("Error: %s", databaseError);
                            }
                        });
                    }
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                /*Toast.makeText(ActiveListDetailsActivity.this, "Long press on position: " + position,
                        Toast.LENGTH_LONG).show();*/

                // Timber.v("mActiveListItemAdapter.getItemCount(): " + mActiveListItemAdapter.getItemCount());
                // Timber.v("mActiveListItemAdapter.getItemId(position): " + mActiveListItemAdapter.getItemId(position));
                // Timber.v("mShoppingListItemsArray.get(position).getItemName(): " + mShoppingListItemsArray.get(position).getItemName());
                // Timber.v("mActiveListItemAdapter.getItemId(position): " + mActiveListItemAdapter.getItemId(position));

                /*
                 * If the person is the owner and not shopping and the item is not bought, then they can edit it.
                 */
                if (mShoppingListItemsArray.get(position).getOwner().equals(mEncodedEmail) && !mShopping && !mShoppingListItemsArray.get(position).isBought()) {
                    showEditListItemNameDialog(mShoppingListItemsArray.get(position).getItemName(), mKey);
                }
            }

        }));

        mTextViewPeopleShopping = findViewById(R.id.text_view_people_shopping);
        mButtonShopping = findViewById(R.id.button_shopping);
        Toolbar toolbar = findViewById(R.id.app_bar);
        /* Common toolbar setup */
        setSupportActionBar(toolbar);
        /* Add back button to the action bar */
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Set appropriate text for Start/Stop shopping button and Who's shopping textView
     * depending on the current user shopping status
     */
    private void setWhosShoppingText(HashMap<String, User> usersShopping) {

        if (usersShopping != null) {
            Timber.v("private void setWhosShoppingText(HashMap<String, User> usersShopping) && usersShopping != null");
            ArrayList<String> usersWhoAreNotYou = new ArrayList<>();
            /*
             * If at least one user is shopping
             * Add userName to the list of users shopping if this user is not current user
             */
            for (User user : usersShopping.values()) {
                // Timber.v("user.getEmail(): %s", user.getEmail());
                // Timber.v("mEncodedEmail" + mEncodedEmail);
                // Timber.v("usersWhoAreNotYou.size(): %s", usersWhoAreNotYou.size());
                if (user != null && !(user.getEmail().equals(mEncodedEmail))) {
                    usersWhoAreNotYou.add(user.getName());
                    // Timber.v("user.getName(): %s", user.getName());
                }
            }

            int numberOfUsersShopping = usersShopping.size();
            // Timber.v("numberOfUsersShopping: %s", numberOfUsersShopping);
            String usersShoppingText;

            /*
             * If current user is shopping...
             * If current user is the only person shopping, set text to "You are shopping"
             * If current user and one user are shopping, set text "You and userName are shopping"
             * Else set text "You and N others shopping"
             */
            // Timber.v("mShopping: %s", mShopping);
            if (mShopping) {
                switch (numberOfUsersShopping) {
                    case 1:
                        usersShoppingText = getString(R.string.text_you_are_shopping);
                        break;
                    case 2:
                        usersShoppingText = String.format(
                                getString(R.string.text_you_and_other_are_shopping),
                                usersWhoAreNotYou.get(0));
                        break;
                    default:
                        usersShoppingText = String.format(
                                getString(R.string.text_you_and_number_are_shopping),
                                usersWhoAreNotYou.size());
                }
                /*
                 * If current user is not shopping..
                 * If there is only one person shopping, set text to "userName is shopping"
                 * If there are two users shopping, set text "userName1 and userName2 are shopping"
                 * Else set text "userName and N others shopping"
                 */
            } else {
                switch (numberOfUsersShopping) {
                    case 1:
                        Timber.v("usersWhoAreNotYou.get(0): %s", usersWhoAreNotYou.get(0));
                        usersShoppingText = String.format(
                                getString(R.string.text_other_is_shopping),
                                usersWhoAreNotYou.get(0));
                        break;
                    case 2:
                        usersShoppingText = String.format(
                                getString(R.string.text_other_and_other_are_shopping),
                                usersWhoAreNotYou.get(0),
                                usersWhoAreNotYou.get(1));
                        break;
                    default:
                        usersShoppingText = String.format(
                                getString(R.string.text_other_and_number_are_shopping),
                                usersWhoAreNotYou.get(0),
                                usersWhoAreNotYou.size() - 1);
                }
            }
            mTextViewPeopleShopping.setText(usersShoppingText);
        } else {
            mTextViewPeopleShopping.setText("");
        }
    }


    /**
     * Archive current list when user selects "Archive" menu item
     */
    public void archiveList() {
    }

    /**
     * Start AddItemsFromMealActivity to add meal ingredients into the shopping list
     * when the user taps on "add meal" fab
     */
    public void addMeal(View view) {
    }

    /**
     * Remove current shopping list and its items from all nodes
     */
    public void removeList() {
        /* Create an instance of the dialog fragment and show it */
        DialogFragment dialog = RemoveListDialogFragment.newInstance(mShoppingList, mListId, mKey,
                mSharedWithUsers);
        dialog.show(getFragmentManager(), "RemoveListDialogFragment");
    }

    /**
     * Show the add list item dialog when user taps "Add list item" fab
     */
    public void showAddListItemDialog(View view) {
        /* Create an instance of the dialog fragment and show it */
        DialogFragment dialog = AddListItemDialogFragment.newInstance(mShoppingList, mListId, mKey,
                mEncodedEmail, mSharedWithUsers);
        dialog.show(getFragmentManager(), "AddListItemDialogFragment");
    }

    /**
     * Show edit list name dialog when user selects "Edit list name" menu item
     */
    public void showEditListNameDialog() {
        /* Create an instance of the dialog fragment and show it */
        DialogFragment dialog = EditListNameDialogFragment.newInstance(mShoppingList, mListId, mKey,
                mEncodedEmail, mSharedWithUsers);
        dialog.show(this.getFragmentManager(), "EditListNameDialogFragment");
    }

    /**
     * Show the edit list item name dialog after longClick on the particular item
     */
    public void showEditListItemNameDialog(String itemName, String itemId) {
        /* Create an instance of the dialog fragment and show it */
        DialogFragment dialog = EditListItemNameDialogFragment.newInstance(mShoppingList, itemName,
                mKey, mListId, mEncodedEmail, mSharedWithUsers);

        dialog.show(this.getFragmentManager(), "EditListItemNameDialogFragment");
    }

    /**
     * This method is called when user taps "Start/Stop shopping" button
     */
    public void toggleShopping(View view) {
        /*
         * If current user is already shopping, remove current user from userLists/usersShopping map. Otherwise, add.
         */

        // Timber.v("toggleShopping(View view)");

        // mCurrentListRef = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USER_LISTS).child(mEncodedEmail).child(mKey);
        Query query = mCurrentListRef.child(Constants.FIREBASE_PROPERTY_USERS_SHOPPING);
        // Timber.v("query.getRef(): %s", query.getRef());

        /* Make the timestamp for last changed */
        final HashMap<String, Object> timestampNowHash = new HashMap<>();
        timestampNowHash.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

        /* Check to see whether user is shopping; use a SingleValueEvent listener for memory efficiency */
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Timber.v("dataSnapshot.getValue(): %s", dataSnapshot.getValue());
                HashMap<String, Object> updatedUserData = new HashMap<String, Object>();



                /* If current user is already shopping, remove current user from usersShopping map */
                if (mShopping) {
                    // Timber.v("mShopping is TRUE");
                    /* user WAS shopping, but now has STOPPED shopping */

                    /*
                     * update the owner's list to remove user that is shopping
                     */
                    updatedUserData.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mShoppingList.getOwner() + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_USERS_SHOPPING + "/" + mEncodedEmail, null); // set the User Shopping value to NULL

                    /* Add the updated timestamp to the owner's list */
                    updatedUserData.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mShoppingList.getOwner() + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, timestampNowHash);

                    /*
                     * update the sharedWith user's list to remove user that is shopping
                     */
                    Iterator<User> it = mSharedWithUsers.values().iterator();
                    for (int i = 0; i < mSharedWithUsers.size(); i++) {
                        if (it.hasNext()) {
                            User user = it.next();
                            String email = user.getEmail();
                            // Timber.v("email: %s", email);
                            updatedUserData.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_USERS_SHOPPING + "/" + mEncodedEmail, null);
                            // Timber.v("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_USERS_SHOPPING + "/" + mEncodedEmail);
                            updatedUserData.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, timestampNowHash);
                        }
                    }

                    /* Do a deep-path update */
                    mCurrentListRef.getRoot().updateChildren(updatedUserData, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Timber.v("%s %s", getString(R.string.log_error_updating_data), databaseError.getMessage());
                            } else {
                                /*
                                 * Set the reversed timestamp for the list owner
                                */
                                databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mEncodedEmail).child(mKey).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                        ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                        // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                        if (list != null) {
                                            long timeReverse = -(list.getTimestampLastChangedLong());
                                            String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                                    + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                            // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mOwner).child(mListKey).child(timeReverseLocation).toString());
                                            databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mShoppingList.getOwner()).child(mKey).child(timeReverseLocation).setValue(timeReverse);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Timber.v("%s %s", "Error updating data: ", databaseError.getMessage());
                                    }
                                });

                                /*
                                 * Set the reversed timestamp for the remaining user that the list is shared with
                                */
                                Iterator<User> it = mSharedWithUsers.values().iterator();
                                for (int i = 0; i < mSharedWithUsers.size(); i++) {
                                    if (it.hasNext()) {
                                        User user = it.next();
                                        final String email = user.getEmail();
                                        databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mKey).addListenerForSingleValueEvent(new ValueEventListener() {

                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                                // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                                if (list != null) {
                                                    long timeReverse = -(list.getTimestampLastChangedLong());
                                                    String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                                            + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                                    // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListKey).child(timeReverseLocation));
                                                    databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mKey).child(timeReverseLocation).setValue(timeReverse);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Timber.v("%s %s", "Error updating data: ", databaseError.getMessage());
                                            }
                                        });
                                    } else {
                                        Timber.v("this list is not shared with anyone");
                                    }
                                }
                            }
                        }
                    });
                } else {
                    // Timber.v("mShopping is FALSE");
                    /* user WAS NOT shopping, but now has STARTED shopping */

                    /* If current user is not shopping, create map to represent User model add to usersShopping map */
                    HashMap<String, Object> currentUser = (HashMap<String, Object>) new ObjectMapper().convertValue(mCurrentUser, Map.class);

                    /*
                     * update the owner's list to add user that is shopping
                     */
                    updatedUserData.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mShoppingList.getOwner() + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_USERS_SHOPPING + "/" + mEncodedEmail, currentUser); // set the User Shopping value to current user

                    /* Add the updated timestamp to the owner's list */
                    updatedUserData.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mShoppingList.getOwner() + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, timestampNowHash);

                    /*
                     * update the sharedWith user's list to add user that is shopping
                     */
                    Iterator<User> it = mSharedWithUsers.values().iterator();
                    for (int i = 0; i < mSharedWithUsers.size(); i++) {
                        if (it.hasNext()) {
                            User user = it.next();
                            String email = user.getEmail();
                            // Timber.v("email: %s", email);
                            updatedUserData.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_USERS_SHOPPING + "/" + mEncodedEmail, currentUser);
                            // Timber.v("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_USERS_SHOPPING + "/" + mEncodedEmail);
                            updatedUserData.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, timestampNowHash);
                        }
                    }

                    /* Do a deep-path update */
                    mCurrentListRef.getRoot().updateChildren(updatedUserData, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Timber.v("%s %s", getString(R.string.log_error_updating_data), databaseError.getMessage());
                            } else {
                                /*
                                 * Set the reversed timestamp for the list owner
                                */
                                databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mEncodedEmail).child(mKey).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                        ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                        // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                        if (list != null) {
                                            long timeReverse = -(list.getTimestampLastChangedLong());
                                            String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                                    + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                            // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mOwner).child(mListKey).child(timeReverseLocation).toString());
                                            databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mShoppingList.getOwner()).child(mKey).child(timeReverseLocation).setValue(timeReverse);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Timber.v("%s %s", "Error updating data: ", databaseError.getMessage());
                                    }
                                });

                                /*
                                 * Set the reversed timestamp for the remaining user that the list is shared with
                                */
                                Iterator<User> it = mSharedWithUsers.values().iterator();
                                for (int i = 0; i < mSharedWithUsers.size(); i++) {
                                    if (it.hasNext()) {
                                        User user = it.next();
                                        final String email = user.getEmail();
                                        databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mKey).addListenerForSingleValueEvent(new ValueEventListener() {

                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                                // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                                if (list != null) {
                                                    long timeReverse = -(list.getTimestampLastChangedLong());
                                                    String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                                            + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                                    // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListKey).child(timeReverseLocation));
                                                    databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mKey).child(timeReverseLocation).setValue(timeReverse);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Timber.v("%s %s", "Error updating data: ", databaseError.getMessage());
                                            }
                                        });
                                    } else {
                                        Timber.v("this list is not shared with anyone");
                                    }
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: %s", databaseError);
            }
        });
    }

    public interface ClickListener {
        void onClick(View view, int position);

        void onLongClick(View view, int position);
    }

    class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private ClickListener clicklistener;
        private GestureDetector gestureDetector;

        RecyclerTouchListener(Context context, final RecyclerView recycleView, final ClickListener clicklistener) {

            this.clicklistener = clicklistener;
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recycleView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clicklistener != null) {
                        clicklistener.onLongClick(child, recycleView.getChildAdapterPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View child = rv.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clicklistener != null && gestureDetector.onTouchEvent(e)) {
                clicklistener.onClick(child, rv.getChildAdapterPosition(child));
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }
}
