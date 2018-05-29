package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import android.app.Dialog;
import android.os.Bundle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingListItem;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import timber.log.Timber;

/**
 * Lets user add new list item.
 */
public class AddListItemDialogFragment extends EditListDialogFragment {

    String mListName, mOwner, mListKey;
    HashMap<String, User> mSharedWithUsers = new HashMap<>();

    /**
     * Public static constructor that creates fragment and passes a bundle with data into it when adapter is created
     */
    public static AddListItemDialogFragment newInstance(ShoppingList shoppingList, String listName,
                                                        String listKey, String encodedEmail,
                                                        HashMap<String, User> sharedWithUsers) {

        Timber.v("%s, %s", "listName: ", listName);
        Timber.v("%s, %s", "listKey: ", listKey);
        Timber.v("%s %s", "encodedEmail: ", encodedEmail);
        AddListItemDialogFragment addListItemDialogFragment = new AddListItemDialogFragment();
        Bundle bundle = EditListDialogFragment.newInstanceHelper(shoppingList,
                R.layout.dialog_add_item, listName, encodedEmail, sharedWithUsers);
        addListItemDialogFragment.setArguments(bundle);
        bundle.putString(Constants.KEY_LIST_NAME, listName);
        bundle.putString(Constants.KEY_LIST_ID, listKey);
        bundle.putString(Constants.KEY_LIST_OWNER, encodedEmail);
        bundle.putSerializable(Constants.KEY_SHARED_WITH_USERS, sharedWithUsers);
        return addListItemDialogFragment;
    }

    /**
     * Initialize instance variables with data from bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListKey = getArguments().getString(Constants.KEY_LIST_ID);
        mListName = getArguments().getString(Constants.KEY_LIST_NAME);
        mOwner = getArguments().getString(Constants.KEY_LIST_OWNER);
        mSharedWithUsers = (HashMap<String, User>) getArguments().getSerializable(Constants.KEY_SHARED_WITH_USERS);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /* {@link EditListDialogFragment#createDialogHelper(int)} is a
         * superclass method that creates the dialog
         **/
        return super.createDialogHelper(R.string.positive_button_add_list_item);
    }

    /**
     * Adds new item to the current shopping list
     */
    @Override
    protected void doListEdit() {
        String mItemName = mEditTextForList.getText().toString();

        // Initialize Firebase components
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mShoppingListItemsDatabaseReference = mFirebaseDatabase.getReference();

        /*
         * Adds list item if the input name is not empty
         */
        if (!mItemName.equals("")) {

            /* Make a map for the item you are adding */
            HashMap<String, Object> updatedItemToAddMap = new HashMap<String, Object>();

            /* Make a POJO for the item and immediately turn it into a HashMap */
            ShoppingListItem itemToAddObject = new ShoppingListItem(mItemName, mOwner);
            HashMap<String, Object> itemToAdd =
                    (HashMap<String, Object>) new ObjectMapper().convertValue(itemToAddObject, Map.class);

            /* Add the shopping list item to the update map */
            updatedItemToAddMap.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/" + mListKey + "/" + mItemName, itemToAdd);


            /* Make the timestamp for last changed */
            HashMap<String, Object> changedTimestampMap = new HashMap<>();
            changedTimestampMap.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

            /* Add the updated timestamp to the owner's list */
            updatedItemToAddMap.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mEncodedEmail + "/" + mListKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, changedTimestampMap);

            /* Add the updated timestamp to the sharedWith user's lists */
            Iterator<User> it = mSharedWith.values().iterator();
            for (int i = 0; i < mSharedWith.size(); i++) {
                if (it.hasNext()) {
                    User user = it.next();
                    String email = user.getEmail();
                    updatedItemToAddMap.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mListKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, changedTimestampMap);
                }
            }

            /* Do the update */
            mShoppingListItemsDatabaseReference.updateChildren(updatedItemToAddMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Timber.v("%s %s", getString(R.string.log_error_updating_data), databaseError.getMessage());
                    } else {
                                /*
                                 * Set the reversed timestamp for the list owner
                                */
                        databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mOwner).child(mListKey).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                if (list != null) {
                                    long timeReverse = -(list.getTimestampLastChangedLong());
                                    String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                            + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                    // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mOwner).child(mListKey).child(timeReverseLocation).toString());
                                    databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mOwner).child(mListKey).child(timeReverseLocation).setValue(timeReverse);
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
                        Iterator<User> it = mSharedWith.values().iterator();
                        for (int i = 0; i < mSharedWith.size(); i++) {
                            if (it.hasNext()) {
                                User user = it.next();
                                final String email = user.getEmail();
                                databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListKey).addListenerForSingleValueEvent(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                        // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                        if (list != null) {
                                            long timeReverse = -(list.getTimestampLastChangedLong());
                                            String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                                    + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                            // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListKey).child(timeReverseLocation));
                                            databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListKey).child(timeReverseLocation).setValue(timeReverse);
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

            /*
             * Close the dialog fragment when done
             */
            AddListItemDialogFragment.this.getDialog().cancel();
        }
    }
}
