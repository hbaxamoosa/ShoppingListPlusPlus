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

import java.util.List;

import timber.log.Timber;

/**
 * Populates the list_view_friends_autocomplete inside AddFriendActivity
 */
public class AutocompleteFriendAdapter extends RecyclerView.Adapter<AutocompleteFriendAdapter.ViewHolder> {
    private Context context;
    private List<User> userList;


    public AutocompleteFriendAdapter(List<User> users) {
        this.userList = users;
    }

    // Easy access to the context object in the recyclerview
    private Context getContext() {
        return context;
    }

    @Override
    public AutocompleteFriendAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_autocomplete_item, parent, false);

        return new AutocompleteFriendAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(AutocompleteFriendAdapter.ViewHolder holder, int position) {
        Timber.v("onBindViewHolder(AutocompleteFriendAdapter.ViewHolder holder, int position)");
        holder.userItem.setText(Utils.decodeEmail(userList.get(position).getEmail()));
    }

    @Override
    public int getItemCount() {
        if (userList != null) {
            return userList.size();
        } else {
            return 0;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView userItem;

        public ViewHolder(View itemView) {
            super(itemView);
            userItem = (TextView) itemView.findViewById(R.id.text_view_autocomplete_item);
            userItem.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Timber.v("class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener");
            int adapterPosition = getAdapterPosition();
            Toast.makeText(v.getContext(), "adapterPosition: " + adapterPosition, Toast.LENGTH_LONG).show();
        }
    }
}
