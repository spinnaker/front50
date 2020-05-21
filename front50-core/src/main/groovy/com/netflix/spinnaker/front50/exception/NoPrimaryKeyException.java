package com.netflix.spinnaker.front50.exception;

public class NoPrimaryKeyException extends RuntimeException {
  public NoPrimaryKeyException() {}

  public NoPrimaryKeyException(String message) {}

  public NoPrimaryKeyException(String message, Throwable cause) {}

  public NoPrimaryKeyException(Throwable cause) {}

  protected NoPrimaryKeyException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {}
}
