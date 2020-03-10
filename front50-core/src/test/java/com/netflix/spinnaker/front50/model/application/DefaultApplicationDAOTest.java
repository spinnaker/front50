/*
 * Copyright 2020 Google, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.front50.model.application;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.netflix.spectator.api.NoopRegistry;
import com.netflix.spinnaker.front50.exception.NotFoundException;
import com.netflix.spinnaker.front50.model.DefaultObjectKeyLoader;
import com.netflix.spinnaker.front50.model.EventuallyConsistentStorageService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import rx.schedulers.Schedulers;

public class DefaultApplicationDAOTest {
  private static final String KEY = "my-key";

  @Test
  void eventuallyConsistentStore() {
    EventuallyConsistentStorageService storageService = new EventuallyConsistentStorageService();
    ApplicationDAO applicationDAO =
        new DefaultApplicationDAO(
            storageService,
            Schedulers.from(Executors.newFixedThreadPool(1)),
            new DefaultObjectKeyLoader(storageService),
            0,
            false,
            new NoopRegistry());

    Application application = new Application();
    application.setName("my-app");
    application.setDescription("my-description");

    // Documenting a bug that currently exists; in the event of an eventually consistent storage
    // service, application creation will fail because we attempt to fetch the newly-created object
    // as part of the creation call.
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> applicationDAO.create(KEY, application));
  }
}
