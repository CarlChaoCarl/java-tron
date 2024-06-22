package org.tron.core.metrics.prometheus;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import io.prometheus.client.CollectorRegistry;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.BaseTest;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricLabels;
import org.tron.common.prometheus.Metrics;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.TestParallelUtil;
import org.tron.common.utils.Utils;
import org.tron.consensus.dpos.DposSlot;
import org.tron.core.ChainBaseManager;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.services.jsonrpc.FullNodeJsonRpcHttpService;
import org.tron.core.services.jsonrpc.types.BlockResult;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

@Slf4j(topic = "metric")
public class PrometheusApiServiceTest extends BaseTest {
  static LocalDateTime localDateTime = LocalDateTime.now();
  @Resource
  private DposSlot dposSlot;
  final int blocks = 512;
  private final String key = PublicMethod.getRandomPrivateKey();
  private final byte[] privateKey = ByteArray.fromHexString(key);
  private static final AtomicInteger port = new AtomicInteger(0);
  private final long time = ZonedDateTime.of(localDateTime,
      ZoneId.systemDefault()).toInstant().toEpochMilli();

  private static BlockCapsule blockCapsule;
  private static TransactionCapsule transactionCapsule1;

  @Resource
  private TronNetDelegate tronNetDelegate;
  @Resource
  private ConsensusService consensusService;
  @Resource
  private ChainBaseManager chainManager;
  @Resource
  private FullNodeJsonRpcHttpService fullNodeJsonRpcHttpService;

  static {
    Args.setParam(new String[] {"-d", dbPath(), "-w"}, Constant.TEST_CONF);
    Args.getInstance().setNodeListenPort(10000 + port.incrementAndGet());
    initParameter(Args.getInstance());
    Metrics.init();
  }

  protected static void initParameter(CommonParameter parameter) {
    parameter.setMetricsPrometheusEnable(true);
  }

  protected void check() throws Exception {
    Double memoryBytes = CollectorRegistry.defaultRegistry.getSampleValue(
        "system_total_physical_memory_bytes");
    Assert.assertNotNull(memoryBytes);
    Assert.assertTrue(memoryBytes.intValue() > 0);

    Double cpus = CollectorRegistry.defaultRegistry.getSampleValue("system_available_cpus");
    Assert.assertNotNull(cpus);
    Assert.assertEquals(cpus.intValue(), Runtime.getRuntime().availableProcessors());

    Double pushBlock = CollectorRegistry.defaultRegistry.getSampleValue(
        "tron:block_process_latency_seconds_count",
        new String[] {"sync"}, new String[] {"false"});
    Assert.assertNotNull(pushBlock);
    Assert.assertEquals(pushBlock.intValue(), blocks + 1);
    Double errorLogs = CollectorRegistry.defaultRegistry.getSampleValue(
        "tron:error_info_total", new String[] {"net"}, new String[] {MetricLabels.UNDEFINED});
    Assert.assertNull(errorLogs);
  }

