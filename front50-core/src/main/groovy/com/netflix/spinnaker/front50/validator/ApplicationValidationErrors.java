package com.netflix.spinnaker.front50.validator;

import com.netflix.spinnaker.front50.UntypedUtils;
import com.netflix.spinnaker.front50.model.application.Application;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.springframework.validation.AbstractErrors;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

public class ApplicationValidationErrors extends AbstractErrors {
  public ApplicationValidationErrors(Application application) {
    this.application = application;
  }

  @Nonnull
  @Override
  public String getObjectName() {
    return application.getClass().getSimpleName();
  }

  @Override
  public void reject(@Nonnull String errorCode, Object[] errorArgs, String defaultMessage) {
    globalErrors.add(
        new ObjectError(getObjectName(), new String[] {errorCode}, errorArgs, defaultMessage));
  }

  @Override
  public void rejectValue(
      String field, @Nonnull String errorCode, Object[] errorArgs, String defaultMessage) {
    fieldErrors.add(
        new FieldError(
            getObjectName(),
            field,
            getFieldValue(field),
            false,
            new String[] {errorCode},
            errorArgs,
            defaultMessage));
  }

  @Override
  public void addAllErrors(Errors errors) {
    globalErrors.addAll(errors.getAllErrors());
  }

  @Override
  public Object getFieldValue(@Nonnull String field) {
    return UntypedUtils.getProperty(application, field);
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  @Nonnull
  @Override
  public List<ObjectError> getGlobalErrors() {
    return globalErrors;
  }

  public void setGlobalErrors(List<ObjectError> globalErrors) {
    this.globalErrors = globalErrors;
  }

  @Nonnull
  @Override
  public List<FieldError> getFieldErrors() {
    return fieldErrors;
  }

  public void setFieldErrors(List<FieldError> fieldErrors) {
    this.fieldErrors = fieldErrors;
  }

  private Application application;
  private List<ObjectError> globalErrors = new ArrayList<ObjectError>();
  private List<FieldError> fieldErrors = new ArrayList<FieldError>();
}
