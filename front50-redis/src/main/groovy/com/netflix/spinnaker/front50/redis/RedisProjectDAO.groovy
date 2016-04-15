/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.front50.redis
import com.netflix.spinnaker.front50.exception.NotFoundException
import com.netflix.spinnaker.front50.model.project.Project
import com.netflix.spinnaker.front50.model.project.ProjectDAO
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.util.Assert

/**
 * @author Greg Turnquist
 */
class RedisProjectDAO implements ProjectDAO {

  RedisTemplate<String, Project> template

  RedisConnectionFactory factory

  @Override
  Project findByName(String name) throws NotFoundException {
    def results = template.opsForValue().get(key(name))
    if (!results) {
      throw new NotFoundException("No Project found by name of ${name}")
    }
    return results
  }

  @Override
  void truncate() {
  }

  @Override
  Project findById(String id) throws NotFoundException {
    def results = template.opsForValue().get(key(id))
    if (!results) {
      throw new NotFoundException("No Project found by id of ${id}")
    }
    results
  }

  @Override
  Collection<Project> all() {
    template.opsForValue().multiGet(template.keys(key('*')))
  }

  @Override
  Project create(String id, Project item) {
    item.id = id
    if (!item.id) {
      item.id = UUID.randomUUID().toString()
    }
    if (!item.createTs) {
      item.createTs = System.currentTimeMillis()
    }
    template.opsForValue().set(key(item.id), item)
    item
  }

  @Override
  void update(String id, Project item) {
    item.updateTs = System.currentTimeMillis()
    create(id, item)
  }

  @Override
  void delete(String id) {
    template.delete(key(id))
  }

  @Override
  void bulkImport(Collection<Project> items) {
    items.each { create(it.id, it) }
  }

  @Override
  boolean isHealthy() {
    try {
      def conn = factory.connection
      conn.close()
      return true
    } catch (Exception e) {
      return false
    }
  }

  void deleteAll() {
    template.delete(template.keys(key('*')))
  }

  private static String key(String id) {
    Assert.notNull(id, 'id can NOT be null!')
    "projects:${id}".toString()
  }

}
