package com.udacity.firebase.shoppinglistplusplus.ui.sharing;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import com.udacity.firebase.shoppinglistplusplus.model.User;

import java.util.List;

/**
 * Populates the list_view_friends_autocomplete inside AddFriendActivity
 */
public class AutocompleteFriendAdapter extends ArrayAdapter<User> {
    private String mEncodedEmail;

    public AutocompleteFriendAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull List<User> objects) {
        super(context, resource, textViewResourceId, objects);
    }
}
