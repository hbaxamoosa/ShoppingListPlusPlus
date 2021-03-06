package com.udacity.firebase.shoppinglistplusplus.ui.activeLists;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;

import java.util.ArrayList;
import java.util.Iterator;

import timber.log.Timber;

/*
 * Great reference on RecyclerView: https://medium.com/@harivigneshjayapalan/android-implementing-custom-recycler-view-part-i-9ce5e9af7fea
 * https://medium.com/@harivigneshjayapalan/android-recyclerview-implementing-single-item-click-and-long-press-part-ii-b43ef8cb6ad8
 */

/*
 * A simple {@link Fragment} subclass that shows a list of all shopping lists a user can see.
 * Use the {@link ShoppingListsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShoppingListsFragment extends Fragment {
    private String mEncodedEmail;
    private ActiveListAdapter mActiveListAdapter;
    private ArrayList<ShoppingList> mShoppingList = new ArrayList<>();
    private ArrayList<String> mListKeys = new ArrayList<>();
    private DatabaseReference mUserListsDatabaseReference;
    private DatabaseReference orderedList;
    private ValueEventListener orderedListListener;

    public ShoppingListsFragment() {
        /* Required empty public constructor */
    }

    /**
     * Create fragment and pass bundle with data as it's arguments
     * Right now there are not arguments...but eventually there will be.
     */
    public static ShoppingListsFragment newInstance(String encodedEmail) {
        ShoppingListsFragment fragment = new ShoppingListsFragment();
        Bundle args = new Bundle();
        args.putString(Constants.KEY_ENCODED_EMAIL, encodedEmail);
        fragment.setArguments(args);
        return fragment;
    }

    /*
     * Initialize instance variables with data from bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mEncodedEmail = getArguments().getString(Constants.KEY_ENCODED_EMAIL);
        }

        // Initialize Firebase components
        FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUserListsDatabaseReference = mFirebaseDatabase.getReference(Constants.FIREBASE_LOCATION_USER_LISTS).child(mEncodedEmail);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_shopping_lists, container, false);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.recyclerView);

        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(manager);

        try {
            mActiveListAdapter = new ActiveListAdapter(getActivity(), mShoppingList, mListKeys, mEncodedEmail);
            mRecyclerView.setAdapter(mActiveListAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mActiveListAdapter != null) {
            mActiveListAdapter.notifyDataSetChanged();
        }
        mRecyclerView.setAdapter(mActiveListAdapter);

        return rootView;
    }

    /*
     * Updates the order of mListView onResume to handle sortOrderChanges properly
     */
    @Override
    public void onResume() {
        super.onResume();
        // Timber.v("onResume()");

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortOrder = sharedPref.getString(Constants.KEY_PREF_SORT_ORDER_LISTS, Constants.ORDER_BY_KEY);
        // Timber.v("sortOrder: " + sortOrder);

        Query orderedList;

        /*
         * Sort active lists by "date created"
         * if it's been selected in the SettingsActivity
         */
        if (sortOrder.equals(Constants.ORDER_BY_KEY)) {
            orderedList = mUserListsDatabaseReference.orderByKey();
        } else {

            /*
             * Sort active by lists by name or datelastChanged. Otherwise
             * depending on what's been selected in SettingsActivity
             */

            orderedList = mUserListsDatabaseReference.orderByChild(sortOrder);
        }

        ValueEventListener orderedListListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Timber.v("ValueEventListener onDataChange(DataSnapshot dataSnapshot) " + dataSnapshot.getValue());
                // Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                mShoppingList.clear();
                mListKeys.clear();
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                    if (it.hasNext()) {
                        DataSnapshot listSnapshot = it.next();
                        // Timber.v("listSnapshot.getValue(): %s", listSnapshot.getValue());
                        ShoppingList shoppingList = listSnapshot.getValue(ShoppingList.class);
                        // Timber.v("listSnapshot.getKey(): %s", listSnapshot.getKey());
                        mListKeys.add(listSnapshot.getKey());
                        mShoppingList.add(shoppingList);
                        mActiveListAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: %s", databaseError.toString());
            }
        };

        orderedList.addValueEventListener(orderedListListener);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Timber.v("onStart()");
    }

    @Override
    public void onStop() {
        super.onStop();
        // Timber.v("onStop()");

        // Remove orderedListListener value event listener
        if (orderedListListener != null) {
            orderedList.removeEventListener(orderedListListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Timber.v("onPause()");

        /*
         * Cleanup the adapter when activity is paused.
         */
        mShoppingList.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Timber.v("onDestroy()");

        /*
         * Cleanup the adapter when activity is paused.
         */
        mShoppingList.clear();
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item selections.
     */
    public interface Callback {
        /**
         * Shopping List Callback for when an item has been selected.
         */
        void onItemSelected(int position);
    }
}