package fi.improveit.req_ex;

public class ExportException extends Exception {

    public ExportException(Throwable cause) {
        super(cause);
    }
    public ExportException(String message) {
        super(message);
    }

}

