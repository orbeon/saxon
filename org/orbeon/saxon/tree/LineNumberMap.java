package org.orbeon.saxon.tree;

/**
  * Line numbers are not held in nodes in the tree, because they are not usually needed.
  * This class provides a map from element sequence numbers to line numbers: it is
  * linked to the root node of the tree.
  *
  * @author Michael H. Kay
  */

public class LineNumberMap {

    private int[] sequenceNumbers;
    private int[] lineNumbers;
    private int allocated;

    public LineNumberMap() {
        sequenceNumbers = new int[1000];
        lineNumbers = new int[1000];
        allocated = 0;
    }

    /**
    * Set the line number corresponding to a given sequence number
    */

    public void setLineNumber(int sequence, int line) {
        if (sequenceNumbers.length <= allocated + 1) {
            int[] s = new int[allocated * 2];
            int[] l = new int[allocated * 2];
            System.arraycopy(sequenceNumbers, 0, s, 0, allocated);
            System.arraycopy(lineNumbers, 0, l, 0, allocated);
            sequenceNumbers = s;
            lineNumbers = l;
        }
        sequenceNumbers[allocated] = sequence;
        lineNumbers[allocated] = line;
        allocated++;
    }

    /**
    * Get the line number corresponding to a given sequence number
    */

    public int getLineNumber(int sequence) {
        // could use a binary chop, but it's not important
        for (int i=1; i<allocated; i++) {
            if (sequenceNumbers[i] > sequence) {
                return lineNumbers[i-1];
            }
        }
        return lineNumbers[allocated-1];
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved. 
//
// Contributor(s): none. 
//
