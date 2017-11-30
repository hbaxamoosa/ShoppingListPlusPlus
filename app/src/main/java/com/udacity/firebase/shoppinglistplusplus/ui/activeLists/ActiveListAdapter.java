package com.udacity.firebase.shoppinglistplusplus.ui.activeLists;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails.ActiveListDetailsActivity;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Populates the list_view_active_lists inside ShoppingListsFragment
 */
public class ActiveListAdapter extends RecyclerView.Adapter<ActiveListAdapter.ViewHolder> {

    public static List<ShoppingList> shoppingList;
    static List<String> listKeys;
    private Context context;
    private String mEncodedEmail;

    ActiveListAdapter(Context c, List<ShoppingList> s, ArrayList<String> k, String encodedEmail) {
        context = c;
        shoppingList = s;
        listKeys = k;
        this.mEncodedEmail = encodedEmail;
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
    public void onBindViewHolder(final ActiveListAdapter.ViewHolder holder, int position) {

        /* Set the list name and owner */
        holder.ownerName.setText(shoppingList.get(position).getOwner());
        holder.listName.setText(shoppingList.get(position).getListName());

        /*
         * Show "1 person is shopping" if one person is shopping
         * Show "N people shopping" if two or more users are shopping
         * Show nothing if nobody is shopping
         */
        if (shoppingList.get(position).getUsersShopping() != null) {
            int usersShopping = shoppingList.get(position).getUsersShopping().size();
            if (usersShopping == 1) {
                holder.textViewUsersShopping.setText(String.format(
                        context.getResources().getString(R.string.person_shopping),
                        usersShopping));
            } else {
                holder.textViewUsersShopping.setText(String.format(
                        context.getResources().getString(R.string.people_shopping),
                        usersShopping));
            }
        } else {
            /* otherwise show nothing */
            holder.textViewUsersShopping.setText("");
        }

        /*
         * Set "Created by" text to "You" if current user is owner of the list
         * Set "Created by" text to userName if current user is NOT owner of the list
         */
        if (shoppingList.get(position).getOwner() != null) {
            if (shoppingList.get(position).getOwner().equals(mEncodedEmail)) {
                holder.textViewCreatedByUser.setText(context.getResources().getString(R.string.text_you));
            } else {

                // Initialize Firebase components
                FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
                DatabaseReference mUserListsDatabaseReference = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USER_LISTS);

                /* Save the most up-to-date version of current user in mCurrentUser */

                mUserListsDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Timber.v("dataSnapshot.getValue(): %s", dataSnapshot.getValue());
                        User user = dataSnapshot.getValue(User.class);

                        if (user != null) {
                            Timber.v("user.getName(): %s", user.getName());
                            holder.textViewCreatedByUser.setText(user.getName());
                            // TODO: 8/23/17 possible bug here? when viewing the shared list from a SharedWith user, the owner name flashes into and out of the textViewCreatedByUser
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Timber.v("error: %s", databaseError.toString());
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return shoppingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView listName, ownerName, textViewCreatedByUser, textViewUsersShopping;

        ViewHolder(View itemView) {
            super(itemView);

            listName = itemView.findViewById(R.id.text_view_list_name);
            ownerName = itemView.findViewById(R.id.created_by);
            textViewCreatedByUser = itemView.findViewById(R.id.text_view_created_by_user);
            textViewUsersShopping = itemView.findViewById(R.id.text_view_people_shopping_count);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            // Timber.v("class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener");
            int adapterPosition = getAdapterPosition();
            // Toast.makeText(getContext(), "adapterPosition: " + adapterPosition, Toast.LENGTH_LONG).show();

            /* Starts an Activity showing the details for the selected list */
            Intent intent = new Intent(v.getContext(), ActiveListDetailsActivity.class);
            intent.putExtra(Constants.KEY_LIST_ITEM_ID, adapterPosition);
            intent.putExtra(Constants.KEY_LIST_NAME, shoppingList.get(adapterPosition).getListName());
            intent.putExtra("listKey", listKeys.get(adapterPosition));
            v.getContext().startActivity(intent);
        }
    }
}
