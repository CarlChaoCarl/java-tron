package org.tron.plugins;

import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.ByteArray;
import org.tron.plugins.utils.db.DBInterface;
import org.tron.plugins.utils.db.DBIterator;
import org.tron.plugins.utils.db.DbTool;
import org.tron.protos.Protocol;
import picocli.CommandLine;
import com.clickhouse.jdbc.ClickHouseConnection;
import com.clickhouse.jdbc.ClickHouseDataSource;
import com.clickhouse.jdbc.ClickHouseStatement;
import com.alibaba.fastjson.*;
import java.sql.SQLException;

@Slf4j(topic = "account-scan")
@CommandLine.Command(name = "account-scan",
    description = "scan data from account.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:query failed,please check toolkit.log"})
public class DbAccountScan implements Callable<Integer> {
  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;
  @CommandLine.Parameters(index = "0",
      description = " db path for account")
  private Path db;
  @CommandLine.Option(names = {"-h", "--help"}, help = true, description = "display a help message")
  private boolean help;
  private static final  String DB = "account";
  private final AtomicLong cnt = new AtomicLong(0);
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

  private int query() throws RocksDBException, IOException , SQLException {
    ClickHouseDataSource dataSource = new ClickHouseDataSource("jdbc:clickhouse://172.16.200.42:8123");
    ClickHouseConnection connection = dataSource.getConnection();
    ClickHouseStatement stat = connection.createStatement();

    try (DBInterface database  = DbTool.getDB(this.db, DB);
         DBIterator iterator = database.iterator()) {
      long c = 0;
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next(), c++) {
        Protocol.Account account = Protocol.Account.parseFrom(iterator.getValue());
        if (c%100000==0) {
          insertIntoClickHouse(iterator.getValue(), stat);
          spec.commandLine().getOut().format("current account size: %d", c).println();
          continue;
        } else {
          continue;
        }
      }
      spec.commandLine().getOut().format("total account size: %d", c).println();
      logger.info("total account size: {}", c);
      spec.commandLine().getOut().format("multi-sig-account size: %d", cnt.get()).println();
      logger.info("multi-sig-account size: {}", cnt.get());
    } finally {
      stat.close();
      connection.close();
    }
    return 0;
  }

  private  void insertIntoClickHouse(byte[] v, ClickHouseStatement stat) {
    String value = ByteArray.toHexString(v);
    try {
// 数据插入
      Protocol.Account account = Protocol.Account.parseFrom(v);
      Map<String, String> stringStringMap = this.account2Map(account);
      String sql = this.getAccountSQL(stringStringMap);
      System.out.println("sql:" + sql);
      stat.executeUpdate(sql);

      JsonFormat.Printer printer = JsonFormat.printer();
      String s = printer.print(account);
      System.out.println("s:"+s);
      logger.info("account: {}" + s);
    } catch (InvalidProtocolBufferException e) {
      logger.error("e", e);
    } catch (Exception e) {
      logger.error("e", e);
    }
    return ;
  }

  private String getAccountSQL(Map<String, String> stringStringMap) {
    int i =0;
    StringBuilder sb = new StringBuilder("INSERT INTO tron_db.account_his(");
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

  private Map<String, String> account2Map(Protocol.Account account) throws InvalidProtocolBufferException {
    JsonFormat.Printer printer = JsonFormat.printer();
    String s = printer.print(account);
    JSONObject  jsonObject = JSONObject.parseObject(s);
    Map<String,Object> mapJsonObject = (Map<String,Object>)jsonObject;

    Map<String, String> map = new HashMap<String, String>();
    map.put("account_name", account.getAccountName().isEmpty()?"":
        String.valueOf(mapJsonObject.get("accountName")));
    map.put("account_id", account.getAccountId().isEmpty()?"":
        String.valueOf(mapJsonObject.get("accountId")));
    map.put("type", account.getType().getNumber()+"");
    map.put("address", String.valueOf(mapJsonObject.get("address")));
    map.put("balance", account.getBalance()==0?"0":account.getBalance()+"");
    map.put("create_time", account.getCreateTime()==0?"0":account.getCreateTime()+"");
    map.put("latest_opration_time", account.getLatestOprationTime()==0?"0":account.getLatestOprationTime()+"");
    map.put("allowance", account.getAllowance()==0?"0":account.getAllowance()+"");

    map.put("votes", account.getVotesList().isEmpty()?"[]":
        String.valueOf(mapJsonObject.get("votes")));
    map.put("asset", account.getAssetMap().isEmpty()?"[]":
        String.valueOf(mapJsonObject.get("asset")));
    map.put("assetV2", account.getAssetV2Map().isEmpty()?"[]":
        String.valueOf(mapJsonObject.get("assetV2")));
    map.put("asset_optimized", account.getAssetOptimized()?"1":"0");
    map.put("latest_withdraw_time", account.getLatestWithdrawTime()==0?"0":account.getLatestWithdrawTime()+"");
    map.put("code", account.getCode().isEmpty()?"":
        String.valueOf(mapJsonObject.get("code")));
    map.put("is_witness", account.getIsWitness()?"1":"0");
    map.put("is_committee", account.getIsCommittee()?"1":"0");

    map.put("frozen_supply", account.getFrozenSupplyList().isEmpty()?"[]":
        String.valueOf(mapJsonObject.get("frozenSupply")));
    map.put("asset_issued_name", account.getAssetIssuedName().isEmpty()?"":
        String.valueOf(mapJsonObject.get("assetIssuedName")));
    map.put("asset_issued_ID", account.getAssetIssuedID().isEmpty()?"":
        String.valueOf(mapJsonObject.get("assetIssuedID")));


    map.put("latest_asset_operation_time", account.getLatestAssetOperationTimeMap().isEmpty()?"[]":
        String.valueOf(mapJsonObject.get("latestAssetOperationTime")));
    map.put("latest_asset_operation_timeV2", account.getLatestAssetOperationTimeV2Map().isEmpty()?"[]":
        String.valueOf(mapJsonObject.get("latestAssetOperationTimeV2")));
    map.put("free_asset_net_usage", account.getFreeAssetNetUsageMap().isEmpty()?"[]":
        String.valueOf(mapJsonObject.get("freeAssetNetUsage")));
    map.put("free_asset_net_usageV2", account.getFreeAssetNetUsageV2Map().isEmpty()?"[]":
        String.valueOf(mapJsonObject.get("freeAssetNetUsageV2")));
    map.put("codeHash", account.getCodeHash().isEmpty()?"":
        String.valueOf(mapJsonObject.get("codeHash")));

    Object ownerPermission = mapJsonObject.get("ownerPermission");
    Object witnessPermission = mapJsonObject.get("witness_permission");
    Object activePermission = mapJsonObject.get("active_permission");
    map.put("owner_permission", ownerPermission==null?"":String.valueOf(ownerPermission));
    map.put("witness_permission", witnessPermission==null?"":String.valueOf(witnessPermission));
    map.put("active_permission", activePermission==null?"":String.valueOf(activePermission));

    map.put("frozen_balance_bandwidth", account.getFrozenList().isEmpty()?"0":account.getFrozen(0).getFrozenBalance()+"");
    map.put("expire_time_bandwidth", account.getFrozenList().isEmpty()?"0":account.getFrozen(0).getExpireTime()+"");
    map.put("net_usage", account.getNetUsage()==0?"0":account.getNetUsage()+"");
    map.put("acquired_delegated_frozen_balance_for_bandwidth", account.getAcquiredDelegatedFrozenBalanceForBandwidth()==0?"0":account.getAcquiredDelegatedFrozenBalanceForBandwidth()+"");
    map.put("delegated_frozen_balance_for_bandwidth", account.getDelegatedFrozenBalanceForBandwidth()==0?"0":account.getDelegatedFrozenBalanceForBandwidth()+"");
    map.put("free_net_usage", account.getNetUsage()==0?"0":account.getNetUsage()+"");
    map.put("latest_consume_free_time", account.getLatestConsumeFreeTime()==0?"0":account.getLatestConsumeFreeTime()+"");
    map.put("net_window_size", account.getNetWindowSize()==0?"0":account.getNetWindowSize()+"");
    //map.put("net_window_optimized", account.getnetwindow ()==0?"0":account.getNetWindowSize()+"");
    map.put("delegated_frozenV2_balance_for_bandwidth", account.getDelegatedFrozenV2BalanceForBandwidth()==0?"0":account.getDelegatedFrozenV2BalanceForBandwidth()+"");
    map.put("acquired_delegated_frozenV2_balance_for_bandwidth", account.getAcquiredDelegatedFrozenV2BalanceForBandwidth()==0?"0":account.getAcquiredDelegatedFrozenV2BalanceForBandwidth()+"");

    map.put("energy_usage", account.getAccountResource().getEnergyUsage()==0?"0":account.getAccountResource().getEnergyUsage()+"");
    map.put("frozen_balance_energy", account.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance()==0?"0":account.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance()+"");
    map.put("expire_time_energy", account.getAccountResource().getFrozenBalanceForEnergy().getExpireTime()==0?"0":account.getAccountResource().getFrozenBalanceForEnergy().getExpireTime()+"");
    map.put("latest_consume_time_for_energy", account.getAccountResource().getLatestConsumeTimeForEnergy()==0?"0":account.getAccountResource().getLatestConsumeTimeForEnergy()+"");
    map.put("acquired_delegated_frozen_balance_for_energy", account.getAccountResource().getAcquiredDelegatedFrozenBalanceForEnergy()==0?"0":account.getAccountResource().getAcquiredDelegatedFrozenBalanceForEnergy()+"");
    map.put("delegated_frozen_balance_for_energy", account.getAccountResource().getDelegatedFrozenBalanceForEnergy()==0?"0":account.getAccountResource().getDelegatedFrozenBalanceForEnergy()+"");
    map.put("storage_limit", account.getAccountResource().getStorageLimit()==0?"0":account.getAccountResource().getStorageLimit()+"");
    map.put("storage_usage", account.getAccountResource().getStorageUsage()==0?"0":account.getAccountResource().getStorageUsage()+"");
    map.put("latest_exchange_storage_time", account.getAccountResource().getLatestExchangeStorageTime()==0?"0":account.getAccountResource().getLatestExchangeStorageTime()+"");
    map.put("energy_window_size", account.getAccountResource().getEnergyWindowSize()==0?"0":account.getAccountResource().getEnergyWindowSize()+"");
    map.put("delegated_frozenV2_balance_for_energy", account.getAccountResource().getDelegatedFrozenV2BalanceForEnergy()==0?"0":account.getAccountResource().getDelegatedFrozenV2BalanceForEnergy()+"");
    map.put("acquired_delegated_frozenV2_balance_for_energy", account.getAccountResource().getAcquiredDelegatedFrozenV2BalanceForEnergy()==0?"0":account.getAccountResource().getAcquiredDelegatedFrozenV2BalanceForEnergy()+"");
    //map.put("energy_window_optimized", account.getAccountResource().energy_window_optimized()==0?"0":account.getAccountResource().energy_window_optimized()+"");

    map.put("old_tron_power", account.getOldTronPower()==0?"0":account.getOldTronPower()+"");
    map.put("frozen_balance_tron_power", account.getTronPower().getFrozenBalance()==0?"0":account.getTronPower().getFrozenBalance()+"");
    map.put("expire_time_tron_power", account.getTronPower().getExpireTime()==0?"0":account.getTronPower().getExpireTime()+"");

    long frozenV2_amount_bandwidth = 0L;
    long frozenV2_amount_energy = 0L;
    long frozenV2_amount_tron_power = 0L;
    for (int i = 0; i < account.getFrozenV2List().size(); i++) {
      if (account.getFrozenV2List().get(i).getType().getNumber() == 0) {
        frozenV2_amount_bandwidth = account.getFrozenV2List().get(i).getAmount();
      } else if (account.getFrozenV2List().get(i).getType().getNumber() == 1) {
        frozenV2_amount_energy = account.getFrozenV2List().get(i).getAmount();
      } else {
        frozenV2_amount_tron_power = account.getFrozenV2List().get(i).getAmount();
      }
    }
    map.put("frozenV2_amount_bandwidth", frozenV2_amount_bandwidth+"");
    map.put("frozenV2_amount_energy", frozenV2_amount_energy+"");
    map.put("frozenV2_amount_tron_power", frozenV2_amount_tron_power+"");

    map.put("unfrozenV2", account.getUnfrozenV2List().isEmpty()?"[]":
        String.valueOf(mapJsonObject.get("unfrozenV2")));

    return map;
  }

  private  void print(byte[] v) {
    String value = ByteArray.toHexString(v);
    try {
      Protocol.Account account = Protocol.Account.parseFrom(v);
      if (account.getOwnerPermission().getKeysCount() > 1
              || account.getActivePermissionList().stream().anyMatch(p -> p.getKeysCount() > 1)) {
        cnt.getAndIncrement();
        logger.info("{}", value);
      }
    } catch (InvalidProtocolBufferException e) {
      logger.error("e", e);
    }
  }

  public static void main(String[] args) {

    int exitCode = new CommandLine(new DbAccountScan()).execute(args);
    System.exit(exitCode);
  }
}
