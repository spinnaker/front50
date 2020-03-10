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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.front50.exception.NotFoundException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A storage service that keeps all objects in memory. In order to simulate eventual consistency,
 * mutating operations are not reflected by accessors until the {@code flush()} method is called on
 * the storage service.
 */
@NotThreadSafe
public class EventuallyConsistentStorageService implements StorageService {
  private final Map<ObjectType, ObjectStore<?>> storage = new HashMap<>();
  private final Queue<Runnable> pendingOperations = new LinkedList<>();

  /** Make any pending mutations to the repository visible to read operations. */
  public void flush() {
    while (!pendingOperations.isEmpty()) {
      pendingOperations.remove().run();
    }
  }

  @Override
  public void ensureBucketExists() {}

  @Override
  public boolean supportsVersioning() {
    return false;
  }

  @Override
  public <T extends Timestamped> T loadObject(ObjectType objectType, String objectKey)
      throws NotFoundException {
    // We are forced to do an unchecked cast here as ObjectType is not parametrized with its
    // class type (and cannot be given that it is an enum).
    @SuppressWarnings("unchecked")
    T result = (T) getObjectStore(objectType).get(objectKey);
    return result;
  }

  @Override
  public void deleteObject(ObjectType objectType, String objectKey) {
    pendingOperations.add(() -> getObjectStore(objectType).delete(objectKey));
  }

  @Override
  public <T extends Timestamped> void storeObject(ObjectType objectType, String objectKey, T item) {
    pendingOperations.add(
        () -> {
          // We are forced to do an unchecked cast here as ObjectType is not parametrized with its
          // class type (and cannot be given that it is an enum).
          @SuppressWarnings("unchecked")
          ObjectStore<T> store = (ObjectStore<T>) getObjectStore(objectType);
          store.put(objectKey, item);
        });
  }

  @Override
  public Map<String, Long> listObjectKeys(ObjectType objectType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends Timestamped> Collection<T> listObjectVersions(
      ObjectType objectType, String objectKey, int maxResults) throws NotFoundException {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLastModified(ObjectType objectType) {
    throw new UnsupportedOperationException();
  }

  private <T extends Timestamped> ObjectStore<? extends Timestamped> getObjectStore(
      ObjectType objectType) {
    return storage.computeIfAbsent(objectType, o -> new ObjectStore<>(objectType.clazz));
  }

  @NotThreadSafe
  private static final class ObjectStore<T extends Timestamped> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Class<T> objectClass;
    private final Map<String, String> store = new HashMap<>();

    ObjectStore(Class<T> objectClass) {
      this.objectClass = objectClass;
    }

    public void put(String objectKey, T item) {
      String serializedObject;
      try {
        serializedObject = objectMapper.writeValueAsString(item);
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
      store.put(objectKey, serializedObject);
    }

    public T get(String objectKey) {
      String serializedObject = store.get(objectKey);
      if (serializedObject == null) {
        throw new NotFoundException(String.format("Object with key %s not found", objectKey));
      }
      try {
        return objectMapper.readValue(serializedObject, objectClass);
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    }

    public void delete(String objectKey) {
      store.remove(objectKey);
    }
  }
}
