package net.sf.saxon.functions;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.SequenceType;

import java.io.PrintStream;
import java.util.ArrayList;


/**
* Abstract superclass for system-defined and user-defined functions
*/

public abstract class SystemFunction extends FunctionCall {

    /**
    * Make a system function (one in the standard function namespace).
    * @param name The local name of the function. It may also be a lexical QName for
     * a recognized built-in function, e.g. saxon:evaluate, in which case the prefix is hard-coded.
    * @return a FunctionCall that implements this function, if it
    * exists, or null if the function is unknown.
    */

    public static FunctionCall makeSystemFunction(String name, int arity, NamePool pool) {
        StandardFunction.Entry entry = StandardFunction.getFunction(name, arity);
        if (entry==null) {
            return null;
        }
        Class functionClass = entry.implementationClass;
        try {
            SystemFunction f = (SystemFunction)functionClass.newInstance();
            f.setDetails(entry);
            //if (name.startsWith("saxon")) {
            //    f.setFunctionNameCode(pool.allocate("saxon", NamespaceConstant.SAXON, name.substring(6)));
            //} else {
                f.setFunctionNameCode(pool.allocate("", NamespaceConstant.FN, name));
            //}
            return f;
        } catch (IllegalAccessException err) {
            return null;
        } catch (InstantiationException err) {
            return null;
        }
    }

    /**
     *
     */

    private StandardFunction.Entry details;
    protected int operation;

    /**
    * Set the details of this type of function
    */

    public void setDetails(StandardFunction.Entry entry) {
        details = entry;
        operation = details.opcode;
    }

    /**
    * Get the details
    */

    protected StandardFunction.Entry getDetails() {
        return details;
    }

    /**
    * Method called during static type checking
    */

    public void checkArguments(StaticContext env) throws XPathException {
        checkArgumentCount(details.minArguments, details.maxArguments, env);
        for (int i=0; i<argument.length; i++) {
            checkArgument(i, env);
        }
    }

    /**
    * Perform static type checking on an argument to a function call, and add
    * type conversion logic where necessary.
    */

    private void checkArgument(int arg, StaticContext env) throws XPathException {
        RoleLocator role = new RoleLocator(RoleLocator.FUNCTION, new Integer(getFunctionNameCode()), arg, env.getNamePool());
        argument[arg] = TypeChecker.staticTypeCheck(
                                argument[arg],
                                getRequiredType(arg),
                                env.isInBackwardsCompatibleMode(),
                                role, env);
        argument[arg] = argument[arg].simplify(env);
    }

    /**
    * Get the required type of the nth argument
    */

    protected SequenceType getRequiredType(int arg) {
        return details.argumentTypes[arg];
        // this is overridden for concat()
    }

    /**
    * Determine the item type of the value returned by the function
    */

    public ItemType getItemType() {
        ItemType type = details.itemType;
        if (type == StandardFunction.SAME_AS_FIRST_ARGUMENT) {
            if (argument.length > 0) {
                return argument[0].getItemType();
            } else {
                return AnyItemType.getInstance();
                // if there is no first argument, an error will be reported
            }
        } else {
            return type;
        }
    }

    /**
    * Determine the cardinality of the function.
    */

