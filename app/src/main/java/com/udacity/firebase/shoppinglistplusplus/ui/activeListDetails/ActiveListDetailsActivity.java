package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

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

import timber.log.Timber;

/**
 * Represents the details screen for the selected shopping list
 */
public class ActiveListDetailsActivity extends BaseActivity {
    private static final String LOG_TAG = ActiveListDetailsActivity.class.getSimpleName();
    public static String mKey;
    private Button mButtonShopping;
    private TextView mTextViewPeopleShopping;
    private RecyclerView mRecyclerView;
    private String mListId;
    private int mPosition;
    private User mCurrentUser;
    private String mEncodedEmail;
    /* Stores whether the current user is shopping */
    private boolean mShopping = false;
    /* Stores whether the current user is the owner */
    private boolean mCurrentUserIsOwner = false;
    private ShoppingList mShoppingList;
    private ArrayList<ShoppingList> mShoppingListArray = new ArrayList<>();
    private ArrayList<ShoppingListItem> mShoppingListItemsArray = new ArrayList<>();
    private HashMap<String, User> mSharedWithUsers;
    private ActiveListItemAdapter mActiveListItemAdapter;

    // SharedPrefs
    private SharedPreferences mSharedPref;

    // Firebase Realtime Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mShoppingListsReference, mShoppingListItemsReference, mCurrentUserRef;
    private ValueEventListener mShoppingListItemsValueEventListener, mCurrentUserRefListener;

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
        mKey = intent.getStringExtra("listKey");

        // get SharedPrefs
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mEncodedEmail = mSharedPref.getString(Constants.KEY_ENCODED_EMAIL, null);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mShoppingListsReference = mFirebaseDatabase.getReference("shoppingLists");
        mShoppingListItemsReference = mFirebaseDatabase.getReference("shoppingListItems" + "/" + mKey);
        mCurrentUserRef = mFirebaseDatabase.getReference("users").child(mEncodedEmail);


        // use this call to Firebase rtdb to grab the correct Shopping List
        mShoppingListsReference.orderByKey().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("dataSnapshot: " + dataSnapshot.getValue().toString());
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                for (int i = 0; i < mPosition + 1; i++) {
                    if (it.hasNext()) {
                        DataSnapshot listSnapshot = it.next();
                        mShoppingList = listSnapshot.getValue(ShoppingList.class);
                        Timber.v("mShoppingList.getListName() : " + mShoppingList.getListName());
                        Timber.v("mShoppingList.getOwner() : " + mShoppingList.getOwner());
                    }
                }
                /* Check if the current user is owner */
                mCurrentUserIsOwner = Utils.checkIfOwner(mShoppingList, mEncodedEmail);
                Timber.v("addListenerForSingleValueEvent mCurrentUserIsOwner: " + mCurrentUserIsOwner);

                /* Calling invalidateOptionsMenu causes onCreateOptionsMenu to be called */
                invalidateOptionsMenu();

