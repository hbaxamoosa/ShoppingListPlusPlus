package com.udacity.firebase.shoppinglistplusplus.ui.activeLists;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails.ActiveListDetailsActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Populates the list_view_active_lists inside ShoppingListsFragment
 */
public class ActiveListAdapter extends RecyclerView.Adapter<ActiveListAdapter.ViewHolder> {

    public static List<ShoppingList> shoppingList;
    public static List<String> listKeys;
    private Context context;

    public ActiveListAdapter(Context c, List<ShoppingList> s, ArrayList<String> k) {
        context = c;
        shoppingList = s;
        listKeys = k;
    }

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return context;
    }

    @Override
    public ActiveListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_active_list, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ActiveListAdapter.ViewHolder holder, int position) {

        holder.ownerName.setText(shoppingList.get(position).getOwner());
        holder.listName.setText(shoppingList.get(position).getListName());
    }

    @Override
    public int getItemCount() {
        return shoppingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView listName, ownerName;

        ViewHolder(View itemView) {
            super(itemView);

            listName = (TextView) itemView.findViewById(R.id.text_view_list_name);
            ownerName = (TextView) itemView.findViewById(R.id.text_view_created_by_user);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            // Timber.v("class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener");
            int adapterPosition = getAdapterPosition();
            // Toast.makeText(getContext(), "adapterPosition: " + adapterPosition, Toast.LENGTH_LONG).show();

            /* Starts an active showing the details for the selected list */
            Intent intent = new Intent(v.getContext(), ActiveListDetailsActivity.class);
            intent.putExtra(Constants.KEY_LIST_ITEM_ID, adapterPosition);
            intent.putExtra(Constants.KEY_LIST_NAME, shoppingList.get(adapterPosition).getListName());
            intent.putExtra("listKey", listKeys.get(adapterPosition));
            v.getContext().startActivity(intent);
        }
    }
}
