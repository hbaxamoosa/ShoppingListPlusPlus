package com.udacity.firebase.shoppinglistplusplus.ui.activeLists;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;
import com.udacity.firebase.shoppinglistplusplus.utils.Constants;

import java.util.ArrayList;
import java.util.Iterator;

import timber.log.Timber;

/**
 * Great reference on RecyclerView: https://medium.com/@harivigneshjayapalan/android-implementing-custom-recycler-view-part-i-9ce5e9af7fea
 * https://medium.com/@harivigneshjayapalan/android-recyclerview-implementing-single-item-click-and-long-press-part-ii-b43ef8cb6ad8
 */

/**
 * A simple {@link Fragment} subclass that shows a list of all shopping lists a user can see.
 * Use the {@link ShoppingListsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ShoppingListsFragment extends Fragment {
    private String mEncodedEmail;
    private ActiveListAdapter mActiveListAdapter;
    private RecyclerView mRecyclerView;
    private ArrayList<ShoppingList> mShoppingList = new ArrayList<>();
    private ArrayList<String> mListKeys = new ArrayList<>();

    // Firebase Realtime Database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mShoppingListDatabaseReference;
    // private ChildEventListener mChildEventListener;
    private ValueEventListener valueEventListener;

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

    /**
     * Initialize instance variables with data from bundle
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mEncodedEmail = getArguments().getString(Constants.KEY_ENCODED_EMAIL);
        }

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mShoppingListDatabaseReference = mFirebaseDatabase.getReference("shoppingLists");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_shopping_lists, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);

        LinearLayoutManager manager = new LinearLayoutManager(getContext());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(manager);

        try {
            mActiveListAdapter = new ActiveListAdapter(getActivity(), mShoppingList, mListKeys);
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

    /**
     * Updates the order of mListView onResume to handle sortOrderChanges properly
     */
    @Override
    public void onResume() {
        super.onResume();

        Timber.v("onResume()");

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String sortOrder = sharedPref.getString(Constants.KEY_PREF_SORT_ORDER_LISTS, Constants.ORDER_BY_KEY);


        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.v("ValueEventListener onDataChange(DataSnapshot dataSnapshot) " + dataSnapshot.getValue());
                Timber.v("onChildAdded");
                Timber.v("dataSnapshot.getValue(): " + dataSnapshot.getValue());
                mShoppingList.clear();
                mListKeys.clear();
                Iterator<DataSnapshot> it = dataSnapshot.getChildren().iterator();
                for (int i = 0; i < dataSnapshot.getChildrenCount(); i++) {
                    if (it.hasNext()) {
                        DataSnapshot listSnapshot = it.next();
                        Timber.v("listSnapshot.getValue(): " + listSnapshot.getValue());
                        ShoppingList shoppingList = listSnapshot.getValue(ShoppingList.class);
                        Timber.v("listSnapshot.getKey(): " + listSnapshot.getKey());
                        mListKeys.add(listSnapshot.getKey());
                        mShoppingList.add(shoppingList);
                        mActiveListAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.v("Error: " + databaseError.toString());
            }
        };
        mShoppingListDatabaseReference.addValueEventListener(valueEventListener);
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

        if (valueEventListener != null) {
            mShoppingListDatabaseReference.removeEventListener(valueEventListener);
        }
    }

    /**
     * Cleanup the adapter when activity is paused.
     */
    @Override
    public void onPause() {
        super.onPause();
        // Timber.v("onPause()");

        mShoppingList.clear();

        if (valueEventListener != null) {
            mShoppingListDatabaseReference.removeEventListener(valueEventListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Timber.v("onDestroy()");

        mShoppingList.clear();

        if (valueEventListener != null) {
            mShoppingListDatabaseReference.removeEventListener(valueEventListener);
        }
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
