package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.ui.MainActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;

import java.util.HashMap;

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

    // Firebase Realtime Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mFirebaseDatabaseReference;

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

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseDatabaseReference = mFirebaseDatabase.getReference();
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
        // TODO: 6/14/17 update the delete action to point to the correct paths
        Query query = mFirebaseDatabaseReference.orderByKey();
        Timber.v("query.getRef(): " + query.getRef());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DataSnapshot mShoppingListsDataSnapshot = dataSnapshot.child("shoppingLists").child(mKey);
                Timber.v("mShoppingListsDataSnapshot.getRef(): " + mShoppingListsDataSnapshot.getRef());
                Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                Timber.v("nodeDataSnapshot.getKey(): " + mShoppingListsDataSnapshot.getKey());

                DataSnapshot mShoppingListItemsDataSnapshot = dataSnapshot.child("shoppingListItems").child(mKey);
                Timber.v("mShoppingListItemsDataSnapshot: " + mShoppingListItemsDataSnapshot.getRef());
                Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                Timber.v("nodeDataSnapshot.getKey(): " + mShoppingListItemsDataSnapshot.getKey());

                HashMap<String, Object> result = new HashMap<>();
                result.put("/" + "shoppingLists" + "/" + mShoppingListsDataSnapshot.getKey(), null); // delete the Shopping List
                result.put("/" + "shoppingListItems" + "/" + mShoppingListItemsDataSnapshot.getKey(), null); // delete the Shopping List Items

                mFirebaseDatabaseReference.updateChildren(result, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if (databaseError != null) {
                            Timber.v(getString(R.string.log_error_updating_data) + databaseError.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: " + databaseError);
            }
        });

        // Go back to MainActivity.java
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
    }

}
