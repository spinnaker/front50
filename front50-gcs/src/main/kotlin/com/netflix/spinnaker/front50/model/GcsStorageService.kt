/*
 * Copyright 2020 Google, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 *
 */

package com.netflix.spinnaker.front50.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.BucketInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.Storage.BlobListOption
import com.google.cloud.storage.Storage.BucketField
import com.google.cloud.storage.Storage.BucketGetOption
import com.google.cloud.storage.StorageException
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.netflix.spinnaker.front50.exception.NotFoundException
import java.io.IOException
import java.util.Comparator
import java.util.concurrent.Executor
import java.util.stream.Collectors
import org.slf4j.LoggerFactory

class GcsStorageService(
  private val storage: Storage,
  private val bucketName: String,
  private val bucketLocation: String,
  private val basePath: String,
  private val dataFilename: String,
  private val objectMapper: ObjectMapper,
  private val executor: Executor
) : StorageService {

  companion object {
    private val log = LoggerFactory.getLogger(GcsStorageService::class.java)
    private const val LAST_MODIFIED_FILENAME = "last-modified"
  }

  private val modTimeState = ObjectType.values().map { it to ModificationTimeState(it) }.toMap()

  fun ensureBucketExists() {
    val bucket = storage.get(bucketName)
    if (bucket == null) {
      val bucketInfo = BucketInfo.newBuilder(bucketName)
        .setVersioningEnabled(true)
      if (bucketLocation.isNotBlank()) {
        bucketInfo.setLocation(bucketLocation)
      }
      storage.create(bucketInfo.build())
    }
  }

  override fun supportsVersioning(): Boolean {
    val bucket = storage.get(bucketName, BucketGetOption.fields(BucketField.VERSIONING))
    return bucket.versioningEnabled() == true
  }

  override fun <T : Timestamped> loadObject(objectType: ObjectType, objectKey: String): T {
    try {
      val blobId = blobIdForKey(objectType, objectKey)
      val blob = storage.get(blobId)
        ?: throw NotFoundException("Couldn't retrieve $objectType $objectKey from GCS")
      val bytes = storage.readAllBytes(blob.blobId)
      val obj: T = parseObject(bytes, objectType, objectKey)
      obj.lastModified = blob.updateTime
      return obj
    } catch (e: StorageException) {
      throw GcsStorageServiceException("error loading $objectType $objectKey", e)
    }
  }

  override fun deleteObject(objectType: ObjectType, objectKey: String) {
    try {
      if (storage.delete(blobIdForKey(objectType, objectKey))) {
        writeLastModified(objectType)
      }
    } catch (e: Exception) {
      throw GcsStorageServiceException("error deleting $objectType $objectKey", e)
    }
  }

  override fun <T : Timestamped?> storeObject(objectType: ObjectType, objectKey: String, item: T) {
    val blobId = blobIdForKey(objectType, objectKey)
    try {
      val bytes = objectMapper.writeValueAsBytes(item)
      storage.create(BlobInfo.newBuilder(blobId).setContentType("application/json").build(), bytes)
      writeLastModified(objectType)
    } catch (e: Exception) {
      throw GcsStorageServiceException("Error writing $objectType $objectKey", e)
    }
  }

  override fun listObjectKeys(objectType: ObjectType): MutableMap<String, Long> {
    val results = ImmutableMap.builder<String, Long>()

    try {
      val rootDirectory = daoRoot(objectType)
      storage.list(bucketName, BlobListOption.prefix("$rootDirectory/"))
        .iterateAll()
        .forEach { blob ->
          val objectKey = getObjectKey(blob, rootDirectory)
          if (objectKey != null) {
            results.put(objectKey, blob.updateTime)
          }
        }
    } catch (e: Exception) {
      throw GcsStorageServiceException("error listing $objectType objects", e)
    }

    return results.build()
  }

  private fun getObjectKey(blob: Blob, rootDirectory: String): String? {
    val name = blob.name
    return if (!name.startsWith("$rootDirectory/") || !name.endsWith("/$dataFilename")) {
      null
    } else name.substring(rootDirectory.length + 1, name.length - dataFilename.length - 1)
  }

  override fun <T : Timestamped> listObjectVersions(
    objectType: ObjectType,
    objectKey: String,
    maxResults: Int
  ): MutableCollection<T> {

    try {
      val path = pathForKey(objectType, objectKey)
      val listResults = storage.list(bucketName, BlobListOption.prefix(path), BlobListOption.versions(true))
      val blobs: List<Blob> = ImmutableList.copyOf(listResults.iterateAll())
      return blobs.stream()
        .filter { blob: Blob -> blob.name == path }
        .sorted(Comparator.comparing { blob: Blob -> blob.updateTime }.reversed())
        .limit(maxResults.toLong())
        .map { blob: Blob ->
          parseObject<T>(blob.getContent(), objectType, objectKey)
            .apply { lastModified = blob.updateTime }
        }
        .collect(Collectors.toList())
    } catch (e: Exception) {
      throw GcsStorageServiceException("error loading $objectType $objectKey", e)
    }
  }

  override fun getLastModified(objectType: ObjectType): Long {
    try {
      val blob = storage.get(lastModifiedBlobId(objectType))
      if (blob == null) {
        writeLastModified(objectType)
        return 0
      } else {
        return blob.updateTime
      }
    } catch (e: Exception) {
      throw GcsStorageServiceException("error loading timestamp for $objectType objects", e)
    }
  }

  private fun writeLastModified(objectType: ObjectType) {
    val updaterState = modTimeState.getValue(objectType)
    updaterState.updateNeeded()
  }

  private fun updateLastModified(objectType: ObjectType, state: ModificationTimeState) {
    state.updateTaskStarted()
    val blobInfo = BlobInfo.newBuilder(lastModifiedBlobId(objectType)).build()
    try {
      // Calling update() is enough to change the modification time on the file, which is all we
      // care about. It doesn't matter if we don't actually specify any fields to change.
      storage.update(blobInfo)
    } catch (e: Exception) {
      when {
        e is StorageException && e.code == 404 ->
          try {
            storage.create(blobInfo)
          } catch (e: Exception) {
            log.warn("Error updating last modified time for $objectType", e)
          }
        else -> log.warn("Error updating last modified time for $objectType", e)
      }
    } finally {
      state.updateTaskFinished()
    }
  }

  private fun scheduleUpdateLastModified(objectType: ObjectType, updaterState: ModificationTimeState) {
    executor.execute { updateLastModified(objectType, updaterState) }
  }

  private inner class ModificationTimeState(private var objectType: ObjectType) {

    private var needsUpdate = false
    private var updateRunning = false

    /**
     * Specify that an object has been modified so the modification time for this objectType needs
     * to be updated.
     */
    @Synchronized
    fun updateNeeded() {
      val neededUpdate = needsUpdate
      needsUpdate = true
      // We need to check `neededUpdate` or else several near-simultaneous calls would schedule
      // multiple executions of the task before any of them have a chance to get up and running
      if (!neededUpdate && !updateRunning) {
        scheduleUpdateLastModified(objectType, this)
      }
    }

    /** Should be called by the update task to indicate it has started. */
    @Synchronized
    fun updateTaskStarted() {
      updateRunning = true
      needsUpdate = false
    }

    /** Should be called by the update task to indicate it has completed. */
    @Synchronized
    fun updateTaskFinished() {
      updateRunning = false
      // If another update request came in while the task was running, we need to launch the task
      // again.
      if (needsUpdate) {
        scheduleUpdateLastModified(objectType, this)
      }
    }
  }

  private fun <T : Timestamped> parseObject(bytes: ByteArray, objectType: ObjectType, objectKey: String): T {
    return try {
      @Suppress("UNCHECKED_CAST")
      objectMapper.readValue(bytes, objectType.clazz as Class<T>)
    } catch (e: IOException) {
      throw GcsStorageServiceException("error reading $objectType $objectKey", e)
    }
  }

  private fun blobIdForKey(objectType: ObjectType, key: String): BlobId {
    return BlobId.of(bucketName, pathForKey(objectType, key))
  }

  private fun pathForKey(objectType: ObjectType, key: String): String {
    return "${daoRoot(objectType)}/$key/$dataFilename"
  }

  private fun lastModifiedBlobId(objectType: ObjectType): BlobId {
    return BlobId.of(bucketName, "${daoRoot(objectType)}/$LAST_MODIFIED_FILENAME")
  }

  private fun daoRoot(objectType: ObjectType): String {
    return "$basePath/${objectType.group}"
  }
}

private class GcsStorageServiceException(message: String, cause: Throwable) : RuntimeException(message, cause)
