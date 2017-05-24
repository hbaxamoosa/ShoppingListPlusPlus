package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingListItem;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.ui.BaseActivity;
import com.udacity.firebase.shoppinglistplusplus.ui.sharing.ShareListActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import timber.log.Timber;

/**
 * Represents the details screen for the selected shopping list
 */
public class ActiveListDetailsActivity extends BaseActivity {
    private static final String LOG_TAG = ActiveListDetailsActivity.class.getSimpleName();
    private Button mButtonShopping;
    private TextView mTextViewPeopleShopping;
    private RecyclerView mRecyclerView;
    private String mListId;
    private String mListKey;
    private int mPosition;
    private User mCurrentUser;
    /* Stores whether the current user is shopping */
    private boolean mShopping = false;
    /* Stores whether the current user is the owner */
    private boolean mCurrentUserIsOwner = false;
    private ShoppingList mShoppingList;
    private ArrayList<ShoppingList> mShoppingListArray = new ArrayList<>();
    private ArrayList<ShoppingListItem> mShoppingListItemsArray = new ArrayList<>();
    private HashMap<String, User> mSharedWithUsers;
    private ActiveListItemAdapter mActiveListItemAdapter;

    // Firebase Realtime Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mShoppingListDatabaseReference;
    private DatabaseReference mShoppingListItemsReference;
    private ChildEventListener mShoppingListsEventListener;
    private ChildEventListener mShoppingListItemsEventListener;

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
        mPosition = intent.getIntExtra(Constants.KEY_LIST_ITEM_ID, 999);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mShoppingListDatabaseReference = mFirebaseDatabase.getReference("shoppingLists");
        mShoppingListItemsReference = mFirebaseDatabase.getReference("shoppingListItems");

