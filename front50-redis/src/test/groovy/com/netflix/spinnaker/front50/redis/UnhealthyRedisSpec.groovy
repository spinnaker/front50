/*
 * Copyright 2019 Netflix, Inc.
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

package com.netflix.spinnaker.front50.redis


import com.netflix.spinnaker.front50.model.pipeline.PipelineStrategyDAO
import com.netflix.spinnaker.front50.model.project.ProjectDAO
import com.netflix.spinnaker.front50.redis.config.UnhealthyRedisConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

@SpringBootTest(classes = [UnhealthyRedisConfig])
@TestPropertySource(properties = ["spinnaker.redis.enabled = true"])
class UnhealthyRedisSpec extends Specification {
  @Autowired
  RedisConnectionFactory redisConnectionFactory

  @Autowired
  RedisApplicationDAO redisApplicationDAO

  @Autowired
  RedisPipelineDAO redisPipelineDAO

  @Autowired
  PipelineStrategyDAO redisPipelineStrategyDAO

  @Autowired
  ProjectDAO redisProjectDAO

  def "applicationDAO should report failing redis connection as not healthy"() {
    when:
    def healthy = redisApplicationDAO.healthy

    then:
    !healthy

    1 * redisApplicationDAO.redisTemplate.connectionFactory.getConnection() >> { throw new RuntimeException('Failed') }
    0 * redisApplicationDAO.redisTemplate.connectionFactory._
  }

  def "pipelineDAO should report failing redis connection as not healthy"() {
    when:
    def healthy = redisPipelineDAO.healthy

    then:
    !healthy

    1 * redisPipelineDAO.redisTemplate.connectionFactory.getConnection() >> {
      throw new RuntimeException('Failed')
    }
    0 * redisPipelineDAO.redisTemplate.connectionFactory._
  }

  def "pipelineStrategyDAO report failing redis connection as not healthy"() {
    given:
    redisPipelineStrategyDAO.redisTemplate.connectionFactory = Mock(RedisConnectionFactory)

    when:
    def healthy = redisPipelineStrategyDAO.healthy

    then:
    !healthy

    1 * redisPipelineStrategyDAO.redisTemplate.connectionFactory.getConnection() >> { throw new RuntimeException('Failed') }
    0 * redisPipelineStrategyDAO.redisTemplate.connectionFactory._
  }

  def "projectDAO should report failing redis connection as not healthy"() {
    given:
    redisProjectDAO.redisTemplate.connectionFactory = Mock(RedisConnectionFactory)

    when:
    def healthy = redisProjectDAO.healthy

    then:
    !healthy

    1 * redisProjectDAO.redisTemplate.connectionFactory.getConnection() >> { throw new RuntimeException('Failed') }
    0 * redisProjectDAO.redisTemplate.connectionFactory._
  }
}
