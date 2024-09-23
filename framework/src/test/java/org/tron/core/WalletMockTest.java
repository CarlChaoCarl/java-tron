package org.tron.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.LazyStringArrayList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.tron.api.GrpcAPI;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.client.WalletClient;
import org.tron.core.capsule.AbiCapsule;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.CodeCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.core.capsule.ContractStateCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionStore;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.HeaderNotFound;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TronException;
import org.tron.core.exception.VMIllegalException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.exception.ZksnarkException;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.store.AbiStore;
import org.tron.core.store.AccountStore;
import org.tron.core.store.CodeStore;
import org.tron.core.store.ContractStateStore;
import org.tron.core.store.ContractStore;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.TransactionHistoryStore;
import org.tron.core.store.TransactionRetStore;
import org.tron.core.zen.ShieldedTRC20ParametersBuilder;
import org.tron.core.zen.ZenTransactionBuilder;
import org.tron.core.zen.address.DiversifierT;
import org.tron.core.zen.address.ExpandedSpendingKey;
import org.tron.core.zen.address.KeyIo;
import org.tron.core.zen.address.PaymentAddress;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.ShieldContract;
import org.tron.protos.contract.SmartContractOuterClass;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Wallet.class,
    Args.class,
    CommonParameter.class,
    TransactionCapsule.class,
    com.google.protobuf.Message.class,
    ByteUtil.class,
    KeyIo.class,
    PaymentAddress.class,
    Protocol.Transaction.Contract.ContractType.class
})
public class WalletMockTest {
  @Before
  public void init() {
  }

  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void testSetTransactionNullException() throws Exception {
    Wallet wallet = new Wallet();
    TransactionCapsule transactionCapsuleMock
        = PowerMockito.mock(TransactionCapsule.class);
    Whitebox.invokeMethod(wallet,
        "setTransaction", transactionCapsuleMock);
    Assert.assertEquals(true, true);
  }

  @Test
  public void testCreateTransactionCapsuleWithoutValidateWithTimeoutNullException()
      throws Exception {
    Wallet wallet = new Wallet();
    com.google.protobuf.Message message =
        PowerMockito.mock(com.google.protobuf.Message.class);
    Protocol.Transaction.Contract.ContractType contractType =
        PowerMockito.mock(Protocol.Transaction.Contract.ContractType.class);
    long timeout = 100L;
    TransactionCapsule transactionCapsuleMock = PowerMockito.mock(TransactionCapsule.class);

    PowerMockito.whenNew(TransactionCapsule.class)
        .withAnyArguments().thenReturn(transactionCapsuleMock);
    try {
      Whitebox.invokeMethod(wallet,
          "createTransactionCapsuleWithoutValidateWithTimeout",
          message, contractType, timeout);
    } catch (Exception e) {
      Assert.assertTrue(false);
    }

    Assert.assertTrue(true);
  }

  @Test
  public void testCreateTransactionCapsuleWithoutValidateWithTimeout()
      throws Exception {
    Wallet wallet = new Wallet();
    com.google.protobuf.Message message =
        PowerMockito.mock(com.google.protobuf.Message.class);
    Protocol.Transaction.Contract.ContractType contractType =
        PowerMockito.mock(Protocol.Transaction.Contract.ContractType.class);
    long timeout = 100L;
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId();

    TransactionCapsule transactionCapsuleMock = PowerMockito.mock(TransactionCapsule.class);
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);

    when(chainBaseManagerMock.getHeadBlockId()).thenReturn(blockId);

