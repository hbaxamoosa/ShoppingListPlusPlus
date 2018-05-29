package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import android.app.Dialog;
import android.os.Bundle;

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
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import timber.log.Timber;

/**
 * Lets user edit list item name for all copies of the current list
 */
public class EditListItemNameDialogFragment extends EditListDialogFragment {
    String mItemName, mListName, mListID;
    HashMap<String, User> mSharedWithUsers = new HashMap<>();

    // Firebase Realtime Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mListItemsRef;

    /**
     * Public static constructor that creates fragment and passes a bundle with data into it when
     * adapter is created
     */
    public static EditListItemNameDialogFragment newInstance(ShoppingList shoppingList, String itemName,
                                                             String listID, String listName, String encodedEmail,
                                                             HashMap<String, User> sharedWithUsers) {
        EditListItemNameDialogFragment editListItemNameDialogFragment = new EditListItemNameDialogFragment();

        // Timber.v("itemName: %s", itemName);
        // Timber.v("itemID: %s", listID);
        // Timber.v("listName: %s", listName);
        // Timber.v("encodedEmail: %s", encodedEmail);

        Bundle bundle = EditListDialogFragment.newInstanceHelper(shoppingList, R.layout.dialog_edit_item,
                listName, encodedEmail, sharedWithUsers);
        bundle.putString(Constants.KEY_LIST_ITEM_NAME, itemName);
        bundle.putString(Constants.KEY_LIST_ID, listID);
        bundle.putString(Constants.KEY_LIST_NAME, listName);
        bundle.putSerializable(Constants.KEY_SHARED_WITH_USERS, sharedWithUsers);
        editListItemNameDialogFragment.setArguments(bundle);

        return editListItemNameDialogFragment;
    }

    /**
     * Initialize instance variables with data from bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItemName = getArguments().getString(Constants.KEY_LIST_ITEM_NAME);
        mListID = getArguments().getString(Constants.KEY_LIST_ID);
        mListName = getArguments().getString(Constants.KEY_LIST_NAME);
        mSharedWithUsers = (HashMap<String, User>) getArguments().getSerializable(Constants.KEY_SHARED_WITH_USERS);
    }


    @Override

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /* {@link EditListDialogFragment#createDialogHelper(int)} is a
         * superclass method that creates the dialog
         */
        Dialog dialog = super.createDialogHelper(R.string.positive_button_edit_item);
        /*
         * {@link EditListDialogFragment#helpSetDefaultValueEditText(String)} is a superclass
         * method that sets the default text of the TextView
         */
        super.helpSetDefaultValueEditText(mItemName);

        return dialog;
    }

    /**
     * Change selected list item name to the editText input if it is not empty
     */
    protected void doListEdit() {
        final String nameInput = mEditTextForList.getText().toString();

        // Timber.v("mItemName: " + mItemName);
        // Timber.v("mItemId: " + mItemId);

        /*
         * Set input text to be the current list item name if it is not empty and is not the
         * previous name.
         */
        if (!nameInput.equals("") && !nameInput.equals(mItemName)) {

            // Initialize Firebase components
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            mListItemsRef = mFirebaseDatabase.getReference();

            /*
             * Check that the user inputted list name is not empty, has changed the original name
             * and that the dialog was properly initialized with the current name and id of the list.
             */

            Query query = mListItemsRef.child(Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS).orderByChild(mListID);
            // Timber.v("query.getRef(): %s", query.getRef());
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Timber.v("dataSnapshot.getValue(): %s", dataSnapshot.getValue());
                    // Timber.v("dataSnapshot.getKey(): %s", dataSnapshot.getKey());

                    /* Make a map for the item you are adding */
                    HashMap<String, Object> updatedItemToAddMap = new HashMap<String, Object>();

                    ShoppingListItem itemToAddObject = new ShoppingListItem(nameInput, mOwner);
                    HashMap<String, Object> itemToAdd =
                            (HashMap<String, Object>) new ObjectMapper().convertValue(itemToAddObject, Map.class);

                    // Add the item to the update map
                    // the path in 'put' must be relative to the database reference, in this case mListItemsRef, which points to root
                    // add the nameInput as a new item
                    updatedItemToAddMap.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/" + mListID + "/" + nameInput, itemToAdd);
                    // delete the existing item
                    updatedItemToAddMap.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/" + mListID + "/" + mItemName, null);

                    /* Make the timestamp for last changed */
                    HashMap<String, Object> changedTimestampMap = new HashMap<>();
                    changedTimestampMap.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

                    /* Add the updated timestamp to the owner's list */
                    updatedItemToAddMap.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mEncodedEmail + "/" + mListID + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, changedTimestampMap);

                    /* Add the updated timestamp to the sharedWith user's lists */
                    Iterator<User> it = mSharedWith.values().iterator();
                    for (int i = 0; i < mSharedWith.size(); i++) {
                        if (it.hasNext()) {
                            User user = it.next();
                            String email = user.getEmail();
                            updatedItemToAddMap.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mListID + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, changedTimestampMap);
                        }
                    }

                    /*
                     * After posting the update payload, we need to use the CompletionListener to update the FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED
                    */
                    mListItemsRef.updateChildren(updatedItemToAddMap, new DatabaseReference.CompletionListener() {

                        @Override
                        public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Timber.v("%s %s", getString(R.string.log_error_updating_data), databaseError.getMessage());
                            } else {
                                /*
                                 * Set the reversed timestamp for the list owner
                                */
                                databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mOwner).child(mListID).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                        ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                        // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                        if (list != null) {
                                            long timeReverse = -(list.getTimestampLastChangedLong());
                                            String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                                    + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                            // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mOwner).child(mListKey).child(timeReverseLocation).toString());
                                            databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mOwner).child(mListID).child(timeReverseLocation).setValue(timeReverse);
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
                                        databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListID).addListenerForSingleValueEvent(new ValueEventListener() {

                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                                // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                                if (list != null) {
                                                    long timeReverse = -(list.getTimestampLastChangedLong());
                                                    String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                                            + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                                    // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListKey).child(timeReverseLocation));
                                                    databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListID).child(timeReverseLocation).setValue(timeReverse);
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
