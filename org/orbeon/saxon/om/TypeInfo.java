package net.sf.saxon.om;

/**
 * This is a copy of the TypeInfo interface defined in DOM Level 3. It is intended that this
 * interface will be replaced by the DOM interface once this is generally available in the
 * Java platform.
 */

public interface TypeInfo {

    public String getTypeName();

    public String getTypeNamespace();

    // DerivationMethods
    public static final int DERIVATION_RESTRICTION    = 0x00000001;
    public static final int DERIVATION_EXTENSION      = 0x00000002;
    public static final int DERIVATION_UNION          = 0x00000004;
    public static final int DERIVATION_LIST           = 0x00000008;

    public boolean isDerivedFrom(String typeNamespaceArg,
                                 String typeNameArg,
                                 int derivationMethod);

}

