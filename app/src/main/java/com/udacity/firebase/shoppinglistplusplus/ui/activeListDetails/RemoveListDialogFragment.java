package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.ui.MainActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.util.HashMap;
import java.util.Iterator;

import timber.log.Timber;

/**
 * Lets the user remove active shopping list
 */
public class RemoveListDialogFragment extends DialogFragment {
    final static String LOG_TAG = RemoveListDialogFragment.class.getSimpleName();
    String mListId;
    String mKey;
    String mListOwner;
    HashMap mSharedWith;

    /**
     * Public static constructor that creates fragment and passes a bundle with data into it when adapter is created
     */
    public static RemoveListDialogFragment newInstance(ShoppingList shoppingList, String listId, String key,
                                                       HashMap<String, User> sharedWithUsers) {
        RemoveListDialogFragment removeListDialogFragment = new RemoveListDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Constants.KEY_LIST_ID, listId);
        bundle.putString("key", key);
        bundle.putString(Constants.KEY_LIST_OWNER, shoppingList.getOwner());
        bundle.putSerializable(Constants.KEY_SHARED_WITH_USERS, sharedWithUsers);
        removeListDialogFragment.setArguments(bundle);
        return removeListDialogFragment;
    }

    /**
     * Initialize instance variables with data from bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListId = getArguments().getString(Constants.KEY_LIST_ID);
        mListOwner = getArguments().getString(Constants.KEY_LIST_OWNER);
        mKey = getArguments().getString("key");
        mSharedWith = (HashMap) getArguments().getSerializable(Constants.KEY_SHARED_WITH_USERS);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.CustomTheme_Dialog)
                .setTitle(getActivity().getResources().getString(R.string.action_remove_list))
                .setMessage(getString(R.string.dialog_message_are_you_sure_remove_list))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        removeList();
                        /* Dismiss the dialog */
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        /* Dismiss the dialog */
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert);

        return builder.create();
    }

    private void removeList() {

        // Firebase Realtime Database
        FirebaseDatabase mFirebaseDatabase;
        final DatabaseReference mFirebaseDatabaseReference;

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseDatabaseReference = mFirebaseDatabase.getReference();

        Query query = mFirebaseDatabaseReference.orderByKey();
        // Timber.v("query.getRef(): " + query.getRef());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DataSnapshot mShoppingListsDataSnapshot = dataSnapshot.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mListOwner).child(mKey);
                // Timber.v("mShoppingListsDataSnapshot.getRef(): " + mShoppingListsDataSnapshot.getRef());
                // Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                // Timber.v("nodeDataSnapshot.getKey(): " + mShoppingListsDataSnapshot.getKey());

                DataSnapshot mShoppingListItemsDataSnapshot = dataSnapshot.child(Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS).child(mKey);
                // Timber.v("mShoppingListItemsDataSnapshot: " + mShoppingListItemsDataSnapshot.getRef());
                // Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                // Timber.v("nodeDataSnapshot.getKey(): " + mShoppingListItemsDataSnapshot.getKey());

                HashMap<String, Object> result = new HashMap<>();

                /*
                 * Delete the list from the Owner
                 */
                result.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mListOwner + "/" + mShoppingListsDataSnapshot.getKey(), null); // delete the Shopping List

                /*
                 * Delete the list from any user that the list is shared with
                 */
                Iterator<User> userListsIterator = mSharedWith.values().iterator();
                for (int i = 0; i < mSharedWith.size(); i++) {
                    if (userListsIterator.hasNext()) {
                        User user = userListsIterator.next();
                        String email = user.getEmail();
                        DataSnapshot mUserSharedWith = dataSnapshot.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mKey);
                        result.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mUserSharedWith.getKey(), null); // delete the Shopping List
                    }
                }

                /*
                 * Delete the user from the sharedWith node, since the list no longer exists
                 */
                Iterator<User> sharedWithIterator = mSharedWith.values().iterator();
                for (int i = 0; i < mSharedWith.size(); i++) {
                    if (sharedWithIterator.hasNext()) {
                        User user = sharedWithIterator.next();
                        String email = user.getEmail();
                        DataSnapshot mUsersSharedWith = dataSnapshot.child(Constants.FIREBASE_LOCATION_LISTS_SHARED_WITH).child(mKey);
                        result.put("/" + Constants.FIREBASE_LOCATION_LISTS_SHARED_WITH + "/" + mUsersSharedWith.getKey(), null); // delete the Shopping List from sharedWith node
                    }
                }

                /*
                 * Remove all shopping list items that are associated with the shopping list being deleted
                 */
                result.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/" + mShoppingListItemsDataSnapshot.getKey(), null); // delete the Shopping List Items

                mFirebaseDatabaseReference.updateChildren(result, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Timber.v("%s%s", getString(R.string.log_error_updating_data), databaseError.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: %s", databaseError);
            }
        });

        // Go back to MainActivity.java
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.putExtra(Constants.KEY_ENCODED_EMAIL, mListOwner);
        startActivity(intent);
    }
}