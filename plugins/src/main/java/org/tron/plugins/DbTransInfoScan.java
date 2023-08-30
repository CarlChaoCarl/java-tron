package org.tron.plugins;

import com.alibaba.fastjson.JSONObject;
import com.clickhouse.jdbc.ClickHouseConnection;
import com.clickhouse.jdbc.ClickHouseDataSource;
import com.clickhouse.jdbc.ClickHouseStatement;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.encoders.Hex;
import org.rocksdb.RocksDBException;
import org.tron.plugins.utils.ByteArray;
import org.tron.plugins.utils.db.DBInterface;
import org.tron.plugins.utils.db.DBIterator;
import org.tron.plugins.utils.db.DbTool;
import org.tron.protos.Protocol;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j(topic = "transinfo-scan")
@CommandLine.Command(name = "transinfo-scan",
    description = "scan data from ret.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:query failed,please check toolkit.log"})
public class DbTransInfoScan implements Callable<Integer> {

  private static final String DB = "transactionRetStore";
  private static final DateFormat DATE_FORMAT_SHORT = new SimpleDateFormat("yyyy-MM-dd");
  private final AtomicLong scanTotal = new AtomicLong(0);
  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;


  @CommandLine.Parameters(index = "0",
      description = " db path for ret")
  private Path db;
  @CommandLine.Option(names = {"-s", "--start"},
      defaultValue = "1",
      description = "start block. Default: ${DEFAULT-VALUE}")
  private long start;

  @CommandLine.Option(names = {"-e", "--end"},
      defaultValue = "-1",
      description = "end block. Default: ${DEFAULT-VALUE}")
  private long end;

  @CommandLine.Option(names = {"-t", "--trans"},
      description = "file for trans hash")
  private File trans;
  @CommandLine.Option(names = {"-h", "--help"}, help = true, description = "display a help message")
  private boolean help;

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


  private int query() throws RocksDBException, IOException, SQLException {
    ClickHouseDataSource dataSource = new ClickHouseDataSource("jdbc:clickhouse://172.16.200.42:8123");
    ClickHouseConnection connection = dataSource.getConnection();
    ClickHouseStatement stat = connection.createStatement();

    List<String> stringList = new ArrayList<>();
    if (Objects.nonNull(trans)) {
      spec.commandLine().getOut().format("scan trans from %s ...", trans).println();
      logger.info("scan trans from {} ...", trans);
      stringList = FileUtils.readLines(trans, StandardCharsets.UTF_8);
      spec.commandLine().getOut().format("scan trans from %s done, line %d", trans,
          stringList.size()).println();
      logger.info("scan trans from {} done,line {}", trans, stringList.size());
    }
    Set<String> stringSet = new HashSet<>(stringList);

    spec.commandLine().getOut().format("scan trans  %s size: ", stringSet.size()).println();
    logger.info("scan trans  {} size: ", stringSet.size());

    try (DBInterface database = DbTool.getDB(this.db, DB);
         DBIterator iterator = database.iterator()) {
      long min = start;
      iterator.seek(ByteArray.fromLong(min));
      if (end > 0) {
        iterator.seek(ByteArray.fromLong(end));
      } else {
        iterator.seekToLast();
      }
      long max = ByteArray.toLong(iterator.getKey());
      if (max < min) {
        max = max + min;
        min = max - min;
        max = max - min;
      }
      long total = max - min + 1;
      spec.commandLine().getOut().format("scan ret start from  %d to %d ", min, max).println();
      logger.info("scan ret start from {} to {}", min, max);
      try (ProgressBar pb = new ProgressBar("ret-scan", total)) {
        for (iterator.seek(ByteArray.fromLong(min));
             iterator.hasNext() && total-- > 0;
             iterator.next()) {
          insertIntoClickHouse(iterator.getKey(), iterator.getValue(), stat);
          pb.step();
          pb.setExtraMessage("Reading...");
          scanTotal.getAndIncrement();
        }
      }
    }
    return 0;
  }

  private void insertIntoClickHouse(byte[] k, byte[] v, ClickHouseStatement stat) {
    try {
      Protocol.TransactionRet ret = Protocol.TransactionRet.parseFrom(v);
      if (ret.getTransactioninfoList().isEmpty()) {
        return;
      }

      Long blockNum = ret.getBlockNumber();
      Long timestamp = ret.getBlockTimeStamp();
      for (int i = 0; i < ret.getTransactioninfoList().size(); i++) {
        Protocol.TransactionInfo transactionInfo = ret.getTransactioninfoList().get(i);
        JsonFormat.Printer printer = JsonFormat.printer();
        String transactionInfoTemp = printer.print(transactionInfo);
        Map<String, String> stringStringMap = null;
        try {
          stringStringMap = this.transaction2Map(blockNum, timestamp, transactionInfo);
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }
        String sql = this.getTransSQL(stringStringMap);
        logger.info("sql, {}", sql);

        try {
          stat.executeUpdate(sql);
        } catch (SQLException throwables) {
          throwables.printStackTrace();
        }
      }
    } catch (InvalidProtocolBufferException e) {
      logger.error("{},{}", k, v);
    }
  }

  private Map<String, String> transaction2Map(Long blockNum, Long timestamp, Protocol.TransactionInfo transactionInfo) throws InvalidProtocolBufferException {
    JsonFormat.Printer printer = JsonFormat.printer();
    String s = printer.print(transactionInfo);
    logger.info("transaction2Map, {}", s);
    JSONObject transactionInfoObj = JSONObject.parseObject(s);
    JSONObject receiptObj = transactionInfoObj.getJSONObject("receipt");

    Map<String, String> map = new HashMap<String, String>();

    map.put("transactionId", Hex.toHexString(transactionInfo.getId().toByteArray()));
    String energyUsage = receiptObj.getString("energyUsage");
    map.put("energyUsage", energyUsage==null?"0":energyUsage);

    String energyFee = receiptObj.getString("energyFee");
    map.put("energyFee", energyFee==null?"0":energyFee);
    String originEnergyUsage = receiptObj.getString("originEnergyUsage");
    map.put("originEnergyUsage", originEnergyUsage==null?"0":originEnergyUsage);
    String energyUsageTotal = receiptObj.getString("energyUsageTotal");
    map.put("energyUsageTotal", energyUsageTotal==null?"0":energyUsageTotal);
    String netUsage = receiptObj.getString("netUsage");
    map.put("netUsage", netUsage==null?"0":netUsage);
    String netFee = receiptObj.getString("netFee");
    map.put("netFee", netFee==null?"0":netFee);
    map.put("result", transactionInfo.getResult().toString());
    String internalTrananctionList = transactionInfoObj.getString("internalTransactions");
    map.put("internalTrananctionList", internalTrananctionList==null?"":internalTrananctionList);

    return map;
  }

  private String getTransSQL(Map<String, String> stringStringMap) {
    int i =0;
    StringBuilder sb = new StringBuilder("INSERT INTO tron_db.trans_info(");
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

  public static void main(String[] args) {
    int exitCode = new CommandLine(new DbTransInfoScan()).execute(args);
    System.exit(exitCode);
  }

}
