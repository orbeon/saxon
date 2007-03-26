package org.orbeon.saxon.event;

/**
* A simple class for testing membership of a fixed set of case-insensitive ASCII strings.
* The class must be initialised with enough space for all the strings,
* it will go into an infinite loop if it fills. The string matching is case-blind,
* using an algorithm that works only for ASCII.
*
* The class implements part of the java.util.Set interface; it could be replaced with
* an implementation of java.util.Set together with a class that implemented a customized
* equals() method.
*/

public class HTMLTagHashSet {

    String[] strings;
    int size;

    public HTMLTagHashSet(int size) {
        strings = new String[size];
        this.size = size;
    }

    public void add(String s) {
        int hash = (hashCode(s) & 0x7fffffff) % size;
        while(true) {
            if (strings[hash]==null) {
                strings[hash] = s;
                return;
            }
            if (strings[hash].equalsIgnoreCase(s)) {
                return;
            }
            hash = (hash + 1) % size;
        }
    }

    public boolean contains(String s) {
        int hash = (hashCode(s) & 0x7fffffff) % size;
        while(true) {
            if (strings[hash]==null) {
                return false;
            }
            if (strings[hash].equalsIgnoreCase(s)) {
                return true;
            }
            hash = (hash + 1) % size;
        }
    }

    private int hashCode(String s) {
        // get a hashcode that doesn't depend on the case of characters.
        // This relies on the fact that char & 0xDF is case-blind in ASCII
        int hash = 0;
        int limit = s.length();
        if (limit>24) limit = 24;
        for (int i=0; i<limit; i++) {
            hash = (hash<<1) + (s.charAt(i) & 0xdf);
        }
        return hash;
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
// The Original Code is: all this file.
//
// The Initial Developer of this module is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//