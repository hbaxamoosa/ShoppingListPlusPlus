package com.udacity.firebase.shoppinglistplusplus.ui.sharing;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.util.HashMap;

import timber.log.Timber;

/**
 * Populates the list_view_friends_share inside ShareListActivity
 */
public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
    // private String mListId;
    private HashMap<String, User> mSharedUsersList;
    private Context context;
    private String mEncodedEmail;

    public FriendAdapter(HashMap<String, User> map, String encodedEmail) {
        mSharedUsersList = map;
        this.mEncodedEmail = encodedEmail;
    }

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return context;
    }

    @Override
    public FriendAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_user_item, parent, false);

        return new FriendAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(FriendAdapter.ViewHolder holder, int position) {
        holder.userName.setText(Utils.decodeEmail(mSharedUsersList.values().iterator().next().getEmail()));
    }

    @Override
    public int getItemCount() {
        return mSharedUsersList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView userName;

        public ViewHolder(View itemView) {
            super(itemView);
            userName = (TextView) itemView.findViewById(R.id.user_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Timber.v("onClick(View v)");
            int adapterPosition = getAdapterPosition();
            Toast.makeText(v.getContext(), "adapterPosition: " + adapterPosition, Toast.LENGTH_SHORT).show();
        }
    }
}
