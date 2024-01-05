package org.tron.consensus.pbft;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AtomicLongMap;
import com.google.protobuf.ByteString;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.tron.common.cache.CommonCache;
import org.tron.common.cache.CommonCacheBuilder;

import org.tron.common.parameter.CommonParameter;
import org.tron.consensus.base.Param;
import org.tron.consensus.base.Param.Miner;
import org.tron.consensus.dpos.MaintenanceManager;
import org.tron.consensus.pbft.message.PbftBaseMessage;
import org.tron.consensus.pbft.message.PbftMessage;
import org.tron.core.ChainBaseManager;
import org.tron.protos.Protocol.PBFTMessage.DataType;



@Slf4j(topic = "pbft")
@Component
public class PbftMessageHandle {

  public static final int TIME_OUT = 60000;
  //Pre-preparation stage voting information
  private Set<String> preVotes = Sets.newConcurrentHashSet();
  //Preparation stage voting information
  private Map<String, PbftMessage> pareVoteMap = Maps.newConcurrentMap();
  private AtomicLongMap<String> agreePare = AtomicLongMap.create();
  private CommonCache<String, PbftMessage> pareMsgCache = CommonCacheBuilder.newBuilder()
      .initialCapacity(1000).maximumSize(10000).expireAfterWrite(2, TimeUnit.MINUTES).build();
  //Submit stage voting information
  private Map<String, PbftMessage> commitVoteMap = Maps.newConcurrentMap();
  private AtomicLongMap<String> agreeCommit = AtomicLongMap.create();
  private CommonCache<String, PbftMessage> commitMsgCache = CommonCacheBuilder.newBuilder()
      .initialCapacity(1000).maximumSize(10000).expireAfterWrite(2, TimeUnit.MINUTES).build();
  //pbft timeout
  private Map<String, Long> timeOuts = Maps.newConcurrentMap();
  //Successfully processed request
  private Map<String, PbftMessage> doneMsg = Maps.newConcurrentMap();

  private LoadingCache<String, List<ByteString>> dataSignCache = CacheBuilder.newBuilder()
      .initialCapacity(100).maximumSize(1000).expireAfterWrite(2, TimeUnit.MINUTES).build(
          new CacheLoader<String, List<ByteString>>() {
            @Override
            public List<ByteString> load(String s) throws Exception {
              return new ArrayList<>();
            }
          });

  private  boolean isCaffeine = CommonParameter.getInstance().caffeineCacheActive;
  private com.github.benmanes.caffeine.cache.LoadingCache<String, List<ByteString>> dataCaffeineSignCache =
      Caffeine.newBuilder()
          .initialCapacity(100).maximumSize(1000).expireAfterWrite(2, TimeUnit.MINUTES)
          .build(key -> createExpensiveGraph(key));


  private PbftMessage srPbftMessage;

  private Timer timer = new Timer("pbft-timer");

  @Autowired
  private PbftMessageAction pbftMessageAction;
  @Setter
  private MaintenanceManager maintenanceManager;
  @Autowired
  private ChainBaseManager chainBaseManager;


  private List<ByteString> createExpensiveGraph(String s) {
    return new ArrayList<>();
  }

  @PostConstruct
  public void init() {
    start();
  }

  @PreDestroy
  public void close() {
    try {
      timer.cancel();
      // help GC
      timer = null;
    } catch (Exception e) {
      logger.warn("pbft-timer cancel error", e);
    }
  }

  public List<Miner> getSrMinerList(long epoch) {
    List<ByteString> compareList;
    if (epoch > maintenanceManager.getBeforeMaintenanceTime()) {
      compareList = maintenanceManager.getCurrentWitness();
    } else {
      compareList = maintenanceManager.getBeforeWitness();
    }
    return Param.getInstance().getMiners().stream()
        .filter(miner -> compareList.contains(miner.getWitnessAddress()))
        .collect(Collectors.toList());
  }

  public void onPrePrepare(PbftMessage message) {
    String key = message.getNo();
    if (message.isSwitch()) {//if is block chain switch,remove the before proposal
      logger.warn("block chain switch, again proposal block num: {}, data: {}",
          message.getNumber(), message.getDataString());
      remove(key);
      return;
    }
    if (preVotes.contains(key)) {
      //The description has been initiated, can not be repeated, can only initiate a vote at the same height
      return;
    }
    preVotes.add(key);
    //Start timeout control
    timeOuts.put(key, System.currentTimeMillis());
    //
    checkPrepareMsgCache(key);
    //Into the preparation phase, if not the sr node does not need to be prepared
    long epoch = message.getPbftMessage().getRawData().getEpoch();
    if (!checkIsCanSendMsg(epoch)) {
      return;
    }
    for (Miner miner : getSrMinerList(epoch)) {
      PbftMessage paMessage = message.buildPrePareMessage(miner);
      forwardMessage(paMessage);
      try {
        paMessage.analyzeSignature();
      } catch (SignatureException e) {
        logger.error("", e);
      }
      onPrepare(paMessage);
    }
    if (message.getDataType() == DataType.SRL) {
      srPbftMessage = message;
    }
  }

