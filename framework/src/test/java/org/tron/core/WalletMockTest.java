package org.tron.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.db.Manager;
import org.tron.core.db.TransactionStore;
import org.tron.core.exception.AccountResourceInsufficientException;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.DupTransactionException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.TaposException;
import org.tron.core.exception.TooBigTransactionException;
import org.tron.core.exception.TronException;
import org.tron.core.exception.ValidateSignatureException;
import org.tron.core.net.TronNetDelegate;
import org.tron.core.net.message.adv.TransactionMessage;
import org.tron.core.net.peer.PeerConnection;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.TransactionHistoryStore;
import org.tron.core.store.TransactionRetStore;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Wallet.class,
    TransactionCapsule.class,
    com.google.protobuf.Message.class,
    Protocol.Transaction.Contract.ContractType.class})
public class WalletMockTest {
  private Wallet wallet = new Wallet();

  @Before
  public void init() {
  }

  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void testSetTransactionNullException() throws Exception {
    TransactionCapsule transactionCapsuleMock
        = PowerMockito.mock(TransactionCapsule.class);
    Whitebox.invokeMethod(wallet,
        "setTransaction", transactionCapsuleMock);
    Assert.assertEquals(true, true);
  }

  @Test
  public void testCreateTransactionCapsuleWithoutValidateWithTimeoutNullException()
      throws Exception {
    com.google.protobuf.Message message =
        PowerMockito.mock(com.google.protobuf.Message.class);
    Protocol.Transaction.Contract.ContractType contractType =
        PowerMockito.mock(Protocol.Transaction.Contract.ContractType.class);
    long timeout = 100L;
    TransactionCapsule transactionCapsuleMock = PowerMockito.mock(TransactionCapsule.class);

    PowerMockito.whenNew(TransactionCapsule.class)
        .withAnyArguments().thenReturn(transactionCapsuleMock);
    Whitebox.invokeMethod(wallet,
        "createTransactionCapsuleWithoutValidateWithTimeout",
        message, contractType, timeout);
    Assert.assertEquals(true, true);
  }

  @Test
  public void testCreateTransactionCapsuleWithoutValidateWithTimeout()
      throws Exception {
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
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new ValidateSignatureException());
    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.SIGERROR, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionValidateContractExeException() throws Exception {
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new ContractExeException());
    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.CONTRACT_EXE_ERROR, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionValidateAccountResourceInsufficientException()
      throws Exception {
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new AccountResourceInsufficientException(""));
    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.BANDWITH_ERROR, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionValidateDupTransactionException()
      throws Exception {
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new DupTransactionException(""));
    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.DUP_TRANSACTION_ERROR, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionValidateTaposException() throws Exception {
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new TaposException(""));
    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.TAPOS_ERROR, ret.getCode());
  }

  @Test
  public void testBroadcastTransactionValidateTooBigTransactionException()
      throws Exception {
    Protocol.Transaction transaction = getExampleTrans();
    mockEnv(wallet, new TooBigTransactionException(""));

    GrpcAPI.Return ret = Whitebox.invokeMethod(wallet,
        "broadcastTransaction", transaction);
    Assert.assertEquals(GrpcAPI.Return.response_code.TOO_BIG_TRANSACTION_ERROR, ret.getCode());
  }

  @Test
  public void testGetBlockByNum() throws Exception {
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    doThrow(new ItemNotFoundException()).when(chainBaseManagerMock).getBlockByNum(anyLong());

    Protocol.Block block = Whitebox.invokeMethod(wallet,
        "getBlockByNum", 0L);
    Assert.assertNull(block);
  }

  @Test
  public void testGetBlockCapsuleByNum() throws Exception {
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    doThrow(new ItemNotFoundException()).when(chainBaseManagerMock).getBlockByNum(anyLong());

    Protocol.Block block = Whitebox.invokeMethod(wallet,
        "getBlockCapsuleByNum", 0L);
    Assert.assertNull(block);
  }

  @Test
  public void testGetTransactionCountByBlockNum() throws Exception {
    ChainBaseManager chainBaseManagerMock = PowerMockito.mock(ChainBaseManager.class);
    Whitebox.setInternalState(wallet, "chainBaseManager", chainBaseManagerMock);
    doThrow(new ItemNotFoundException()).when(chainBaseManagerMock).getBlockByNum(anyLong());

    long count = Whitebox.invokeMethod(wallet,
        "getTransactionCountByBlockNum", 0L);
    Assert.assertEquals(count, 0L);
  }

  @Test
  public void testGetTransactionById() throws Exception {
    ByteString transactionId = null;
    Protocol.Transaction transaction = Whitebox.invokeMethod(wallet,
        "getTransactionById", transactionId);
    Assert.assertEquals(transaction, null);
  }

  @Test
  public void testGetTransactionById2() throws Exception {
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
    ByteString transactionId = null;
    Protocol.Transaction transaction = Whitebox.invokeMethod(wallet,
        "getTransactionCapsuleById", transactionId);
    Assert.assertEquals(transaction, null);
  }

  @Test
  public void testGetTransactionCapsuleById1() throws Exception {
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
    ByteString transactionId = null;
    Protocol.TransactionInfo transactionInfo = Whitebox.invokeMethod(wallet,
        "getTransactionInfoById", transactionId);
    Assert.assertEquals(transactionInfo, null);
  }

  @Test
  public void testGetTransactionInfoById1() throws Exception {
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
    ByteString proposalId = null;
    Protocol.TransactionInfo transactionInfo = Whitebox.invokeMethod(wallet,
        "getProposalById", proposalId);
    Assert.assertEquals(transactionInfo, null);
  }

  @Test
  public void testGetMemoFeePrices() throws Exception {
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


}