        mShoppingListDatabaseReference.orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("dataSnapshot: " + dataSnapshot.getValue().toString());
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                for (int i = 0; i < mPosition + 1; i++) {
                    if (it.hasNext()) {
                        DataSnapshot listSnapshot = it.next();
                        mShoppingList = listSnapshot.getValue(ShoppingList.class);
                        Timber.v("listSnapshot.getKey(): " + listSnapshot.getKey());
                        mListKey = listSnapshot.getKey();
                        Timber.v("mListKey: " + mListKey);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("error: " + databaseError.toString());
            }
        });

        /**
         * Link layout elements from XML and setup the toolbar
         */
        initializeScreen();

        /* Calling invalidateOptionsMenu causes onCreateOptionsMenu to be called */
        invalidateOptionsMenu();

        /* Set title appropriately. */
        setTitle(mListId);

        // for testing purposes
        mCurrentUserIsOwner = true;

        /* Save the most up-to-date version of current user in mCurrentUser */

        final Activity thisActivity = this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Inflate the menu; this adds items to the action bar if it is present. */
        getMenuInflater().inflate(R.menu.menu_list_details, menu);

        /**
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

        /**
         * Show edit list dialog when the edit action is selected
         */
        if (id == R.id.action_edit_list_name) {
            showEditListNameDialog();
            return true;
        }

        /**
         * removeList() when the remove action is selected
         */
        if (id == R.id.action_remove_list) {
            removeList();
            return true;
        }

        /**
         * Eventually we'll add this
         */
        if (id == R.id.action_share_list) {
            Intent intent = new Intent(ActiveListDetailsActivity.this, ShareListActivity.class);
            intent.putExtra(Constants.KEY_LIST_ID, mListId);
            /* Starts an active showing the details for the selected list */
            startActivity(intent);
            return true;
        }

        /**
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
        Timber.v("onResume()");

        /**
         * Get information about Shopping List
         */
        if (mShoppingListsEventListener == null) {
            mShoppingListsEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Timber.v("onChildAdded in mShoppingListDatabaseReference");
                    ShoppingList shoppingList = dataSnapshot.getValue(ShoppingList.class);
                    mShoppingListArray.add(shoppingList);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Timber.v("onChildChanged in mShoppingListDatabaseReference");
                    mShoppingListArray.clear();
                    ShoppingList shoppingList = dataSnapshot.getValue(ShoppingList.class);
                    mShoppingListArray.add(shoppingList);
                    /* Calling invalidateOptionsMenu causes onCreateOptionsMenu to be called */
                    invalidateOptionsMenu();

                    /* Set title appropriately. */
                    mListId = shoppingList.getListName();
                    setTitle(mListId);
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Timber.v("onChildRemoved");
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    Timber.v("onChildMoved");
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Timber.v("onCancelled");
                }
            };
            mShoppingListDatabaseReference.addChildEventListener(mShoppingListsEventListener);
        }

        /**
         * Get information about Shopping List Items
         */

        if (mShoppingListItemsEventListener == null) {
            mShoppingListItemsEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Timber.v("onChildAdded in mShoppingListItemsReference");
                    Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                    // Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                    // Timber.v("dataSnapshot.getChildren().iterator().next().getValue(): " + dataSnapshot.getChildren().iterator().next().getValue());
                    // Timber.v("dataSnapshot.getChildren().iterator().next().getKey(): " + dataSnapshot.getChildren().iterator().next().getKey());
                    // Timber.v("dataSnapshot.child(dataSnapshot.getChildren().iterator().next().getKey()).getValue(): " + dataSnapshot.child(dataSnapshot.getChildren().iterator().next().getKey()).getValue());
                    if (mListKey.equals(dataSnapshot.getKey())) {
                        Timber.v("dataSnapshot.getChildrenCount(): " + dataSnapshot.getChildrenCount());
                        Timber.v("my value is: " + dataSnapshot.getValue());
                        for (DataSnapshot ShoppingListItemsSnapshot : dataSnapshot.getChildren()) {
                            Timber.v("ShoppingListItemsSnapshot.getKey(): " + ShoppingListItemsSnapshot.getKey());
                            Timber.v("ShoppingListItemsSnapshot.getValue(): " + ShoppingListItemsSnapshot.getValue());
                            ShoppingListItem shoppingListItems = ShoppingListItemsSnapshot.getValue(ShoppingListItem.class);
                            mShoppingListItemsArray.add(shoppingListItems);
                        }
                        mActiveListItemAdapter.notifyDataSetChanged();
                    } else {
                        Timber.v("this list does not contain any items!");
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Timber.v("onChildChanged in mShoppingListItemsReference");
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    Timber.v("onChildRemoved in mShoppingListItemsReference");
                    mShoppingListItemsArray.clear();
                    Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                    // Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                    // Timber.v("dataSnapshot.getChildren().iterator().next().getValue(): " + dataSnapshot.getChildren().iterator().next().getValue());
                    // Timber.v("dataSnapshot.getChildren().iterator().next().getKey(): " + dataSnapshot.getChildren().iterator().next().getKey());
                    // Timber.v("dataSnapshot.child(dataSnapshot.getChildren().iterator().next().getKey()).getValue(): " + dataSnapshot.child(dataSnapshot.getChildren().iterator().next().getKey()).getValue());
                    if (mListKey.equals(dataSnapshot.getKey())) {
                        Timber.v("dataSnapshot.getChildrenCount(): " + dataSnapshot.getChildrenCount());
                        Timber.v("my value is: " + dataSnapshot.getValue());
                        for (DataSnapshot ShoppingListItemsSnapshot : dataSnapshot.getChildren()) {
                            Timber.v("ShoppingListItemsSnapshot.getKey(): " + ShoppingListItemsSnapshot.getKey());
                            Timber.v("ShoppingListItemsSnapshot.getValue(): " + ShoppingListItemsSnapshot.getValue());
                            ShoppingListItem shoppingListItems = ShoppingListItemsSnapshot.getValue(ShoppingListItem.class);
                            mShoppingListItemsArray.add(shoppingListItems);
                        }
                        mActiveListItemAdapter.notifyDataSetChanged();
                    } else {
                        Timber.v("this list does not contain any items!");
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    Timber.v("onChildMoved");
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Timber.v("onCancelled");
                }
            };
            mShoppingListItemsReference.addChildEventListener(mShoppingListItemsEventListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Timber.v("onPause()");

        if (mShoppingListsEventListener != null) {
            mShoppingListDatabaseReference.removeEventListener(mShoppingListsEventListener);
        }
        if (mShoppingListItemsEventListener != null) {
            mShoppingListItemsReference.removeEventListener(mShoppingListItemsEventListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Timber.v("onStop()");

        if (mShoppingListsEventListener != null) {
            mShoppingListDatabaseReference.removeEventListener(mShoppingListsEventListener);
        }
        if (mShoppingListItemsEventListener != null) {
            mShoppingListItemsReference.removeEventListener(mShoppingListItemsEventListener);
        }
    }

    /**
     * Cleanup when the activity is destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mShoppingListsEventListener != null) {
            mShoppingListDatabaseReference.removeEventListener(mShoppingListsEventListener);
        }
        if (mShoppingListItemsEventListener != null) {
            mShoppingListItemsReference.removeEventListener(mShoppingListItemsEventListener);
        }
    }

    /**
     * Link layout elements from XML and setup the toolbar
     */
    private void initializeScreen() {
        Timber.v("initializeScreen()");
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        LinearLayoutManager manager = new LinearLayoutManager(ActiveListDetailsActivity.this);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(manager);

        try {
            mActiveListItemAdapter = new ActiveListItemAdapter(ActiveListDetailsActivity.this, mShoppingListItemsArray, mListKey);
            mRecyclerView.setAdapter(mActiveListItemAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mActiveListItemAdapter != null) {
            mActiveListItemAdapter.notifyDataSetChanged();
        }
        mRecyclerView.setAdapter(mActiveListItemAdapter);

        mTextViewPeopleShopping = (TextView) findViewById(R.id.text_view_people_shopping);
        mButtonShopping = (Button) findViewById(R.id.button_shopping);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
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
            ArrayList<String> usersWhoAreNotYou = new ArrayList<>();
            /**
             * If at least one user is shopping
             * Add userName to the list of users shopping if this user is not current user
             */
            for (User user : usersShopping.values()) {
                if (user != null && !(user.getEmail().equals(mEncodedEmail))) {
                    usersWhoAreNotYou.add(user.getName());
                }
            }

            int numberOfUsersShopping = usersShopping.size();
            String usersShoppingText;

            /**
             * If current user is shopping...
             * If current user is the only person shopping, set text to "You are shopping"
             * If current user and one user are shopping, set text "You and userName are shopping"
             * Else set text "You and N others shopping"
             */
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
                /**
                 * If current user is not shopping..
                 * If there is only one person shopping, set text to "userName is shopping"
                 * If there are two users shopping, set text "userName1 and userName2 are shopping"
                 * Else set text "userName and N others shopping"
                 */
            } else {
                switch (numberOfUsersShopping) {
                    case 1:
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
        DialogFragment dialog = RemoveListDialogFragment.newInstance(mShoppingList, mListId,
                mSharedWithUsers);
        dialog.show(getFragmentManager(), "RemoveListDialogFragment");
    }

    /**
     * Show the add list item dialog when user taps "Add list item" fab
     */
    public void showAddListItemDialog(View view) {
        /* Create an instance of the dialog fragment and show it */
        DialogFragment dialog = AddListItemDialogFragment.newInstance(mShoppingList, mListId,
                mEncodedEmail, mSharedWithUsers);
        dialog.show(getFragmentManager(), "AddListItemDialogFragment");
    }

    /**
     * Show edit list name dialog when user selects "Edit list name" menu item
     */
    public void showEditListNameDialog() {
        /* Create an instance of the dialog fragment and show it */
        DialogFragment dialog = EditListNameDialogFragment.newInstance(mShoppingList, mListId,
                mEncodedEmail, mSharedWithUsers);
        dialog.show(this.getFragmentManager(), "EditListNameDialogFragment");
    }

    /**
     * Show the edit list item name dialog after longClick on the particular item
     */
    public void showEditListItemNameDialog(String itemName, String itemId) {
        /* Create an instance of the dialog fragment and show it */
        DialogFragment dialog = EditListItemNameDialogFragment.newInstance(mShoppingList, itemName,
                itemId, mListId, mEncodedEmail, mSharedWithUsers);

        dialog.show(this.getFragmentManager(), "EditListItemNameDialogFragment");
    }

    /**
     * This method is called when user taps "Start/Stop shopping" button
     */
    public void toggleShopping(View view) {
        /**
         * Create map and fill it in with deep path multi write operations list
         */
        HashMap<String, Object> updatedUserData = new HashMap<String, Object>();
        String propertyToUpdate = Constants.FIREBASE_PROPERTY_USERS_SHOPPING + "/" + mEncodedEmail;

        /**
         * If current user is already shopping, remove current user from usersShopping map
         */
        if (mShopping) {

        } else {
        }
    }

}
