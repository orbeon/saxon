package net.sf.saxon.type;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.xpath.XPathException;

/**
 * ValidSchemaType represents a SimpleType or ComplexType component that has been fully validated:
 * in particular, one that contains no unresolved references to other schema components. Thos means that
 * methods that rely on the presence of other component never raise exceptions. This interface is provided
 * so that code that only needs to handle validated schema components can be written without having to deal
 * with the exceptions that would arise from using components with unresolved references.
 */
public interface ValidSchemaType extends SchemaType {

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     * @return a lexical QName identifying the type
     */

    String getDisplayName();

    /**
     * Returns the base type that this type inherits from.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     * @return the base type.
    */

    SchemaType getBaseType();

    /**
     * Get the typed value of a node that is annotated with this schema type. This method must be called
     * only for a valid type.
     * @param node the node whose typed value is required
     * @return a SequenceIterator over the atomic values making up the typed value of the specified
     * node. The objects returned by this iterator are of type {@link net.sf.saxon.value.AtomicValue}
     */

    SequenceIterator getTypedValue(NodeInfo node)
            throws XPathException;

    /**
     * Get a description of this type for use in diagnostics. In the case of a named type, this is the
     * same as the display name. In the case of a type known to be defined immediately within an element
     * or attribute declaration, it is a phrase that identifies the containing declaration. In other cases,
     * it is a phrase of the form "defined at line L of URI". The description is designed to be inserted
     * in a context such as "the type X is ..."
     */

    String getDescription();

}
