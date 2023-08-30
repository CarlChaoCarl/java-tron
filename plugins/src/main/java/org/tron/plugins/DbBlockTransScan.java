package org.tron.plugins;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.clickhouse.jdbc.ClickHouseConnection;
import com.clickhouse.jdbc.ClickHouseDataSource;
import com.clickhouse.jdbc.ClickHouseStatement;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.ByteArray;
import org.tron.plugins.utils.DBUtils;
import org.tron.plugins.utils.Sha256Hash;
import org.tron.plugins.utils.StringUtil;
import org.tron.plugins.utils.db.DBInterface;
import org.tron.plugins.utils.db.DBIterator;
import org.tron.plugins.utils.db.DbTool;
import org.tron.protos.Protocol;
import org.tron.protos.contract.AccountContract;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.ExchangeContract;
import org.tron.protos.contract.MarketContract;
import org.tron.protos.contract.ProposalContract;
import org.tron.protos.contract.ShieldContract;
import org.tron.protos.contract.SmartContractOuterClass;
import org.tron.protos.contract.StorageContract;
import org.tron.protos.contract.VoteAssetContractOuterClass;
import org.tron.protos.contract.WitnessContract;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


@Slf4j(topic = "blocktrans-scan")
@CommandLine.Command(name = "blocktrans-scan",
    description = "scan data from block.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:query failed,please check toolkit.log"})
public class DbBlockTransScan implements Callable<Integer> {

  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;
  @CommandLine.Parameters(index = "0",
      description = " db path for block")
  private Path db;

  @CommandLine.Option(names = {"-s", "--start" },
          defaultValue = "7588859",
          description = "start block. Default: ${DEFAULT-VALUE}")
  private  long start;

  @CommandLine.Option(names = {"-h", "--help"}, help = true, description = "display a help message")
  private boolean help;

  private static final  String DB = "block";

  private final AtomicLong cnt = new AtomicLong(0);
  private final AtomicLong scanTotal = new AtomicLong(0);


