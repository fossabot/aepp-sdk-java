package com.kryptokrauts.aeternity.generated.api;

import com.google.common.collect.ImmutableMap;
import com.kryptokrauts.aeternity.generated.model.DryRunResult;
import com.kryptokrauts.aeternity.generated.model.DryRunResults;
import com.kryptokrauts.aeternity.generated.model.TxInfoObject;
import com.kryptokrauts.aeternity.sdk.constants.BaseConstants;
import com.kryptokrauts.aeternity.sdk.constants.Network;
import com.kryptokrauts.aeternity.sdk.exception.TransactionCreateException;
import com.kryptokrauts.aeternity.sdk.service.aeternity.AeternityServiceConfiguration;
import com.kryptokrauts.aeternity.sdk.service.aeternity.impl.AeternityService;
import com.kryptokrauts.aeternity.sdk.service.domain.account.AccountResult;
import com.kryptokrauts.aeternity.sdk.service.domain.transaction.PostTransactionResult;
import com.kryptokrauts.aeternity.sdk.service.transaction.AccountParameter;
import com.kryptokrauts.aeternity.sdk.service.transaction.type.model.ContractCallTransactionModel;
import com.kryptokrauts.aeternity.sdk.service.transaction.type.model.ContractCreateTransactionModel;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransactionContractsTest extends BaseTest {

  static String localDeployedContractId;

  /**
   * create an unsigned native CreateContract transaction
   *
   * @param context
   */
  @Test
  public void buildCreateContractTransactionTest(TestContext context) {
    String ownerId = baseKeyPair.getPublicKey();
    BigInteger abiVersion = BigInteger.ONE;
    BigInteger vmVersion = BigInteger.valueOf(4);
    BigInteger amount = BigInteger.valueOf(100);
    BigInteger deposit = BigInteger.valueOf(100);
    BigInteger ttl = BigInteger.valueOf(20000l);
    BigInteger gas = BigInteger.valueOf(1000);
    BigInteger gasPrice = BigInteger.valueOf(1100000000l);
    BigInteger nonce = BigInteger.ONE;
    BigInteger fee = BigInteger.valueOf(1098660000000000l);

    ContractCreateTransactionModel contractTx =
        ContractCreateTransactionModel.builder()
            .abiVersion(abiVersion)
            .amount(amount)
            .callData(TestConstants.testContractCallData)
            .contractByteCode(TestConstants.testContractByteCode)
            .deposit(deposit)
            .fee(fee)
            .gas(gas)
            .gasPrice(gasPrice)
            .nonce(nonce)
            .ownerId(ownerId)
            .ttl(ttl)
            .vmVersion(vmVersion)
            .build();

    String unsignedTxNative =
        aeternityServiceNative.transactions.blockingCreateUnsignedTransaction(contractTx);

    String unsignedTxDebug =
        aeternityServiceDebug.transactions.blockingCreateUnsignedTransaction(contractTx);

    context.assertEquals(unsignedTxDebug, unsignedTxNative);
  }

  @Test
  public void buildCallContractTransactionTest(TestContext context) {
    Async async = context.async();
    rule.vertx()
        .executeBlocking(
            future -> {
              try {
                String callerId = baseKeyPair.getPublicKey();
                BigInteger abiVersion = BigInteger.ONE;
                BigInteger ttl = BigInteger.valueOf(20000);
                BigInteger gas = BigInteger.valueOf(1000);
                BigInteger gasPrice = BigInteger.valueOf(1000000000);
                BigInteger nonce = getNextBaseKeypairNonce();
                String callContractCalldata = TestConstants.encodedServiceCall;

                ContractCallTransactionModel callTx =
                    ContractCallTransactionModel.builder()
                        .abiVersion(abiVersion)
                        .callData(callContractCalldata)
                        .contractId(localDeployedContractId)
                        .gas(gas)
                        .gasPrice(gasPrice)
                        .nonce(nonce)
                        .callerId(callerId)
                        .ttl(ttl)
                        .fee(BigInteger.valueOf(1454500000000000l))
                        .build();

                String unsignedTxNative =
                    this.aeternityServiceNative.transactions.blockingCreateUnsignedTransaction(
                        callTx);

                _logger.info("Call contract tx hash (native unsigned): " + unsignedTxNative);

                String unsignedTxDebug =
                    this.aeternityServiceDebug.transactions.blockingCreateUnsignedTransaction(
                        callTx);
                _logger.debug("Call contract tx hash (debug unsigned): " + unsignedTxDebug);

                context.assertEquals(unsignedTxNative, unsignedTxDebug);
              } catch (Throwable e) {
                context.fail(e);
              }
              future.complete();
            },
            success -> async.complete());
  }

  @Test
  public void staticCallContractOnLocalNode(TestContext context) {

    BigInteger nonce = getNextBaseKeypairNonce();

    // Compile the call contract
    String calldata =
        encodeCalldata(
            TestConstants.testContractSourceCode,
            TestConstants.testContractFunction,
            TestConstants.testContractFunctionParams);

    DryRunResults results =
        this.aeternityServiceNative.transactions.blockingDryRunTransactions(
            Arrays.asList(
                ImmutableMap.of(AccountParameter.PUBLIC_KEY, baseKeyPair.getPublicKey()),
                ImmutableMap.of(AccountParameter.PUBLIC_KEY, baseKeyPair.getPublicKey())),
            null,
            Arrays.asList(
                createUnsignedContractCallTx(context, nonce, calldata, null),
                createUnsignedContractCallTx(context, nonce.add(ONE), calldata, null)));

    _logger.info(results.toString());
    for (DryRunResult result : results.getResults()) {
      context.assertEquals("ok", result.getResult());
    }
  }

  @Test
  public void staticCallContractFailOnLocalNode(TestContext context) {
    // Compile the call contract
    String calldata =
        encodeCalldata(
            TestConstants.testContractSourceCode,
            TestConstants.testContractFunction,
            TestConstants.testContractFunctionParams);

    DryRunResults results =
        this.aeternityServiceNative.transactions.blockingDryRunTransactions(
            Arrays.asList(ImmutableMap.of(AccountParameter.PUBLIC_KEY, baseKeyPair.getPublicKey())),
            null,
            Arrays.asList(createUnsignedContractCallTx(context, ONE, calldata, null)));

    _logger.info(results.toString());
    for (DryRunResult result : results.getResults()) {
      context.assertEquals("error", result.getResult());
    }
  }

  /**
   * min gas is not sufficient, validate this!
   *
   * @param context
   * @throws Throwable
   */
  @Test
  public void callContractAfterDryRunOnLocalNode(TestContext context) throws Throwable {

    // Compile the call contract
    String calldata =
        encodeCalldata(
            TestConstants.testContractSourceCode,
            TestConstants.testContractFunction,
            TestConstants.testContractFunctionParams);

    DryRunResults results =
        performDryRunTransactions(
            Arrays.asList(ImmutableMap.of(AccountParameter.PUBLIC_KEY, baseKeyPair.getPublicKey())),
            null,
            Arrays.asList(
                createUnsignedContractCallTx(context, getNextBaseKeypairNonce(), calldata, null)));
    _logger.info("callContractAfterDryRunOnLocalNode: " + results.toString());

    for (DryRunResult result : results.getResults()) {
      context.assertEquals("ok", result.getResult());

      ContractCallTransactionModel callTx =
          ContractCallTransactionModel.builder()
              .abiVersion(ONE)
              .callData(calldata)
              .contractId(localDeployedContractId)
              .gas(result.getCallObj().getGasUsed())
              .gasPrice(result.getCallObj().getGasPrice())
              .nonce(getNextBaseKeypairNonce())
              .callerId(baseKeyPair.getPublicKey())
              .ttl(ZERO)
              .build();

      PostTransactionResult response =
          this.aeternityServiceNative.transactions.blockingPostTransaction(callTx);

      context.assertEquals(
          response.getTxHash(), this.aeternityServiceNative.transactions.computeTxHash(callTx));
      _logger.info("Call contract tx hash: " + response.getTxHash());

      // get the tx info object to resolve the result
      TxInfoObject txInfoObject = waitForTxInfoObject(response.getTxHash());

      // decode the result to json
      JsonObject json =
          decodeCalldata(
              txInfoObject.getCallInfo().getReturnValue(),
              TestConstants.testContractFunctionSophiaType);
      context.assertEquals(
          TestConstants.testContractFuntionParam, json.getValue("value").toString());
    }
  }

  private String createUnsignedContractCallTx(
      TestContext context, BigInteger nonce, String calldata, BigInteger gasPrice) {
    return this.aeternityServiceNative.transactions.blockingCreateUnsignedTransaction(
        createCallContractModel(nonce, calldata, gasPrice));
  }

  private ContractCallTransactionModel createCallContractModel(
      BigInteger nonce, String calldata, BigInteger gasPrice) {
    String callerId = baseKeyPair.getPublicKey();
    BigInteger abiVersion = BigInteger.ONE;
    BigInteger ttl = BigInteger.ZERO;
    BigInteger gas = BigInteger.valueOf(1579000);
    ContractCallTransactionModel model =
        ContractCallTransactionModel.builder()
            .abiVersion(abiVersion)
            .callData(calldata)
            .contractId(localDeployedContractId)
            .gas(gas)
            .gasPrice(
                gasPrice != null ? gasPrice : BigInteger.valueOf(BaseConstants.MINIMAL_GAS_PRICE))
            .nonce(nonce)
            .callerId(callerId)
            .ttl(ttl)
            .build();
    return model;
  }

  @Test
  public void aDeployContractNativeOnLocalNode(TestContext context) throws Throwable {

    BigInteger vmVersion = BigInteger.valueOf(4);
    BigInteger gas = BigInteger.valueOf(1000000);
    BigInteger gasPrice = BigInteger.valueOf(2000000000);

    ContractCreateTransactionModel contractTx =
        ContractCreateTransactionModel.builder()
            .abiVersion(ONE)
            .amount(ZERO)
            .callData(TestConstants.testContractCallData)
            .contractByteCode(TestConstants.testContractByteCode)
            .deposit(ZERO)
            .gas(gas)
            .gasPrice(gasPrice)
            .nonce(getNextBaseKeypairNonce())
            .ownerId(baseKeyPair.getPublicKey())
            .ttl(ZERO)
            .vmVersion(vmVersion)
            .build();

    PostTransactionResult result =
        this.aeternityServiceNative.transactions.blockingPostTransaction(contractTx);
    TxInfoObject txInfoObject = waitForTxInfoObject(result.getTxHash());
    localDeployedContractId = txInfoObject.getCallInfo().getContractId();
    _logger.info("Deployed contract - hash " + result.getTxHash() + " - " + txInfoObject);
  }

  @Test
  public void callContractOnLocalNodeTest(TestContext context) throws Throwable {

    String callData =
        this.aeternityServiceNative.compiler.blockingEncodeCalldata(
            TestConstants.testContractSourceCode,
            TestConstants.testContractFunction,
            TestConstants.testContractFunctionParams);

    // post the signed contract call tx
    PostTransactionResult result =
        this.aeternityServiceNative.transactions.blockingPostTransaction(
            createCallContractModel(getNextBaseKeypairNonce(), callData, null));
    context.assertEquals(
        result.getTxHash(),
        this.aeternityServiceNative.transactions.computeTxHash(
            createCallContractModel(getNextBaseKeypairNonce(), callData, null)));
    _logger.info("CreateContractTx hash: " + result.getTxHash());

    // get the tx info object to resolve the result
    TxInfoObject txInfoObject = waitForTxInfoObject(result.getTxHash());

    // decode the result to json
    JsonObject json =
        decodeCalldata(
            txInfoObject.getCallInfo().getReturnValue(),
            TestConstants.testContractFunctionSophiaType);
    context.assertEquals(TestConstants.testContractFuntionParam, json.getValue("value").toString());
  }

  @Test
  @Ignore // specific testcase we don't want to run each time
  public void deployContractNativeOnTestNetworkTest(TestContext context)
      throws TransactionCreateException {
    baseKeyPair =
        keyPairService.generateBaseKeyPairFromSecret(TestConstants.testnetAccountPrivateKey);

    AeternityService testnetService =
        new AeternityService(
            AeternityServiceConfiguration.configure()
                .baseUrl(TestConstants.testnetURL)
                .network(Network.TESTNET)
                .vertx(rule.vertx())
                .compile());

    AccountResult account =
        testnetService.accounts.blockingGetAccount(Optional.of(baseKeyPair.getPublicKey()));
    String ownerId = baseKeyPair.getPublicKey();
    BigInteger abiVersion = BigInteger.ONE;
    BigInteger vmVersion = BigInteger.valueOf(4);
    BigInteger amount = BigInteger.ZERO;
    BigInteger deposit = BigInteger.ZERO;
    BigInteger ttl = BigInteger.ZERO;
    BigInteger gas = BigInteger.valueOf(1000);
    BigInteger gasPrice = BigInteger.valueOf(1100000000);
    BigInteger nonce = getNextBaseKeypairNonce();

    ContractCreateTransactionModel testnetCreateTx =
        ContractCreateTransactionModel.builder()
            .abiVersion(abiVersion)
            .amount(amount)
            .callData(TestConstants.testContractCallData)
            .contractByteCode(TestConstants.testContractByteCode)
            .deposit(deposit)
            .gas(gas)
            .gasPrice(gasPrice)
            .nonce(nonce)
            .ownerId(ownerId)
            .ttl(ttl)
            .vmVersion(vmVersion)
            .build();

    PostTransactionResult result =
        testnetService.transactions.blockingPostTransaction(testnetCreateTx);

    context.assertEquals(
        result.getTxHash(), testnetService.transactions.computeTxHash(testnetCreateTx));
  }
}
