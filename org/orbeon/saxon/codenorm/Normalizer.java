package org.orbeon.saxon.codenorm;

import org.orbeon.saxon.om.XMLChar;

/**
 * Implements Unicode Normalization Forms C, D, KC, KD.
 * Copyright (c) 1991-2005 Unicode, Inc.
 * For terms of use, see http://www.unicode.org/terms_of_use.html
 * For documentation, see UAX#15.<br>
 * The Unicode Consortium makes no expressed or implied warranty of any
 * kind, and assumes no liability for errors or omissions.
 * No liability is assumed for incidental and consequential damages
 * in connection with or arising out of the use of the information here.
 * @author Mark Davis
 * Updates for supplementary code points: Vladimir Weinstein & Markus Scherer
 * Modified to remove dependency on ICU code: Michael Kay
 */

public class Normalizer {

    /**
     * Create a normalizer for a given form.
     */
    public Normalizer(byte form) {
        this.form = form;
        if (data == null) {
            data = UnicodeDataParser.build(); // load 1st time
        }
    }

    /**
    * Masks for the form selector
    */
    static final byte
        COMPATIBILITY_MASK = 1,
        COMPOSITION_MASK = 2;

    /**
    * Normalization Form Selector
    */
    public static final byte
        D = 0 ,
        C = COMPOSITION_MASK,
        KD = COMPATIBILITY_MASK,
        KC = (byte)(COMPATIBILITY_MASK + COMPOSITION_MASK);

    /**
    * Normalizes text according to the chosen form,
    * replacing contents of the target buffer.
    * @param   source      the original text, unnormalized
    * @param   target      the resulting normalized text
    */
    public StringBuffer normalize(CharSequence source, StringBuffer target) {

        // First decompose the source into target,
        // then compose if the form requires.

        if (source.length() != 0) {
            internalDecompose(source, target);
            if ((form & COMPOSITION_MASK) != 0) {
                internalCompose(target);
            }
        }
        return target;
    }

    /**
    * Normalizes text according to the chosen form
    * @param   source      the original text, unnormalized
    * @return  target      the resulting normalized text
    */
    public CharSequence normalize(CharSequence source) {
        return normalize(source, new StringBuffer(source.length()+8));
    }

    // ======================================
    //                  PRIVATES
    // ======================================

    /**
     * The current form.
     */
    private byte form;

    /**
    * Decomposes text, either canonical or compatibility,
    * replacing contents of the target buffer.
//    * @param   form        the normalization form. If COMPATIBILITY_MASK
//    *                      bit is on in this byte, then selects the recursive
//    *                      compatibility decomposition, otherwise selects
//    *                      the recursive canonical decomposition.
    * @param   source      the original text, unnormalized
    * @param   target      the resulting normalized text
    */
    private void internalDecompose(CharSequence source, StringBuffer target) {
        StringBuffer buffer = new StringBuffer(8);
        boolean canonical = (form & COMPATIBILITY_MASK) == 0;
        int ch32;
        //for (int i = 0; i < source.length(); i += (ch32<65536 ? 1 : 2)) {
        for (int i = 0; i < source.length();) {
            buffer.setLength(0);
            //ch32 = UTF16.charAt(source, i);
            ch32 = source.charAt(i++);
            if (XMLChar.isHighSurrogate(ch32)) {
                char low = source.charAt(i++);
                ch32 = XMLChar.supplemental((char)ch32, low);
            }
            data.getRecursiveDecomposition(canonical, ch32, buffer);

            // add all of the characters in the decomposition.
            // (may be just the original character, if there was
            // no decomposition mapping)

            int ch;
            //for (int j = 0; j < buffer.length(); j += (ch<65536 ? 1 : 2)) {
            for (int j = 0; j < buffer.length();) {
                //ch = UTF16.charAt(buffer, j);
                ch = buffer.charAt(j++);
                if (XMLChar.isHighSurrogate(ch32)) {
                    char low = buffer.charAt(j++);
                    ch = XMLChar.supplemental((char)ch, low);
                }
                int chClass = data.getCanonicalClass(ch);
                int k = target.length(); // insertion point
                if (chClass != 0) {

                    // bubble-sort combining marks as necessary

                    int ch2;
                    while (k > 0) {
                        ch2 = target.charAt(k-1);
                        if (XMLChar.isSurrogate(ch2)) {
                            k--;
                            char high = buffer.charAt(k-1);
                            ch2 = XMLChar.supplemental(high, (char)ch2);
                        }
                        if (data.getCanonicalClass(ch2) <= chClass) break;
                        k--;
                    }
//                    for (; k > 0; k -= (ch2<65536 ? 1 : 2)) {
//                        ch2 = UTF16.charAt(target, k-1);
//                        if (data.getCanonicalClass(ch2) <= chClass) break;
//                    }
                }
                if (ch < 65536) {
                    target.insert(k, (char)ch);
                } else {
                    String s = "" + XMLChar.highSurrogate(ch) + XMLChar.lowSurrogate(ch);
                    target.insert(k, s);
                }
                //target.insert(k, UTF16.valueOf(ch));
            }
        }
    }

