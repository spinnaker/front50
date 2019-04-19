package com.netflix.spinnaker.front50.migrations;

import com.netflix.spinnaker.fiat.model.Authorization;
import com.netflix.spinnaker.fiat.model.resources.Permissions;
import com.netflix.spinnaker.front50.model.application.Application;
import com.netflix.spinnaker.front50.model.application.ApplicationPermissionDAO;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;

@Component
@Slf4j
public class ApplicationExecutePermissionMigration implements Migration {

  // Only valid until May 1st, 2020
  private static final LocalDate VALID_UNTIL = LocalDate.of(2020, Month.APRIL, 1);

  private ApplicationPermissionDAO applicationPermissionDAO;

  @Setter
  private Clock clock = Clock.systemDefaultZone();

  @Autowired
  public ApplicationExecutePermissionMigration(
      ApplicationPermissionDAO applicationPermissionDAO) {
    this.applicationPermissionDAO = applicationPermissionDAO;
  }

  @Override
  public boolean isValid() {
    return LocalDate.now(clock).isBefore(VALID_UNTIL);
  }

  @Override
  public void run() {
    log.info("Starting migration of applications to include execute permissions ({})", this.getClass().getSimpleName());

    applicationPermissionDAO.all().stream()
        .filter(ap -> ap.getPermissions()!= null && ap.getPermissions().isRestricted()) // Ignore unrestricted applications
        .filter(this::executeNotPresent) // Ignore applications that already have EXECUTE set
        .forEach(app -> migrate(applicationPermissionDAO, app));
  }

  private boolean executeNotPresent(Application.Permission app) {
    return CollectionUtils.isEmpty(app.getPermissions().get(Authorization.EXECUTE));
  }

  private void migrate(ApplicationPermissionDAO applicationPermissionDAO, Application.Permission app) {
    log.info("Adding execute permissions to application: {} for users in groups: {}", app.getId(), app.getPermissions().allGroups());
    Permissions permissions = app.getPermissions();
    Permissions.Builder builder = new Permissions.Builder();
    builder.add(Authorization.READ, permissions.get(Authorization.READ));
    builder.add(Authorization.WRITE, permissions.get(Authorization.WRITE));

    // Currently, anyone with READ permission can execute a pipeline and WRITE implies a READ.
    // So, we'll give EXECUTE permissions to all groups that have READ or WRITE on an application.
    builder.add(Authorization.EXECUTE, new ArrayList<>(permissions.allGroups()));
    app.setPermissions(builder.build());
    applicationPermissionDAO.update(app.getId(), app);
  }
}
