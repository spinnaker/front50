package com.netflix.spinnaker.front50.controllers.v2

import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.front50.events.ApplicationEventListener
import com.netflix.spinnaker.front50.controllers.exception.InvalidApplicationRequestException
import com.netflix.spinnaker.front50.model.application.Application
import com.netflix.spinnaker.front50.model.application.ApplicationDAO
import com.netflix.spinnaker.front50.model.notification.NotificationDAO
import com.netflix.spinnaker.front50.model.pipeline.PipelineDAO
import com.netflix.spinnaker.front50.model.pipeline.PipelineStrategyDAO
import com.netflix.spinnaker.front50.model.project.ProjectDAO
import com.netflix.spinnaker.front50.validator.ApplicationValidator
import groovy.util.logging.Slf4j
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletResponse

@Slf4j
@RestController
@RequestMapping("/v2/applications")
@Api(value = "application", description = "Application API")
public class ApplicationsController {
  @Autowired
  MessageSource messageSource

  @Autowired
  ApplicationDAO applicationDAO

  @Autowired
  ProjectDAO projectDAO

  @Autowired
  NotificationDAO notificationDAO

  @Autowired
  PipelineDAO pipelineDAO

  @Autowired
  PipelineStrategyDAO pipelineStrategyDAO

  @Autowired
  List<ApplicationValidator> applicationValidators

  @Autowired(required = false)
  List<ApplicationEventListener> applicationEventListeners = []

  @Autowired
  Registry registry

  @PreAuthorize("@fiatPermissionEvaluator.storeWholePermission()")
  @PostFilter("hasPermission(filterObject.name, 'APPLICATION', 'READ')")
  @ApiOperation(value = "", notes = """Fetch all applications.

    Supports filtering by one or more attributes:
    - ?email=my@email.com
    - ?email=my@email.com&name=flex""")
  @RequestMapping(method = RequestMethod.GET)
  Set<Application> applications(@RequestParam Map<String, String> params) {
    if (params.isEmpty()) {
      return applicationDAO.all()
    }

    return applicationDAO.search(params)
  }

  // TODO(ttomsu): Think through application creation permissions.
  @ApiOperation(value = "", notes = "Create an application")
  @RequestMapping(method = RequestMethod.POST)
  Application create(@RequestBody final Application app) {
    return getApplication().initialize(app).withName(app.getName()).save()
  }

  @PreAuthorize("hasPermission(#applicationName, 'APPLICATION', 'WRITE')")
  @ApiOperation(value = "", notes = "Delete an application")
  @RequestMapping(method = RequestMethod.DELETE, value = "/{applicationName}")
  void delete(@PathVariable String applicationName, HttpServletResponse response) {
    getApplication().initialize(new Application().withName(applicationName)).delete()
    response.setStatus(HttpStatus.NO_CONTENT.value())
  }

  @PreAuthorize("hasPermission(#app.name, 'APPLICATION', 'WRITE')")
  @ApiOperation(value = "", notes = "Update an existing application")
  @RequestMapping(method = RequestMethod.PATCH, value = "/{applicationName}")
  Application update(@PathVariable String applicationName, @RequestBody final Application app) {
    if (!applicationName.trim().equalsIgnoreCase(app.getName())) {
      throw new InvalidApplicationRequestException("Application name '${app.getName()}' does not match path parameter '${applicationName}'")
    }

    def application = getApplication()
    Application existingApplication = application.findByName(app.getName())
    application.initialize(existingApplication).withName(app.getName()).update(app)
    return app
  }

  // This method uses @PostAuthorize in order to throw 404s if the application doesn't exist,
  // vs. 403s if the app exists, but the user doesn't have access to it.
  @PostAuthorize("hasPermission(#applicationName, 'APPLICATION', 'READ')")
  @ApiOperation(value = "", notes = "Fetch a single application by name")
  @RequestMapping(method = RequestMethod.GET, value = "/{applicationName}")
  Application get(@PathVariable final String applicationName) {
    return applicationDAO.findByName(applicationName.toUpperCase())
  }

  @PreAuthorize("hasPermission(#applicationName, 'APPLICATION', 'READ')")
  @RequestMapping(value = '{applicationName}/history', method = RequestMethod.GET)
  Collection<Application> getHistory(@PathVariable String applicationName,
                                     @RequestParam(value = "limit", defaultValue = "20") int limit) {
    return applicationDAO.getApplicationHistory(applicationName, limit)
  }

  @PreAuthorize("@fiatPermissionEvaluator.isAdmin()")
  @RequestMapping(method = RequestMethod.POST, value = "/batch/applications")
  void batchUpdate(@RequestBody final Collection<Application> applications) {
    applicationDAO.bulkImport(applications)
  }

  @ExceptionHandler(Application.ValidationException)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  Map handleValidationException(Application.ValidationException ex) {
    def locale = LocaleContextHolder.locale
    def errorStrings = []
    ex.errors.each { Errors errors ->
      errors.allErrors.each { ObjectError objectError ->
        def message = messageSource.getMessage(objectError.code, objectError.arguments, objectError.defaultMessage, locale)
        errorStrings << message
      }
    }
    return [error: "Validation Failed.", errors: errorStrings, status: HttpStatus.BAD_REQUEST]
  }

  @ExceptionHandler(AccessDeniedException)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  Map handleAccessDeniedException(AccessDeniedException ade) {
    return [error: "Access is denied", status: HttpStatus.FORBIDDEN.value()]
  }

  private Application getApplication() {
    return new Application(
      dao: applicationDAO,
      projectDao: projectDAO,
      notificationDao: notificationDAO,
      pipelineDao: pipelineDAO,
      pipelineStrategyDao: pipelineStrategyDAO,
      validators: applicationValidators,
      applicationEventListeners: applicationEventListeners
    )
  }
}
