package com.netflix.spinnaker.front50.validator;

import com.google.common.base.Strings;
import com.netflix.spinnaker.front50.model.application.Application;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HasEmailValidator implements ApplicationValidator {
  @Override
  public void validate(Application application, ApplicationValidationErrors validationErrors) {
    if (Strings.isNullOrEmpty(Optional.ofNullable(application.getEmail()).orElse("").trim())) {
      validationErrors.rejectValue(
          "email", "application.email.empty", "Application must have a non-empty email");
    }
  }
}
