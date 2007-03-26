package org.orbeon.saxon.codenorm;

import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.sort.IntToIntHashMap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.StringTokenizer;

/**
 * This class reads the data compiled into class UnicodeData, and builds hash tables
 * that can be used by the Unicode normalization routines. This operation is performed
 * once only, the first time normalization is attempted after Saxon is loaded.
 */

class UnicodeDataParser {

    // This class is never instantiated
    private UnicodeDataParser(){}

    /**
     * Called exactly once by NormalizerData to build the static data
     */

    static NormalizerData build() {
        IntToIntHashMap canonicalClass = new IntToIntHashMap(400);
        canonicalClass.setDefaultValue(0);
        IntHashMap decompose = new IntHashMap(18000);
        IntToIntHashMap compose = new IntToIntHashMap(15000);
        compose.setDefaultValue(NormalizerData.NOT_COMPOSITE);
        BitSet isCompatibility = new BitSet(128000);
        BitSet isExcluded = new BitSet(128000);

        readExclusionList(isExcluded);
        readCompatibilityList(isCompatibility);
        readCanonicalClassTable(canonicalClass);
        readDecompositionTable(decompose, compose, isExcluded, isCompatibility);

        return new NormalizerData(canonicalClass, decompose, compose,
              isCompatibility, isExcluded);
    }

    /**
     * Reads exclusion list and stores the data
     */

    private static void readExclusionList(BitSet isExcluded) {
        for (int i=0; i<UnicodeData.exclusionList.length; i++) {
            String s = UnicodeData.exclusionList[i];
            StringTokenizer st = new StringTokenizer(s, ",");
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                int value = Integer.parseInt(tok, 32);
                isExcluded.set(value);
            }
        }
    }

    /**
     * Reads exclusion list and stores the data
     */

    private static void readCompatibilityList(BitSet isCompatible) {
        for (int i=0; i<UnicodeData.compatibilityList.length; i++) {
            String s = UnicodeData.compatibilityList[i];
            StringTokenizer st = new StringTokenizer(s, ",");
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                int value = Integer.parseInt(tok, 32);
                isCompatible.set(value);
            }
        }
    }

    /**
     * Read canonical class table (mapping from character codes to their canonical class)
     */

    private static void readCanonicalClassTable(IntToIntHashMap canonicalClasses) {
        ArrayList keys = new ArrayList(5000);
        for (int i=0; i<UnicodeData.canonicalClassKeys.length; i++) {
            String s = UnicodeData.canonicalClassKeys[i];
            StringTokenizer st = new StringTokenizer(s, ",");
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                int value = Integer.parseInt(tok, 32);
                keys.add(new Integer(value));
            }
        }
        int k = 0;
        for (int i=0; i<UnicodeData.canonicalClassValues.length; i++) {
            String s = UnicodeData.canonicalClassValues[i];
            StringTokenizer st = new StringTokenizer(s, ",");
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                int clss = Integer.parseInt(tok, 32);
                canonicalClasses.put(((Integer)keys.get(k++)).intValue(), clss);
            }
        }
    }

    /**
     * Read canonical class table (mapping from character codes to their canonical class)
     */

    private static void readDecompositionTable(IntHashMap decompose, IntToIntHashMap compose,
                                               BitSet isExcluded, BitSet isCompatibility) {
        int k = 0;
        for (int i=0; i<UnicodeData.decompositionKeys.length; i++) {
            String s = UnicodeData.decompositionKeys[i];
            StringTokenizer st = new StringTokenizer(s, ",");
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                int key = Integer.parseInt(tok, 32);
                String value = UnicodeData.decompositionValues[k++];
                decompose.put(key, value);
                                // only compositions are canonical pairs
                // skip if script exclusion

                if (!isCompatibility.get(key) && !isExcluded.get(key)) {
                    char first = '\u0000';
                    char second = value.charAt(0);
                    if (value.length() > 1) {
                        first = second;
                        second = value.charAt(1);
                    }

                    // store composition pair in single integer

                    int pair = (first << 16) | second;
                    compose.put(pair, key);
                }
            }
        }

        // Add algorithmic Hangul decompositions
        // This fragment code is copied from the normalization code published by Unicode consortium.
        // See module org.orbeon.saxon.codenorm.Normalizer for applicable copyright information.

        for (int SIndex = 0; SIndex < SCount; ++SIndex) {
            int TIndex = SIndex % TCount;
            char first, second;
            if (TIndex != 0) { // triple
                first = (char)(SBase + SIndex - TIndex);
                second = (char)(TBase + TIndex);
            } else {
                first = (char)(LBase + SIndex / NCount);
                second = (char)(VBase + (SIndex % NCount) / TCount);
            }
            int pair = (first << 16) | second;
            int key = SIndex + SBase;
            decompose.put(key, String.valueOf(first) + second);
            compose.put(pair, key);
        }
    }

    /**
     * Hangul composition constants
     */
    private static final int
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount;   // 11172

    // end of Unicode consortium code

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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// The code for generating Hangul decompositions is Copyright (C) Unicode, Inc. All Rights Reserved.
// See statement below.
//
// Contributor(s): none.
//

// * Copyright (c) 1991-2005 Unicode, Inc.
// * For terms of use, see http://www.unicode.org/terms_of_use.html
// * For documentation, see UAX#15.<br>
// * The Unicode Consortium makes no expressed or implied warranty of any
// * kind, and assumes no liability for errors or omissions.
// * No liability is assumed for incidental and consequential damages
// * in connection with or arising out of the use of the information here.