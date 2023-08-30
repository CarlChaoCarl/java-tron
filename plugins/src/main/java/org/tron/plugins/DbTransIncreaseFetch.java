package org.tron.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import javafx.util.Pair;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j(topic = "trans-incr-fetch")
public class DbTransIncreaseFetch  {
  private static int query() {

    //https://api.tronex.io/blocks?limit=1&sort=-timeStamp&start=0&block=0
    Long latestSolidifiedBlockNumber = 0L;
    Integer transactionSize = 0;
    try {
      Pair<Long, Integer> longIntegerPair = fetchLastestBlock();
      latestSolidifiedBlockNumber = longIntegerPair.getKey();
      transactionSize = longIntegerPair.getValue();
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      List<TransactionInfo> transactionInfoList = fetchBlockTransactionList(
          latestSolidifiedBlockNumber, transactionSize);
      insertIntoClickHouse(transactionInfoList);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return 0;
  }


  private static Pair<Long, Integer> fetchLastestBlock() throws IOException {
    Long latestSolidifiedBlockNumber = 0L;
    Integer transactionSize = 0;
    OkHttpClient client = new OkHttpClient();
    URL urlStr = new URL("https://api.tronex.io/blocks?limit=1&sort=-timeStamp&start=0&block=0");
    Request request = new Request.Builder()
        .url(urlStr)
        .get()
        .build();

    Response response = client.newCall(request).execute();
    String jsong = response.body().string();
    JSONObject jsonObject = JSONObject.parseObject(jsong);
    Integer total = jsonObject.getInteger("total");
    if(total!=null && total==1) {
      JSONObject dataObj = jsonObject.getJSONArray("data").getJSONObject(0);
      latestSolidifiedBlockNumber = Long.valueOf(dataObj.getString("latestSolidifiedBlockNumber"));
      transactionSize = dataObj.getInteger("transactionSize");
    }
    return new Pair<>(latestSolidifiedBlockNumber,  transactionSize);
  }

  private static List<TransactionInfo> fetchBlockTransactionList(Long blockNumber, Integer transactionSize) throws IOException {
    OkHttpClient client = new OkHttpClient();
    String Url = "https://api.tronex.io/transactions?limit="+transactionSize+"&sort=blockNumber&start=0&block="+blockNumber;

    URL urlStr = new URL(Url);
    Request request = new Request.Builder()
        .url(urlStr)
        .get()
        .build();

    Response response = client.newCall(request).execute();
    String jsong = response.body().string();
    System.out.println(jsong);
    JSONObject jsonObject = JSONObject.parseObject(jsong);
    JSONArray dataArr = jsonObject.getJSONArray("data");
    List<TransactionInfo> transactionInfoList = JSONArray.parseArray(jsonObject.getString("data"), TransactionInfo.class);

    return transactionInfoList;
  }


  private static void insertIntoClickHouse(List<TransactionInfo> transactionInfoList) {
    try {
      if (transactionInfoList.isEmpty()) {
        return;
      }

      for (int i = 0; i < transactionInfoList.size(); i++) {
        TransactionInfo transactionInfo = transactionInfoList.get(i);
        Map<String, String> stringStringMap = null;
        try {
          stringStringMap = trans2Map(transactionInfo);
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }
        String transSQL = getTransSQL("tron_db.trans", stringStringMap);
        System.out.println("transSQL:" + transSQL);

        try {
          stringStringMap = transInfo2Map(transactionInfo);
        } catch (InvalidProtocolBufferException e) {
          e.printStackTrace();
        }
        String transInfoSQL = getTransSQL("tron_db.trans_info", stringStringMap);
        System.out.println("transInfoSQL:" + transInfoSQL);
      }
    } catch (Exception e) {
      logger.error("{}", e);
    }
    return ;
  }

  private static  Map<String, String> trans2Map(TransactionInfo transactionInfo) throws InvalidProtocolBufferException {
    Map<String, String> map = new HashMap<String, String>();
    map.put("transactionId", transactionInfo.transactionId);
    map.put("blockNumber", transactionInfo.blockNumber+"");
    map.put("contractType", transactionInfo.contractType);
    map.put("fromAddress", transactionInfo.fromAddress);
    map.put("toAddress", transactionInfo.toAddress);
    map.put("contractAddress", transactionInfo.contractAddress);
    map.put("assetName", transactionInfo.assetName);
    map.put("assetAmount", transactionInfo.assetAmount+"");
    map.put("feeLimit", transactionInfo.feeLimit+"");
    map.put("timeStamp", transactionInfo.timeStamp+"");
    map.put("contractCallValue", transactionInfo.contractCallValue+"");
    map.put("contractResult", transactionInfo.contractResult);
    return map;
  }

  private static  Map<String, String> transInfo2Map(TransactionInfo transactionInfo) throws InvalidProtocolBufferException {
    Map<String, String> map = new HashMap<String, String>();
    map.put("transactionId", transactionInfo.transactionId);
    map.put("energyUsage", transactionInfo.energyUsage+"");
    map.put("energyFee", transactionInfo.energyFee+"");
    map.put("originEnergyUsage", transactionInfo.originEnergyUsage+"");
    map.put("energyUsageTotal", transactionInfo.energyUsageTotal+"");
    map.put("netUsage", transactionInfo.netUsage+"");
    map.put("netFee", transactionInfo.netFee+"");
    map.put("result", transactionInfo.result);
    map.put("internalTrananctionList", transactionInfo.internalTrananctionList);
    return map;
  }

  private static String getTransSQL(String tableName, Map<String, String> stringStringMap) {
    int i =0;
    StringBuilder sb = new StringBuilder("INSERT INTO "+tableName+"(");
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

  public static class TransactionInfo {
    public String transactionId;
    public String blockHash;
    public Long blockNumber;
    public Long energyUsage;
    public Long energyFee;
    public Long originEnergyUsage;
    public Long energyUsageTotal;
    public Long netUsage;
    public Long netFee;
    public String result;
    public String contractAddress;
    public String contractType;
    public Long feeLimit;
    public Long contractCallValue;
    public Long timeStamp;
    public String triggerName;
    public String internalTrananctionList;
    public String fromAddress;
    public String toAddress;
    public String assetName;
    public Long assetAmount;
    public String contractResult;
    public Long latestSolidifiedBlockNumber;
    public String data;
  }


  public static void main(String[] args) throws RocksDBException, IOException {
    //TODO 每3秒执行一次
    ScheduledExecutorService pool = Executors.newScheduledThreadPool(10);
    pool.scheduleAtFixedRate(() -> {
      query();
    }, 2000, 3000, TimeUnit.MILLISECONDS);

    query();
  }

}
