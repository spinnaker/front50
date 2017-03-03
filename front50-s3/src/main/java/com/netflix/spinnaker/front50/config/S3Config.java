package com.netflix.spinnaker.front50.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.awsobjectmapper.AmazonObjectMapperConfigurer;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.clouddriver.aws.bastion.BastionConfig;
import com.netflix.spinnaker.clouddriver.aws.security.AmazonClientProvider;
import com.netflix.spinnaker.front50.model.S3StorageService;
import com.netflix.spinnaker.front50.model.StorageService;
import com.netflix.spinnaker.front50.model.application.ApplicationDAO;
import com.netflix.spinnaker.front50.model.application.ApplicationPermissionDAO;
import com.netflix.spinnaker.front50.model.application.DefaultApplicationDAO;
import com.netflix.spinnaker.front50.model.application.DefaultApplicationPermissionDAO;
import com.netflix.spinnaker.front50.model.notification.DefaultNotificationDAO;
import com.netflix.spinnaker.front50.model.notification.NotificationDAO;
import com.netflix.spinnaker.front50.model.pipeline.DefaultPipelineDAO;
import com.netflix.spinnaker.front50.model.pipeline.DefaultPipelineStrategyDAO;
import com.netflix.spinnaker.front50.model.pipeline.PipelineDAO;
import com.netflix.spinnaker.front50.model.pipeline.PipelineStrategyDAO;
import com.netflix.spinnaker.front50.model.project.DefaultProjectDAO;
import com.netflix.spinnaker.front50.model.project.ProjectDAO;
import com.netflix.spinnaker.front50.model.serviceaccount.DefaultServiceAccountDAO;
import com.netflix.spinnaker.front50.model.serviceaccount.ServiceAccountDAO;
import com.netflix.spinnaker.front50.model.snapshot.DefaultSnapshotDAO;
import com.netflix.spinnaker.front50.model.snapshot.SnapshotDAO;
import com.netflix.spinnaker.front50.model.tag.DefaultEntityTagsDAO;
import com.netflix.spinnaker.front50.model.tag.EntityTagsDAO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;
import rx.schedulers.Schedulers;

import java.util.Optional;
import java.util.concurrent.Executors;

@Configuration
@ConditionalOnExpression("${spinnaker.s3.enabled:false}")
@Import(BastionConfig.class)
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {
  @Bean
  public AmazonClientProvider amazonClientProvider() {
    return new AmazonClientProvider();
  }

  @Bean
  public AmazonS3 awsS3Client(AWSCredentialsProvider awsCredentialsProvider, S3Properties s3Properties) {
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    if (s3Properties.getProxyProtocol() != null) {
      if (s3Properties.getProxyProtocol().equalsIgnoreCase("HTTPS")) {
        clientConfiguration.setProtocol(Protocol.HTTPS);
      } else {
        clientConfiguration.setProtocol(Protocol.HTTP);
      }
      Optional.ofNullable(s3Properties.getProxyHost())
        .ifPresent(clientConfiguration::setProxyHost);
      Optional.ofNullable(s3Properties.getProxyPort())
        .map(Integer::parseInt)
        .ifPresent(clientConfiguration::setProxyPort);
    }

    AmazonS3Client client = new AmazonS3Client(awsCredentialsProvider, clientConfiguration);

    if (s3Properties.getEndpoint() != null) {
      client.setEndpoint(s3Properties.getEndpoint());
      client.setS3ClientOptions(S3ClientOptions.builder().setPathStyleAccess(true).build());
    } else {
      Optional.ofNullable(s3Properties.getRegion())
        .map(Regions::fromName)
        .map(Region::getRegion)
        .ifPresent(client::setRegion);
    }

    return client;
  }

  @Bean
  @ConditionalOnMissingBean(RestTemplate.class)
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  public S3StorageService s3StorageService(AmazonS3 amazonS3, S3Properties s3Properties) {
    ObjectMapper awsObjectMapper = new ObjectMapper();
    AmazonObjectMapperConfigurer.configure(awsObjectMapper);

    S3StorageService service = new S3StorageService(awsObjectMapper, amazonS3, s3Properties.getBucket(), s3Properties.getRootFolder(), s3Properties.isFailoverEnabled(), s3Properties.getRegion());
    service.ensureBucketExists();
    return service;
  }

  @Bean
  public ApplicationDAO applicationDAO(StorageService storageService, Registry registry) {
    return new DefaultApplicationDAO(storageService, Schedulers.from(Executors.newFixedThreadPool(20)), 15000, registry);
  }

  @Bean
  public ApplicationPermissionDAO applicationPermissionDAO(StorageService storageService, Registry registry) {
    return new DefaultApplicationPermissionDAO(storageService, Schedulers.from(Executors.newFixedThreadPool(20)), 45000, registry);
  }

  @Bean
  public ServiceAccountDAO serviceAccountDAO(StorageService storageService, Registry registry) {
    return new DefaultServiceAccountDAO(storageService, Schedulers.from(Executors.newFixedThreadPool(5)), 30000, registry);
  }

  @Bean
  public ProjectDAO projectDAO(StorageService storageService, Registry registry) {
    return new DefaultProjectDAO(storageService, Schedulers.from(Executors.newFixedThreadPool(20)), 30000, registry);
  }

  @Bean
  public NotificationDAO notificationDAO(StorageService storageService, Registry registry) {
    return new DefaultNotificationDAO(storageService, Schedulers.from(Executors.newFixedThreadPool(20)), 30000, registry);
  }

  @Bean
  public PipelineStrategyDAO pipelineStrategyDAO(StorageService storageService, Registry registry) {
    return new DefaultPipelineStrategyDAO(storageService, Schedulers.from(Executors.newFixedThreadPool(20)), 20000, registry);
  }

  @Bean
  public PipelineDAO pipelineDAO(StorageService storageService, Registry registry) {
    return new DefaultPipelineDAO(storageService, Schedulers.from(Executors.newFixedThreadPool(25)), 10000, registry);
  }

  @Bean
  public SnapshotDAO snapshotDAO(StorageService storageService, Registry registry) {
    return new DefaultSnapshotDAO(storageService, Schedulers.from(Executors.newFixedThreadPool(5)), 60000, registry);
  }

  @Bean
  public EntityTagsDAO entityTagsDAO(StorageService storageService, Registry registry) {
    return new DefaultEntityTagsDAO(storageService, Schedulers.from(Executors.newFixedThreadPool(25)), 80000, registry);
  }
}
