package com.netflix.spinnaker.front50.validator;

import com.google.common.base.Strings;
import com.netflix.spinnaker.front50.UntypedUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.springframework.validation.AbstractErrors;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

public class GenericValidationErrors extends AbstractErrors {

  private final Object source;
  private final List<ObjectError> globalErrors = new ArrayList<>();
  private final List<FieldError> fieldErrors = new ArrayList<>();

  public GenericValidationErrors(Object source) {
    this.source = source;
  }

  @Nonnull
  @Override
  public String getObjectName() {
    final Class<?> clazz = source.getClass();
    final String name = (clazz == null ? null : clazz.getSimpleName());
    return Strings.isNullOrEmpty(name) ? "unknown" : name;
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
    return UntypedUtils.getProperty(source, field);
  }

  @Nonnull
  @Override
  public final List<ObjectError> getGlobalErrors() {
    return globalErrors;
  }

  @Nonnull
  @Override
  public final List<FieldError> getFieldErrors() {
    return fieldErrors;
  }
}
