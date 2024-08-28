package org.tron.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import picocli.CommandLine;

@Slf4j(topic = "accountpermission")
@CommandLine.Command(name = "accountpermission", aliases = "accountpermission",
    description = "Quick diff accountpermission data.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:Internal error: exception occurred,please check toolkit.log"})
public class AccountPermissionDiff implements Callable<Integer> {
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

    HashMap<String, String> accountPermission1 = transAccountPermission(src, address);
    HashMap<String, String> accountPermission2 = transAccountPermission(dest, address);
    if (accountPermissionDiff(accountPermission1, accountPermission2)) {
      logger.info("accountPermission is the same");
    } else {
      logger.info("accountPermission is not the same");
    }

    long during = (System.currentTimeMillis() - time) / 1000;
    logger.info("total use {} seconds", during);
    return 0;
  }

  private boolean accountPermissionDiff(HashMap<String, String> accountPermission1,
                                        HashMap<String, String> accountPermission2) {
    if (accountPermission1.get("owner_permission") == null
        && accountPermission2.get("owner_permission") != null
    ) {
      return false;
    }
    if (accountPermission1.get("owner_permission") != null
        && accountPermission2.get("owner_permission") == null
    ) {
      return false;
    }
    if (accountPermission1.get("owner_permission") != null
        && accountPermission2.get("owner_permission") != null
        && !accountPermission1.get("owner_permission")
        .equals(accountPermission2.get("owner_permission"))
    ) {
      return false;
    }

    if (accountPermission1.get("witness_permission") == null
        && accountPermission2.get("witness_permission") != null
    ) {
      return false;
    }
    if (accountPermission1.get("witness_permission") != null
        && accountPermission2.get("witness_permission") == null
    ) {
      return false;
    }
    if (accountPermission1.get("witness_permission") != null
        && accountPermission2.get("witness_permission") != null
        && !accountPermission1.get("witness_permission")
        .equals(accountPermission2.get("witness_permission"))
    ) {
      return false;
    }
    if (accountPermission1.get("active_permission") == null
        && accountPermission2.get("active_permission") != null
    ) {
      return false;
    }
    if (accountPermission1.get("active_permission") != null
        && accountPermission2.get("active_permission") == null
    ) {
      return false;
    }
    if (accountPermission1.get("active_permission") != null
        && accountPermission2.get("active_permission") != null
        && !accountPermission1.get("active_permission")
        .equals(accountPermission2.get("active_permission"))
    ) {
      return false;
    }

    return true;
  }

  private HashMap<String, String> transAccountPermission(String apiNode,
                                                         String address) {
    HashMap<String, String> accountPermission = getAccountPermission(apiNode, address);
    logger.info("accountPermission {}", JSON.toJSON(accountPermission));
    return accountPermission;
  }

  private HashMap<String, String> getAccountPermission(String apiNode, String address) {
    String owner_permission = "";
    String witness_permission = "";
    String active_permission = "";

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

      if (transInfoObject.get("owner_permission") != null) {
        owner_permission =  transInfoObject.get("owner_permission").toString();
      }
      if (transInfoObject.get("witness_permission") != null) {
        witness_permission =  transInfoObject.get("witness_permission").toString();
      }
      if (transInfoObject.get("active_permission") != null) {
        active_permission =  transInfoObject.get("active_permission").toString();
      }
    } catch (SocketTimeoutException e) {
      // retry
      logger.error("getAccountPermission exception {}", e);
    } catch (Exception e) {
      logger.error("getAccountPermission exception {}", e);
    }
    HashMap<String, String> ret = new HashMap<>();
    if (!owner_permission.isEmpty()) {
      ret.put("owner_permission", owner_permission);
    }
    if (!witness_permission.isEmpty()) {
      ret.put("witness_permission", witness_permission);
    }
    if (!active_permission.isEmpty()) {
      ret.put("active_permission", active_permission);
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
