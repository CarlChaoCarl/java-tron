package org.tron.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.io.IOException;
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


@Slf4j(topic = "accountenergy")
@CommandLine.Command(name = "accountenergy", aliases = "accountenergy",
    description = "Quick diff accountenergy data.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:Internal error: exception occurred,please check toolkit.log"})
public class AccountEnergyDiff implements Callable<Integer> {

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
    HashMap<String, String> accountEnergy1 = transAccountEnergy(src, address);
    HashMap<String, String> accountEnergy2 = transAccountEnergy(dest, address);
    if (accountEnergyDiff(accountEnergy1, accountEnergy2)) {
      logger.info("accountEnergy is the same");
    } else {
      logger.info("accountEnergy is not the same");
    }

    long during = (System.currentTimeMillis() - time) / 1000;
    logger.info("total use {} seconds", during);
    return 0;
  }

  private boolean accountEnergyDiff(HashMap<String, String> accountEnergy1,
                                    HashMap<String, String> accountEnergy2) {
    List<String> fieldList = new ArrayList<String>();
    List initList = Arrays.asList(
        "energy_usage",
        "frozen_balance_for_energy",
        "latest_consume_time_for_energy",
        "acquired_delegated_frozen_balance_for_energy",
        "delegated_frozen_balance_for_energy",
        "energy_window_size",
        "delegated_frozenV2_balance_for_energy",
        "acquired_delegated_frozenV2_balance_for_energy",
        "energy_window_optimized",
        "frozenV2_energy",
        "unfrozenV2_energy"
    );
    fieldList.addAll(initList);

    for (int i = 0; i < fieldList.size(); i++) {
      String fieldName = fieldList.get(i);
      if (accountEnergy1.get(fieldName) == null
          && accountEnergy2.get(fieldName) != null) {
        return false;
      }
      if (accountEnergy1.get(fieldName) != null
          && accountEnergy2.get(fieldName) == null) {
        return false;
      }

      if (accountEnergy1.get(fieldName) != null
          && accountEnergy2.get(fieldName) != null
          && !accountEnergy1.get(fieldName).equals(accountEnergy2.get(fieldName))) {
        logger.warn("field name:{},  value1:{} and value2:{}, is not the same",
            fieldName,
            accountEnergy1.get(fieldName),
            accountEnergy2.get(fieldName)
        );
        return false;
      }
    }

    return true;
  }

  private HashMap<String, String> transAccountEnergy(String apiNode,
                                                     String address) {
    HashMap<String, String> accountEnergy = getAccountEnergy(apiNode, address);
    logger.info("accountEnergy {}", JSON.toJSON(accountEnergy));
    return accountEnergy;
  }

  private HashMap<String, String> getAccountEnergy(String apiNode, String address) {
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

      JSONObject accountResource = (JSONObject)transInfoObject.get("account_resource");
      if (accountResource.get("energy_usage") != null) {
        String energy_usage =  accountResource.get("energy_usage").toString();
        ret.put("energy_usage", energy_usage);
      }
      if (accountResource.get("frozen_balance_for_energy") != null) {
        String frozen_balance_for_energy
            =  accountResource.get("frozen_balance_for_energy").toString();
        ret.put("frozen_balance_for_energy", frozen_balance_for_energy);
      }
      if (accountResource.get("latest_consume_time_for_energy") != null) {
        String latest_consume_time_for_energy
            =  accountResource.get("latest_consume_time_for_energy").toString();
        ret.put("latest_consume_time_for_energy",
            latest_consume_time_for_energy);
      }
      if (accountResource.get("acquired_delegated_frozen_balance_for_energy") != null) {
        String acquired_delegated_frozen_balance_for_energy
            =  accountResource.get("acquired_delegated_frozen_balance_for_energy").toString();
        ret.put("acquired_delegated_frozen_balance_for_energy",
            acquired_delegated_frozen_balance_for_energy);
      }
      if (accountResource.get("delegated_frozen_balance_for_energy") != null) {
        String delegated_frozen_balance_for_energy
            =  accountResource.get("delegated_frozen_balance_for_energy").toString();
        ret.put("delegated_frozen_balance_for_energy",
            delegated_frozen_balance_for_energy);
      }
      if (accountResource.get("energy_window_size") != null) {
        String energy_window_size =  accountResource.get("energy_window_size").toString();
        ret.put("energy_window_size", energy_window_size);
      }
      if (accountResource.get("delegated_frozenV2_balance_for_energy") != null) {
        String delegated_frozenV2_balance_for_energy
            =  accountResource.get("delegated_frozenV2_balance_for_energy").toString();
        ret.put("delegated_frozenV2_balance_for_energy",
            delegated_frozenV2_balance_for_energy);
      }
      if (accountResource.get("acquired_delegated_frozenV2_balance_for_energy") != null) {
        String acquired_delegated_frozenV2_balance_for_energy
            =  accountResource.get("acquired_delegated_frozenV2_balance_for_energy").toString();
        ret.put("acquired_delegated_frozenV2_balance_for_energy",
            acquired_delegated_frozenV2_balance_for_energy);
      }
      if (accountResource.get("energy_window_optimized") != null) {
        String energy_window_optimized =  accountResource.get("energy_window_optimized").toString();
        ret.put("energy_window_optimized", energy_window_optimized);
      }
      if (transInfoObject.get("frozenV2") != null) {
        String frozenV2Energy =  transInfoObject.get("frozenV2").toString();
        ret.put("frozenV2_energy", frozenV2Energy);
      }
      if (transInfoObject.get("unfrozenV2") != null) {
        String unfrozenV2Energy =  transInfoObject.get("unfrozenV2").toString();
        ret.put("unfrozenV2_energy", unfrozenV2Energy);
      }
    } catch (SocketTimeoutException e) {
      logger.error("getAccountEnergy exception {}", e);
    } catch (Exception e) {
      logger.error("getAccountEnergy exception {}", e);
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
