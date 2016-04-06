/*
 * Copyright 2016 Pivotal, Inc.
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

import com.netflix.spinnaker.front50.model.application.Application
import com.netflix.spinnaker.front50.model.notification.Notification
import com.netflix.spinnaker.front50.model.pipeline.Pipeline
import com.netflix.spinnaker.front50.model.project.Project
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
/**
 * @author Greg Turnquist
 */
@Configuration
@ConditionalOnExpression('${spinnaker.redis.enabled:false}')
class RedisConfig {

  @Bean
  RedisApplicationDAO redisApplicationDAO(RedisTemplate<String, Application> template, RedisConnectionFactory connectionFactory) {
    new RedisApplicationDAO(template: template, factory: connectionFactory)
  }

  @Bean
  RedisProjectDAO redisProjectDAO(RedisTemplate<String, Project> template, RedisConnectionFactory connectionFactory) {
    new RedisProjectDAO(template: template, factory: connectionFactory)
  }

  @Bean
  RedisPipelineStrategyDAO redisPipelineStrategyDAO(RedisTemplate<String, Pipeline> template, RedisConnectionFactory connectionFactory) {
    new RedisPipelineStrategyDAO(template: template, factory: connectionFactory)
  }

  @Bean
  RedisPipelineDAO redisPipelineDAO(RedisTemplate<String, Pipeline> template, RedisConnectionFactory connectionFactory) {
    new RedisPipelineDAO(template: template, factory: connectionFactory)
  }

  @Bean
  RedisNotificationDAO redisNotificationDAO(RedisTemplate<String, Notification> template) {
    new RedisNotificationDAO(template: template)
  }

  @Bean
  RedisConnectionFactory jedisConnectionFactory() {
    new JedisConnectionFactory()
  }

  @Bean
  RedisTemplate<String, Application> applicationRedisTemplate(RedisConnectionFactory connectionFactory) {

    RedisTemplate<String, Application> template = new RedisTemplate<>()
    template.connectionFactory = connectionFactory
    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Application))
    template.setKeySerializer(new StringRedisSerializer())

    template
  }

  @Bean
  RedisTemplate<String, Project> projectRedisTemplate(RedisConnectionFactory connectionFactory) {

    RedisTemplate<String, Project> template = new RedisTemplate<>()
    template.connectionFactory = connectionFactory
    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Project))
    template.setKeySerializer(new StringRedisSerializer())

    template
  }

  @Bean
  RedisTemplate<String, Pipeline> pipelineRedisTemplate(RedisConnectionFactory connectionFactory) {

    RedisTemplate<String, Pipeline> template = new RedisTemplate<>()
    template.connectionFactory = connectionFactory
    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Pipeline))
    template.setKeySerializer(new StringRedisSerializer())

    template
  }

  @Bean
  RedisTemplate<String, Notification> notificationRedisTemplate(RedisConnectionFactory connectionFactory) {

    RedisTemplate<String, Notification> template = new RedisTemplate<>()
    template.connectionFactory = connectionFactory
    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Notification))
    template.setKeySerializer(new StringRedisSerializer())

    template
  }

}
