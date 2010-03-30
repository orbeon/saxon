package org.orbeon.saxon.tree;

import java.util.Arrays;

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
    private int[] columnNumbers;
    private int allocated;

    /**
     * Create a LineNumberMap with an initial capacity of 200 nodes, which is expanded as necessary
     */

    public LineNumberMap() {
        sequenceNumbers = new int[200];
        lineNumbers = new int[200];
        columnNumbers = new int[200];
        allocated = 0;
    }

    /**
    * Set the line number corresponding to a given sequence number
     * @param sequence the sequence number of the node
     * @param line the line number position of the node
     * @param column the column position of the node
    */

    public void setLineAndColumn(int sequence, int line, int column) {
        if (sequenceNumbers.length <= allocated + 1) {
            int[] s = new int[allocated * 2];
            int[] l = new int[allocated * 2];
            int[] c = new int[allocated * 2];
            System.arraycopy(sequenceNumbers, 0, s, 0, allocated);
            System.arraycopy(lineNumbers, 0, l, 0, allocated);
            System.arraycopy(columnNumbers, 0, c, 0, allocated);
            sequenceNumbers = s;
            lineNumbers = l;
            columnNumbers = c;
        }
        sequenceNumbers[allocated] = sequence;
        lineNumbers[allocated] = line;
        columnNumbers[allocated] = column;
        allocated++;
    }

    /**
    * Get the line number corresponding to a given sequence number
     * @param sequence the sequence number held in the node
     * @return the corresponding line number
    */

    public int getLineNumber(int sequence) {
        if (sequenceNumbers.length > allocated) {
            condense();
        }
        int index = Arrays.binarySearch(sequenceNumbers, sequence);
        if (index < 0) {
            index = -index - 1;
        }
        return lineNumbers[index];
    }

    /**
    * Get the column number corresponding to a given sequence number
     * @param sequence the sequence number held in the node
     * @return the corresponding column number
    */

    public int getColumnNumber(int sequence) {
        if (sequenceNumbers.length > allocated) {
            condense();
        }
        int index = Arrays.binarySearch(sequenceNumbers, sequence);
        if (index < 0) {
            index = -index - 1;
        }
        return columnNumbers[index];
    }

    private synchronized void condense() {
        int[] s = new int[allocated];
        int[] l = new int[allocated];
        int[] c = new int[allocated];
        System.arraycopy(sequenceNumbers, 0, s, 0, allocated);
        System.arraycopy(lineNumbers, 0, l, 0, allocated);
        System.arraycopy(columnNumbers, 0, c, 0, allocated);
        sequenceNumbers = s;
        lineNumbers = l;
        columnNumbers = c;
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
