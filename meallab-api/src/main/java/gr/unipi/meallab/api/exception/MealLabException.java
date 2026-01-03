package gr.unipi.meallab.api.exception;

/**
 * Custom runtime exception for MealLab API errors.
 * 
 * Thrown by MealLabClient when:
 * - API calls fail (network errors, timeouts, HTTP errors)
 * - JSON parsing fails (invalid response format)
 * - Requested meal not found (lookup by ID returns no results)
 * 
 * This is an unchecked exception (RuntimeException), so callers don't need
 * to declare it. Use try-catch only if you want to handle API errors gracefully.
 * 
 * Example usage:
 *   try {
 *       MealDetails meal = client.lookupById("12345");
 *   } catch (MealLabException e) {
 *       System.err.println("Meal not found: " + e.getMessage());
 *   }
 */
public class MealLabException extends RuntimeException {
	private static final long serialVersionUID = 1L;

    /**
     * Create an exception with a descriptive message.
     * 
     * @param message error description (e.g., "No meal found for id=999")
     */
    public MealLabException(String message) {
        super(message);
    }

    /**
     * Create an exception with a message and root cause.
     * Use this when wrapping lower-level exceptions (e.g., IOException from network).
     * 
     * @param message error description
     * @param cause the underlying exception that triggered this error
     */
    public MealLabException(String message, Throwable cause) {
        super(message, cause);
    }
}