    /**
    * Composes text in place. Target must already
    * have been decomposed.
    * @param   target      input: decomposed text.
    *                      output: the resulting normalized text.
    */
    private void internalCompose(StringBuffer target) {

        int starterPos = 0;
        //int starterCh = UTF16.charAt(target,0);
        //int compPos = (starterCh<65536 ? 1 : 2); // length of last composition
        int starterCh = target.charAt(0);
        int compPos = 1;
        if (XMLChar.isHighSurrogate(starterCh)) {
            starterCh = XMLChar.supplemental((char)starterCh, target.charAt(1));
            compPos++;
        }
        int lastClass = data.getCanonicalClass(starterCh);
        if (lastClass != 0) lastClass = 256; // fix for strings staring with a combining mark
        int oldLen = target.length();

        // Loop on the decomposed characters, combining where possible

        int ch;
        //for (int decompPos = compPos; decompPos < target.length(); decompPos += (ch<65536 ? 1 : 2)) {
        for (int decompPos = compPos; decompPos < target.length();) {
            ch = target.charAt(decompPos++);
            if (XMLChar.isHighSurrogate(ch)) {
                ch = XMLChar.supplemental((char)ch, target.charAt(decompPos++));
            }
            //ch = UTF16.charAt(target, decompPos);
            int chClass = data.getCanonicalClass(ch);
            int composite = data.getPairwiseComposition(starterCh, ch);
            if (composite != NormalizerData.NOT_COMPOSITE && (lastClass < chClass || lastClass == 0)) {
                setCharAt(target, starterPos, composite);
                // we know that we will only be replacing non-supplementaries by non-supplementaries
                // so we don't have to adjust the decompPos
                starterCh = composite;
            } else {
                if (chClass == 0) {
                    starterPos = compPos;
                    starterCh  = ch;
                }
                lastClass = chClass;
                setCharAt(target, compPos, ch);
                if (target.length() != oldLen) { // MAY HAVE TO ADJUST!
                    decompPos += target.length() - oldLen;
                    oldLen = target.length();
                }
                compPos += (ch<65536 ? 1 : 2);
            }
        }
        target.setLength(compPos);
    }

    /**
     * Set the 32-bit character at a particular 16-bit offset in a string buffer,
     * replacing the previous character at that position, and taking account of the
     * fact that either, both, or neither of the characters might be a surrogate pair.
     */

    private static void setCharAt(StringBuffer target, int offset, int ch32) {
        if (ch32 < 65536) {
            if (XMLChar.isHighSurrogate(target.charAt(offset))) {
                target.setCharAt(offset, (char)ch32);
                target.deleteCharAt(offset+1);
            } else {
                target.setCharAt(offset, (char)ch32);
            }
        } else {
            if (XMLChar.isHighSurrogate(target.charAt(offset))) {
                target.setCharAt(offset, XMLChar.highSurrogate(ch32));
                target.setCharAt(offset+1, XMLChar.lowSurrogate(ch32));
            } else {
                target.setCharAt(offset, XMLChar.highSurrogate(ch32));
                target.insert(offset+1, XMLChar.lowSurrogate(ch32));
            }
        }
    }

    /**
    * Contains normalization data from the Unicode Character Database.
    * use false for the minimal set, true for the real set.
    */
    private static NormalizerData data = null;

    /**
    * Just accessible for testing.
    */
    boolean getExcluded (char ch) {
        return data.getExcluded(ch);
    }

    /**
    * Just accessible for testing.
    */
    String getRawDecompositionMapping (char ch) {
        return data.getRawDecompositionMapping(ch);
    }
}