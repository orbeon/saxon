package net.sf.saxon.om;

/**
 * An iterator over nodes, that prepends a given node to the nodes
 * returned by another iterator. Used to modify an iterator over axis A
 * to one that iterates over A-OR-SELF.
 */

public class PrependIterator implements AxisIterator {

    NodeInfo start;
    AxisIterator base;
    boolean doneFirst = false;

    public PrependIterator(NodeInfo start, AxisIterator base) {
        this.start = start;
        this.base = base;
    }

    /**
     * Get the next item in the sequence. <BR>
     *
     * @return the next Item. If there are no more nodes, return null.
     */

    public Item next() {
        if (!doneFirst) {
            doneFirst = true;
            return start;
        }
        return base.next();
    }

    /**
     * Get the current item in the sequence.
     *
     * @return the current item, that is, the item most recently returned by
     *         next()
     */

    public Item current() {
        if (position() == 1) {
            return start;
        } else {
            return base.current();
        }
    }

    /**
     * Get the current position
     *
     * @return the position of the current item (the item most recently
     *         returned by next()), starting at 1 for the first node
     */

    public int position() {
        if (doneFirst) {
            return base.position() + 1;
        } else {
            return 1;
        }
    }

    /**
     * Get another iterator over the same sequence of items, positioned at the
     * start of the sequence
     *
     * @return a new iterator over the same sequence
     */

    public SequenceIterator getAnother() {
        return new PrependIterator(start, base);
    }


}
