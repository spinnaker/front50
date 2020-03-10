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

package com.netflix.spinnaker.front50.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.netflix.spinnaker.front50.exception.NotFoundException;
import com.netflix.spinnaker.front50.model.application.Application;
import com.netflix.spinnaker.front50.model.pipeline.Pipeline;
import org.junit.jupiter.api.Test;

public class EventuallyConsistentStorageServiceTest {
  private static final String KEY = "my-key";
  private static final String OTHER_KEY = "my-other-key";

  @Test
  void emptyRepository() {
    EventuallyConsistentStorageService storageService = new EventuallyConsistentStorageService();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> storageService.loadObject(ObjectType.APPLICATION, KEY));
  }

  @Test
  void storeAndRetreive() {
    EventuallyConsistentStorageService storageService = new EventuallyConsistentStorageService();

    Application application = new Application();
    application.setName("my-app");
    application.setDescription("my-description");

    storageService.storeObject(ObjectType.APPLICATION, KEY, application);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> storageService.loadObject(ObjectType.APPLICATION, KEY));

    storageService.flush();

    Application result = storageService.loadObject(ObjectType.APPLICATION, KEY);
    assertThat(result.getName()).isEqualTo(application.getName());
    assertThat(result.getDescription()).isEqualTo(application.getDescription());
  }

  @Test
  void storeAndUpdate() {
    EventuallyConsistentStorageService storageService = new EventuallyConsistentStorageService();

    Application application = new Application();
    application.setName("my-app");
    application.setDescription("my-description");

    storageService.storeObject(ObjectType.APPLICATION, KEY, application);
    storageService.flush();

    Application newApplication = new Application();
    newApplication.setName("new-app");
    newApplication.setDescription("new-description");

    storageService.storeObject(ObjectType.APPLICATION, KEY, newApplication);

    Application result = storageService.loadObject(ObjectType.APPLICATION, KEY);
    assertThat(result.getName()).isEqualTo(application.getName());
    assertThat(result.getDescription()).isEqualTo(application.getDescription());

    storageService.flush();

    Application newResult = storageService.loadObject(ObjectType.APPLICATION, KEY);
    assertThat(newResult.getName()).isEqualTo(newApplication.getName());
    assertThat(newResult.getDescription()).isEqualTo(newApplication.getDescription());
  }

  @Test
  void storeAndDelete() {
    EventuallyConsistentStorageService storageService = new EventuallyConsistentStorageService();

    Application application = new Application();
    application.setName("my-app");
    application.setDescription("my-description");
    storageService.storeObject(ObjectType.APPLICATION, KEY, application);
    storageService.flush();

    storageService.deleteObject(ObjectType.APPLICATION, KEY);

    Application result = storageService.loadObject(ObjectType.APPLICATION, KEY);
    assertThat(result.getName()).isEqualTo(application.getName());
    assertThat(result.getDescription()).isEqualTo(application.getDescription());

    storageService.flush();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> storageService.loadObject(ObjectType.APPLICATION, KEY));
  }

  @Test
  void sameTypeDifferentKeys() {
    EventuallyConsistentStorageService storageService = new EventuallyConsistentStorageService();

    Application application = new Application();
    application.setName("my-app");
    application.setDescription("my-description");

    storageService.storeObject(ObjectType.APPLICATION, KEY, application);
    storageService.flush();

    Application newApplication = new Application();
    newApplication.setName("new-app");
    newApplication.setDescription("new-description");

    storageService.storeObject(ObjectType.APPLICATION, OTHER_KEY, newApplication);
    storageService.flush();

    Application result = storageService.loadObject(ObjectType.APPLICATION, KEY);
    assertThat(result.getName()).isEqualTo(application.getName());
    assertThat(result.getDescription()).isEqualTo(application.getDescription());

    Application otherResult = storageService.loadObject(ObjectType.APPLICATION, OTHER_KEY);
    assertThat(otherResult.getName()).isEqualTo(newApplication.getName());
    assertThat(otherResult.getDescription()).isEqualTo(newApplication.getDescription());
  }

  @Test
  void sameKeyDifferentTypes() {
    EventuallyConsistentStorageService storageService = new EventuallyConsistentStorageService();

    Application application = new Application();
    application.setName("my-app");
    application.setDescription("my-description");
    storageService.storeObject(ObjectType.APPLICATION, KEY, application);
    storageService.flush();

    Pipeline pipeline = new Pipeline();
    pipeline.setName("my-pipeline");
    storageService.storeObject(ObjectType.PIPELINE, KEY, pipeline);
    storageService.flush();

    Application applicationResult = storageService.loadObject(ObjectType.APPLICATION, KEY);
    assertThat(applicationResult.getName()).isEqualTo(application.getName());
    assertThat(applicationResult.getDescription()).isEqualTo(application.getDescription());

    Pipeline pipelineResult = storageService.loadObject(ObjectType.PIPELINE, KEY);
    assertThat(pipelineResult.getName()).isEqualTo(pipeline.getName());
  }
}