    PowerMockito.whenNew(TransactionCapsule.class)
        .withAnyArguments().thenReturn(transactionCapsuleMock);
    Whitebox.invokeMethod(wallet,
        "createTransactionCapsuleWithoutValidateWithTimeout",
        message, contractType, timeout);
    Assert.assertEquals(true, true);
  }


  @Test
  public void testBroadcastTransactionBlockUnsolidified() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();

    TronNetDelegate tronNetDelegateMock = PowerMockito.mock(TronNetDelegate.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(true);

    Whitebox.setInternalState(wallet, "tronNetDelegate", tronNetDelegateMock);

    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);

    Assert.assertEquals(GrpcAPI.Return.response_code.BLOCK_UNSOLIDIFIED, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionNoConnection() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    List<PeerConnection> peerConnections = new ArrayList<>();

    TronNetDelegate tronNetDelegateMock = PowerMockito.mock(TronNetDelegate.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);

    Whitebox.setInternalState(wallet, "tronNetDelegate", tronNetDelegateMock);
    Whitebox.setInternalState(wallet, "minEffectiveConnection", 10);
    when(tronNetDelegateMock.getActivePeer()).thenReturn(peerConnections);

    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);

    Assert.assertEquals(GrpcAPI.Return.response_code.NO_CONNECTION, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionConnectionNotEnough() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    List<PeerConnection> peerConnections = new ArrayList<>();
    PeerConnection p1 = new PeerConnection();
    PeerConnection p2 = new PeerConnection();
    peerConnections.add(p1);
    peerConnections.add(p2);

    TronNetDelegate tronNetDelegateMock = PowerMockito.mock(TronNetDelegate.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);

    Whitebox.setInternalState(wallet, "tronNetDelegate", tronNetDelegateMock);
    Whitebox.setInternalState(wallet, "minEffectiveConnection", 10);
    when(tronNetDelegateMock.getActivePeer()).thenReturn(peerConnections);

    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);

    Assert.assertEquals(GrpcAPI.Return.response_code.NOT_ENOUGH_EFFECTIVE_CONNECTION,
        ret.getCode());
  }

  @Test
  public void testBroadcastTransactionTooManyPending() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();

    TronNetDelegate tronNetDelegateMock = PowerMockito.mock(TronNetDelegate.class);
    Manager managerMock = PowerMockito.mock(Manager.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);
    when(managerMock.isTooManyPending()).thenReturn(true);

    Whitebox.setInternalState(wallet, "tronNetDelegate", tronNetDelegateMock);
    Whitebox.setInternalState(wallet, "dbManager", managerMock);

    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);

    Assert.assertEquals(GrpcAPI.Return.response_code.SERVER_BUSY, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionAlreadyExists() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    TransactionCapsule trx = new TransactionCapsule(transaction);
    trx.setTime(System.currentTimeMillis());
    Sha256Hash txID = trx.getTransactionId();

    Cache<Sha256Hash, Boolean> transactionIdCache = CacheBuilder
        .newBuilder().maximumSize(10)
        .expireAfterWrite(1, TimeUnit.HOURS).recordStats().build();
    transactionIdCache.put(txID, true);

    TronNetDelegate tronNetDelegateMock = PowerMockito.mock(TronNetDelegate.class);
    Manager managerMock = PowerMockito.mock(Manager.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);
    when(managerMock.isTooManyPending()).thenReturn(false);
    when(managerMock.getTransactionIdCache()).thenReturn(transactionIdCache);

    Whitebox.setInternalState(wallet, "tronNetDelegate", tronNetDelegateMock);
    Whitebox.setInternalState(wallet, "dbManager", managerMock);
    Whitebox.setInternalState(wallet, "trxCacheEnable", true);

    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);

    Assert.assertEquals(GrpcAPI.Return.response_code.DUP_TRANSACTION_ERROR,
        ret.getCode());
  }


  @Test
  public void testBroadcastTransactionNoContract() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();

    TronNetDelegate tronNetDelegateMock = PowerMockito.mock(TronNetDelegate.class);
    Manager managerMock = PowerMockito.mock(Manager.class);
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock
        = PowerMockito.mock(DynamicPropertiesStore.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);
    when(managerMock.isTooManyPending()).thenReturn(false);
    when(chainBaseManagerMock.getDynamicPropertiesStore())
        .thenReturn(dynamicPropertiesStoreMock);
    when(dynamicPropertiesStoreMock.supportVM()).thenReturn(false);

    Whitebox.setInternalState(wallet, "tronNetDelegate", tronNetDelegateMock);
    Whitebox.setInternalState(wallet, "dbManager", managerMock);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    Whitebox.setInternalState(wallet, "trxCacheEnable", false);

    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);

    Assert.assertEquals(GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR,
        ret.getCode());
  }

  @Test
  public void testBroadcastTransactionOtherException() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = getExampleTrans();

    TronNetDelegate tronNetDelegateMock = PowerMockito.mock(TronNetDelegate.class);
    Manager managerMock = PowerMockito.mock(Manager.class);
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock
        = PowerMockito.mock(DynamicPropertiesStore.class);
    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);
    when(managerMock.isTooManyPending()).thenReturn(false);
    when(chainBaseManagerMock.getDynamicPropertiesStore())
        .thenReturn(dynamicPropertiesStoreMock);
    when(dynamicPropertiesStoreMock.supportVM()).thenReturn(false);

    Whitebox.setInternalState(wallet, "tronNetDelegate", tronNetDelegateMock);
    Whitebox.setInternalState(wallet, "dbManager", managerMock);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    Whitebox.setInternalState(wallet, "trxCacheEnable", false);

    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);

    Assert.assertEquals(GrpcAPI.Return.response_code.OTHER_ERROR, ret.getCode());
  }

  private Protocol.Transaction getExampleTrans() {
    BalanceContract.TransferContract transferContract =
        BalanceContract.TransferContract.newBuilder()
            .setAmount(10)
            .setOwnerAddress(ByteString.copyFromUtf8("aaa"))
            .setToAddress(ByteString.copyFromUtf8("bbb"))
            .build();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 6666; i++) {
      sb.append("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }
    return Protocol.Transaction.newBuilder().setRawData(
        Protocol.Transaction.raw.newBuilder()
            .setData(ByteString.copyFrom(sb.toString().getBytes(StandardCharsets.UTF_8)))
            .addContract(
                Protocol.Transaction.Contract.newBuilder()
                    .setParameter(Any.pack(transferContract))
                    .setType(Protocol.Transaction.Contract.ContractType.TransferContract)))
        .build();
  }

  private void mockEnv(Wallet wallet, TronException tronException) throws Exception {
    TronNetDelegate tronNetDelegateMock = PowerMockito.mock(TronNetDelegate.class);
    Manager managerMock = PowerMockito.mock(Manager.class);
    ChainBaseManager chainBaseManagerMock
        = PowerMockito.mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock
        = PowerMockito.mock(DynamicPropertiesStore.class);
    TransactionMessage transactionMessageMock
        = PowerMockito.mock(TransactionMessage.class);

    when(tronNetDelegateMock.isBlockUnsolidified()).thenReturn(false);
    when(managerMock.isTooManyPending()).thenReturn(false);
    when(chainBaseManagerMock.getDynamicPropertiesStore())
        .thenReturn(dynamicPropertiesStoreMock);
    when(dynamicPropertiesStoreMock.supportVM()).thenReturn(false);
    PowerMockito.whenNew(TransactionMessage.class)
        .withAnyArguments().thenReturn(transactionMessageMock);
    doThrow(tronException).when(managerMock).pushTransaction(any());

    Whitebox.setInternalState(wallet, "tronNetDelegate", tronNetDelegateMock);
    Whitebox.setInternalState(wallet, "dbManager", managerMock);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    Whitebox.setInternalState(wallet, "trxCacheEnable", false);
  }

  @Test
  public void testBroadcastTransactionValidateSignatureException() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new ValidateSignatureException());
    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.SIGERROR, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionValidateContractExeException() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new ContractExeException());
    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.CONTRACT_EXE_ERROR, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionValidateAccountResourceInsufficientException()
      throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new AccountResourceInsufficientException(""));
    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.BANDWITH_ERROR, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionValidateDupTransactionException()
      throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new DupTransactionException(""));
    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.DUP_TRANSACTION_ERROR, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionValidateTaposException() throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new TaposException(""));
    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.TAPOS_ERROR, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionValidateTooBigTransactionException()
      throws Exception {
    Wallet wallet = new Wallet();
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new TooBigTransactionException(""));

    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.TOO_BIG_TRANSACTION_ERROR, ret.getCode());
  }

  @Test
  public void testGetBlockByNum() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    doThrow(new ItemNotFoundException()).when(chainBaseManagerMock).getBlockByNum(anyLong());

    Protocol.Block block = Whitebox.invokeMethod(wallet,
        "getBlockByNum", 0L);
    Assert.assertNull(block);
  }

  @Test
  public void testGetBlockCapsuleByNum() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    doThrow(new ItemNotFoundException()).when(chainBaseManagerMock).getBlockByNum(anyLong());

    Protocol.Block block = Whitebox.invokeMethod(wallet,
        "getBlockCapsuleByNum", 0L);
    Assert.assertNull(block);
  }

  @Test
  public void testGetTransactionCountByBlockNum() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    doThrow(new ItemNotFoundException()).when(chainBaseManagerMock).getBlockByNum(anyLong());

    long count = Whitebox.invokeMethod(wallet,
        "getTransactionCountByBlockNum", 0L);
    Assert.assertEquals(count, 0L);
  }

  @Test
  public void testGetTransactionById() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = null;
    Protocol.Transaction transaction = Whitebox.invokeMethod(wallet,
        "getTransactionById", transactionId);
    Assert.assertEquals(transaction, null);
  }

  @Test
  public void testGetTransactionById2() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = ByteString.empty();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    TransactionStore transactionStoreMock = PowerMockito.mock(TransactionStore.class);

    when(chainBaseManagerMock.getTransactionStore()).thenReturn(transactionStoreMock);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    doThrow(new BadItemException()).when(transactionStoreMock).get(any());

    Protocol.Transaction transaction = Whitebox.invokeMethod(wallet,
        "getTransactionById", transactionId);
    Assert.assertEquals(transaction, null);
  }

  @Test
  public void testGetTransactionById3() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = ByteString.empty();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    TransactionStore transactionStoreMock = PowerMockito.mock(TransactionStore.class);
    TransactionCapsule transactionCapsuleMock = PowerMockito.mock(TransactionCapsule.class);
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();

    when(chainBaseManagerMock.getTransactionStore()).thenReturn(transactionStoreMock);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    when(transactionStoreMock.get(any())).thenReturn(transactionCapsuleMock);
    when(transactionCapsuleMock.getInstance()).thenReturn(transaction);

    Protocol.Transaction transactionRet = Whitebox.invokeMethod(wallet,
        "getTransactionById", transactionId);
    Assert.assertEquals(transaction, transactionRet);
  }

  @Test
  public void testGetTransactionCapsuleById() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = null;
    Protocol.Transaction transaction = Whitebox.invokeMethod(wallet,
        "getTransactionCapsuleById", transactionId);
    Assert.assertEquals(transaction, null);
  }

  @Test
  public void testGetTransactionCapsuleById1() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = ByteString.empty();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    TransactionStore transactionStoreMock = PowerMockito.mock(TransactionStore.class);

    when(chainBaseManagerMock.getTransactionStore()).thenReturn(transactionStoreMock);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    doThrow(new BadItemException()).when(transactionStoreMock).get(any());

    Protocol.Transaction transaction = Whitebox.invokeMethod(wallet,
        "getTransactionCapsuleById", transactionId);
    Assert.assertEquals(transaction, null);
  }

  @Test
  public void testGetTransactionInfoById() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = null;
    Protocol.TransactionInfo transactionInfo = Whitebox.invokeMethod(wallet,
        "getTransactionInfoById", transactionId);
    Assert.assertEquals(transactionInfo, null);
  }

  @Test
  public void testGetTransactionInfoById1() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = ByteString.empty();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    TransactionRetStore transactionRetStoreMock = PowerMockito.mock(TransactionRetStore.class);

    when(chainBaseManagerMock.getTransactionRetStore()).thenReturn(transactionRetStoreMock);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    doThrow(new BadItemException()).when(transactionRetStoreMock).getTransactionInfo(any());

    Protocol.TransactionInfo transactionInfo = Whitebox.invokeMethod(wallet,
        "getTransactionInfoById", transactionId);
    Assert.assertEquals(transactionInfo, null);
  }

  @Test
  public void testGetTransactionInfoById2() throws Exception {
    Wallet wallet = new Wallet();
    ByteString transactionId = ByteString.empty();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    TransactionRetStore transactionRetStoreMock = PowerMockito.mock(TransactionRetStore.class);
    TransactionHistoryStore transactionHistoryStoreMock =
        PowerMockito.mock(TransactionHistoryStore.class);

    when(chainBaseManagerMock.getTransactionRetStore())
        .thenReturn(transactionRetStoreMock);
    when(chainBaseManagerMock.getTransactionHistoryStore())
        .thenReturn(transactionHistoryStoreMock);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    when(transactionRetStoreMock.getTransactionInfo(any())).thenReturn(null);
    doThrow(new BadItemException()).when(transactionHistoryStoreMock).get(any());

    Protocol.TransactionInfo transactionInfo = Whitebox.invokeMethod(wallet,
        "getTransactionInfoById", transactionId);
    Assert.assertEquals(transactionInfo, null);
  }

  @Test
  public void testGetProposalById() throws Exception {
    Wallet wallet = new Wallet();
    ByteString proposalId = null;
    Protocol.TransactionInfo transactionInfo = Whitebox.invokeMethod(wallet,
        "getProposalById", proposalId);
    Assert.assertEquals(transactionInfo, null);
  }

  @Test
  public void testGetMemoFeePrices() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock =
        PowerMockito.mock(DynamicPropertiesStore.class);

    when(chainBaseManagerMock.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStoreMock);
    doThrow(new IllegalArgumentException("not found MEMO_FEE_HISTORY"))
        .when(dynamicPropertiesStoreMock).getMemoFeeHistory();

    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);

    GrpcAPI.PricesResponseMessage responseMessage = Whitebox.invokeMethod(wallet,
        "getMemoFeePrices");
    Assert.assertEquals(responseMessage, null);
  }

  @Test
  public void testGetEnergyFeeByTime() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock =
        PowerMockito.mock(DynamicPropertiesStore.class);
    long now = System.currentTimeMillis();

    when(chainBaseManagerMock.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStoreMock);
    doThrow(new IllegalArgumentException("not found ENERGY_PRICE_HISTORY"))
        .when(dynamicPropertiesStoreMock).getEnergyPriceHistory();
    when(dynamicPropertiesStoreMock.getEnergyFee()).thenReturn(10L);

    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);

    long energyFee = Whitebox.invokeMethod(wallet,
        "getEnergyFee", now);
    Assert.assertEquals(energyFee, 10L);
  }

  @Test
  public void testGetEnergyPrices() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock =
        PowerMockito.mock(DynamicPropertiesStore.class);

    when(chainBaseManagerMock.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStoreMock);
    doThrow(new IllegalArgumentException("not found ENERGY_PRICE_HISTORY"))
        .when(dynamicPropertiesStoreMock).getEnergyPriceHistory();

    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);

    GrpcAPI.PricesResponseMessage pricesResponseMessage = Whitebox.invokeMethod(wallet,
        "getEnergyPrices");
    Assert.assertEquals(pricesResponseMessage, null);
  }

  @Test
  public void testGetBandwidthPrices() throws Exception {
    Wallet wallet = new Wallet();
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    DynamicPropertiesStore dynamicPropertiesStoreMock =
        PowerMockito.mock(DynamicPropertiesStore.class);

    when(chainBaseManagerMock.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStoreMock);
    doThrow(new IllegalArgumentException("not found BANDWIDTH_PRICE_HISTORY"))
        .when(dynamicPropertiesStoreMock).getBandwidthPriceHistory();

    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);

    GrpcAPI.PricesResponseMessage pricesResponseMessage = Whitebox.invokeMethod(wallet,
        "getBandwidthPrices");
    Assert.assertEquals(pricesResponseMessage, null);
  }

  @Test
  public void testCheckBlockIdentifier() {
    Wallet wallet = new Wallet();
    BalanceContract.BlockBalanceTrace.BlockIdentifier blockIdentifier =
        BalanceContract.BlockBalanceTrace.BlockIdentifier.newBuilder()
        .build();
    blockIdentifier = blockIdentifier.getDefaultInstanceForType();
    try {
      wallet.checkBlockIdentifier(blockIdentifier);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IllegalArgumentException);
    }

    blockIdentifier =
        BalanceContract.BlockBalanceTrace.BlockIdentifier.newBuilder()
            .setNumber(-1L)
            .build();
    try {
      wallet.checkBlockIdentifier(blockIdentifier);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IllegalArgumentException);
    }

    blockIdentifier =
        BalanceContract.BlockBalanceTrace.BlockIdentifier.newBuilder()
            .setHash(ByteString.copyFrom("".getBytes(StandardCharsets.UTF_8)))
            .build();
    try {
      wallet.checkBlockIdentifier(blockIdentifier);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IllegalArgumentException);
    }
  }

  @Test
  public void testCheckAccountIdentifier() {
    Wallet wallet = new Wallet();
    BalanceContract.AccountIdentifier accountIdentifier =
        BalanceContract.AccountIdentifier.newBuilder()
            .build();
    accountIdentifier = accountIdentifier.getDefaultInstanceForType();
    try {
      wallet.checkAccountIdentifier(accountIdentifier);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IllegalArgumentException);
    }

    accountIdentifier = BalanceContract.AccountIdentifier.newBuilder()
            .build();
    try {
      wallet.checkAccountIdentifier(accountIdentifier);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IllegalArgumentException);
    }
  }

  @Test
  public void testGetTriggerInputForShieldedTRC20Contract()  {
    Wallet wallet = new Wallet();
    GrpcAPI.ShieldedTRC20TriggerContractParameters.Builder triggerParam =
        GrpcAPI.ShieldedTRC20TriggerContractParameters
            .newBuilder();
    GrpcAPI.ShieldedTRC20Parameters shieldedTRC20Parameters =
        GrpcAPI.ShieldedTRC20Parameters.newBuilder().build();
    GrpcAPI.BytesMessage bytesMessage =
        GrpcAPI.BytesMessage.newBuilder().build();

    triggerParam.setShieldedTRC20Parameters(shieldedTRC20Parameters);
    triggerParam.addSpendAuthoritySignature(bytesMessage);

    CommonParameter commonParameterMock = PowerMockito.mock(Args.class);
    PowerMockito.mockStatic(CommonParameter.class);
    PowerMockito.when(CommonParameter.getInstance()).thenReturn(commonParameterMock);
    when(commonParameterMock.isFullNodeAllowShieldedTransactionArgs()).thenReturn(true);

    try {
      wallet.getTriggerInputForShieldedTRC20Contract(triggerParam.build());
    } catch (Exception e) {
      Assert.assertTrue( e instanceof  ZksnarkException);
    }

  }

  @Test
  public void testGetTriggerInputForShieldedTRC20Contract1()
      throws ZksnarkException, ContractValidateException {
    Wallet wallet = new Wallet();
    ShieldContract.SpendDescription spendDescription =
        ShieldContract.SpendDescription.newBuilder().build();
    GrpcAPI.ShieldedTRC20TriggerContractParameters.Builder triggerParam =
        GrpcAPI.ShieldedTRC20TriggerContractParameters
            .newBuilder();
    GrpcAPI.ShieldedTRC20Parameters shieldedTRC20Parameters =
        GrpcAPI.ShieldedTRC20Parameters.newBuilder()
            .addSpendDescription(spendDescription)
            .setParameterType("transfer")
            .build();
    GrpcAPI.BytesMessage bytesMessage =
        GrpcAPI.BytesMessage.newBuilder().build();

    triggerParam.setShieldedTRC20Parameters(shieldedTRC20Parameters);
    triggerParam.addSpendAuthoritySignature(bytesMessage);

    CommonParameter commonParameterMock = PowerMockito.mock(Args.class);
    PowerMockito.mockStatic(CommonParameter.class);
    PowerMockito.when(CommonParameter.getInstance()).thenReturn(commonParameterMock);
    when(commonParameterMock.isFullNodeAllowShieldedTransactionArgs()).thenReturn(true);

    GrpcAPI.BytesMessage reponse =
        wallet.getTriggerInputForShieldedTRC20Contract(triggerParam.build());
    Assert.assertNotNull(reponse);
  }

  @Test
  public void testGetShieldedContractScalingFactorException() {
    Wallet wallet = new Wallet();
    byte[] contractAddress = "".getBytes(StandardCharsets.UTF_8);
    try {
      wallet.getShieldedContractScalingFactor(contractAddress);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }
  }

  @Test
  public void testGetShieldedContractScalingFactorRuntimeException()
      throws VMIllegalException, HeaderNotFound, ContractValidateException, ContractExeException {
    Wallet walletMock = mock(Wallet.class);
    byte[] contractAddress = "".getBytes(StandardCharsets.UTF_8);
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    when(walletMock.triggerConstantContract(any(),any(),any(),any())).thenReturn(transaction);
    when(walletMock.getShieldedContractScalingFactor(any())).thenCallRealMethod();

    try {
      walletMock.getShieldedContractScalingFactor(contractAddress);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }
  }

  @Test
  public void testGetShieldedContractScalingFactorSuccess()
      throws Exception {
    Wallet walletMock = mock(Wallet.class);
    byte[] contractAddress = "".getBytes(StandardCharsets.UTF_8);
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    when(walletMock.triggerConstantContract(any(),any(),any(),any())).thenReturn(transaction);
    when(walletMock.createTransactionCapsule(any(), any())).thenReturn(new TransactionCapsule(transaction));
    when(walletMock.getShieldedContractScalingFactor(any())).thenCallRealMethod();
    try {
      byte[] listBytes = walletMock.getShieldedContractScalingFactor(contractAddress);
      Assert.assertNotNull(listBytes);
    } catch (Exception e) {
      Assert.assertNull(e);
    }
  }

  @Test
  public void testGetShieldedContractScalingFactorContractExeException()
      throws Exception {
    Wallet walletMock = mock(Wallet.class);
    byte[] contractAddress = "".getBytes(StandardCharsets.UTF_8);
    Protocol.Transaction transaction = Protocol.Transaction.newBuilder().build();
    doThrow(new ContractExeException(""))
        .when(walletMock).triggerConstantContract(any(),any(),any(),any());
    when(walletMock.createTransactionCapsule(any(), any()))
        .thenReturn(new TransactionCapsule(transaction));
    when(walletMock.getShieldedContractScalingFactor(any())).thenCallRealMethod();
    try {
      walletMock.getShieldedContractScalingFactor(contractAddress);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractExeException);
    }
  }

  @Test
  public void testCheckBigIntegerRange() {
    Wallet wallet = new Wallet();
    try {
      Whitebox.invokeMethod(wallet, "checkBigIntegerRange",
          new BigInteger("-1"));
    } catch (Exception e) {
      Assert.assertEquals("public amount must be non-negative", e.getMessage());
    }
  }

  @Test
  public void testCheckPublicAmount() throws ContractExeException {
    Wallet walletMock = mock(Wallet.class);

    byte[] address = "".getBytes(StandardCharsets.UTF_8);
    BigInteger fromAmount = new BigInteger("10");
    BigInteger toAmount = new BigInteger("10");
    doThrow(new ContractExeException("")).when(walletMock).getShieldedContractScalingFactor(any());
    try {
      PowerMockito.when(walletMock,
          "checkPublicAmount",
          address, fromAmount, toAmount
      ).thenCallRealMethod();
    } catch (Exception e) {
      Assert.assertTrue( e instanceof ContractExeException);
    }
  }

  @Test
  public void testCheckPublicAmount1() throws ContractExeException {
    Wallet walletMock = mock(Wallet.class);

    byte[] address = "".getBytes(StandardCharsets.UTF_8);
    BigInteger fromAmount = new BigInteger("300");
    BigInteger toAmount = new BigInteger("255");

    byte[] scalingFactorBytes = ByteUtil.bigIntegerToBytes(new BigInteger("-1"));

    when(walletMock.getShieldedContractScalingFactor(any())).thenReturn(scalingFactorBytes);
    try {
      PowerMockito.when(walletMock,
          "checkPublicAmount",
          address, fromAmount, toAmount
      ).thenCallRealMethod();
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
    }
  }

  @Test
  public void testCheckPublicAmount2() throws ContractExeException {
    Wallet walletMock = mock(Wallet.class);

    byte[] address = "".getBytes(StandardCharsets.UTF_8);
    BigInteger fromAmount = new BigInteger("300");
    BigInteger toAmount = new BigInteger("255");

    byte[] scalingFactorBytes = ByteUtil.bigIntegerToBytes(new BigInteger("-1"));
    PowerMockito.mockStatic(ByteUtil.class);
    PowerMockito.when(ByteUtil.bytesToBigInteger(any())).thenReturn(new BigInteger("-1"));
    when(walletMock.getShieldedContractScalingFactor(any())).thenReturn(scalingFactorBytes);
    try {
      PowerMockito.when(walletMock,
          "checkPublicAmount",
          address, fromAmount, toAmount
      ).thenCallRealMethod();
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ContractValidateException);
    }
  }

  @Test
  public void testGetShieldedTRC20Nullifier() {
    GrpcAPI.Note note = GrpcAPI.Note.newBuilder()
        .setValue(100)
        .setPaymentAddress("address")
        .setRcm(ByteString.copyFrom("rcm".getBytes(StandardCharsets.UTF_8)))
        .setMemo(ByteString.copyFrom("memo".getBytes(StandardCharsets.UTF_8)))
        .build();
    long pos = 100L;
    byte[] ak = "ak".getBytes(StandardCharsets.UTF_8);
    byte[] nk = "nk".getBytes(StandardCharsets.UTF_8);
    Wallet walletMock = mock(Wallet.class);
    PowerMockito.mockStatic(KeyIo.class);
    PowerMockito.when(KeyIo.decodePaymentAddress(any())).thenReturn(null);
    try {
      PowerMockito.when(walletMock,
          "getShieldedTRC20Nullifier",
          note, pos, ak, nk
      ).thenCallRealMethod();
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ZksnarkException);
    }
  }

  @Test
  public void testGetShieldedTRC20LogType() {
    Wallet walletMock = mock(Wallet.class);
    Protocol.TransactionInfo.Log log = Protocol.TransactionInfo.Log.newBuilder().build();
    byte[] contractAddress = "contractAddress".getBytes(StandardCharsets.UTF_8);
    LazyStringArrayList topicsList = new LazyStringArrayList();
    try {
      Whitebox.invokeMethod(walletMock, "getShieldedTRC20LogType", log, contractAddress, topicsList);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ZksnarkException);
    }
  }
  @Test
  public void testGetShieldedTRC20LogType1() {
    Wallet wallet = new Wallet();
    final String SHIELDED_CONTRACT_ADDRESS_STR = "TGAmX5AqVUoXCf8MoHxbuhQPmhGfWTnEgA";
    byte[] contractAddress = WalletClient.decodeFromBase58Check(SHIELDED_CONTRACT_ADDRESS_STR);

    byte[] addressWithoutPrefix = new byte[20];
    System.arraycopy(contractAddress, 1, addressWithoutPrefix, 0, 20);
    Protocol.TransactionInfo.Log log = Protocol.TransactionInfo.Log.newBuilder()
        .setAddress(ByteString.copyFrom(addressWithoutPrefix))
        .build();

    LazyStringArrayList topicsList = new LazyStringArrayList();
    try {
      Whitebox.invokeMethod(wallet,
          "getShieldedTRC20LogType",
          log,
          contractAddress,
          topicsList);
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
    Assert.assertTrue(true);
  }


  @Test
  public void testGetShieldedTRC20LogType2() {
    Wallet wallet = new Wallet();
    final String SHIELDED_CONTRACT_ADDRESS_STR = "TGAmX5AqVUoXCf8MoHxbuhQPmhGfWTnEgA";
    byte[] contractAddress = WalletClient.decodeFromBase58Check(SHIELDED_CONTRACT_ADDRESS_STR);

    byte[] addressWithoutPrefix = new byte[20];
    System.arraycopy(contractAddress, 1, addressWithoutPrefix, 0, 20);
    Protocol.TransactionInfo.Log log = Protocol.TransactionInfo.Log.newBuilder()
        .setAddress(ByteString.copyFrom(addressWithoutPrefix))
        .addTopics(ByteString.copyFrom("topic".getBytes()))
        .build();

    LazyStringArrayList topicsList = new LazyStringArrayList();
    topicsList.add("topic");
    try {
      Whitebox.invokeMethod(wallet,
          "getShieldedTRC20LogType",
          log,
          contractAddress,
          topicsList);
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
    Assert.assertTrue(true);
  }

  @Test
  public void testBuildShieldedTRC20InputWithAK() throws ZksnarkException {
    ShieldedTRC20ParametersBuilder builder =  new ShieldedTRC20ParametersBuilder("transfer");
    GrpcAPI.Note note = GrpcAPI.Note.newBuilder()
        .setValue(100)
        .setPaymentAddress("address")
        .setRcm(ByteString.copyFrom("rcm".getBytes(StandardCharsets.UTF_8)))
        .setMemo(ByteString.copyFrom("memo".getBytes(StandardCharsets.UTF_8)))
        .build();
    GrpcAPI.SpendNoteTRC20 spendNote = GrpcAPI.SpendNoteTRC20.newBuilder()
        .setNote(note)
        .setAlpha(ByteString.copyFrom("alpha".getBytes()))
        .setRoot(ByteString.copyFrom("root".getBytes()))
        .setPath(ByteString.copyFrom("path".getBytes()))
        .setPos(0L)
        .build();
    byte[] ak = "ak".getBytes(StandardCharsets.UTF_8);
    byte[] nk = "nk".getBytes(StandardCharsets.UTF_8);
    Wallet walletMock = mock(Wallet.class);
    PowerMockito.mockStatic(KeyIo.class);
    PowerMockito.when(KeyIo.decodePaymentAddress(any())).thenReturn(null);
    try {
      PowerMockito.when(walletMock,
          "buildShieldedTRC20InputWithAK",
          builder,
          spendNote,
          ak, nk
      ).thenCallRealMethod();
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ZksnarkException);
    }
  }

  @Test
  public void testBuildShieldedTRC20InputWithAK1() throws ZksnarkException {
    ShieldedTRC20ParametersBuilder builder =  new ShieldedTRC20ParametersBuilder("transfer");
    GrpcAPI.Note note = GrpcAPI.Note.newBuilder()
        .setValue(100)
        .setPaymentAddress("address")
        .setRcm(ByteString.copyFrom("rcm".getBytes(StandardCharsets.UTF_8)))
        .setMemo(ByteString.copyFrom("memo".getBytes(StandardCharsets.UTF_8)))
        .build();
    GrpcAPI.SpendNoteTRC20 spendNote = GrpcAPI.SpendNoteTRC20.newBuilder()
        .setNote(note)
        .setAlpha(ByteString.copyFrom("alpha".getBytes()))
        .setRoot(ByteString.copyFrom("root".getBytes()))
        .setPath(ByteString.copyFrom("path".getBytes()))
        .setPos(0L)
        .build();
    byte[] ak = "ak".getBytes(StandardCharsets.UTF_8);
    byte[] nk = "nk".getBytes(StandardCharsets.UTF_8);
    PaymentAddress paymentAddress = mock(PaymentAddress.class);
    DiversifierT diversifierT = mock(DiversifierT.class);
    Wallet walletMock = mock(Wallet.class);
    PowerMockito.mockStatic(KeyIo.class);
    PowerMockito.when(KeyIo.decodePaymentAddress(any())).thenReturn(paymentAddress);
    when(paymentAddress.getD()).thenReturn(diversifierT);
    when(paymentAddress.getPkD()).thenReturn("pkd".getBytes());
    try {
      PowerMockito.when(walletMock,
          "buildShieldedTRC20InputWithAK",
          builder,
          spendNote,
          ak, nk
      ).thenCallRealMethod();
    } catch (Exception e) {
      Assert.assertTrue(e instanceof ZksnarkException);
    }
  }

  @Test
  public void testBuildShieldedTRC20Input() throws ZksnarkException {
    ShieldedTRC20ParametersBuilder builder =  new ShieldedTRC20ParametersBuilder("transfer");
    GrpcAPI.Note note = GrpcAPI.Note.newBuilder()
        .setValue(100)
        .setPaymentAddress("address")
        .setRcm(ByteString.copyFrom("rcm".getBytes(StandardCharsets.UTF_8)))
        .setMemo(ByteString.copyFrom("memo".getBytes(StandardCharsets.UTF_8)))
        .build();
    GrpcAPI.SpendNoteTRC20 spendNote = GrpcAPI.SpendNoteTRC20.newBuilder()
        .setNote(note)
        .setAlpha(ByteString.copyFrom("alpha".getBytes()))
        .setRoot(ByteString.copyFrom("root".getBytes()))
        .setPath(ByteString.copyFrom("path".getBytes()))
        .setPos(0L)
        .build();
    ExpandedSpendingKey expandedSpendingKey = mock(ExpandedSpendingKey.class);
    PaymentAddress paymentAddress = mock(PaymentAddress.class);
    DiversifierT diversifierT = mock(DiversifierT.class);
    Wallet walletMock = mock(Wallet.class);
    PowerMockito.mockStatic(KeyIo.class);
    PowerMockito.when(KeyIo.decodePaymentAddress(any())).thenReturn(paymentAddress);
    when(paymentAddress.getD()).thenReturn(diversifierT);
    when(paymentAddress.getPkD()).thenReturn("pkd".getBytes());
    try {
      PowerMockito.when(walletMock,
          "buildShieldedTRC20Input",
          builder,
          spendNote,
          expandedSpendingKey
      ).thenCallRealMethod();
    } catch (Exception e) {
      Assert.assertTrue(false);
    }
  }

  @Test
  public void testGetContractInfo() {
    Wallet wallet = new Wallet();
    GrpcAPI.BytesMessage bytesMessage = GrpcAPI.BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom("test".getBytes()))
        .build();

    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    AccountStore accountStore = PowerMockito.mock(AccountStore.class);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    when(chainBaseManagerMock.getAccountStore()).thenReturn(accountStore);
    when(accountStore.get(any())).thenReturn(null);

    SmartContractOuterClass.SmartContractDataWrapper smartContractDataWrapper =
        wallet.getContractInfo(bytesMessage);
    Assert.assertEquals(null, smartContractDataWrapper);
  }

  @Test
  public void testGetContractInfo1() {
    Wallet wallet = new Wallet();
    GrpcAPI.BytesMessage bytesMessage = GrpcAPI.BytesMessage.newBuilder()
        .setValue(ByteString.copyFrom("test".getBytes()))
        .build();

    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    AccountStore accountStore = PowerMockito.mock(AccountStore.class);
    ContractStore contractStore = PowerMockito.mock(ContractStore.class);
    AbiStore abiStore = mock(AbiStore.class);
    CodeStore codeStore = mock(CodeStore.class);
    ContractStateStore contractStateStore = mock(ContractStateStore.class);
    DynamicPropertiesStore dynamicPropertiesStore = mock(DynamicPropertiesStore.class);

    AccountCapsule accountCapsule = mock(AccountCapsule.class);
    ContractCapsule contractCapsule = mock(ContractCapsule.class);
    ContractStateCapsule contractStateCapsule = new ContractStateCapsule(10L);

    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    when(chainBaseManagerMock.getAccountStore()).thenReturn(accountStore);
    when(chainBaseManagerMock.getContractStore()).thenReturn(contractStore);
    when(chainBaseManagerMock.getAbiStore()).thenReturn(abiStore);
    when(chainBaseManagerMock.getCodeStore()).thenReturn(codeStore);
    when(chainBaseManagerMock.getContractStateStore()).thenReturn(contractStateStore);
    when(chainBaseManagerMock.getDynamicPropertiesStore()).thenReturn(dynamicPropertiesStore);

    when(accountStore.get(any())).thenReturn(accountCapsule);
    when(contractStore.get(any())).thenReturn(contractCapsule);
    when(contractCapsule.generateWrapper())
        .thenReturn(SmartContractOuterClass.SmartContractDataWrapper.newBuilder().build());
    when(abiStore.get(any())).thenReturn(null);
    when(codeStore.get(any())).thenReturn(null);
    when(contractStateStore.get(any())).thenReturn(contractStateCapsule);
    when(dynamicPropertiesStore.getCurrentCycleNumber()).thenReturn(100L);

    SmartContractOuterClass.SmartContractDataWrapper smartContractDataWrapper =
        wallet.getContractInfo(bytesMessage);
    Assert.assertNotEquals(null, smartContractDataWrapper);
  }
}
