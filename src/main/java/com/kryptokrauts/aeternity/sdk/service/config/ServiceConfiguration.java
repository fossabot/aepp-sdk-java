package com.kryptokrauts.aeternity.sdk.service.config;

import com.kryptokrauts.aeternity.sdk.service.wallet.WalletServiceConfiguration;

import io.vertx.core.Vertx;
import lombok.Builder.Default;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * a lomboked class with service parameters should extend this configuration
 * class like
 * 
 * <pre>
 *   
 * {@literal @}Getter
 * {@literal @}Builder( builderMethodName = "configure", buildMethodName =
 * "compile" )
 * public class <Servicename>ServiceConfiguration implements
 * ServiceConfiguration
 * </pre>
 * 
 * parameters should provide default values in the following way
 * 
 * <pre>
 * {@literal @}Builder.Default
 * <type> paramName = "value";
 * </pre>
 * 
 * examples see {@link WalletServiceConfiguration}
 */
@SuperBuilder
@NoArgsConstructor
public class ServiceConfiguration {

    /**
     * points to testnet
     */
    @Default
    protected String base_url = "https://sdk-edgenet.aepps.com/v2";

    /**
     * the vertx instance
     */
    @Default
    protected Vertx vertx = Vertx.vertx();

}
