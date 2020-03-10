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

package com.netflix.spinnaker.front50.model;

import com.netflix.spinnaker.front50.exception.NotFoundException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface StorageService {
  /** Check to see if the bucket exists, creating it if it is not there. */
  void ensureBucketExists();

  /** @return true if the storage service supports versioning. */
  boolean supportsVersioning();

  default boolean supportsEventing(ObjectType objectType) {
    return true;
  }

  <T extends Timestamped> T loadObject(ObjectType objectType, String objectKey)
      throws NotFoundException;

  default <T extends Timestamped> List<T> loadObjects(
      ObjectType objectType, List<String> objectKeys) {
    throw new UnsupportedOperationException();
  }

  void deleteObject(ObjectType objectType, String objectKey);

  default void bulkDeleteObjects(ObjectType objectType, Collection<String> objectKeys) {
    for (String objectKey : objectKeys) {
      deleteObject(objectType, objectKey);
    }
  }

  /**
   * Stores the provided object into the persistent store, using the provided key.
   *
   * <p>Implementations may modify the object before storing it (such as to add timestamps) but must
   * do so on the provided object so that the input object reflects exactly what was stored in the
   * persistent store upon returning from this function.
   *
   * <p>In other words, given the sequence of calls
   *
   * <pre>{@code
   * storeObject(objectType, objectKey, item)
   * newItem = loadObject(objectType, objectKey)
   * }</pre>
   *
   * the implementation must guarantee that {@code newItem.equals(item)}.
   *
   * <p>This, in particular, precludes implementations from copying/serializing the input item and
   * mutating that copy/serialization before storing it. It also precludes implementations that have
   * triggers at the storage layer that mutate items upon storing.
   *
   * @param objectType The type of object to store
   * @param objectKey The key under which to store the object
   * @param item The object to store
   * @param <T> The type of object to store
   */
  <T extends Timestamped> void storeObject(ObjectType objectType, String objectKey, T item);

  Map<String, Long> listObjectKeys(ObjectType objectType);

  <T extends Timestamped> Collection<T> listObjectVersions(
      ObjectType objectType, String objectKey, int maxResults) throws NotFoundException;

  long getLastModified(ObjectType objectType);

  default long getHealthIntervalMillis() {
    return Duration.ofSeconds(30).toMillis();
  }
}
