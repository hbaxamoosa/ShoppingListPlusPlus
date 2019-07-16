package com.udacity.firebase.shoppinglistplusplus.ui.sharing;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.User;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;
import com.udacity.firebase.shoppinglistplusplus.utils.Utils;

import java.util.List;

import timber.log.Timber;

/**
 * Populates the list_view_friends_autocomplete inside AddFriendActivity
 */
public class AutocompleteFriendAdapter extends RecyclerView.Adapter<AutocompleteFriendAdapter.ViewHolder> {
    private static List<User> userList;
    private static String mEncodedEmail;

    AutocompleteFriendAdapter(List<User> users, Context c) {
        userList = users;
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        mEncodedEmail = sharedPref.getString(Constants.KEY_ENCODED_EMAIL, null);
        // Timber.v("mEncodedEmail: " + mEncodedEmail);
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

        ViewHolder(View itemView) {
            super(itemView);
            userItem = itemView.findViewById(R.id.text_view_autocomplete_item);
            userItem.setOnClickListener(this);
        }

        @Override
        public void onClick(final View v) {
            // Timber.v("class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener");
            final int adapterPosition = getAdapterPosition();
            // Toast.makeText(v.getContext(), "adapterPosition: " + adapterPosition, Toast.LENGTH_SHORT).show();

            /*
             * If selected user is not current user proceed
             */
            if (isNotCurrentUser(userList.get(adapterPosition))) {

                /*
                 * Create Firebase references
                 */

                FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
                final DatabaseReference mUserFriendsReference = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USER_FRIENDS)
                        .child(mEncodedEmail).child(userList.get(adapterPosition).getEmail());

                // Timber.v("mUserFriendsReference.getRef(): %s", mUserFriendsReference.getRef());
                /*
                 * Add listener for single value event to perform a one time operation
                 */
                mUserFriendsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        // Timber.v("dataSnapshot.getValue(): %s", dataSnapshot.getValue());
                        // Timber.v("userList.get(adapterPosition).getEmail(): %s", userList.get(adapterPosition).getEmail());

                        /*
                         * Add selected user to current user's friends if not in friends yet
                         */
                        if (isNotAlreadyAdded(dataSnapshot, userList.get(adapterPosition))) {
                            mUserFriendsReference.setValue(userList.get(adapterPosition));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Timber.v("%s%s", itemView.getContext().getString(R.string.log_error_the_read_failed), databaseError.getMessage());
                    }
                });

            }
        }

        private boolean isNotCurrentUser(User user) {
            if (user.getEmail().equals(mEncodedEmail)) {
            /* Toast appropriate error message if the user is trying to add themselves  */
                Toast.makeText(itemView.getContext(),
                        itemView.getContext().getResources().getString(R.string.toast_you_cant_add_yourself),
                        Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        private boolean isNotAlreadyAdded(DataSnapshot dataSnapshot, User user) {
            if (dataSnapshot.getValue(User.class) != null) {
                /* Toast appropriate error message if the user is already a friend of the user */
                String friendError = String.format(itemView.getContext().getResources().
                                getString(R.string.toast_is_already_your_friend),
                        user.getName());

                Toast.makeText(itemView.getContext(),
                        friendError,
                        Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }
    }
}
