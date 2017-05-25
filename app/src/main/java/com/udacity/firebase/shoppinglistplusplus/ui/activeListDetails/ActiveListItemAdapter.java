package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingListItem;

import java.util.HashMap;
import java.util.List;

import timber.log.Timber;


/**
 * Populates list_view_shopping_list_items inside ActiveListDetailsActivity
 */
public class ActiveListItemAdapter extends RecyclerView.Adapter<ActiveListItemAdapter.ViewHolder> {

    public static String listKey;
    public static DatabaseReference mShoppingListItemsDatabaseReference;
    private static List<ShoppingListItem> shoppingListItems;
    private Context context;
    // Firebase Realtime Database
    private FirebaseDatabase mFirebaseDatabase;

    public ActiveListItemAdapter(Context c, List<ShoppingListItem> s) {
        context = c;
        shoppingListItems = s;

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mShoppingListItemsDatabaseReference = mFirebaseDatabase.getReference("shoppingListItems");
    }

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return context;
    }

    @Override
    public ActiveListItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_active_list_item, parent, false);

        return new ActiveListItemAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ActiveListItemAdapter.ViewHolder holder, int position) {
        Timber.v("onBindViewHolder(ActiveListItemAdapter.ViewHolder holder, int position)");
        Timber.v("position: " + position);
        Timber.v("shoppingListItems.get(position).getItemName(): " + shoppingListItems.get(position).getItemName());
        Timber.v("shoppingListItems.get(position).getBoughtBy(): " + shoppingListItems.get(position).getBoughtBy());

        holder.listName.setText(shoppingListItems.get(position).getItemName());
        holder.boughtBy.setText(shoppingListItems.get(position).getBoughtBy());
    }

    @Override
    public int getItemCount() {
        Timber.v("shoppingListItems.size(): " + shoppingListItems.size());
        return shoppingListItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView listName, boughtBy;
        ImageButton deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);

            listName = (TextView) itemView.findViewById(R.id.text_view_active_list_item_name);
            boughtBy = (TextView) itemView.findViewById(R.id.text_view_bought_by);
            deleteButton = (ImageButton) itemView.findViewById(R.id.button_remove_item);
            deleteButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Toast.makeText(v.getContext(), "delete clicked", Toast.LENGTH_LONG).show();
            Timber.v("ActiveListDetailsActivity.mListId: " + ActiveListDetailsActivity.mListKey);
            Timber.v("mShoppingListItemsDatabaseReference.getRef(): " + mShoppingListItemsDatabaseReference.getRef());

            Query one = mShoppingListItemsDatabaseReference.getRef().orderByKey();
            one.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                    DataSnapshot nodeDataSnapshot = dataSnapshot.getChildren().iterator().next();
                    Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                    Timber.v("nodeDataSnapshot.getKey(): " + nodeDataSnapshot.getKey());

                    HashMap<String, Object> result = new HashMap<>();
                    result.put("/" + dataSnapshot.getKey(), null);

                    mShoppingListItemsDatabaseReference.updateChildren(result, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Timber.v("Error: " + databaseError.getMessage());
                            }
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Timber.v("Error: " + databaseError);
                }
            });



            /*Query query = mShoppingListItemsDatabaseReference.child("-KksB-WMEyDHi5wp7ys1").child(shoppingListItems.get(getAdapterPosition()).getItemName());
            Timber.v("query.getRef()" + query.getRef());
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                    DataSnapshot nodeDataSnapshot = dataSnapshot.getChildren().iterator().next();
                    Timber.v("dataSnapshot.getKey(): " + dataSnapshot.getKey());
                    Timber.v("nodeDataSnapshot.getKey(): " + nodeDataSnapshot.getKey());

                    HashMap<String, Object> result = new HashMap<>();
                    result.put("/" + dataSnapshot.getKey(), null);

                    mShoppingListItemsDatabaseReference.updateChildren(result, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if (databaseError != null) {
                                Timber.v("Error: " + databaseError.getMessage());
                            }
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Timber.v("Error: " + databaseError);
                }
            });*/
        }
    }
}