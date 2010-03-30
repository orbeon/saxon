package org.orbeon.saxon.sort;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU cache, based on <code>LinkedHashMap</code>.
 * Synthesized and simplified from various published examples of the genre.
 * The methods are not synchronized.
 */
public class LRUCache {

    private LinkedHashMap map;

    /**
     * Creates a new LRU cache.
     *
     * @param cacheSize the maximum number of entries that will be kept in this cache.
     */
    public LRUCache(final int cacheSize) {
        map = new LinkedHashMap(cacheSize, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return cacheSize < size();
            }
        };
    }

    /**
     * Retrieves an entry from the cache.<br>
     * The retrieved entry becomes the most recently used entry.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value associated to this key, or null if no value with this key exists in the cache.
     */
    public Object get(Object key) {
        return map.get(key);
    }

    /**
     * Adds an entry to this cache.
     * If the cache is full, the LRU (least recently used) entry is dropped.
     *
     * @param key   the key with which the specified value is to be associated.
     * @param value a value to be associated with the specified key.
     */
    public void put(Object key, Object value) {
        map.put(key, value);
    }

    /**
     * Clear the cache
     */
    public void clear() {
        map.clear();
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

