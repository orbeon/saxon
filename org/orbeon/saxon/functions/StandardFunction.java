package net.sf.saxon.functions;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceType;

import java.util.HashMap;

/**
* This class contains static data tables defining the properties of standard functions. "Standard functions"
* here means the XPath 2.0 functions, the XSLT 2.0 functions, and a few selected extension functions
* which need special recognition.
*/

public abstract class StandardFunction {

    /**
     * This class is never instantiated
     */

    private StandardFunction() {
    }

    /**
     * Register a system function in the table of function details.
     * @param name the function name
     * @param implementationClass the class used to implement the function
     * @param opcode identifies the function when a single class implements several functions
     * @param minArguments the minimum number of arguments required
     * @param maxArguments the maximum number of arguments allowed
     * @param itemType the item type of the result of the function
     * @param cardinality the cardinality of the result of the function
     * @return the entry describing the function. The entry is incomplete, it does not yet contain information
     * about the function arguments.
    */

    private static Entry register( String name,
                                 Class implementationClass,
                                 int opcode,
                                 int minArguments,
                                 int maxArguments,
                                 ItemType itemType,
                                 int cardinality ) {
        Entry e = makeEntry(name, implementationClass, opcode, minArguments, maxArguments, itemType, cardinality);
        functionTable.put(name, e);
        return e;
    }

    /**
     * Make a table entry describing the signature of a function, with a reference to the implementation class.
     * @param name the function name
     * @param implementationClass the class used to implement the function
     * @param opcode identifies the function when a single class implements several functions
     * @param minArguments the minimum number of arguments required
     * @param maxArguments the maximum number of arguments allowed
     * @param itemType the item type of the result of the function
     * @param cardinality the cardinality of the result of the function
     * @return the entry describing the function. The entry is incomplete, it does not yet contain information
     * about the function arguments.
     */
    public static Entry makeEntry(String name, Class implementationClass, int opcode,
                                   int minArguments, int maxArguments, ItemType itemType, int cardinality) {
        Entry e = new Entry();
        int hash = name.indexOf('#');
        if (hash < 0) {
            e.name = name;
        } else {
            e.name = name.substring(0, hash);
        }
        e.implementationClass = implementationClass;
        e.opcode = opcode;
        e.minArguments = minArguments;
        e.maxArguments = maxArguments;
        e.itemType = itemType;
        e.cardinality = cardinality;
        if (maxArguments > 100) {
            // special case for concat()
            e.argumentTypes = new SequenceType[1];
        } else {
            e.argumentTypes = new SequenceType[maxArguments];
        }
        return e;
    }

    /**
     * Add information to a function entry about the argument types of the function
     * @param e the entry for the function
     * @param a the position of the argument, counting from zero
     * @param type the item type of the argument
     * @param cardinality the cardinality of the argument
     */

    public static void arg(Entry e, int a, ItemType type, int cardinality) {
        try {
            e.argumentTypes[a] = SequenceType.makeSequenceType(type, cardinality);
        } catch (ArrayIndexOutOfBoundsException err) {
            System.err.println("Internal Saxon error: Can't set argument " + a + " of " + e.name);
        }
    }

    private static HashMap functionTable = new HashMap(200);

    protected static ItemType SAME_AS_FIRST_ARGUMENT = NodeKindTest.NAMESPACE;
                // this could be any item type that is used only for this purpose

