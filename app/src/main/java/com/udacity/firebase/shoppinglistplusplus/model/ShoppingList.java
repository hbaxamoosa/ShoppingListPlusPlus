package com.udacity.firebase.shoppinglistplusplus.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the data structure for both Active and Archived ShoppingList objects.
 */

public class ShoppingList {
    private String listName;
    private String owner;

    /**
     * Required public constructor
     */
    public ShoppingList() {
    }

    /**
     * Use this constructor to create new ShoppingLists.
     * Takes shopping list listName and owner. Set's the last
     * changed time to what is stored in ServerValue.TIMESTAMP
     *
     * @param listName
     * @param owner
     */
    public ShoppingList(String listName, String owner) {
        this.listName = listName;
        this.owner = owner;
    }

    public String getListName() {
        return listName;
    }

    public String getOwner() {
        return owner;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("listName", listName);
        result.put("owner", owner);

        return result;
    }
}

