package com.udacity.firebase.shoppinglistplusplus.ui.activeLists;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails.ActiveListDetailsActivity;

import java.util.List;

import timber.log.Timber;

/**
 * Populates the list_view_active_lists inside ShoppingListsFragment
 */
public class ActiveListAdapter extends RecyclerView.Adapter<ActiveListAdapter.ViewHolder> {

    private Context context;
    private List<ShoppingList> shoppingList;

    public ActiveListAdapter(Context c, List<ShoppingList> s) {
        Timber.v("inside constructor");
        context = c;
        shoppingList = s;
        Timber.v("shoppingList size is " + shoppingList.size());
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
        Timber.v("inside onBindViewHolder(ActiveListAdapter.ViewHolder holder, int position)");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element

        holder.ownerName.setText(shoppingList.get(position).getOwner());
        holder.listName.setText(shoppingList.get(position).getListName());

        Timber.v("position: " + position);
        Timber.v("shoppingList.get(position).getOwner(): " + shoppingList.get(position).getOwner());
        Timber.v("shoppingList.get(position).getListName(): " + shoppingList.get(position).getListName());
    }

    @Override
    public int getItemCount() {
        return shoppingList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

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
            Toast.makeText(getContext(), "adapterPosition: " + adapterPosition, Toast.LENGTH_LONG).show();

            /* Starts an active showing the details for the selected list */
            Intent intent = new Intent(v.getContext(), ActiveListDetailsActivity.class);
            intent.putExtra("position", adapterPosition);
            intent.putExtra("listName", shoppingList.get(adapterPosition).getListName());
            v.getContext().startActivity(intent);
        }
    }
}
