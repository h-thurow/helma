package helma.objectmodel.db;

/**
 * Represents the information, that some database operation didn't succeed, because of the corresponding JDBC driver
 * not being present.
 */
public class NoDriverException extends Exception {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -4498614734005646616L;

    /**
     * @param cause
     *  The cause for the no driver exception.
     */
    public NoDriverException(Throwable cause) {
        // delegate
        super(cause);
    }

}