  public synchronized void onPrepare(PbftMessage message) {
    String key = message.getKey();

    if (!preVotes.contains(message.getNo())) {
      //Must be prepared in advance
      pareMsgCache.put(key, message);
      return;
    }
    if (pareVoteMap.containsKey(key)) {
      //Explain that the vote has been voted and cannot be repeated
      if (!pareVoteMap.get(key).getPbftMessage().getRawData().getData()
          .equals(message.getPbftMessage().getRawData().getData())) {
        //todo:Penalize multiple signatures at the same height

      }
      return;
    }
    pareVoteMap.put(key, message);
    //
    checkCommitMsgCache(message.getNo());
    long epoch = message.getPbftMessage().getRawData().getEpoch();
    if (!checkIsCanSendMsg(epoch)) {
      return;
    }
    //The number of votes plus 1
    if (!doneMsg.containsKey(message.getNo())) {
      long agCou = agreePare.incrementAndGet(message.getDataKey());
      if (agCou >= Param.getInstance().getAgreeNodeCount()) {
        agreePare.remove(message.getDataKey());
        //Entering the submission stage
        for (Miner miner : getSrMinerList(epoch)) {
          PbftMessage cmMessage = message.buildCommitMessage(miner);
          doneMsg.put(message.getNo(), cmMessage);
          forwardMessage(cmMessage);
          try {
            cmMessage.analyzeSignature();
          } catch (SignatureException e) {
            logger.error("", e);
          }
          onCommit(cmMessage);
        }
      }
    }
    //Subsequent votes will definitely not be satisfied, timeout will be automatically cleared.
  }

  public synchronized void onCommit(PbftMessage message) {
    String key = message.getKey();
    if (!pareVoteMap.containsKey(key)) {
      //Must be prepared
      commitMsgCache.put(key, message);
      return;
    }
    if (commitVoteMap.containsKey(key)) {
      //Explain that the node has voted on the data and cannot vote repeatedly.
      if (!commitVoteMap.get(key).getPbftMessage().getRawData().getData()
          .equals(message.getPbftMessage().getRawData().getData())) {
        //todo:Penalize multiple signatures at the same height

      }
      return;
    }
    commitVoteMap.put(key, message);
    //The number of votes plus 1
    long agCou = agreeCommit.incrementAndGet(message.getDataKey());
    if (isCaffeine) {
      dataCaffeineSignCache.get(message.getDataKey())
          .add(message.getPbftMessage().getSignature());
    } else {
      dataSignCache.getUnchecked(message.getDataKey())
          .add(message.getPbftMessage().getSignature());
    }

    if (agCou >= Param.getInstance().getAgreeNodeCount()) {
      srPbftMessage = null;
      remove(message.getNo());
      //commit,
      if (!isSyncing()) {
        if (isCaffeine) {
          pbftMessageAction.action(message, dataCaffeineSignCache.get(message.getDataKey()));
        } else {
          pbftMessageAction.action(message, dataSignCache.getUnchecked(message.getDataKey()));
        }

      }
    }
  }

  public void onRequestData(PbftBaseMessage message) {

  }

  public void onChangeView(PbftBaseMessage message) {

  }

  public void forwardMessage(PbftBaseMessage message) {
    Param.getInstance().getPbftInterface().forwardMessage(message);
  }

  private void checkPrepareMsgCache(String key) {
    for (Entry<String, PbftMessage> entry : pareMsgCache.asMap().entrySet()) {
      if (StringUtils.startsWith(entry.getKey(), key)) {
        pareMsgCache.invalidate(entry.getKey());
        onPrepare(entry.getValue());
      }
    }
  }

  private void checkCommitMsgCache(String key) {
    for (Entry<String, PbftMessage> entry : commitMsgCache.asMap().entrySet()) {
      if (StringUtils.startsWith(entry.getKey(), key)) {
        commitMsgCache.invalidate(entry.getKey());
        onCommit(entry.getValue());
      }
    }
  }

  public boolean checkIsCanSendMsg(long epoch) {
    if (!Param.getInstance().isEnable()) {//is witness
      return false;
    }
    if (getSrMinerList(epoch).isEmpty()) {
      return false;
    }
    return !isSyncing();
  }

  public boolean isSyncing() {
    return Param.getInstance().getPbftInterface().isSyncing();
  }

  //Cleanup related status
  private void remove(String no) {
    String pre = String.valueOf(no) + "_";
    preVotes.remove(no);
    pareVoteMap.keySet().removeIf(vp -> StringUtils.startsWith(vp, pre));
    commitVoteMap.keySet().removeIf(vp -> StringUtils.startsWith(vp, pre));

    agreePare.asMap().keySet().forEach(s -> {
      if (StringUtils.startsWith(s, pre)) {
        long value = agreePare.remove(s);
        logger.debug("{} agreePare count:{}", no, value);
      }
    });
    agreeCommit.asMap().keySet().forEach(s -> {
      if (StringUtils.startsWith(s, pre)) {
        long value = agreeCommit.remove(s);
        logger.debug("{} agreeCommit count:{}", no, value);
      }
    });
    doneMsg.remove(no);
    timeOuts.remove(no);
    //just try once
    if (srPbftMessage != null && StringUtils.equals(no, srPbftMessage.getNo())) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
      }
      onPrePrepare(srPbftMessage);
      srPbftMessage = null;
    }
  }

  /**
   * Detect timeout
   */
  private void checkTimer() {
    for (Entry<String, Long> item : timeOuts.entrySet()) {
      if (System.currentTimeMillis() - item.getValue() > TIME_OUT) {
        //If the timeout has not been agreed, the vote will be invalid.
        logger.info("vote will be invalid:{}", item.getKey());
        remove(item.getKey());
      }
    }
  }

  public void start() {
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        checkTimer();
      }
    }, 10, 1000);
  }
}