/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.front50.controllers

import com.netflix.spinnaker.fiat.shared.FiatClientConfigurationProperties
import com.netflix.spinnaker.fiat.shared.FiatService
import com.netflix.spinnaker.front50.model.serviceaccount.ServiceAccount
import com.netflix.spinnaker.front50.model.serviceaccount.ServiceAccountDAO
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import retrofit.RetrofitError

@Slf4j
@RestController
@RequestMapping("/serviceAccounts")
@ConditionalOnExpression('${spinnaker.gcs.enabled:false} || ${spinnaker.s3.enabled:false} || ${spinnaker.azs.enabled:false}')
public class ServiceAccountsController {

  @Autowired
  ServiceAccountDAO serviceAccountDAO;

  @Autowired(required = false)
  FiatService fiatService

  @Autowired
  FiatClientConfigurationProperties fiatClientConfigurationProperties

  @RequestMapping(method = RequestMethod.GET)
  Set<ServiceAccount> getAllServiceAccounts() {
    serviceAccountDAO.all();
  }

  @RequestMapping(method = RequestMethod.POST)
  ServiceAccount createServiceAccount(@RequestBody ServiceAccount serviceAccount) {
    def acct = serviceAccountDAO.create(serviceAccount.id, serviceAccount)
    syncUsers(acct)
    return acct
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "/{serviceAccountId:.+}")
  void deleteServiceAccount(@PathVariable String serviceAccountId) {
    def acct = serviceAccountDAO.findById(serviceAccountId)
    serviceAccountDAO.delete(serviceAccountId)
    syncUsers(acct)
  }

  private void syncUsers(ServiceAccount serviceAccount) {
    if (!fiatClientConfigurationProperties.enabled || !fiatService || !serviceAccount) {
      return
    }

    def roles = [StringUtils.substringBefore(serviceAccount.name.replaceAll('%40', "@"), "@")]
    try {
      fiatService.sync(roles)
      log.debug("Synced users with roles $roles")
    } catch (RetrofitError re) {
      log.warn("Error syncing users", re)
    }
  }
}
