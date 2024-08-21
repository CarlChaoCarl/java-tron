package org.tron.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bouncycastle.util.encoders.Hex;
import org.tron.common.utils.StringUtil;
import picocli.CommandLine;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


@Slf4j(topic = "blockaccount")
@CommandLine.Command(name = "blockaccount", aliases = "blockaccount",
    description = "Quick diff blockaccount data.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:Internal error: exception occurred,please check toolkit.log"})
public class BlockTransAccount implements Callable<Integer> {
  @CommandLine.Spec
  CommandLine.Model.CommandSpec spec;
  @CommandLine.Parameters(index = "0", defaultValue = "",
      description = "src http api address. Default: ${DEFAULT-VALUE}")
  private String src;
  @CommandLine.Parameters(index = "1", defaultValue = "",
      description = "dest http api address. Default: ${DEFAULT-VALUE}")
  private String dest;

  @CommandLine.Parameters(index = "2", defaultValue = "",
      description = "block num. Default: ${DEFAULT-VALUE}")
  private Integer blockNum;

  @CommandLine.Option(names = {"-h", "--help"})
  private boolean help;

  @Override
  public Integer call() throws Exception {
    final long time = System.currentTimeMillis();
    if (help) {
      spec.commandLine().usage(System.out);
      return 0;
    }

    if (src.isEmpty()) {
      logger.info(" {} does not exist.", src);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s does not exist.", src)));
      return 404;
    }
    if (dest.isEmpty()) {
      logger.info(" {} exist, please delete it first.", dest);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s exist, please delete it first.", dest)));
      return 402;
    }

    String blockInfo = getBlockInfo();
    JSONArray jsonArray = JSON.parseArray(blockInfo);
    for (int i = 0; i < jsonArray.size(); i++) {
      JSONObject jsonObject = (JSONObject)jsonArray.get(i);
      String id = (String)jsonObject.get("id");
      logger.info("trans id {}", id);
      String transInfo = getTransactionInfo(id);
      transAccount(transInfo);
    }

    long during = (System.currentTimeMillis() - time) / 1000;
    logger.info("total use {} seconds", during);
    return 0;
  }

  private void transAccount(String transInfo) throws IOException {
      JSONObject transInfoObject = (JSONObject)JSON.parse(transInfo);
      JSONObject rawDataObject = (JSONObject)transInfoObject.get("raw_data");
      JSONArray contractArr = (JSONArray)rawDataObject.get("contract");
      JSONObject contractObject = (JSONObject) contractArr.get(0);
      String contractType = contractObject.get("type").toString();
      String ownerAddress =  getOwnerAddress(contractObject);
      if (ownerAddress !=null) {
        logger.info("trans_type {}, owner_address {}", contractType, ownerAddress);
      } else {
        logger.info("trans_type {}", contractType);
      }
      Long balance = getAccountBalance(ownerAddress);
      logger.info("balance {}", balance);
  }

  private Long getAccountBalance(String address) throws IOException {
    String url = "http://3.225.171.164:8090/wallet/getaccount?address=" + address + "&visible=true";
    try {
      Request request = new Request.Builder()
          .addHeader("Content-Type", "application/json")
          .url(url)
          .build();
      OkHttpClient httpClient = createHttpClient(100, 30000);
      Response response = httpClient.newCall(request).execute();
      String res = response.body().string();
      JSONObject transInfoObject = (JSONObject)JSON.parse(res);
      if (transInfoObject.get("balance") == null) {
        return 0L;
      } else {
        return Long.valueOf(transInfoObject.get("balance").toString());
      }
    } catch (SocketTimeoutException e) {
      // retry
      logger.error("getAccountBalance exception {}", e);
    } catch (Exception e) {
      logger.error("getAccountBalance exception {}", e);
    }
    return 0L;
  }

  private String getBlockInfo() throws IOException {
    Request request = new Request.Builder()
        .addHeader("Content-Type", "application/json")
        .url("http://3.225.171.164:8090/wallet/gettransactioninfobyblocknum?num=64354122")
        .build();
    OkHttpClient httpClient = createHttpClient(100, 30000);
    Response response = httpClient.newCall(request).execute();
    if (!response.isSuccessful()) {
      throw new IOException("Unexpected code " + response);
    }
    return response.body().string();
  }

  private String getTransactionInfo(String transId) throws IOException {
    StringBuilder url = new StringBuilder().append("http://3.225.171.164:8090/wallet/gettransactionbyid?value=")
        .append(transId);
    Request request = new Request.Builder()
        .addHeader("Content-Type", "application/json")
        .url(url.toString())
        .build();
    OkHttpClient httpClient = createHttpClient(100, 30000);
    Response response = httpClient.newCall(request).execute();
    if (!response.isSuccessful()) {
      throw new IOException("Unexpected code " + response);
    }
    return response.body().string();
  }

  private static OkHttpClient createHttpClient(int maxTotalConnections, long connectionKeepAliveTimeInMillis) {
    ConnectionPool connectionPool = new ConnectionPool(maxTotalConnections, connectionKeepAliveTimeInMillis, TimeUnit.MILLISECONDS);
    return new OkHttpClient.Builder()
        .followRedirects(false)
        .retryOnConnectionFailure(true)
        .connectionPool(connectionPool)
        .build();
  }

  public static String getOwnerAddress(JSONObject contractObject) {
    String ownerAddress = "";
    try {
      JSONObject parameter = (JSONObject)contractObject.get("parameter");
      JSONObject value = (JSONObject)parameter.get("value");
      ownerAddress = value.get("owner_address").toString();
      ownerAddress = StringUtil.encode58Check(Hex.decode(ownerAddress));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return ownerAddress;
  }

}
