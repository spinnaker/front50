package com.netflix.spinnaker.front50.migrations

import com.netflix.spinnaker.fiat.model.Authorization
import com.netflix.spinnaker.fiat.model.resources.Permissions
import com.netflix.spinnaker.front50.model.application.Application
import com.netflix.spinnaker.front50.model.application.ApplicationPermissionDAO
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ApplicationExecutePermissionMigrationSpec extends Specification {

  def applicationPermissionDao = Mock(ApplicationPermissionDAO)

  @Subject
  def migration = new ApplicationExecutePermissionMigration(applicationPermissionDao)

  def setup() {
    migration.setClock(Clock.fixed(Instant.parse("2019-04-01T10:15:30.00Z"), ZoneId.of("Z")))
  }

  @Unroll
  def "should #shouldRun migration if time is #date"() {
    given:
    migration.setClock(Clock.fixed(Instant.parse(date), ZoneId.of("Z")))

    when:
    def valid = migration.isValid()

    then:
    valid == expectedValid

    where:
    date                      || expectedValid
    "2019-04-01T10:15:30.00Z" || true
    "2020-03-31T23:59:59.99Z" || true
    "2020-04-01T00:00:00.00Z" || false
    "2020-04-02T10:15:30.00Z" || false
    shouldRun = "${expectedValid ? '' : 'not '}run"

  }

  def "should not migrate unrestricted applications"() {
    given:
    def applicationPermission = new Application.Permission(name: "foo", permissions: givenPermissions)

    when:
    migration.run()

    then:
    1 * applicationPermissionDao.all() >> [applicationPermission]
    0 * applicationPermissionDao.update("foo", applicationPermission)

    applicationPermission.permissions == expectedPermissions

    where:
    givenPermissions       || expectedPermissions
    null                   || null
    Permissions.EMPTY      || Permissions.EMPTY

  }

  def "should not migrate an application if it already contains an EXECUTE permission"() {
    given:
    def builder = new Permissions.Builder()
    builder.add(Authorization.READ, "readers")
    builder.add(Authorization.WRITE, "writers")
    builder.add(Authorization.EXECUTE, "executors")
    def permissions = builder.build()

    def applicationPermission = new Application.Permission(name: "foo", permissions: permissions)

    when:
    migration.run()

    then:
    1 * applicationPermissionDao.all() >> [applicationPermission]
    0 * applicationPermissionDao.update("foo", applicationPermission)
    applicationPermission.permissions == permissions
  }

  def "should add EXECUTE permissions to all groups with READ or WRITE permission"() {
    given:
    def builder = new Permissions.Builder()
    builder.add(Authorization.READ, "readers")
    builder.add(Authorization.WRITE, "writers")

    def initialPermissions = builder.build()

    builder.add(Authorization.EXECUTE, ["readers", "writers"])
    def expectedPermissions = builder.build()

    def applicationPermission = new Application.Permission(
        name: "foo",
        permissions: initialPermissions
    )

    when:
    migration.run()

    then:
    1 * applicationPermissionDao.all() >> [applicationPermission]
    1 * applicationPermissionDao.update("foo", applicationPermission)

    applicationPermission.permissions == expectedPermissions
  }
}