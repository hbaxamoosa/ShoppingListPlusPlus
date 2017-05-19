package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.udacity.firebase.shoppinglistplusplus.R;


/**
 * Populates list_view_shopping_list_items inside ActiveListDetailsActivity
 */
public class ActiveListItemAdapter extends RecyclerView.Adapter<ActiveListItemAdapter.ActiveListItemAdapterViewHolder> {

    @Override
    public ActiveListItemAdapter.ActiveListItemAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View shoppingListView = inflater.inflate(R.layout.fragment_shopping_lists, parent, false);

        // Return a new holder instance
        ActiveListItemAdapterViewHolder viewHolder = new ActiveListItemAdapterViewHolder(shoppingListView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ActiveListItemAdapter.ActiveListItemAdapterViewHolder holder, int position) {


    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public static class ActiveListItemAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ActiveListItemAdapterViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void onClick(View v) {

        }
    }
}