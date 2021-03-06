package com.udacity.firebase.shoppinglistplusplus.ui.activeListDetails;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingListItem;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;


/**
 * Populates list_view_shopping_list_items inside ActiveListDetailsActivity
 */
public class ActiveListItemAdapter extends RecyclerView.Adapter<ActiveListItemAdapter.ViewHolder> {

    private static List<ShoppingListItem> shoppingListItems;
    private static String mListKey;
    private static ShoppingList mShoppingList;
    private static HashMap<String, User> mSharedWithUsers;
    private Context mContex;
    private String mEncodedEmail;

    /*
     * Public constructor that initializes private instance variables when adapter is created
     */
    ActiveListItemAdapter(Context c, List<ShoppingListItem> s, String userEmail, String key) {
        mContex = c;
        shoppingListItems = s;
        mEncodedEmail = userEmail;
        mListKey = key;
    }

    /*
     * Public method that is used to pass shoppingList object when it is loaded in ValueEventListener
     */
    public void setShoppingList(ShoppingList shoppingList) {
        mShoppingList = shoppingList;
        // Timber.v("%s %s", "shoppingList.getListName(): ", shoppingList.getListName());
        // Timber.v("%s %s", "shoppingList.getOwner(): ", shoppingList.getOwner());
        this.notifyDataSetChanged();
    }

    public void setSharedWithUsers(HashMap<String, User> sharedWithUsers) {
        mSharedWithUsers = sharedWithUsers;
        // Timber.v("%s %s", "sharedWithUsers.size(): ", sharedWithUsers.size());
        this.notifyDataSetChanged();
    }