                /**
                 * Handle the Start/Stop Shopping button
                 */
                HashMap<String, User> usersShopping = mShoppingList.getUsersShopping();
                if (mShoppingList.getUsersShopping() != null) {
                    Timber.v("mShoppingList.getUsersShopping(): " + mShoppingList.getUsersShopping().toString());
                }
                if (usersShopping != null && usersShopping.size() != 0 && usersShopping.containsKey(mEncodedEmail)) {
                    Timber.v("mShopping is TRUE");
                    mShopping = true;
                    mButtonShopping.setText(getString(R.string.button_stop_shopping));
                    mButtonShopping.setBackgroundColor(ContextCompat.getColor(ActiveListDetailsActivity.this, R.color.dark_grey));
                } else {
                    Timber.v("mShopping is FALSE");
                    mShopping = false;
                    mButtonShopping.setText(getString(R.string.button_start_shopping));
                    mButtonShopping.setBackgroundColor(ContextCompat.getColor(ActiveListDetailsActivity.this, R.color.primary_dark));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("error: " + databaseError.toString());
            }
        });

        /* Save the most up-to-date version of current user in mCurrentUser */
        mCurrentUserRefListener = mCurrentUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                User currentUser = dataSnapshot.getValue(User.class);
                if (currentUser != null) mCurrentUser = currentUser;
                else finish();
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

        /* Set title appropriately. */
        setTitle(mListId);

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
        // Timber.v("onResume()");

        mShoppingListItemsValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("onChildAdded in mShoppingListItemsReference");
                mShoppingListItemsArray.clear();
                Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                Timber.v("mKey: " + mKey);
                if (mKey.equals(dataSnapshot.getKey())) {
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
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: " + databaseError.toString());
            }
        };
        mShoppingListItemsReference.addValueEventListener(mShoppingListItemsValueEventListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Timber.v("onPause()");

        if (mShoppingListItemsValueEventListener != null) {
            mShoppingListItemsReference.removeEventListener(mShoppingListItemsValueEventListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Timber.v("onStop()");

        if (mShoppingListItemsValueEventListener != null) {
            mShoppingListItemsReference.removeEventListener(mShoppingListItemsValueEventListener);
        }

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

        if (mShoppingListItemsValueEventListener != null) {
            mShoppingListItemsReference.removeEventListener(mShoppingListItemsValueEventListener);
        }

        if (mCurrentUserRefListener != null) {
            mCurrentUserRef.removeEventListener(mCurrentUserRefListener);
        }
    }

    /**
     * Link layout elements from XML and setup the toolbar
     */
    private void initializeScreen() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
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

                    if (selectedListItem != null) {

                        /* If current user is shopping */
                        if (mShopping) {
                            /* Create map and fill it in with deep path multi write operations list */
                            final HashMap<String, Object> updatedItemBoughtData = new HashMap<String, Object>();

                            /* Buy selected item if it is NOT already bought */
                            if (!selectedListItem.isBought()) {
                                updatedItemBoughtData.put("/" + selectedListItem.getItemName() + "/" + Constants.FIREBASE_PROPERTY_BOUGHT, true);
                                updatedItemBoughtData.put("/" + selectedListItem.getItemName() + "/" + Constants.FIREBASE_PROPERTY_BOUGHT_BY, mEncodedEmail);
                            } else {
                                updatedItemBoughtData.put("/" + selectedListItem.getItemName() + "/" + Constants.FIREBASE_PROPERTY_BOUGHT, false);
                                updatedItemBoughtData.put("/" + selectedListItem.getItemName() + "/" + Constants.FIREBASE_PROPERTY_BOUGHT_BY, null);
                            }

                            Query query = mShoppingListItemsReference.child(selectedListItem.getItemName());
                            Timber.v("query.getRef(): " + query.getRef());
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                                    Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());

                                    Timber.v("updatedItemBoughtData.toString(): " + updatedItemBoughtData.toString());
                                /* Do the update */
                                    mShoppingListItemsReference.updateChildren(updatedItemBoughtData, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                            if (databaseError != null) {
                                                Timber.v("datebaseError: " + databaseError.toString());
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Timber.v("Error: " + databaseError);
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(ActiveListDetailsActivity.this, "Long press on position: " + position,
                        Toast.LENGTH_LONG).show();
                // Timber.v("mActiveListItemAdapter.getItemCount(): " + mActiveListItemAdapter.getItemCount());
                // Timber.v("mActiveListItemAdapter.getItemId(position): " + mActiveListItemAdapter.getItemId(position));
                RecyclerView hasnain = (RecyclerView) view.findViewById(R.id.recyclerView);
                // Timber.v("mShoppingListItemsArray.get(position).getItemName(): " + mShoppingListItemsArray.get(position).getItemName());

                // Timber.v("mActiveListItemAdapter.getItemId(position): " + mActiveListItemAdapter.getItemId(position));
                showEditListItemNameDialog(mShoppingListItemsArray.get(position).getItemName(), mKey);
            }

        }));

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
        DialogFragment dialog = RemoveListDialogFragment.newInstance(mShoppingList, mListId, mKey,
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
                mKey, mListId, mEncodedEmail, mSharedWithUsers);

        dialog.show(this.getFragmentManager(), "EditListItemNameDialogFragment");
    }

    /**
     * This method is called when user taps "Start/Stop shopping" button
     */
    public void toggleShopping(View view) {
        Timber.v("toggleShopping(View view)");
        /**
         * If current user is already shopping, remove current user from usersShopping map
         */

        Query query = mShoppingListsReference.child(mKey).child("usersShopping").child(mEncodedEmail);
        Timber.v("query.getRef(): " + query.getRef());
            /* Check to see whether user is shopping; use a SingleValueEvent listener for memory efficiency */
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                    /* Either add or remove the current user from the usersShopping map */
                if (mShopping) {
                    Timber.v("mShopping is TRUE");
                        /* user WAS shopping, but now has STOPPED shopping */
                    mShoppingListsReference.child(mKey).child("usersShopping").child(mEncodedEmail).removeValue();
                } else {
                    Timber.v("mShopping is FALSE");
                        /* user WAS NOT shopping, but now has STARTED shopping */
                    mShoppingListsReference.child(mKey).child("usersShopping").child(mEncodedEmail).setValue(mCurrentUser);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: " + databaseError);
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

        public RecyclerTouchListener(Context context, final RecyclerView recycleView, final ClickListener clicklistener) {

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
