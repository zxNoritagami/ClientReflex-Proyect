package dev.zxnoragami.clientreflex.net.antidisconnect;

import java.util.regex.Pattern;

/**
 * Clasifica errores de conexión como recuperables o no recuperables.
 */
public class RecoverableErrorClassifier {
    
    private static final Pattern[] RECOVERABLE_PATTERNS = {
        Pattern.compile("(?i)timeout"),
        Pattern.compile("(?i)timed out"),
        Pattern.compile("(?i)tiempo de espera"),
        Pattern.compile("(?i)readtimeout"),
        Pattern.compile("(?i)connection lost"),
        Pattern.compile("(?i)conexión perdida"),
        Pattern.compile("(?i)connection reset"),
        Pattern.compile("(?i)conexión reseteada"),
        Pattern.compile("(?i)internal exception.*ioexception"),
        Pattern.compile("(?i)disconnected"),
        Pattern.compile("(?i)channel.*closed"),
        Pattern.compile("(?i)canal.*cerrado")
    };
    
    private static final Pattern[] NON_RECOVERABLE_PATTERNS = {
        Pattern.compile("(?i)banned"),
        Pattern.compile("(?i)baneado"),
        Pattern.compile("(?i)kicked"),
        Pattern.compile("(?i)expulsado"),
        Pattern.compile("(?i)you are not whitelisted"),
        Pattern.compile("(?i)no estás en la lista blanca"),
        Pattern.compile("(?i)server.*full"),
        Pattern.compile("(?i)servidor.*lleno")
    };
    
    /**
     * Verifica si un error es recuperable.
     * 
     * @param error El mensaje de error o excepción
     * @return true si el error es recuperable, false en caso contrario
     */
    public static boolean isRecoverable(String error) {
        if (error == null || error.isEmpty()) {
            return true; // Si no hay razón, asumimos que es recuperable
        }
        
        String errorLower = error.toLowerCase();
        
        // Primero verificar si es NO recuperable
        for (Pattern pattern : NON_RECOVERABLE_PATTERNS) {
            if (pattern.matcher(errorLower).find()) {
                return false;
            }
        }
        
        // Luego verificar si es recuperable
        for (Pattern pattern : RECOVERABLE_PATTERNS) {
            if (pattern.matcher(errorLower).find()) {
                return true;
            }
        }
        
        // Por defecto, asumimos que es recuperable
        return true;
    }
    
    /**
     * Verifica si una excepción es recuperable.
     * 
     * @param exception La excepción
     * @return true si la excepción es recuperable, false en caso contrario
     */
    public static boolean isRecoverable(Throwable exception) {
        if (exception == null) {
            return true;
        }
        
        String message = exception.getMessage();
        String className = exception.getClass().getSimpleName();
        
        // Verificar por nombre de clase
        if (className.contains("ReadTimeoutException") ||
            className.contains("TimeoutException") ||
            className.contains("IOException")) {
            return true;
        }
        
        // Verificar por mensaje
        if (message != null) {
            return isRecoverable(message);
        }
        
        return true;
    }
}

