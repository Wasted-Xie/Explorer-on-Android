// SearchResultItem.aidl
package com.explorercore.plugin;

/**
 * Represents a single search result item that a plugin can return.
 */
parcelable SearchResultItem {
    String id;           // Unique ID for the result
    String title;        // Main title text
    String subtitle;     // Subtitle text (optional)
    String iconUri;      // URI to icon (content:// or file:// or android.resource://)
    String intentAction; // Intent action to perform when clicked (optional)
    String intentData;   /* Intent data URI (optional) */
    Bundle extras;       // Extra data (optional)
}