  @Override
  public Integer call() throws Exception {
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }
    if (!db.toFile().exists()) {
      logger.info(" {} does not exist.", db);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s does not exist.", db)));
      return 404;
    }
    return query();
  }


  private int query() throws SQLException {
    ClickHouseDataSource dataSource = new ClickHouseDataSource("jdbc:clickhouse://172.16.200.42:8123");
    ClickHouseConnection connection = dataSource.getConnection();
    ClickHouseStatement stat = connection.createStatement();

    try (DBInterface database  = DbTool.getDB(this.db, DB);
         DBIterator iterator = database.iterator()) {
      long min =  this.start;
      iterator.seek(ByteArray.fromLong(min));
      min = new Sha256Hash.BlockId(Sha256Hash.wrap(iterator.getKey())).getNum();
      iterator.seekToLast();
      long max = new Sha256Hash.BlockId(Sha256Hash.wrap(iterator.getKey())).getNum();
      long total = max - min + 1;
      spec.commandLine().getOut().format("scan block start from  %d to %d ", min, max).println();
      logger.info("scan block start from {} to {}", min, max);
      try (ProgressBar pb = new ProgressBar("block-scan", total)) {
        for (iterator.seek(ByteArray.fromLong(min)); iterator.hasNext(); iterator.next()) {
          Protocol.Block block = Protocol.Block.parseFrom(iterator.getValue());
          logger.info("dealing block num:{}", block.getBlockHeader().getRawData().getNumber());
          if(!block.getTransactionsList().isEmpty()) {
            insertIntoClickHouse(block, stat);
            pb.step();
            pb.setExtraMessage("Reading...");
            scanTotal.getAndIncrement();
          } else {
            logger.info("block.getTransactionsList is empty");
          }
        }
      } catch (Exception e) {
        logger.info("Exception: ", e);
      }
      spec.commandLine().getOut().format("total scan block size: %d", scanTotal.get()).println();
      logger.info("total scan block size: {}", scanTotal.get());
      spec.commandLine().getOut().format("illegal multi-sig  size: %d", cnt.get()).println();
      logger.info("illegal multi-sig size: {}", cnt.get());
    } catch (Exception e ) {
      logger.info("Exception: ", e);
    }
    return 0;
  }


  private  void insertIntoClickHouse(Protocol.Block block, ClickHouseStatement stat) {
    try {
      //Protocol.Block block = Protocol.Block.parseFrom(v);
      Protocol.BlockHeader.raw headerData = block.getBlockHeader().getRawData();
      long num = block.getBlockHeader().getRawData().getNumber();
      List<Protocol.Transaction> list = block.getTransactionsList().stream()
          .filter(trans -> trans.getSignatureCount() > 0).collect(Collectors.toList());
      list.forEach(transaction -> {
        Map<String, String> stringStringMap = null;
        try {
          stringStringMap = this.transaction2Map(headerData, transaction);
        } catch (InvalidProtocolBufferException e) {
          logger.info("InvalidProtocolBufferException: ", e);
          e.printStackTrace();
        }
        String sql = this.getTransSQL(stringStringMap);
        logger.info("SQL: {} ", sql);

        try {
          stat.executeUpdate(sql);
        } catch (SQLException throwables) {
          logger.info("SQLException: ", throwables);
          throwables.printStackTrace();
        }
      });
    } catch (Exception e) {
      logger.info("Exception: ", e);
      logger.error("e", e);
    }

    return ;
  }

  private String getTransSQL(Map<String, String> stringStringMap) {
    int i =0;
    StringBuilder sb = new StringBuilder("INSERT INTO tron_db.trans(");
    for (Map.Entry<String, String> entry : stringStringMap.entrySet()) {
      String k = entry.getKey();
      if (i!=0){
        sb.append(",");
      }
      sb.append(k);
      i++;
    }
    sb.append(") VALUES (");

    i = 0;
    for (Map.Entry<String, String> entry : stringStringMap.entrySet()) {
      String v = entry.getValue();
      if (i!=0){
        sb.append(",");
      }
      sb.append("'");
      sb.append(v);
      sb.append("'");
      i++;
    }

    sb.append(")");

    return sb.toString();
  }

  private Map<String, String> transaction2Map(Protocol.BlockHeader.raw header, Protocol.Transaction transaction) throws InvalidProtocolBufferException {

    JsonFormat.TypeRegistry typeRegistry = JsonFormat.TypeRegistry.newBuilder()
        .add(AccountContract.AccountCreateContract.getDescriptor())
        .add(BalanceContract.TransferContract.getDescriptor())
        .add(AssetIssueContractOuterClass.TransferAssetContract.getDescriptor())
        .add(VoteAssetContractOuterClass.VoteAssetContract.getDescriptor())
        .add(WitnessContract.VoteWitnessContract.getDescriptor())
        .add(WitnessContract.WitnessCreateContract.getDescriptor())
        .add(AssetIssueContractOuterClass.AssetIssueContract.getDescriptor())
        .add(WitnessContract.WitnessUpdateContract.getDescriptor())
        .add(AssetIssueContractOuterClass.ParticipateAssetIssueContract.getDescriptor())
        .add(AccountContract.AccountUpdateContract.getDescriptor())
        .add(BalanceContract.FreezeBalanceContract.getDescriptor())
        .add(BalanceContract.UnfreezeBalanceContract.getDescriptor())
        .add(BalanceContract.WithdrawBalanceContract.getDescriptor())
        .add(AssetIssueContractOuterClass.UnfreezeAssetContract.getDescriptor())
        .add(AssetIssueContractOuterClass.UpdateAssetContract.getDescriptor())
        .add(ProposalContract.ProposalCreateContract.getDescriptor())
        .add(ProposalContract.ProposalApproveContract.getDescriptor())
        .add(ProposalContract.ProposalDeleteContract.getDescriptor())
        .add(AccountContract.SetAccountIdContract.getDescriptor())
        .add(SmartContractOuterClass.CreateSmartContract.getDescriptor())
        .add(SmartContractOuterClass.TriggerSmartContract.getDescriptor())
        .add(SmartContractOuterClass.UpdateSettingContract.getDescriptor())
        .add(SmartContractOuterClass.UpdateEnergyLimitContract.getDescriptor())
        .add(SmartContractOuterClass.ClearABIContract.getDescriptor())
        .add(ExchangeContract.ExchangeCreateContract.getDescriptor())
        .add(ExchangeContract.ExchangeInjectContract.getDescriptor())
        .add(ExchangeContract.ExchangeWithdrawContract.getDescriptor())
        .add(ExchangeContract.ExchangeTransactionContract.getDescriptor())
        .add(AccountContract.AccountPermissionUpdateContract.getDescriptor())
        .add(StorageContract.UpdateBrokerageContract.getDescriptor())
        .add(ShieldContract.ShieldedTransferContract.getDescriptor())
        .add(MarketContract.MarketSellAssetContract.getDescriptor())
        .add(MarketContract.MarketCancelOrderContract.getDescriptor())
        .add(BalanceContract.FreezeBalanceV2Contract.getDescriptor())
        .add(BalanceContract.UnfreezeBalanceV2Contract.getDescriptor())
        .add(BalanceContract.WithdrawExpireUnfreezeContract.getDescriptor())
        .add(BalanceContract.DelegateResourceContract.getDescriptor())
        .add(BalanceContract.UnDelegateResourceContract.getDescriptor())
        .add(BalanceContract.CancelAllUnfreezeV2Contract.getDescriptor())
        .build();

    JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry);
    String s = printer.print(transaction);
    s = s.replace("@type", "type");

    JSONObject jsonObject = JSONObject.parseObject(s);
    Map<String,Object> mapJsonObject = (Map<String,Object>)jsonObject;
    Map<String, String> map = new HashMap<String, String>();

    String tid = DBUtils.getTransactionId(transaction).toString();
    JSONObject rawData = (JSONObject)mapJsonObject.get("rawData");
    JSONObject contractObject = rawData.getJSONArray("contract").getJSONObject(0);
    JSONArray resultObjectArr = (JSONArray)mapJsonObject.get("ret");
    JSONObject resultObject = resultObjectArr.getJSONObject(0);

    map.put("transactionId", tid);
    map.put("blockNumber", header.getNumber()+"");

    map.put("contractType", contractObject.getString("type"));
    Triple<String, String, String>  getAddress = getAddress(transaction.getRawData().getContract(0));
    map.put("fromAddress", getAddress.getLeft());
    map.put("toAddress", getAddress.getMiddle());
    map.put("contractAddress", getAddress.getRight());

    Pair<String, Long>  pair = getAssetAmount(transaction.getRawData().getContract(0));
    map.put("assetName", pair.getLeft());
    map.put("assetAmount", pair.getRight()+"");

    String timestamp = rawData.getString("timestamp");
    map.put("timeStamp", timestamp==null?"0":timestamp);
    String feeLimit = rawData.getString("feeLimit");
    map.put("feeLimit", feeLimit==null?"0":feeLimit);

    String contractCallValue = getContractCallValue(transaction.getRawData().getContract(0))+"";
    map.put("contractCallValue", contractCallValue);

    String contractRet = resultObject.getString("contractRet");
    map.put("contractResult", contractRet==null?"0":contractRet);

    return map;
  }



  public Long getContractCallValue(Protocol.Transaction.Contract contract) throws InvalidProtocolBufferException {
    Long contractCallValue = 0L;

    switch (contract.getType()) {
      case TriggerSmartContract:
        SmartContractOuterClass.TriggerSmartContract triggerSmartContract = contract.getParameter()
            .unpack(SmartContractOuterClass.TriggerSmartContract.class);

        if (Objects.nonNull(triggerSmartContract.getCallValue())) {
          contractCallValue = triggerSmartContract.getCallValue();
        }
        break;
    }
    return contractCallValue;
  }

  public Pair<String, Long> getAssetAmount(Protocol.Transaction.Contract contract) throws InvalidProtocolBufferException {
    Long assetAmount = 0L;
    String assetName = "";

    switch (contract.getType()) {
      case TransferContract:
        BalanceContract.TransferContract transferContract = contract.getParameter()
            .unpack(BalanceContract.TransferContract.class);

        if (Objects.nonNull(transferContract)) {
          assetName = "trx";
          assetAmount = transferContract.getAmount();
        }
        break;
      case TransferAssetContract:
        AssetIssueContractOuterClass.TransferAssetContract transferAssetContract = contract.getParameter()
            .unpack(AssetIssueContractOuterClass.TransferAssetContract.class);
        if (Objects.nonNull(transferAssetContract)) {
          if (Objects.nonNull(transferAssetContract.getAssetName())) {
            assetName = transferAssetContract.getAssetName().toStringUtf8();
          }
          assetAmount = transferAssetContract.getAmount();
        }
        break;
    }
    return new ImmutablePair<>(assetName, assetAmount);
  }

  public Triple<String, String, String>  getAddress(Protocol.Transaction.Contract contract) throws InvalidProtocolBufferException {
    String fromAddr = "";
    String toAddr = "";
    String contractAddr = "";
    switch (contract.getType()) {
      case TransferContract:
        BalanceContract.TransferContract transferContract = contract.getParameter().unpack(BalanceContract.TransferContract.class);
        if (Objects.nonNull(transferContract)) {
          if (Objects.nonNull(transferContract.getOwnerAddress())) {
            fromAddr = StringUtil.encode58Check(
                transferContract.getOwnerAddress().toByteArray());
          }
          if (Objects.nonNull(transferContract.getToAddress())) {
            toAddr = StringUtil.encode58Check(
                transferContract.getToAddress().toByteArray());
          }
        }
        break;
      case TransferAssetContract:
        AssetIssueContractOuterClass.TransferAssetContract transferAssetContract = contract.getParameter()
            .unpack(AssetIssueContractOuterClass.TransferAssetContract.class);

        if (Objects.nonNull(transferAssetContract)) {
          if (Objects.nonNull(transferAssetContract.getOwnerAddress())) {
            fromAddr = StringUtil.encode58Check(
                transferAssetContract.getOwnerAddress().toByteArray());
          }

          if (Objects.nonNull(transferAssetContract.getToAddress())) {
            toAddr = StringUtil.encode58Check(
                transferAssetContract.getToAddress().toByteArray());
          }
        }
        break;
      case TriggerSmartContract:
        SmartContractOuterClass.TriggerSmartContract triggerSmartContract = contract.getParameter()
            .unpack(SmartContractOuterClass.TriggerSmartContract.class);

        if (Objects.nonNull(triggerSmartContract.getOwnerAddress())) {
          fromAddr = StringUtil.encode58Check(triggerSmartContract.getOwnerAddress().toByteArray());
        }

        if (Objects.nonNull(triggerSmartContract.getContractAddress())) {
          toAddr = StringUtil.encode58Check(
              triggerSmartContract.getContractAddress().toByteArray());
          contractAddr  = StringUtil.encode58Check(
              triggerSmartContract.getContractAddress().toByteArray());
        }
        break;
      case CreateSmartContract:
        SmartContractOuterClass.CreateSmartContract createSmartContract = contract.getParameter()
            .unpack(SmartContractOuterClass.CreateSmartContract.class);

        if (Objects.nonNull(createSmartContract.getOwnerAddress())) {
          fromAddr = StringUtil.encode58Check(createSmartContract.getOwnerAddress().toByteArray());
        }
        break;
      default:
        break;
    }
    Triple<String, String, String> triple = new ImmutableTriple<>(fromAddr, toAddr, contractAddr);
    return triple;
  }

}