  @Before
  public void init() throws Exception {
    logger.info("Full node running.");
    consensusService.start();
    chainBaseManager = dbManager.getChainBaseManager();
    byte[] address = PublicMethod.getAddressByteByPrivateKey(key);
    ByteString addressByte = ByteString.copyFrom(address);
    WitnessCapsule witnessCapsule = new WitnessCapsule(addressByte);
    chainBaseManager.getWitnessStore().put(addressByte.toByteArray(), witnessCapsule);
    chainBaseManager.addWitness(addressByte);

    AccountCapsule accountCapsule =
            new AccountCapsule(Protocol.Account.newBuilder().setAddress(addressByte).build());
    chainBaseManager.getAccountStore().put(addressByte.toByteArray(), accountCapsule);



    blockCapsule = new BlockCapsule(
        1,
        Sha256Hash.wrap(ByteString.copyFrom(
            ByteArray.fromHexString(
                "0304f784e4e7bae517bcab94c3e0c9214fb4ac7ff9d7d5a937d1f40031f87b81"))),
        1,
        ByteString.copyFromUtf8("testAddress"));
    BalanceContract.TransferContract transferContract1 = BalanceContract.TransferContract.newBuilder()
        .setAmount(1L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086"))))
        .build();

    BalanceContract.TransferContract transferContract2 = BalanceContract.TransferContract.newBuilder()
        .setAmount(2L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "ED738B3A0FE390EAA71B768B6D02CDBD18FB207B"))))
        .build();

    transactionCapsule1 =
        new TransactionCapsule(transferContract1, Protocol.Transaction.Contract.ContractType.TransferContract);
    transactionCapsule1.setBlockNum(blockCapsule.getNum());
    TransactionCapsule transactionCapsule2 = new TransactionCapsule(transferContract2,
        Protocol.Transaction.Contract.ContractType.TransferContract);
    transactionCapsule2.setBlockNum(2L);

    blockCapsule.addTransaction(transactionCapsule1);
    blockCapsule.addTransaction(transactionCapsule2);
    //dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(1L);
    dbManager.getBlockIndexStore().put(blockCapsule.getBlockId());
    dbManager.getBlockStore().put(blockCapsule.getBlockId().getBytes(), blockCapsule);
  }


  private void generateBlock(Map<ByteString, String> witnessAndAccount) throws Exception {

    BlockCapsule block =
        createTestBlockCapsule(
            chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp() + 3000,
            chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber() + 1,
            chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderHash().getByteString(),
            witnessAndAccount);

    tronNetDelegate.processBlock(block, false);
  }

  @Test
  public void testMetric() throws Exception {

    final ECKey ecKey = ECKey.fromPrivate(privateKey);
    Assert.assertNotNull(ecKey);
    byte[] address = ecKey.getAddress();
    WitnessCapsule witnessCapsule = new WitnessCapsule(ByteString.copyFrom(address));
    chainBaseManager.getWitnessScheduleStore().saveActiveWitnesses(new ArrayList<>());
    chainBaseManager.addWitness(ByteString.copyFrom(address));

    Protocol.Block block = getSignedBlock(witnessCapsule.getAddress(), time, privateKey);

    tronNetDelegate.processBlock(new BlockCapsule(block), false);

    Map<ByteString, String> witnessAndAccount = addTestWitnessAndAccount();
    witnessAndAccount.put(ByteString.copyFrom(address), key);
    for (int i = 0; i < blocks; i++) {
      generateBlock(witnessAndAccount);
    }
    check();
  }

  private Map<ByteString, String> addTestWitnessAndAccount() {
    chainBaseManager.getWitnesses().clear();
    return IntStream.range(0, 2)
        .mapToObj(
            i -> {
              ECKey ecKey = new ECKey(Utils.getRandom());
              String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
              ByteString address = ByteString.copyFrom(ecKey.getAddress());

              WitnessCapsule witnessCapsule = new WitnessCapsule(address);
              chainBaseManager.getWitnessStore().put(address.toByteArray(), witnessCapsule);
              chainBaseManager.addWitness(address);

              AccountCapsule accountCapsule =
                  new AccountCapsule(Protocol.Account.newBuilder().setAddress(address).build());
              chainBaseManager.getAccountStore().put(address.toByteArray(), accountCapsule);

              return Maps.immutableEntry(address, privateKey);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private BlockCapsule createTestBlockCapsule(long time,
                                              long number, ByteString hash,
                                              Map<ByteString, String> witnessAddressMap) {
    ByteString witnessAddress = dposSlot.getScheduledWitness(dposSlot.getSlot(time));
    BlockCapsule blockCapsule = new BlockCapsule(number, Sha256Hash.wrap(hash), time,
        witnessAddress);
    blockCapsule.generatedByMyself = true;
    blockCapsule.setMerkleRoot();
    blockCapsule.sign(ByteArray.fromHexString(witnessAddressMap.get(witnessAddress)));
    return blockCapsule;
  }



  @Test
  public void testGetBlockByNumber2() {

    int jsonRpcHttpFullNodePort = CommonParameter.getInstance().getJsonRpcHttpFullNodePort();
    fullNodeJsonRpcHttpService.init(Args.getInstance());
    fullNodeJsonRpcHttpService.start();

    JsonArray params = new JsonArray();
    params.add(ByteArray.toJsonHex(blockCapsule.getNum()));
    params.add(false);
    JsonObject requestBody = new JsonObject();
    requestBody.addProperty("jsonrpc", "2.0");
    requestBody.addProperty("method", "eth_getBlockByNumber");
    requestBody.add("params", params);
    requestBody.addProperty("id", 1);
    CloseableHttpResponse response;
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      String requestUrl = "http://127.0.0.1:" + jsonRpcHttpFullNodePort + "/jsonrpc";
      HttpPost httpPost = new HttpPost(requestUrl);
      httpPost.addHeader("Content-Type", "application/json");
      httpPost.setEntity(new StringEntity(requestBody.toString()));
      response = httpClient.execute(httpPost);
      String resp = EntityUtils.toString(response.getEntity());
      BlockResult blockResult = JSON.parseObject(resp).getObject("result", BlockResult.class);
      Assert.assertEquals(ByteArray.toJsonHex(blockCapsule.getNum()),
          blockResult.getNumber());
      Assert.assertEquals(blockCapsule.getTransactions().size(),
          blockResult.getTransactions().length);
      Assert.assertEquals("0x0000000000000000",
          blockResult.getNonce());
      response.close();
      logger.error("isMetricsPrometheusEnable:"
          + (CommonParameter.getInstance().isMetricsPrometheusEnable() ? "true":"false"));
      Double d = CollectorRegistry.defaultRegistry.getSampleValue(
          "tron:jsonrpc_service_latency_seconds_count",
          new String[] {"method"}, new String[] {"eth_getBlockByNumber"});
      System.out.println("d:" + d);
      Assert.assertEquals(1, CollectorRegistry.defaultRegistry.getSampleValue(
          "tron:jsonrpc_service_latency_seconds_count",
          new String[] {"method"}, new String[] {"eth_getBlockByNumber"}).intValue());
    } catch (Exception e) {
      logger.error("testGetBlockByNumber2 exception:", e);
      Assert.fail(e.getMessage());
    } finally {
      fullNodeJsonRpcHttpService.stop();
      CommonParameter.getInstance().setJsonRpcHttpFullNodePort(jsonRpcHttpFullNodePort);
    }
  }

}