package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.util.HashMap;
import java.util.Iterator;

import timber.log.Timber;

/**
 * Lets user edit the list name for all copies of the current list
 */
public class EditListNameDialogFragment extends EditListDialogFragment {
    // private static final String LOG_TAG = ActiveListDetailsActivity.class.getSimpleName();
    static String mListId;
    static String mOwner;
    static String mListKey;

    /**
     * Public static constructor that creates fragment and passes a bundle with data into it when adapter is created
     */
    public static EditListNameDialogFragment newInstance(ShoppingList shoppingList, String listId,
                                                         String listKey, String encodedEmail,
                                                         HashMap<String, User> sharedWithUsers) {
        EditListNameDialogFragment editListNameDialogFragment = new EditListNameDialogFragment();
        Bundle bundle = EditListDialogFragment.newInstanceHelper(shoppingList,
                R.layout.dialog_edit_list, listId, encodedEmail, sharedWithUsers);
        bundle.putString(Constants.KEY_LIST_NAME, shoppingList.getListName());
        editListNameDialogFragment.setArguments(bundle);
        mListId = listId;
        mListKey = listKey;
        mOwner = encodedEmail;
        return editListNameDialogFragment;
    }

    /**
     * Initialize instance variables with data from bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListId = getArguments().getString(Constants.KEY_LIST_NAME);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        /** {@link EditListDialogFragment#createDialogHelper(int)} is a
         * superclass method that creates the dialog
         **/
        Dialog dialog = super.createDialogHelper(R.string.positive_button_edit_item);
        /**
         * {@link EditListDialogFragment#helpSetDefaultValueEditText(String)} is a superclass
         * method that sets the default text of the TextView
         */
        helpSetDefaultValueEditText(mListId);
        return dialog;
    }

    /**
     * Changes the list name in all copies of the current list
     */
    protected void doListEdit() {
        final String inputListName = mEditTextForList.getText().toString();
        /**
         * Check that the user inputted list name is not empty, has changed the original name
         * and that the dialog was properly initialized with the current name and id of the list.
         */

        // Initialize Firebase components
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference mUserListsDatabaseReference = mFirebaseDatabase.getReference();

        Query query = mUserListsDatabaseReference.orderByChild(Constants.FIREBASE_PROPERTY_LIST_NAME).equalTo(mListId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("TimberArgCount")
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("dataSnapshot.getKey(): %s", dataSnapshot.getKey());

                final HashMap<String, Object> result = new HashMap<>();
                result.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mOwner + "/" + mListKey + "/" + Constants.FIREBASE_PROPERTY_LIST_NAME, inputListName);

                final HashMap<String, Object> timestampNowHash = new HashMap<>();
                timestampNowHash.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

                result.put("/" + mOwner + "/" + mListKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, timestampNowHash);

                Timber.v("mSharedWith: %s", mSharedWith.size());

                Iterator<User> it = mSharedWith.values().iterator();
                for (int i = 0; i < mSharedWith.size(); i++) {
                    if (it.hasNext()) {
                        User user = it.next();
                        String email = user.getEmail();
                        Timber.v("email: %s", email);
                        result.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mListKey + "/" + Constants.FIREBASE_PROPERTY_LIST_NAME, inputListName);
                        Timber.v("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mListKey + "/" + Constants.FIREBASE_PROPERTY_LIST_NAME, inputListName);
                        result.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mListKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, timestampNowHash);
                    }
                }

                Timber.v("result: %s", result.size());

                mUserListsDatabaseReference.updateChildren(result);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: %s", databaseError);
            }
        });
    }
}

