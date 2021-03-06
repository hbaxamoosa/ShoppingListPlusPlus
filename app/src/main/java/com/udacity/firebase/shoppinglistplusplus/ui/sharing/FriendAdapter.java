package com.udacity.firebase.shoppinglistplusplus.ui.sharing;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Populates the list_view_friends_share inside ShareListActivity
 */
public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
    private static ArrayList<User> mSharedUsersList;
    private static String mListId;
    private static ShoppingList mShoppingList;
    private static DatabaseReference mSharedFriendInShoppingListRef;
    private Context context;

    FriendAdapter(ArrayList<User> arrayList, String listId, String encodedEmail, DatabaseReference sharedFriendInShoppingListRef) {
        mSharedUsersList = arrayList;
        mListId = listId;
        mSharedFriendInShoppingListRef = sharedFriendInShoppingListRef;
        // Timber.v("mSharedFriendInShoppingListRef: %s", mSharedFriendInShoppingListRef.getRef());
    }

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return context;
    }

    @Override
    public FriendAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_user_item, parent, false);
        return new FriendAdapter.ViewHolder(v);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        Timber.v("onDetachedFromRecyclerView(RecyclerView recyclerView)");

    }

    @Override
    public void onBindViewHolder(final FriendAdapter.ViewHolder holder, final int position) {
        if (mSharedUsersList.get(position) != null) {
            holder.userName.setText(Utils.decodeEmail(mSharedUsersList.get(position).getEmail()));
            // holder.userName.setText("hasnain");
        } else {
            Timber.v("mSharedUsersList.get(" + position + ") is NULL");
        }
        /*
         * Find the specific node for the Friend
         */
        // Timber.v("position: %s", position);
        // Timber.v("mSharedUsersList.size(): %s", mSharedUsersList.size());
        // Timber.v("mSharedUsersList.get(position).getEmail(): %s", mSharedUsersList.get(position).getEmail());
        // Timber.v("mSharedUsersList.get(position).toString(): %s", mSharedUsersList.get(position).toString());
        DatabaseReference mFriendRef = mSharedFriendInShoppingListRef.child(mSharedUsersList.get(position).getEmail());
        // Timber.v("onBindViewHolder mFriendRef.getRef(): %s", mFriendRef.getRef());

        holder.userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.v("clicking on name");
                Toast.makeText(v.getContext(), "position: " + position, Toast.LENGTH_SHORT).show();
            }
        });


        /*
         * Gets the value of the friend from the ShoppingList's sharedWith list of users
         * and then allows the friend to be toggled as shared with or not.
         *
         * The friend in the snapshot (sharedFriendInShoppingList) will either be a User object
         * (if they are in the the sharedWith list) or null (if they are not in the sharedWith list)
         */

        if (mFriendRef != null) {
            // Timber.v("ViewHolder mFriendRef.getRef(): %s", mFriendRef.getRef());
            ValueEventListener listener = mFriendRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final User sharedFriendInShoppingList = dataSnapshot.getValue(User.class);

                    /*
                     * If list is already being shared with this friend, set the buttonToggleShare
                     * to remove selected friend from sharedWith onClick and change the
                     * buttonToggleShare image to green
                     */
                    if (sharedFriendInShoppingList != null) {
                        Timber.v("sharedFriendInShoppingList != null");
                        holder.buttonToggleShare.setImageResource(R.drawable.ic_shared_check);
                        holder.buttonToggleShare.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                // Timber.v("clicking on checkbox icon");
                                addFriend(false);
                            }
                        });
                    } else {

                        Timber.v("sharedFriendInShoppingList == null");
                        /*
                         * Set the buttonToggleShare onClickListener to add selected friend to sharedWith
                         * and change the buttonToggleShare image to grey otherwise
                         */
                        holder.buttonToggleShare.setImageResource(R.drawable.icon_add_friend);
                        holder.buttonToggleShare.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Timber.v("clicking on plus icon");
                                addFriend(true);
                            }
                        });
                    }
                }

                private void addFriend(boolean isFriend) {
                    // Timber.v("inside addFriend");

                    HashMap<String, Object> updatedUserData = new HashMap<String, Object>();
                    String propertyToUpdateUser = "/" + Constants.FIREBASE_LOCATION_LISTS_SHARED_WITH + "/" + mListId + "/" + mSharedUsersList.get(position).getEmail();
                    String propertyToUpdateList = "/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + mSharedUsersList.get(position).getEmail() + "/" + mListId;

                    if (isFriend) { // we are adding a user/friend to the list

                        // create hashmap of user that needs to be added to the sharedWith node
                        HashMap<String, Object> currentUser = (HashMap<String, Object>) new ObjectMapper().convertValue(mSharedUsersList.get(position), Map.class);
                        updatedUserData.put(propertyToUpdateUser, currentUser); // add the selected User to the hashMap

                        HashMap<String, Object> changedTimestampMap = new HashMap<>();
                        changedTimestampMap.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

                        // create hashmap of list that needs to be added to the userLists node
                        updatedUserData.put(propertyToUpdateList, mShoppingList); // add the current Shopping List to the sharedWith user's userLists

                        /* Do a deep-path update */
                        mSharedFriendInShoppingListRef.getRoot().updateChildren(updatedUserData, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Timber.v("%s %s", R.string.log_error_updating_data, databaseError.getMessage());
                                }
                            }
                        });

                    } else { // we are removing a user/friend from the list
                        Timber.v("isFriend: %s", isFriend);
                        // create hashmap of user that needs to be added to the sharedWith node
                        HashMap<String, Object> currentUser = (HashMap<String, Object>) new ObjectMapper().convertValue(mSharedUsersList.get(position), Map.class);
                        updatedUserData.put(propertyToUpdateUser, null); // set the User Shopping value to NULL

                        HashMap<String, Object> changedTimestampMap = new HashMap<>();
                        changedTimestampMap.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

                        // create hashmap of list that needs to be added to the userLists node
                        updatedUserData.put(propertyToUpdateList, null); // set the User Shopping value to current user

                        /* Do a deep-path update */
                        mSharedFriendInShoppingListRef.getRoot().updateChildren(updatedUserData, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Timber.v("%s %s", R.string.log_error_updating_data, databaseError.getMessage());
                                }
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Timber.v("Error: %s", databaseError);
                }
            });

            // Timber.v("listener.toString(): %s", listener.toString());
            ShareListActivity.setShareWithListener(listener);
        } else {
            Timber.v("ref is NULL");
        }
    }

    @Override
    public int getItemCount() {
        // Timber.v("mSharedUsersList.size(): %s", mSharedUsersList.size());
        return mSharedUsersList.size();
    }

    /**
     * Public method that is used to pass ShoppingList object when it is loaded in
     * ValueEventListener
     */
    public void setShoppingList(ShoppingList shoppingList) {
        mShoppingList = shoppingList;
        this.notifyDataSetChanged();
    }

    /**
     * Public method that is used to pass SharedUsers when they are loaded in ValueEventListener
     */
    void setSharedWithUsers(ArrayList<User> sharedUsersList) {
        mSharedUsersList = sharedUsersList;
        this.notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView userName;
        ImageButton buttonToggleShare;

        ViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            buttonToggleShare = itemView.findViewById(R.id.button_toggle_share);
        }
    }
}
