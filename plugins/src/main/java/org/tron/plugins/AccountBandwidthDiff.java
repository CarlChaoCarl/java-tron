package org.tron.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import picocli.CommandLine;


@Slf4j(topic = "accountbandwidth")
@CommandLine.Command(name = "accountbandwidth", aliases = "accountbandwidth",
    description = "Quick diff accountbandwidth data.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:Internal error: exception occurred,please check toolkit.log"})
public class AccountBandwidthDiff implements Callable<Integer> {

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
  private String address;

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
      logger.info(" {} does not exist.", dest);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s does not exist.", dest)));
      return 404;
    }

    //
    HashMap<String, String> accountBandwidth1 = transAccountBandwidth(src, address);
    HashMap<String, String> accountBandwidth2 = transAccountBandwidth(dest, address);
    if (accountBandwidthDiff(accountBandwidth1, accountBandwidth2)) {
      logger.info("accountBandwidth is the same");
    } else {
      logger.info("accountBandwidth is not the same");
    }

    long during = (System.currentTimeMillis() - time) / 1000;
    logger.info("total use {} seconds", during);
    return 0;
  }

  private boolean accountBandwidthDiff(HashMap<String, String> accountBandwidth1,
                                       HashMap<String, String> accountBandwidth2) {
    List<String> fieldList = new ArrayList<String>();
    List initList = Arrays.asList(
        "net_usage",
        "frozen",
        "latest_consume_time",
        "acquired_delegated_frozen_balance_for_bandwidth",
        "delegated_frozen_balance_for_bandwidth",
        "net_window_size",
        "delegated_frozenV2_balance_for_bandwidth",
        "acquired_delegated_frozenV2_balance_for_bandwidth",
        "net_window_optimized",
        "frozenV2_bandwidth",
        "unfrozenV2_bandwidth"
    );
    fieldList.addAll(initList);

    for (int i = 0; i < fieldList.size(); i++) {
      String fieldName = fieldList.get(i);
      if (accountBandwidth1.get(fieldName) == null
          && accountBandwidth2.get(fieldName) != null) {
        return false;
      }
      if (accountBandwidth1.get(fieldName) != null
          && accountBandwidth2.get(fieldName) == null) {
        return false;
      }

      if (accountBandwidth1.get(fieldName) != null
          && accountBandwidth2.get(fieldName) != null
          && !accountBandwidth1.get(fieldName).equals(accountBandwidth2.get(fieldName))) {
        logger.warn("field name:{},  value1:{} and value2:{}, is not the same",
            fieldName,
            accountBandwidth1.get(fieldName),
            accountBandwidth2.get(fieldName)
        );
        return false;
      }
    }

    return true;
  }

  private HashMap<String, String> transAccountBandwidth(String apiNode,
                                                        String address) {
    HashMap<String, String> accountBandwidth = getAccountBandwidth(apiNode, address);
    logger.info("accountBandwidth {}", JSON.toJSON(accountBandwidth));
    return accountBandwidth;
  }

  private HashMap<String, String> getAccountBandwidth(String apiNode,
                                                      String address) {
    HashMap<String, String> ret = new HashMap<>();

    String url = "http://" + apiNode + "/wallet/getaccount?address=" + address + "&visible=true";
    try {
      Request request = new Request.Builder()
          .addHeader("Content-Type", "application/json")
          .url(url)
          .build();
      OkHttpClient httpClient = createHttpClient(100, 30000);
      Response response = httpClient.newCall(request).execute();
      String res = response.body().string();
      JSONObject transInfoObject = (JSONObject)JSON.parse(res);

      if (transInfoObject.get("net_usage") != null) {
        String net_usage =  transInfoObject.get("net_usage").toString();
        ret.put("net_usage", net_usage);
      }
      if (transInfoObject.get("frozen") != null) {
        String frozen =  transInfoObject.get("frozen").toString();
        ret.put("frozen", frozen);
      }
      if (transInfoObject.get("latest_consume_time") != null) {
        String latest_consume_time =  transInfoObject.get("latest_consume_time").toString();
        ret.put("latest_consume_time", latest_consume_time);
      }
      if (transInfoObject.get("acquired_delegated_frozen_balance_for_bandwidth") != null) {
        String acquired_delegated_frozen_balance_for_bandwidth
            =  transInfoObject.get("acquired_delegated_frozen_balance_for_bandwidth").toString();
        ret.put("acquired_delegated_frozen_balance_for_bandwidth",
            acquired_delegated_frozen_balance_for_bandwidth);
      }
      if (transInfoObject.get("delegated_frozen_balance_for_bandwidth") != null) {
        String delegated_frozen_balance_for_bandwidth
            =  transInfoObject.get("delegated_frozen_balance_for_bandwidth").toString();
        ret.put("delegated_frozen_balance_for_bandwidth",
            delegated_frozen_balance_for_bandwidth);
      }
      if (transInfoObject.get("net_window_size") != null) {
        String net_window_size =  transInfoObject.get("net_window_size").toString();
        ret.put("net_window_size", net_window_size);
      }
      if (transInfoObject.get("delegated_frozenV2_balance_for_bandwidth") != null) {
        String delegated_frozenV2_balance_for_bandwidth
            =  transInfoObject.get("delegated_frozenV2_balance_for_bandwidth").toString();
        ret.put("delegated_frozenV2_balance_for_bandwidth",
            delegated_frozenV2_balance_for_bandwidth);
      }
      if (transInfoObject.get("acquired_delegated_frozenV2_balance_for_bandwidth") != null) {
        String acquired_delegated_frozenV2_balance_for_bandwidth
            =  transInfoObject.get("acquired_delegated_frozenV2_balance_for_bandwidth").toString();
        ret.put("acquired_delegated_frozenV2_balance_for_bandwidth",
            acquired_delegated_frozenV2_balance_for_bandwidth);
      }
      if (transInfoObject.get("net_window_optimized") != null) {
        String net_window_optimized =  transInfoObject.get("net_window_optimized").toString();
        ret.put("net_window_optimized", net_window_optimized);
      }
      if (transInfoObject.get("frozenV2") != null) {
        String frozenV2Bandwidth = transInfoObject.get("frozenV2").toString();
        ret.put("frozenV2_bandwidth", frozenV2Bandwidth);
      }
      if (transInfoObject.get("unfrozenV2") != null) {
        String unfrozenV2Bandwidth =  transInfoObject.get("unfrozenV2").toString();
        ret.put("unfrozenV2_bandwidth", unfrozenV2Bandwidth);
      }
    } catch (SocketTimeoutException e) {
      logger.error("getAccountBandwidth exception {}", e);
    } catch (Exception e) {
      logger.error("getAccountBandwidth exception {}", e);
    }

    return ret;
  }

  private static OkHttpClient createHttpClient(int maxTotalConnections,
                                               long connectionKeepAliveTimeInMillis) {
    ConnectionPool connectionPool = new ConnectionPool(maxTotalConnections,
        connectionKeepAliveTimeInMillis,
        TimeUnit.MILLISECONDS);
    return new OkHttpClient.Builder()
        .followRedirects(false)
        .retryOnConnectionFailure(true)
        .connectionPool(connectionPool)
        .build();
  }

}
