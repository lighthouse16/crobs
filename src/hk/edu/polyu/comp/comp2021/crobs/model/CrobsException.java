package hk.edu.polyu.comp.comp2021.crobs.model;

/**
 * Custom checked exception for domain and business rule violations in CROBS.
 */
public class CrobsException extends Exception {

    /**
     * Constructs a new CrobsException with a detailed error message.
     *
     * @param message human-readable error explanation.
     */
    public CrobsException(String message) {
        super(message);
    }
}
