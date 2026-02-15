package com.ada.proj.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3StorageConfig {

    @Bean
    public S3Client s3Client(S3StorageProperties props) {
        Region region;
        if (StringUtils.hasText(props.getRegion())) {
            region = Region.of(props.getRegion());
        } else {
            region = DefaultAwsRegionProviderChain.builder().build().getRegion();
            if (region == null) {
                throw new IllegalStateException("S3 region is required. Set app.storage.s3.region or AWS_REGION/AWS_DEFAULT_REGION");
            }
        }

        var builder = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(region)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(props.isPathStyleAccess())
                        .build());

        if (StringUtils.hasText(props.getEndpoint())) {
            builder = builder.endpointOverride(URI.create(props.getEndpoint()));
        }

        return builder.build();
    }
}
