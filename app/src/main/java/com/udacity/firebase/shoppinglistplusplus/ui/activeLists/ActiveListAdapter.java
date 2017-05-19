package com.udacity.firebase.shoppinglistplusplus.ui.activeLists;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;

import java.util.List;

/**
 * Populates the list_view_active_lists inside ShoppingListsFragment
 */
public class ActiveListAdapter extends ArrayAdapter<ShoppingList> {
    private String mEncodedEmail;

    public ActiveListAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull List<ShoppingList> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    /**
     * Protected method that populates the view attached to the adapter (list_view_active_lists)
     * with items inflated from single_active_list.xml
     * populateView also handles data changes and updates the listView accordingly
     */

}
