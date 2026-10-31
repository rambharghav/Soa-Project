package com.payvault.transaction.context;

public class LedgerContext {

    private static final ThreadLocal<String> TRANSACTION_REF = new ThreadLocal<>();
    private static final ThreadLocal<String> DESCRIPTION = new ThreadLocal<>();

    public static void set(String transactionRef, String description) {
        TRANSACTION_REF.set(transactionRef);
        DESCRIPTION.set(description);
    }

    public static String getTransactionRef() {
        return TRANSACTION_REF.get();
    }

    public static String getDescription() {
        return DESCRIPTION.get();
    }

    public static void clear() {
        TRANSACTION_REF.remove();
        DESCRIPTION.remove();
    }
}