    static {
        Entry e;
        e = register("abs", Rounding.class, Rounding.ABS, 1, 1, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.NUMBER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("adjust-date-to-timezone", Adjust.class, 0, 1, 2, Type.DATE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("adjust-dateTime-to-timezone", Adjust.class, 0, 1, 2, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("adjust-time-to-timezone", Adjust.class, 0, 1, 2, Type.TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("avg", Aggregate.class, Aggregate.AVG, 1, 1, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
                // can't say "same as first argument" because the avg of a set of integers is decimal
            arg(e, 0, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

        e = register("base-uri", BaseURI.class, 0, 0, 1, Type.ANY_URI_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("boolean", BooleanFn.class, BooleanFn.BOOLEAN, 1, 1, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

        e = register("ceiling", Rounding.class, Rounding.CEILING, 1, 1, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.NUMBER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("codepoints-to-string", Unicode.class, Unicode.FROM_CODEPOINTS, 1, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

        e = register("collection", Collection.class, 0, 0, 1, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("compare", Compare.class, 0, 2, 3, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("concat", Concat.class, 0, 2, Integer.MAX_VALUE, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            // Note, this has a variable number of arguments so it is treated specially

        e = register("contains", Contains.class, Contains.CONTAINS, 2, 3, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("count", Aggregate.class, Aggregate.COUNT, 1, 1, Type.INTEGER_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

            register("current", Current.class, 0, 0, 0, Type.ITEM_TYPE, StaticProperty.EXACTLY_ONE);
            register("current-date", CurrentDateTime.class, 0, 0, 0, Type.DATE_TYPE, StaticProperty.EXACTLY_ONE);
            register("current-dateTime", CurrentDateTime.class, 0, 0, 0, Type.DATE_TIME_TYPE, StaticProperty.EXACTLY_ONE);
            register("current-time", CurrentDateTime.class, 0, 0, 0, Type.TIME_TYPE, StaticProperty.EXACTLY_ONE);

            register("current-group", CurrentGroup.class, CurrentGroup.CURRENT_GROUP, 0, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            register("current-grouping-key", CurrentGroup.class, CurrentGroup.CURRENT_GROUPING_KEY, 0, 0, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("data", Data.class, 0, 1, 1, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

        e = register("dateTime", DateTimeConstructor.class, 0, 2, 2, Type.DATE_TIME_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.DATE_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 1, Type.TIME_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("day-from-date", Component.class, (Component.DAY<<16) + Type.DATE, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("day-from-dateTime", Component.class, (Component.DAY<<16) + Type.DATE_TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        // TODO: obsolete name, delete this function at some stage
        e = register("days-from-dayTimeDuration", Component.class, (Component.DAY<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("days-from-duration", Component.class, (Component.DAY<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
                    arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("deep-equal", DeepEqual.class, 0, 2, 3, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

            register("default-collation", DefaultCollation.class, 0, 0, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("distinct-values", DistinctValues.class, 0, 1, 2, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("doc", Doc.class, 0, 1, 1, NodeKindTest.DOCUMENT, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("document", Document.class, 0, 1, 2, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.NODE_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("document-uri", NamePart.class, NamePart.DOCUMENT_URI, 1, 1, Type.ANY_URI_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, NodeKindTest.DOCUMENT, StaticProperty.ALLOWS_ZERO_OR_MORE);

        e = register("empty", Existence.class, Existence.EMPTY, 1, 1, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

        e = register("ends-with", Contains.class, Contains.ENDSWITH, 2, 3, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("element-available", Available.class, Available.ELEMENT_AVAILABLE, 1, 1, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("error", Error.class, 0, 0, 3, Type.ITEM_TYPE, StaticProperty.EXACTLY_ONE);
            // TODO: the return type here needs attention.
            arg(e, 0, Type.QNAME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

        e = register("escape-uri", EscapeURI.class, 0, 2, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("exactly-one", TreatFn.class, StaticProperty.EXACTLY_ONE, 1, 1, SAME_AS_FIRST_ARGUMENT, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.EXACTLY_ONE);
                // because we don't do draconian static type checking, we can do the work in the argument type checking code

        e = register("exists", Existence.class, Existence.EXISTS, 1, 1, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

        // TODO: delete this obsolete name (replaced by fn:QName)
        e = register("expanded-QName", QNameFn.class, 0, 2, 2, Type.QNAME_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

            register("false", BooleanFn.class, BooleanFn.FALSE, 0, 0, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("floor", Rounding.class, Rounding.FLOOR, 1, 1, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.NUMBER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("format-date", FormatDate.class, Type.DATE, 2, 5, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.DATE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 3, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 4, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("format-dateTime", FormatDate.class, Type.DATE_TIME, 2, 5, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 3, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 4, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("format-number", FormatNumber2.class, 0, 2, 3, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.NUMBER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        // TODO: the following is retained in case anyone still needs the 1.0 implementation.
        // It should be removed eventually.

        e = register("format-number-1.0", FormatNumber.class, 0, 2, 3, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.NUMBER_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("format-time", FormatDate.class, Type.TIME, 2, 5, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 3, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 4, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("function-available", Available.class, Available.FUNCTION_AVAILABLE, 1, 2, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 1, Type.INTEGER_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("generate-id", NamePart.class, NamePart.GENERATE_ID, 0, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        // TODO: the versions of the following functions currently support both the old and the new names

        e = register("get-year-from-dateTime", Component.class, (Component.YEAR<<16) + Type.DATE_TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-month-from-dateTime", Component.class, (Component.MONTH<<16) + Type.DATE_TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-day-from-dateTime", Component.class, (Component.DAY<<16) + Type.DATE_TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-hours-from-dateTime", Component.class, (Component.HOURS<<16) + Type.DATE_TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-minutes-from-dateTime", Component.class, (Component.MINUTES<<16) + Type.DATE_TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-seconds-from-dateTime", Component.class, (Component.SECONDS<<16) + Type.DATE_TIME, 1, 1, Type.DECIMAL_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-timezone-from-dateTime", Component.class, (Component.TIMEZONE<<16) + Type.DATE_TIME, 1, 1, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-year-from-date", Component.class, (Component.YEAR<<16) + Type.DATE, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-month-from-date", Component.class, (Component.MONTH<<16) + Type.DATE, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-day-from-date", Component.class, (Component.DAY<<16) + Type.DATE, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-timezone-from-date", Component.class, (Component.TIMEZONE<<16) + Type.DATE, 1, 1, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-hours-from-time", Component.class, (Component.HOURS<<16) + Type.TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-minutes-from-time", Component.class, (Component.MINUTES<<16) + Type.TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-seconds-from-time", Component.class, (Component.SECONDS<<16) + Type.TIME, 1, 1, Type.DECIMAL_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-timezone-from-time", Component.class, (Component.TIMEZONE<<16) + Type.TIME, 1, 1, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-years-from-yearMonthDuration", Component.class, (Component.YEAR<<16) + Type.YEAR_MONTH_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.YEAR_MONTH_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-months-from-yearMonthDuration", Component.class, (Component.MONTH<<16) + Type.YEAR_MONTH_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.YEAR_MONTH_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-days-from-dayTimeDuration", Component.class, (Component.DAY<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-hours-from-dayTimeDuration", Component.class, (Component.HOURS<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-minutes-from-dayTimeDuration", Component.class, (Component.MINUTES<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-seconds-from-dayTimeDuration", Component.class, (Component.SECONDS<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.DECIMAL_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-local-name-from-QName", Component.class, (Component.LOCALNAME<<16) + Type.QNAME, 1, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.QNAME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-namespace-from-QName", Component.class, (Component.NAMESPACE<<16) + Type.QNAME, 1, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.QNAME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("get-in-scope-prefixes", InScopePrefixes.class, 0, 1, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, NodeKindTest.ELEMENT, StaticProperty.EXACTLY_ONE);

        e = register("get-namespace-uri-for-prefix", NamespaceForPrefix.class, 0, 2, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 1, NodeKindTest.ELEMENT, StaticProperty.EXACTLY_ONE);

        e = register("hours-from-dateTime", Component.class, (Component.HOURS<<16) + Type.DATE_TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        // TODO: delete this obsolete name
        e = register("hours-from-dayTimeDuration", Component.class, (Component.HOURS<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("hours-from-duration", Component.class, (Component.HOURS<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
                    arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("hours-from-time", Component.class, (Component.HOURS<<16) + Type.TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("id", Id.class, 0, 1, 2, NodeKindTest.ELEMENT, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.NODE_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("idref", Idref.class, 0, 1, 2, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.NODE_TYPE, StaticProperty.EXACTLY_ONE);

            register("implicit-timezone", CurrentDateTime.class, 0, 0, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("in-scope-prefixes", InScopePrefixes.class, 0, 1, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, NodeKindTest.ELEMENT, StaticProperty.EXACTLY_ONE);

        e = register("index-of", IndexOf.class, 0, 2, 3, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.ANY_ATOMIC_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("insert-before", Insert.class, 0, 3, 3, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.INTEGER_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

        e = register("key", Key.class, 0, 2, 3, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 1, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 2, Type.NODE_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("lang", Lang.class, 0, 1, 2, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 1, Type.NODE_TYPE, StaticProperty.EXACTLY_ONE);

            register("last", Last.class, 0, 0, 0, Type.INTEGER_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("local-name", NamePart.class, NamePart.LOCAL_NAME, 0, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("local-name-from-QName", Component.class, (Component.LOCALNAME<<16) + Type.QNAME, 1, 1, Type.NCNAME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.QNAME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("lower-case", ForceCase.class, ForceCase.LOWERCASE, 1, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("matches", Matches.class, 0, 2, 3, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("max", Minimax.class, Minimax.MAX, 1, 2, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("min", Minimax.class, Minimax.MIN, 1, 2, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("minutes-from-dateTime", Component.class, (Component.MINUTES<<16) + Type.DATE_TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        // TODO: delete this obsolete synonym
        e = register("minutes-from-dayTimeDuration", Component.class, (Component.MINUTES<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
                    arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("minutes-from-duration", Component.class, (Component.MINUTES<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("minutes-from-time", Component.class, (Component.MINUTES<<16) + Type.TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("month-from-date", Component.class, (Component.MONTH<<16) + Type.DATE, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("month-from-dateTime", Component.class, (Component.MONTH<<16) + Type.DATE_TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("months-from-duration", Component.class, (Component.MONTH<<16) + Type.YEAR_MONTH_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.YEAR_MONTH_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        // TODO: delete this obsolete synonym
        e = register("months-from-yearMonthDuration", Component.class, (Component.MONTH<<16) + Type.YEAR_MONTH_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.YEAR_MONTH_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("name", NamePart.class, NamePart.NAME, 0, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("namespace-uri", NamePart.class, NamePart.NAMESPACE_URI, 0, 1, Type.ANY_URI_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("namespace-uri-for-prefix", NamespaceForPrefix.class, 0, 2, 2, Type.ANY_URI_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 1, NodeKindTest.ELEMENT, StaticProperty.EXACTLY_ONE);

        e = register("namespace-uri-from-QName", Component.class, (Component.NAMESPACE<<16) + Type.QNAME, 1, 1, Type.ANY_URI_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.QNAME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        // TODO: support "nilled" function

        e = register("node-name", NamePart.class, NamePart.NODE_NAME, 0, 1, Type.QNAME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("not", BooleanFn.class, BooleanFn.NOT, 1, 1, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

            register("normalize-space#0", NormalizeSpace.class, 0, 0, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("normalize-space#1", NormalizeSpace.class, 0, 1, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("number", NumberFn.class, 0, 0, 1, Type.DOUBLE_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("one-or-more", TreatFn.class, StaticProperty.ALLOWS_ONE_OR_MORE, 1, 1, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ONE_OR_MORE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ONE_OR_MORE);
                // because we don't do draconian static type checking, we can do the work in the argument type checking code

            register("position", Position.class, 0, 0, 0, Type.INTEGER_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("prefix-from-QName", Component.class, (Component.PREFIX<<16) + Type.QNAME, 1, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.QNAME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("QName", QNameFn.class, 0, 2, 2, Type.QNAME_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("regex-group", RegexGroup.class, 0, 1, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.INTEGER_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("remove", Remove.class, 0, 2, 2, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.INTEGER_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("replace", Replace.class, 0, 3, 4, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 3, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("resolve-QName", ResolveQName.class, 0, 2, 2, Type.QNAME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, NodeKindTest.ELEMENT, StaticProperty.EXACTLY_ONE);

        e = register("resolve-uri", ResolveURI.class, 0, 1, 2, Type.ANY_URI_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("reverse", Reverse.class, 0, 1, 1, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

        e = register("root", Root.class, 0, 0, 1, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("round", Rounding.class, Rounding.ROUND, 1, 1, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.NUMBER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("round-half-to-even", Rounding.class, Rounding.HALF_EVEN, 1, 2, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.NUMBER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.INTEGER_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("seconds-from-dateTime", Component.class, (Component.SECONDS<<16) + Type.DATE_TIME, 1, 1, Type.DECIMAL_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        // TODO: remove obsolete synonym
        e = register("seconds-from-dayTimeDuration", Component.class, (Component.SECONDS<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.DECIMAL_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("seconds-from-duration", Component.class, (Component.SECONDS<<16) + Type.DAY_TIME_DURATION, 1, 1, Type.DECIMAL_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
                    arg(e, 0, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("seconds-from-time", Component.class, (Component.SECONDS<<16) + Type.TIME, 1, 1, Type.DECIMAL_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("starts-with", Contains.class, Contains.STARTSWITH, 2, 3, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

            register("static-base-uri", StaticBaseURI.class, 0, 0, 0, Type.ANY_URI_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("string", StringFn.class, 0, 0, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

            register("string-length#0", StringLength.class, 0, 0, 0, Type.INTEGER_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("string-length#1", StringLength.class, 0, 1, 1, Type.INTEGER_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("string-join", StringJoin.class, 0, 2, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("string-to-codepoints", Unicode.class, Unicode.TO_CODEPOINTS, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("subsequence", Subsequence.class, 0, 2, 3, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.NUMBER_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.NUMBER_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("substring", Substring.class, 0, 2, 3, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.NUMBER_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.NUMBER_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("substring-after", Contains.class, Contains.AFTER, 2, 3, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("substring-before", Contains.class, Contains.BEFORE, 2, 3, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("sum", Aggregate.class, Aggregate.SUM, 1, 2, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.ANY_ATOMIC_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("system-property", SystemProperty.class, 0, 1, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("timezone-from-date", Component.class, (Component.TIMEZONE<<16) + Type.DATE, 1, 1, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("timezone-from-dateTime", Component.class, (Component.TIMEZONE<<16) + Type.DATE_TIME, 1, 1, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("timezone-from-time", Component.class, (Component.TIMEZONE<<16) + Type.TIME, 1, 1, Type.DAY_TIME_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
                    arg(e, 0, Type.TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("trace", Trace.class, 0, 2, 2, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

            register("true", BooleanFn.class, BooleanFn.TRUE, 0, 0, Type.BOOLEAN_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("translate", Translate.class, 0, 3, 3, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("tokenize", Tokenize.class, 0, 2, 3, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        e = register("unordered", Unordered.class, 0, 1, 1, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_MORE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);

        e = register("upper-case", ForceCase.class, ForceCase.UPPERCASE, 1, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("unparsed-entity-uri", UnparsedEntity.class, UnparsedEntity.URI, 1, 1, Type.ANY_URI_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        // internal version of unparsed-entity-uri with second argument representing the current document
        e = register("unparsed-entity-uri_9999_", UnparsedEntity.class, UnparsedEntity.URI, 2, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 1, NodeKindTest.DOCUMENT, StaticProperty.EXACTLY_ONE);

        e = register("unparsed-entity-public-id", UnparsedEntity.class, UnparsedEntity.PUBLIC_ID, 1, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);

        // internal version of unparsed-entity-public-id with second argument representing the current document
        e = register("unparsed-entity-public-id_9999_", UnparsedEntity.class, UnparsedEntity.PUBLIC_ID, 2, 2, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            arg(e, 1, NodeKindTest.DOCUMENT, StaticProperty.EXACTLY_ONE);

        e = register("unparsed-text", UnparsedText.class, 0, 2, 2, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.STRING_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 1, Type.STRING_TYPE, StaticProperty.EXACTLY_ONE);
            // TODO: single-argument version not yet supported

        e = register("year-from-date", Component.class, (Component.YEAR<<16) + Type.DATE, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("year-from-dateTime", Component.class, (Component.YEAR<<16) + Type.DATE_TIME, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.DATE_TIME_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("years-from-duration", Component.class, (Component.YEAR<<16) + Type.YEAR_MONTH_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.YEAR_MONTH_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        // TODO: delete obsolete signature
        e = register("years-from-yearMonthDuration", Component.class, (Component.YEAR<<16) + Type.YEAR_MONTH_DURATION, 1, 1, Type.INTEGER_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.YEAR_MONTH_DURATION_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);

        e = register("zero-or-one", TreatFn.class, StaticProperty.ALLOWS_ZERO_OR_ONE, 1, 1, SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_ONE);
            arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE);
                // because we don't do draconian static type checking, we can do the work in the argument type checking code
    }

    /**
     * Get the table entry for the function with a given name
     * @param name the name of the function. This may be an unprefixed local-name for functions in the
     * system namespace, or may use the conventional prefix "saxon:" in the case of Saxon extension functions
     * that are specially recognized
     * @return if the function name is known, an Entry containing information about the function. Otherwise,
     * null
     */

    public static Entry getFunction(String name, int arity) {
        // try first for an entry of the form name#arity
        Entry e = (Entry)functionTable.get(name + '#' + arity);
        if (e != null) {
            return e;
        }
        // try for a generic entry
        return (Entry)functionTable.get(name);
    }

    /**
     * An entry in the table describing the properties of a function
     */
    public static class Entry implements java.io.Serializable {
        /**
         * The name of the function: a local name in the case of functions in the standard library, or a
         * name with the conventional prefix "saxon:" in the case of Saxon extension functions
         */
        public String name;
        /**
         * The class containing the implementation of this function (always a subclass of SystemFunction)
         */
        public Class implementationClass;
        /**
         * Some classes support more than one function. In these cases the particular function is defined
         * by an integer opcode, whose meaning is local to the implementation class.
         */
        public int opcode;
        /**
         * The minimum number of arguments required
         */
        public int minArguments;
        /**
         * The maximum number of arguments permitted
         */
        public int maxArguments;
        /**
         * The item type of the result of the function
         */
        public ItemType itemType;
        /**
         * The cardinality of the result of the function
         */
        public int cardinality;
        /**
         * An array holding the types of the arguments to the function
         */
        public SequenceType[] argumentTypes;
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License];
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay 
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
