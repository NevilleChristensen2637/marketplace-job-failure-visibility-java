package example.infrai;
public final class InfraiException extends RuntimeException { public final int status; public InfraiException(String message,int status){super(message);this.status=status;} }
