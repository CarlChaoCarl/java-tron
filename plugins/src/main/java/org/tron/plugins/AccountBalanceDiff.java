package org.tron.plugins;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


@Slf4j(topic = "accountbalance")
@CommandLine.Command(name = "accountbalance", aliases = "accountbalance",
    description = "Quick diff accountbalance data.",
    exitCodeListHeading = "Exit Codes:%n",
    exitCodeList = {
        "0:Successful",
        "n:Internal error: exception occurred,please check toolkit.log"})
public class AccountBalanceDiff implements Callable<Integer> {
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
      logger.info(" {} exist, please delete it first.", dest);
      spec.commandLine().getErr().println(spec.commandLine().getColorScheme()
          .errorText(String.format("%s exist, please delete it first.", dest)));
      return 402;
    }

    Long balance1 = transAccount(src, address);
    Long balance2 = transAccount(dest, address);
    if (balance1.equals(balance2)) {
      logger.info("balance is the same");
    } else {
      logger.info("balance is not the same");
    }

    long during = (System.currentTimeMillis() - time) / 1000;
    logger.info("total use {} seconds", during);
    return 0;
  }

  private Long transAccount(String apiNode, String address) throws IOException {
      Long balance = getAccountBalance(apiNode, address);
      logger.info("balance {}", balance);
      return balance;
  }

  private Long getAccountBalance(String apiNode, String address) {
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

  private static OkHttpClient createHttpClient(int maxTotalConnections, long connectionKeepAliveTimeInMillis) {
    ConnectionPool connectionPool = new ConnectionPool(maxTotalConnections, connectionKeepAliveTimeInMillis, TimeUnit.MILLISECONDS);
    return new OkHttpClient.Builder()
        .followRedirects(false)
        .retryOnConnectionFailure(true)
        .connectionPool(connectionPool)
        .build();
  }

}
