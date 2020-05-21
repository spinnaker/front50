package com.netflix.spinnaker.front50.validator;

import com.google.common.base.Strings;
import com.netflix.spinnaker.front50.model.application.Application;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HasNameValidator implements ApplicationValidator {
  @Override
  public void validate(Application application, ApplicationValidationErrors validationErrors) {
    if (Strings.isNullOrEmpty(Optional.ofNullable(application.getName()).orElse("").trim())) {
      validationErrors.rejectValue(
          "name", "application.name.empty", "Application must have a non-empty name");
    }
  }
}
