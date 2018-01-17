package com.udacity.firebase.shoppinglistplusplus.utils;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.udacity.firebase.shoppinglistplusplus.model.ShoppingList;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import timber.log.Timber;

/*
 * Utility class
 */
public class Utils {
    /*
     * Format the timestamp with SimpleDateFormat
     */
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private Context mContext = null;


    /*
     * Public constructor that takes mContext for later use
     */
    public Utils(Context con) {
        mContext = con;
    }

    /*
     * Return true if currentUserEmail equals to shoppingList.owner()
     * Return false otherwise
     */
    public static boolean checkIfOwner(ShoppingList shoppingList, String currentUserEmail) {
        // Timber.v("shoppingList.getOwner(): " + shoppingList.getOwner() + " & " + "currentUserEmail: " + currentUserEmail);
        return (shoppingList.getOwner() != null &&
                shoppingList.getOwner().equals(currentUserEmail));
    }

    /*
     * Encode user email to use it as a Firebase key (Firebase does not allow "." in the key name)
     * Encoded email is also used as "userEmail", list and item "owner" value
     */
    public static String encodeEmail(String userEmail) {
        return userEmail.replace(".", ",");
    }

    /*
     * Email is being decoded just once to display real email in AutocompleteFriendAdapter
     *
     * @see com.udacity.firebase.shoppinglistplusplus.ui.sharing.AutocompleteFriendAdapter
     */
    public static String decodeEmail(String userEmail) {
        return userEmail.replace(",", ".");
    }

    /*
     * Adds values to a pre-existing HashMap for updating a property for all of the ShoppingList
     * copies. The HashMap can then be used with to update the property for all ShoppingList
     * copies.
     *
     * @param listId           The id of the shopping list.
     * @param owner            The owner of the shopping list.
     * @param mapToUpdate      The map containing the key, value pairs which will be used to update
     *                         the Firebase database. This MUST be a Hashmap of key value pairs
     *                         who's urls are absolute (i.e. from the root node)
     * @param propertyToUpdate The property to update
     * @param valueToUpdate    The value to update
     * @return The updated HashMap with the new value inserted in all lists
     */
    public static HashMap<String, Object> updateMapForAllWithValue
    (final String listId,
     final String owner, HashMap<String, Object> mapToUpdate,
     String propertyToUpdate, Object valueToUpdate) {

        mapToUpdate.put("/" + Constants.FIREBASE_LOCATION_USER_LISTS + "/" + owner + "/"
                + listId + "/" + propertyToUpdate, valueToUpdate);

        Timber.v("mapToUpdate.toString(): %s", mapToUpdate.toString());

        return mapToUpdate;
    }

    /*
     * Adds values to a pre-existing HashMap for updating all Last Changed Timestamps for all of
     * the ShoppingList copies. This method uses {@link #updateMapForAllWithValue} to update the
     * last changed timestamp for all ShoppingList copies.
     *
     * @param listId               The id of the shopping list.
     * @param owner                The owner of the shopping list.
     * @param mapToAddDateToUpdate The map containing the key, value pairs which will be used to
     *                             update the Firebase database. This MUST be a Hashmap of key value
     *                             pairs who's urls are absolute (i.e. from the root node)
     */
    public static HashMap<String, Object> updateMapWithTimestampLastChanged
    (final String listId, final String owner, HashMap<String, Object> mapToAddDateToUpdate) {
        /*
          Set raw version of date to the ServerValue.TIMESTAMP value and save into dateCreatedMap
         */
        HashMap<String, Object> timestampNowHash = new HashMap<>();
        timestampNowHash.put(Constants.FIREBASE_PROPERTY_TIMESTAMP, ServerValue.TIMESTAMP);

        updateMapForAllWithValue(listId, owner, mapToAddDateToUpdate,
                Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED, timestampNowHash);

        return mapToAddDateToUpdate;
    }

    /*
     * Once an update is made to a ShoppingList, this method is responsible for updating the
     * reversed timestamp to be equal to the negation of the current timestamp. This comes after
     * the updateMapWithTimestampChanged because ServerValue.TIMESTAMP must be resolved to a long
     * value.
     */
    public static void updateTimestampReversed(DatabaseError databaseError, final DatabaseReference databaseReference) {
        if (databaseError != null) {
            Timber.v("%s %s", "Error updating data: ", databaseError.getMessage());
        } else {
            Timber.v("detabaseReserence is " + databaseReference.toString());
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    ShoppingList list = dataSnapshot.getValue(ShoppingList.class);
                    Timber.v("list.getTimestampLastChangedLong(): %s", list.getTimestampLastChangedLong());
                    if (list != null) {
                        long timeReverse = -(list.getTimestampLastChangedLong());
                        String timeReverseLocation = Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE
                                + "/" + Constants.FIREBASE_PROPERTY_TIMESTAMP;

                        Timber.v("path is " + databaseReference.toString() + "/" + timeReverseLocation);
                        databaseReference.child(Constants.FIREBASE_PROPERTY_TIMESTAMP_LAST_CHANGED_REVERSE)
                                .child(Constants.FIREBASE_PROPERTY_TIMESTAMP).setValue(timeReverse);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Timber.v("%s %s", "Error updating data: ", databaseError.getMessage());
                }
            });
        }
    }
}
