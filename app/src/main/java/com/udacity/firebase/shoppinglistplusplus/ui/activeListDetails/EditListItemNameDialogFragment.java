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

import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Lets user edit list item name for all copies of the current list
 */
public class EditListItemNameDialogFragment extends EditListDialogFragment {
    String mItemName, mItemId;

    // Firebase Realtime Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mListItemsRef;

    /**
     * Public static constructor that creates fragment and passes a bundle with data into it when
     * adapter is created
     */
    public static EditListItemNameDialogFragment newInstance(ShoppingList shoppingList, String itemName,
                                                             String itemId, String listId, String encodedEmail,
                                                             HashMap<String, User> sharedWithUsers) {
        EditListItemNameDialogFragment editListItemNameDialogFragment = new EditListItemNameDialogFragment();

        Bundle bundle = EditListDialogFragment.newInstanceHelper(shoppingList, R.layout.dialog_edit_item,
                listId, encodedEmail, sharedWithUsers);
        bundle.putString(Constants.KEY_LIST_ITEM_NAME, itemName);
        bundle.putString(Constants.KEY_LIST_ITEM_ID, itemId);
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
        mItemId = getArguments().getString(Constants.KEY_LIST_ITEM_ID);

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mListItemsRef = mFirebaseDatabase.getReference();
    }


    @Override

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        /** {@link EditListDialogFragment#createDialogHelper(int)} is a
         * superclass method that creates the dialog
         */
        Dialog dialog = super.createDialogHelper(R.string.positive_button_edit_item);
        /**
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

        /**
         * Set input text to be the current list item name if it is not empty and is not the
         * previous name.
         */
        if (!nameInput.equals("") && !nameInput.equals(mItemName)) {

            /**
             * Check that the user inputted list name is not empty, has changed the original name
             * and that the dialog was properly initialized with the current name and id of the list.
             */

            Query query = mListItemsRef.child(Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS).orderByChild(mItemId);
            Timber.v("query.getRef(): %s", query.getRef());
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Timber.v("dataSnapshot.getValue(): %s", dataSnapshot.getValue());
                    Timber.v("dataSnapshot.getKey(): %s", dataSnapshot.getKey());

                    /* Make a map for the item you are adding */
                    HashMap<String, Object> updatedItemToAddMap = new HashMap<String, Object>();

                    ShoppingListItem itemToAddObject = new ShoppingListItem(nameInput, mOwner);
                    HashMap<String, Object> itemToAdd =
                            (HashMap<String, Object>) new ObjectMapper().convertValue(itemToAddObject, Map.class);

                    /* Add the item to the update map */
                    /**
                     * the path in 'put' must be the fully declared path
                     */
                    // add the nameInput as a new item
                    updatedItemToAddMap.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/" + mItemId + "/" + nameInput, itemToAdd);
                    // delete the existing item
                    updatedItemToAddMap.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/" + mItemId + "/" + mItemName, null);

                    /* Make the timestamp for last changed */
                    HashMap<String, Object> changedTimestampMap = new HashMap<>();
                    changedTimestampMap.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

                    /* Add the updated timestamp to the user list */
                    updatedItemToAddMap.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mEncodedEmail + "/" + mItemId + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, changedTimestampMap);

                    /* Do the update */
                    mListItemsRef.updateChildren(updatedItemToAddMap);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Timber.v("Error: %s", databaseError);
                }
            });

        }
    }
}
