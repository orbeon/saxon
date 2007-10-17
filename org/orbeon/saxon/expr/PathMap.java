package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.sxpath.XPathEvaluator;
import org.orbeon.saxon.sxpath.XPathExpression;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A PathMap is a description of all the paths followed by an expression.
 * It is a set of trees. Each tree contains as its root an expression that selects
 * nodes without any dependency on the context. The arcs in the tree are axis steps.
 * So the expression doc('a.xml')/a[b=2]/c has a single root (the call on doc()), with
 * a single arc representing child::a, this leads to a node which has two further arcs
 * representing child::b and child::c. Because element b is atomized, there will also be
 * an arc for the step descendant::text() indicating the requirement to access the text
 * nodes of the element.
 *
 * <p>The current implementation works only for XPath 2.0 expressions (for example, constructs
 * like xsl:for-each-group are not handled.)</p>
 */

public class PathMap {

    private List pathMapRoots = new ArrayList(); // a list of PathMapRoot objects
    private Configuration config;

    /**
     * A node in the path map. A node holds a set of arcs, each representing a link to another
     * node in the path map.
     */

    public static class PathMapNode {
        private List arcs;   // a list of PathMapArcs

        /**
         * Create a node in the PathMap (initially with no arcs)
         */

        private PathMapNode() {
            arcs = new ArrayList();
        }

        /**
         * Create a new arc
         * @param step the AxisExpression representing this step
         * @return the newly-constructed target of the new arc
         */

        public PathMapNode createArc(AxisExpression step) {
            PathMapNode target = new PathMapNode();
            PathMapArc arc = new PathMapArc(step, target);
            arcs.add(arc);
            return target;
        }

        /**
         * Get the arcs emanating from this node in the PathMap
         * @return the arcs, each representing an AxisStep. The order of arcs in the array is undefined.
         */

        public PathMapArc[] getArcs() {
            return (PathMapArc[])arcs.toArray(new PathMapArc[arcs.size()]);
        }
    }

    /**
     * A root node in the path map. A root node represents either (a) a subexpression that is the first step in
     * a path expression, or (b) a subexpression that is not the first step in a path, but which returns nodes
     * (for example, a call on the doc() function).
     */

    public static class PathMapRoot extends PathMapNode {

        private Expression rootExpression;

        /**
         * Create a PathMapRoot
         * @param root the expression at the root of a path
         */
        private PathMapRoot(Expression root) {
            this.rootExpression = root;
        }


        public Expression getRootExpression() {
            return rootExpression;
        }
    }

    /**
     * An arc joining two nodes in the path map. The arc has a target (destination) node, and is
     * labelled with an AxisExpression representing a step in a path expression
     */

    private static class PathMapArc {
        private PathMapNode target;
        private AxisExpression step;

        /**
         * Create a PathMapArc
         * @param step the axis step, represented by an AxisExpression
         * @param target the node reached by following this arc
         */
        private PathMapArc(AxisExpression step, PathMapNode target) {
            this.step = step;
            this.target = target;
        }

        /**
         * Get the AxisExpression associated with this arc
         * @return the AxisExpression
         */

        public AxisExpression getStep() {
            return step;
        }

        /**
         * Get the target node representing the destination of this arc
         * @return the target node
         */

        public PathMapNode getTarget() {
            return target;
        }
    }

    /**
     * Create the PathMap for an expression
     * @param exp the expression whose PathMap is required
     */

    public PathMap(ComputedExpression exp, Configuration config) {
        this.config = config;
        exp.addToPathMap(this, null);
    }

    /**
     * Make a new root node in the path map
     * @param exp the expression represented by this root node
     * @return the new root node
     */

    public PathMapRoot makeNewRoot(Expression exp) {
        PathMapRoot root = new PathMapRoot(exp);
        pathMapRoots.add(root);
        return root;
    }

    /**
     * Get all the root expressions from the path map
     * @return an array containing the root expressions
     */

    public PathMapRoot[] getPathMapRoots() {
        return (PathMapRoot[])pathMapRoots.toArray(new PathMapRoot[pathMapRoots.size()]);
    }

    /**
     * Display a printed representation of the path map
     * @param out the output stream to which the output will be written
     */

    public void diagnosticDump(PrintStream out) {
        for (int i=0; i<pathMapRoots.size(); i++) {
            out.println("\nROOT EXPRESSION " + i);
            PathMapRoot mapRoot = (PathMapRoot)pathMapRoots.get(i);
            Expression exp = mapRoot.rootExpression;
            exp.display(0, out, config);
            out.println("\nTREE FOR EXPRESSION " + i);
            showArcs(out, mapRoot, 2);
        }
    }

    /**
     * Internal helper method called by diagnosticDump, to show the arcs emanating from a node
     * @param out the output stream
     * @param node the node in the path map whose arcs are to be displayed
     * @param indent the indentation level in the output
     */

    private void showArcs(PrintStream out, PathMapNode node, int indent) {
        String pad = "                                           ".substring(0, indent);
        List arcs = node.arcs;
        for (int i=0; i<arcs.size(); i++) {
            out.println(pad + ((PathMapArc)arcs.get(i)).step);
            showArcs(out, ((PathMapArc)arcs.get(i)).target, indent+2);
        }
    }

    /**
     * Main method for testing
     * @param args Takes one argument, the XPath expression to be analyzed
     * @throws Exception
     */

    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration();
        XPathEvaluator xpath = new XPathEvaluator(config);
        XPathExpression xpexp = xpath.createExpression(args[0]);
        Expression exp = xpexp.getInternalExpression();
        exp.display(0, System.err, config);
        PathMap initialPath = new PathMap((ComputedExpression)exp, config);
        initialPath.diagnosticDump(System.err);
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

