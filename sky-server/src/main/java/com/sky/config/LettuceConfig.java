package com.sky.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;

/**
 * @description lettuce客户端配置
 * @author 
 */
@Configuration
public class LettuceConfig implements LettuceClientConfigurationBuilderCustomizer {

    @Override
    public void customize(LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigurationBuilder) {
        clientConfigurationBuilder.clientOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
    }
}
