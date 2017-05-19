package com.udacity.firebase.shoppinglistplusplus.ui.sharing;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;

import com.udacity.firebase.shoppinglistplusplus.R;
import com.udacity.firebase.shoppinglistplusplus.ui.BaseActivity;

/**
 * Represents the Add Friend screen and functionality
 */
public class AddFriendActivity extends BaseActivity {
    private EditText mEditTextAddFriendEmail;
    private AutocompleteFriendAdapter mFriendsAutocompleteAdapter;
    private String mInput;
    private ListView mListViewAutocomplete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);
        /**
         * Create Firebase references
         */


        /**
         * Link layout elements from XML and setup the toolbar
         */
        initializeScreen();

        /**
         * Set interactive bits, such as click events/adapters
         */

        mEditTextAddFriendEmail.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
                /* Get the input after every textChanged event and transform it to lowercase */
            mInput = mEditTextAddFriendEmail.getText().toString().toLowerCase();

            /* Clean up the old adapter */
            // if (mFriendsAutocompleteAdapter != null) mFriendsAutocompleteAdapter.cleanup();
            /* Nullify the adapter data if the input length is less than 2 characters */
            if (mInput.equals("") || mInput.length() < 2) {
                mListViewAutocomplete.setAdapter(null);

            /* Define and set the adapter otherwise. */
            } else {

            }

        }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Link layout elements from XML and setup the toolbar
     */
    public void initializeScreen() {
        mListViewAutocomplete = (ListView) findViewById(R.id.list_view_friends_autocomplete);
        mEditTextAddFriendEmail = (EditText) findViewById(R.id.edit_text_add_friend_email);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        /* Add back button to the action bar */
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
}
