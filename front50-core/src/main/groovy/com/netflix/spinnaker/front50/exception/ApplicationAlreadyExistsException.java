package com.netflix.spinnaker.front50.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Application already exists")
public class ApplicationAlreadyExistsException extends RuntimeException {
  public ApplicationAlreadyExistsException() {}

  public ApplicationAlreadyExistsException(String message) {}

  public ApplicationAlreadyExistsException(String message, Throwable cause) {}

  public ApplicationAlreadyExistsException(Throwable cause) {}

  protected ApplicationAlreadyExistsException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {}
}