    public int computeCardinality() {
        if (details==null) {
            System.err.println("**** No details for " + getClass() + " at " + this);
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
        return details.cardinality;
    }

    /**
     * Determine the special properties of this expression. The general rule
     * is that a system function call is non-creative if its return type is
     * atomic, or if all its arguments are non-creative. This is overridden
     * for the generate-id() function, which is considered creative if
     * its operand is creative (because the result depends on the
     * identity of the operand)
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        if (getItemType() instanceof AtomicType) {
            return p | StaticProperty.NON_CREATIVE;
        }
        for (int i=0; i<argument.length; i++) {
            if ((argument[i].getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {
                // the argument is creative
                return p;
            }
        }
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Set "." as the default value for the first and only argument. Called from subclasses.
    */

    protected final void useContextItemAsDefault() {
        if (argument.length==0) {
            argument = new Expression[1];
            argument[0] = new ContextItemExpression();
            ExpressionTool.copyLocationInfo(this, argument[0]);
            ((ContextItemExpression)argument[0]).setParentExpression(this);
        }
        // Note that the extra argument is added before type-checking takes place. The
        // type-checking will add any necessary checks to ensure that the context item
        // is a node, in cases where this is required.
    }

    /**
    * Add an implicit argument referring to the context document. Called by functions such as
    * id() and key() that take the context document as an implicit argument
    */

    protected final void addContextDocumentArgument(int pos, String augmentedName)
    throws StaticError {
        if (argument.length > pos) {
            return;
            // this can happen during optimization, if the extra argument is already present
        }
        if (argument.length != pos) {
            throw new StaticError("Too few arguments in call to " + augmentedName + "() function");
        }
        Expression[] newArgs = new Expression[pos+1];
        System.arraycopy(argument, 0, newArgs, 0, argument.length);
        final RootExpression rootExpression = new RootExpression();
        ExpressionTool.copyLocationInfo(this, newArgs[pos]);
        rootExpression.setParentExpression(this);
        newArgs[pos] = rootExpression;
        argument = newArgs;
        setDetails(StandardFunction.getFunction(augmentedName, newArgs.length));
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "function " + getDisplayName(pool));
        for (int a=0; a<argument.length; a++) {
            argument[a].display(level+1, pool, out);
        }
    }

    /**
     * The main() method of this class is not intended to be called, it merely
     * tells the code inspection tools in IDEA that the constructors of each
     * function class are actual entry points
     */

    public static void main(String[] args) throws Exception {
        ArrayList a = new ArrayList(20);
        a.add(new Adjust());
        a.add(new Aggregate());
        a.add(new Available());
        a.add(new BaseURI());
        a.add(new BooleanFn());
        a.add(new Collection());
        a.add(new Compare());
        a.add(new Component());
        a.add(new Concat());
        a.add(new Contains());
        a.add(new Current());
        a.add(new CurrentDateTime());
        a.add(new CurrentGroup());
        a.add(new Data());
        a.add(new DeepEqual());
        a.add(new DefaultCollation());
        a.add(new DistinctValues());
        a.add(new Doc());
        a.add(new Document());
        a.add(new Error());
        a.add(new EscapeURI());
        a.add(new Evaluate());
        a.add(new Existence());
        a.add(new ForceCase());
        a.add(new FormatDate());
        a.add(new FormatNumber());
        a.add(new FormatNumber2());
        a.add(new Id());
        a.add(new Idref());
        a.add(new IndexOf());
        a.add(new InScopePrefixes());
        a.add(new Insert());
        a.add(new Key());
        a.add(new Lang());
        a.add(new Last());
        a.add(new Matches());
        a.add(new Minimax());
        a.add(new NamePart());
        a.add(new NamespaceForPrefix());
        a.add(new NormalizeSpace());
        a.add(new NumberFn());
        a.add(new Parse());
        a.add(new Position());
        a.add(new QNameFn());
        a.add(new RegexGroup());
        a.add(new Remove());
        a.add(new Replace());
        a.add(new ResolveQName());
        a.add(new ResolveURI());
        a.add(new Reverse());
        a.add(new Root());
        a.add(new Rounding());
        a.add(new Serialize());
        a.add(new StaticBaseURI());
        a.add(new StringFn());
        a.add(new StringJoin());
        a.add(new StringLength());
        a.add(new Subsequence());
        a.add(new Substring());
        a.add(new SystemProperty());
        a.add(new Tokenize());
        a.add(new Trace());
        a.add(new Translate());
        a.add(new TreatFn());
        a.add(new Unicode());
        a.add(new Unordered());
        a.add(new UnparsedEntity());
        a.add(new UnparsedText());
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