    @Override
    public ActiveListItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_active_list_item, parent, false);

        return new ActiveListItemAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ActiveListItemAdapter.ViewHolder holder, int position) {
        // Timber.v("onBindViewHolder(ActiveListItemAdapter.ViewHolder holder, int position)");
        // Timber.v("position: " + position);
        // Timber.v("shoppingListItems.get(position).getItemName(): " + shoppingListItems.get(position).getItemName());
        // Timber.v("shoppingListItems.get(position).getBoughtBy(): " + shoppingListItems.get(position).getBoughtBy());

        holder.textViewItemName.setText(shoppingListItems.get(position).getItemName());
        // holder.textViewBoughtBy.setText(shoppingListItems.get(position).getBoughtBy());

        /*
         * If selected item is bought
         * Set "Bought by" text to "You" if current user is owner of the list
         * Set "Bought by" text to userName if current user is NOT owner of the list
         * Set the remove item button invisible if current user is NOT list or item owner
         */
        if (shoppingListItems.get(position).isBought() && shoppingListItems.get(position).getBoughtBy() != null) {

            holder.textViewBoughtBy.setVisibility(View.VISIBLE);
            holder.textViewBoughtByUser.setVisibility(View.VISIBLE);

            /*
             * If you are the owner of the item or the owner of the list, then the remove icon
             * is visible.
             */
            if (shoppingListItems.get(position).getOwner().equals(mEncodedEmail) || (shoppingListItems != null && shoppingListItems.get(position).getOwner().equals(mEncodedEmail))) {
                holder.buttonRemoveItem.setVisibility(View.VISIBLE);
            } else {
                holder.buttonRemoveItem.setVisibility(View.INVISIBLE);
            }


            /* Add a strike-through */
            holder.textViewItemName.setPaintFlags(holder.textViewItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            if (shoppingListItems.get(position).getBoughtBy().equals(mEncodedEmail)) {
                holder.textViewBoughtByUser.setText(mContex.getString(R.string.text_you));
            } else {
                // Initialize Firebase components
                FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
                DatabaseReference mShoppingListDatabaseReference = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USERS);

                Query query = mShoppingListDatabaseReference.child(shoppingListItems.get(position).getOwner());
                // Timber.v("query.getRef(): %s", query.getRef());
                /* Get the item's owner's name; use a SingleValueEvent listener for memory efficiency */
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            holder.textViewBoughtByUser.setText(user.getName());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Timber.v("Error: %s", databaseError);
                    }
                });
            }
        } else {
            /*
             * If selected item is NOT bought
             * Set "Bought by" text to be empty and invisible
             * Set the remove item button visible if current user is owner of the list or selected item
             */

            /* Remove the strike-through */
            holder.textViewItemName.setPaintFlags(holder.textViewItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

            holder.textViewBoughtBy.setVisibility(View.INVISIBLE);
            holder.textViewBoughtByUser.setVisibility(View.INVISIBLE);
            holder.textViewBoughtByUser.setText("");
            holder.buttonRemoveItem.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        // Timber.v("shoppingListItems.size(): " + shoppingListItems.size());
        return shoppingListItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView textViewItemName, textViewBoughtBy, textViewBoughtByUser;
        ImageButton buttonRemoveItem;
        private Context context;

        ViewHolder(View itemView) {
            super(itemView);

            textViewItemName = itemView.findViewById(R.id.text_view_active_list_item_name);
            textViewBoughtBy = itemView.findViewById(R.id.text_view_bought_by);
            textViewBoughtByUser = itemView.findViewById(R.id.text_view_bought_by_user);
            buttonRemoveItem = itemView.findViewById(R.id.button_remove_item);
            buttonRemoveItem.setOnClickListener(this);
        }

        @Override
        public void onClick(final View v) {
            Toast.makeText(v.getContext(), "delete clicked", Toast.LENGTH_LONG).show();

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(v.getContext(), R.style.CustomTheme_Dialog)
                    .setTitle(v.getContext().getString(R.string.remove_item_option))
                    .setMessage(v.getContext().getString(R.string.dialog_message_are_you_sure_remove_item))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            removeItem(v);
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

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        private void removeItem(View v) {
            // Initialize Firebase components
            FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
            final DatabaseReference mShoppingListItemsDatabaseReference = mFirebaseDatabase.getReference();
            // Timber.v("ActiveListDetailsActivity.mKey " + ActiveListDetailsActivity.mKey);
            // Timber.v("mShoppingListItemsDatabaseReference.getRef(): " + mShoppingListItemsDatabaseReference.getRef());


            Query query = mShoppingListItemsDatabaseReference.child(Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS).child(mListKey);
            // Timber.v("%s %s", "query.getRef(): ", query.getRef());
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Timber.v("%s %s", "dataSnapshot.getValue(): ", dataSnapshot.getValue());
                    for (DataSnapshot listItems : dataSnapshot.getChildren()) {
                        // Timber.v("%s %s", "listItems.getKey(): ", listItems.getKey());
                        // Timber.v("%s %s", "listItems.getValue(): ", listItems.getValue());
                        // Timber.v("%s %s", "shoppingListItems.get(getAdapterPosition()).getItemName()): ", shoppingListItems.get(getAdapterPosition()).getItemName());
                        if (listItems.getKey().equals(shoppingListItems.get(getAdapterPosition()).getItemName())) {
                            // Timber.v("true");
                            HashMap<String, Object> result = new HashMap<>();
                            result.put("/" + Constants.FIREBASE_LOCATION_SHOPPING_LIST_ITEMS + "/" + mListKey + "/" + shoppingListItems.get(getAdapterPosition()).getItemName(), null);

                            /* Make the timestamp for last changed */
                            HashMap<String, Object> changedTimestampMap = new HashMap<>();
                            changedTimestampMap.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

                            /* Add the updated timestamp to the owner's list */
                            result.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mShoppingList.getOwner() + "/" + mListKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, changedTimestampMap);

                            /* Add the updated timestamp to the sharedWith user's lists */
                            Iterator<User> it = mSharedWithUsers.values().iterator();
                            for (int i = 0; i < mSharedWithUsers.size(); i++) {
                                if (it.hasNext()) {
                                    User user = it.next();
                                    String email = user.getEmail();
                                    result.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + Utils.encodeEmail(email) + "/" + mListKey + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, changedTimestampMap);
                                }
                            }

                            mShoppingListItemsDatabaseReference.updateChildren(result, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, final DatabaseReference databaseReference) {
                                    if (databaseError != null) {
                                        Timber.v("Error: %s", databaseError.getMessage());
                                    } else {
                                        // Timber.v("%s %s", "mShoppingList.getListName(): ", mShoppingList.getListName());
                                        // Timber.v("%s %s", "mShoppingList.getOwner(): ", mShoppingList.getOwner());
                                        // Timber.v("%s %s", "mSharedWithUsers.size(): ", mSharedWithUsers.size());

                                        /*
                                         * Set the reversed timestamp for the list owner
                                        */
                                        databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mShoppingList.getOwner()).child(mListKey).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {

                                                ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                                // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                                if (list != null) {
                                                    long timeReverse = -(list.getTimestampLastChangedLong());
                                                    String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                                            + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                                    // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mOwner).child(mListKey).child(timeReverseLocation).toString());
                                                    databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(mShoppingList.getOwner()).child(mListKey).child(timeReverseLocation).setValue(timeReverse);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Timber.v("%s %s", "Error updating data: ", databaseError.getMessage());
                                            }
                                        });

                                        /*
                                         * Set the reversed timestamp for the remaining user that the list is shared with
                                        */
                                        Iterator<User> it = mSharedWithUsers.values().iterator();
                                        for (int i = 0; i < mSharedWithUsers.size(); i++) {
                                            if (it.hasNext()) {
                                                User user = it.next();
                                                final String email = user.getEmail();
                                                databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListKey).addListenerForSingleValueEvent(new ValueEventListener() {

                                                    @Override
                                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                                        ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                                                        // Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                                                        if (list != null) {
                                                            long timeReverse = -(list.getTimestampLastChangedLong());
                                                            String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                                                    + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                                                            // Timber.v("path is %s", databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListKey).child(timeReverseLocation));
                                                            databaseReference.child(Constants.FIREBASE_LOCATION_USER_LISTS).child(Utils.encodeEmail(email)).child(mListKey).child(timeReverseLocation).setValue(timeReverse);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(DatabaseError databaseError) {
                                                        Timber.v("%s %s", "Error updating data: ", databaseError.getMessage());
                                                    }
                                                });
                                            } else {
                                                Timber.v("this list is not shared with anyone");
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Timber.v("Error: %s", databaseError);
                }
            });
        }
    }
}