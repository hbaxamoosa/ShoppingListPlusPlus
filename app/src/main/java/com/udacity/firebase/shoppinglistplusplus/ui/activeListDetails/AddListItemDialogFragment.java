package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import android.app.Dialog;
import android.os.Bundle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingListItem;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Lets user add new list item.
 */
public class AddListItemDialogFragment extends EditListDialogFragment {

    static String mListId;
    static String mOwner;
    private String listKey;

    // Firebase Realtime Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mShoppingListDatabaseReference;

    /**
     * Public static constructor that creates fragment and passes a bundle with data into it when adapter is created
     */
    public static AddListItemDialogFragment newInstance(ShoppingList shoppingList, String listId,
                                                        String encodedEmail,
                                                        HashMap<String, User> sharedWithUsers) {
        AddListItemDialogFragment addListItemDialogFragment = new AddListItemDialogFragment();
        Bundle bundle = EditListDialogFragment.newInstanceHelper(shoppingList,
                R.layout.dialog_add_item, listId, encodedEmail, sharedWithUsers);
        addListItemDialogFragment.setArguments(bundle);

        mListId = listId;
        mOwner = encodedEmail;
        return addListItemDialogFragment;
    }

    /**
     * Initialize instance variables with data from bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mShoppingListDatabaseReference = mFirebaseDatabase.getReference();
        // Timber.v("mFirebaseDatabase.getReference(): " + mFirebaseDatabase.getReference());

        Query queryList = mShoppingListDatabaseReference.child("shoppingLists").orderByChild("listName").equalTo(mListId);
        queryList.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DataSnapshot nodeDataSnapshot = dataSnapshot.getChildren().iterator().next();
                // Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                // Timber.v("nodeDataSnapshot.getKey(): " + nodeDataSnapshot.getKey());

                listKey = nodeDataSnapshot.getKey(); // find the node's key in shoppingLists
                // Timber.v("01 listKey: " + listKey);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: " + databaseError);
            }
        });

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /** {@link EditListDialogFragment#createDialogHelper(int)} is a
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
        /**
         * Adds list item if the input name is not empty
         */
        if (!mItemName.equals("")) {

            /* Make a map for the item you are adding */
            HashMap<String, Object> updatedItemToAddMap = new HashMap<String, Object>();

            /* Make a POJO for the item and immediately turn it into a HashMap */
            ShoppingListItem itemToAddObject = new ShoppingListItem(mItemName, mOwner);
            HashMap<String, Object> itemToAdd =
                    (HashMap<String, Object>) new ObjectMapper().convertValue(itemToAddObject, Map.class);

            // Timber.v("02 listKey: " + listKey);
            /* Add the item to the update map*/
            updatedItemToAddMap.put("/" + "shoppingListItems" + "/" + listKey + "/" + mItemName, itemToAdd);


            /* Make the timestamp for last changed */
            HashMap<String, Object> changedTimestampMap = new HashMap<>();
            changedTimestampMap.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

            /* Add the updated timestamp */
            updatedItemToAddMap.put("/" + "shoppingLists" + "/" + listKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, changedTimestampMap);

            /* Do the update */
            mShoppingListDatabaseReference.updateChildren(updatedItemToAddMap);

            /**
             * Close the dialog fragment when done
             */
            AddListItemDialogFragment.this.getDialog().cancel();
        }
    }
}
