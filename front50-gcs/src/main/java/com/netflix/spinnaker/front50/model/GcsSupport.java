/*
 * Copyright 2016 Google, Inc.
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

import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.*;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.util.DateTime;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.front50.exception.NotFoundException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.Long;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class GcsSupport<T extends Timestamped> {
  private static final String LAST_MODIFIED_FILENAME = "last-modified";
  private static final long HEALTH_MILLIS = 45000;
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ObjectMapper objectMapper;
  private final Storage storage;
  private final Scheduler scheduler;
  private final int refreshIntervalMs;
  private final String bucket;

  private long lastRefreshedTime;

  protected final String rootFolder;

  protected final AtomicReference<Set<T>> allItemsCache = new AtomicReference<>();

  public GcsSupport(ObjectMapper objectMapper,
                    Storage storage,
                    Scheduler scheduler,
                    int refreshIntervalMs,
                    String bucket,
                    String rootFolder) {
    this.objectMapper = objectMapper;
    this.storage = storage;
    this.scheduler = scheduler;
    this.refreshIntervalMs = refreshIntervalMs;
    this.bucket = bucket;
    this.rootFolder = rootFolder;
  }

  @PostConstruct
  void startRefresh() {
    Observable
        .timer(refreshIntervalMs, TimeUnit.MILLISECONDS, scheduler)
        .repeat()
        .subscribe(interval -> {
          try {
            log.debug("Refreshing");
            refresh();
          } catch (Exception e) {
            log.error("Unable to refresh: {}", e);
          }
        });
  }

  public Collection<T> all() {
    if (readLastModified() > lastRefreshedTime || allItemsCache.get() == null) {
      // only refresh if there was a modification since our last refresh cycle
      refresh();
    }

    return allItemsCache.get().stream().collect(Collectors.toList());
  }

  /**
   * @return Healthy if refreshed in the past HEALTH_MILLIS
   */
  public boolean isHealthy() {
    return (System.currentTimeMillis() - lastRefreshedTime) < HEALTH_MILLIS
            && allItemsCache.get() != null;
  }

  public T findById(String id) throws NotFoundException {
    try {
      String key = buildStorageKey(id);
      StorageObject storage_object = storage.objects().get(bucket, key).execute();
      T item = deserialize(storage_object, true);
      item.setLastModified(storage_object.getUpdated().getValue());
      return item;
    } catch (HttpResponseException e) {
      if (e.getStatusCode() == 404) {
          throw new NotFoundException(String.format("No item found with id=%s",
                                                    id.toLowerCase()));
      }
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new IllegalStateException(e);

    }
  }

  public Collection<T> allVersionsOf(String id, int limit) throws NotFoundException {
    ArrayList<T> result = new ArrayList<T>();
    try {
      Storage.Objects.List listObjects = storage.objects().list(bucket)
          .setPrefix(buildStorageKey(id))
          .setVersions(true)
          .setMaxResults(new Long(limit));
      com.google.api.services.storage.model.Objects objects;
      do {
          objects = listObjects.execute();
          List<StorageObject> items = objects.getItems();
          if (items != null) {
            for (StorageObject item : items) {
                T have = deserialize(item, false);
                if (have != null) {
                  result.add(have);
                }
            }
          }
          listObjects.setPageToken(objects.getNextPageToken());
      } while (objects.getNextPageToken() != null);
    } catch (IOException e) {
      log.error("Could not fetch versions from Google Cloud Storage: {}", e);
      return new HashSet<>();
    }
    return result;
  }

  public void update(String id, T item) {
    try {
      byte[] bytes = objectMapper.writeValueAsBytes(item);
      StorageObject object = new StorageObject().setBucket(bucket).setName(buildStorageKey(id));
      storage.objects().insert(bucket, object,
                                   new ByteArrayContent("application/json", bytes)).execute();
      writeLastModified();
    } catch (IOException e) {
        log.error("Update failed on id={}: {}", id, e);
        throw new IllegalStateException(e);
    }
  }

  public void delete(String id) {
    try {
      storage.objects().delete(bucket, buildStorageKey(id)).execute();
      writeLastModified();
    } catch (IOException e) {
      log.error("Delete failed on id={}", id);
      throw new IllegalStateException(e);
    }
  }

  public void bulkImport(Collection<T> items) {
    Observable
        .from(items)
        .buffer(10)
        .flatMap(itemSet -> Observable
            .from(itemSet)
            .flatMap(item -> {
              update(item.getId(), item);
              return Observable.just(item);
            })
            .subscribeOn(scheduler)
        ).subscribeOn(scheduler)
        .toList()
        .toBlocking()
        .single();
  }

  /**
   * Update local cache with any recently modified items.
   */
  protected void refresh() {
    allItemsCache.set(fetchAllItems(allItemsCache.get()));
  }

  /**
   * Fetch any previously cached applications that have been updated since last retrieved.
   *
   * @param existingItems Previously cached applications
   * @return Refreshed applications
   */
  protected Set<T> fetchAllItems(Set<T> existingItems) {
    if (existingItems == null) {
      existingItems = new HashSet<>();
    }

    Long refreshTime = System.currentTimeMillis();
    List<StorageObject> summaries = new ArrayList<StorageObject>();

    try {
      Storage.Objects.List listObjects = storage.objects().list(bucket);
      com.google.api.services.storage.model.Objects objects;
      do {
          objects = listObjects.execute();
          List<StorageObject> items = objects.getItems();
          if (items != null) {
            summaries.addAll(items);
          }
          listObjects.setPageToken(objects.getNextPageToken());
      } while (objects.getNextPageToken() != null);
    } catch (IOException e) {
      log.error("Could not fetch items from Google Cloud Storage: {}", e);
      return new HashSet<>();
    }


    Map<String, StorageObject> summariesByName = summaries
        .stream()
        .filter(this::filterStorageObject)
        .collect(Collectors.toMap(StorageObject::getName, Function.identity()));

    Map<String, T> existingItemsByName = existingItems
        .stream()
        .filter(a -> summariesByName.containsKey(buildStorageKey(a)))
        .collect(Collectors.toMap(Timestamped::getId, Function.identity()));

    summaries = summariesByName
        .values()
        .stream()
        .filter(storageObject -> {
          String itemName = extractItemName(storageObject);
          T existingItem = existingItemsByName.get(itemName);

          return existingItem == null || existingItem.getLastModified() == null
                 || storageObject.getUpdated().getValue() > existingItem.getLastModified();
         })
        .collect(Collectors.toList());


    Observable
        .from(summaries)
        .buffer(10)
        .flatMap(ids -> Observable
            .from(ids)
            .flatMap(storageObject -> {
                  try {
                      return Observable.just(storage.objects().get(storageObject.getBucket(),
                                                                   storageObject.getName()).execute());
                  } catch (HttpResponseException e) {
                    if (e.getStatusCode() == 404) {
                      // an item has been removed between the time that object summaries were fetched and now
                      existingItemsByName.remove(extractItemName(storageObject));
                      return Observable.empty();
                    }

                    throw new IllegalStateException(e);
                  } catch (IOException e) {
                    throw new IllegalStateException(e);
                  }
                }
            )
            .subscribeOn(scheduler)
        )
        .map(storageObject -> {
          try {
            T item = deserialize(storageObject, true);
            item.setLastModified(storageObject.getUpdated().getValue());
            return item;
          } catch (IOException e) {
            throw new IllegalStateException(e);
          }
        })
        .subscribeOn(scheduler)
        .toList()
        .toBlocking()
        .single()
        .forEach(item -> {
          existingItemsByName.put(item.getId().toLowerCase(), item);
        });

    existingItems = existingItemsByName.values().stream().collect(Collectors.toSet());
    this.lastRefreshedTime = refreshTime;
    return existingItems;
  }

  protected String buildStorageKey(T item) {
      return buildStorageKey(item.getId());
  }

  protected String buildStorageKey(String id) {
      return rootFolder + id.toLowerCase() + "/" + getMetadataFilename();
  }

  private void writeLastModified() {
      // We'll just touch the file since the StorageObject manages a timestamp.
      byte[] bytes = "{}".getBytes();
      ByteArrayContent content = new ByteArrayContent("application/json", bytes);
      StorageObject object = new StorageObject()
          .setBucket(bucket)
          .setName(rootFolder + LAST_MODIFIED_FILENAME)
          .setUpdated(new DateTime(System.currentTimeMillis()));
      try {
          storage.objects().insert(bucket, object, content).execute();
      } catch (HttpResponseException e) {
          log.error("writeLastModified failed: {}", e);
          try {
              storage.objects().update(bucket, object.getName(), object);
          } catch (IOException ioex) {
              log.error("writeLastModified update failed too: {}", ioex);
              throw new IllegalStateException(ioex);
          }
      } catch (IOException e) {
          log.error("writeLastModified failed: {}", e);
          throw new IllegalStateException(e);
      }
  }

  private Long readLastModified() {
      try {
          return storage.objects().get(bucket, rootFolder + LAST_MODIFIED_FILENAME).execute()
              .getUpdated().getValue();
      } catch (Exception e) {
          return 0L;
      }
  }

  private T deserialize(StorageObject object, boolean current_version) throws java.io.UnsupportedEncodingException {
      try {
          ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
          Storage.Objects.Get getter = storage.objects().get(object.getBucket(), object.getName());
          if (!current_version) {
              getter.setGeneration(object.getGeneration());
          }
          getter.executeMediaAndDownloadTo(output);
          String json = output.toString("UTF8");
          return objectMapper.readValue(json, getSerializedClass());
      } catch (IOException ex) {
          if (current_version) {
            log.error("Error reading {}: {}", object.getName(), ex);
          } else {
            log.error("Error reading {} generation={}: {}",
                      object.getName(), object.getGeneration(), ex);
          }
          return null;
      }
  }

  private boolean filterStorageObject(StorageObject object) {
      return object.getName().endsWith(getMetadataFilename());
  }

  private String extractItemName(StorageObject object) {
      return object.getName().replaceAll(rootFolder, "").replaceAll("/" + getMetadataFilename(), "");
  }

  abstract Class<T> getSerializedClass();

  abstract String getMetadataFilename();
}
