package com.kryptokrauts.aeternity.sdk.service.aeternity;

import com.kryptokrauts.aeternity.sdk.constants.BaseConstants;
import com.kryptokrauts.aeternity.sdk.constants.Network;
import com.kryptokrauts.aeternity.sdk.service.ServiceConfiguration;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(builderMethodName = "configure", buildMethodName = "compile")
public class AeternityServiceConfiguration extends ServiceConfiguration {

  @Default private boolean nativeMode = true;

  @Default private Network network = Network.TESTNET;

  @Default private long minimalGasPrice = BaseConstants.MINIMAL_GAS_PRICE;

  @Default private boolean waitForTxIncludedInBlockEnabled = true;

  @Default private int numTrialsToWaitForTxIncludedInBlock = 60;

  @Default private long millisBetweenTrialsToWaitForTxIncludedInBlock = 1000l;

  @Default private int numOfConfirmations = 10;

  @Default private long millisBetweenTrailsToWaitForConfirmation = 10000l;
}
