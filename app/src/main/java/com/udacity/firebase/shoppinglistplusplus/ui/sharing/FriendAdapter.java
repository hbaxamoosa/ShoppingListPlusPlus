package com.udacity.firebase.shoppinglistplusplus.ui.sharing;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.User;

import java.util.HashMap;
import java.util.List;

/**
 * Populates the list_view_friends_share inside ShareListActivity
 */
public class FriendAdapter extends ArrayAdapter<User> {
    private static final String LOG_TAG = FriendAdapter.class.getSimpleName();
    private ShoppingList mShoppingList;
    private String mListId;
    private HashMap<String, User> mSharedUsersList;


    public FriendAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull List<User> objects) {
        super(context, resource, textViewResourceId, objects);
    }
}
