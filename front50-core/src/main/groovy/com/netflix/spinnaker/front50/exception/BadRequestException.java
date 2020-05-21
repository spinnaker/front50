package com.netflix.spinnaker.front50.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
  public BadRequestException() {}

  public BadRequestException(String message) {}

  public BadRequestException(String message, Throwable cause) {}

  public BadRequestException(Throwable cause) {}

  protected BadRequestException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {}
}
