package org.tron.core.store;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.config.Parameter.ChainConstant;
import org.tron.core.db.TronStoreWithRevoking;

@Slf4j(topic = "DB")
@Component
public class ProposalSettingStore extends TronStoreWithRevoking<BytesCapsule> {

  private static final byte[] LATEST_BLOCK_HEADER_TIMESTAMP = "latest_block_header_timestamp"
      .getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_NUMBER = "latest_block_header_number".getBytes();
  private static final byte[] LATEST_BLOCK_HEADER_HASH = "latest_block_header_hash".getBytes();
  private static final byte[] STATE_FLAG = "state_flag"
      .getBytes(); // 1 : is maintenance, 0 : is not maintenance
  private static final byte[] LATEST_SOLIDIFIED_BLOCK_NUM = "LATEST_SOLIDIFIED_BLOCK_NUM"
      .getBytes();

  private static final byte[] LATEST_PROPOSAL_NUM = "LATEST_PROPOSAL_NUM".getBytes();

  private static final byte[] LATEST_EXCHANGE_NUM = "LATEST_EXCHANGE_NUM".getBytes();

  private static final byte[] BLOCK_FILLED_SLOTS = "BLOCK_FILLED_SLOTS".getBytes();

  private static final byte[] BLOCK_FILLED_SLOTS_INDEX = "BLOCK_FILLED_SLOTS_INDEX".getBytes();

  private static final byte[] NEXT_MAINTENANCE_TIME = "NEXT_MAINTENANCE_TIME".getBytes();

  private static final byte[] MAX_FROZEN_TIME = "MAX_FROZEN_TIME".getBytes();

  private static final byte[] MIN_FROZEN_TIME = "MIN_FROZEN_TIME".getBytes();

  private static final byte[] MAX_FROZEN_SUPPLY_NUMBER = "MAX_FROZEN_SUPPLY_NUMBER".getBytes();

  private static final byte[] MAX_FROZEN_SUPPLY_TIME = "MAX_FROZEN_SUPPLY_TIME".getBytes();

  private static final byte[] MIN_FROZEN_SUPPLY_TIME = "MIN_FROZEN_SUPPLY_TIME".getBytes();

  private static final byte[] WITNESS_ALLOWANCE_FROZEN_TIME = "WITNESS_ALLOWANCE_FROZEN_TIME"
      .getBytes();

  private static final byte[] MAINTENANCE_TIME_INTERVAL = "MAINTENANCE_TIME_INTERVAL".getBytes();
  private static final String MAINTENANCE_TIME_INTERVAL_STR = "MAINTENANCE_TIME_INTERVAL";

  private static final byte[] ACCOUNT_UPGRADE_COST = "ACCOUNT_UPGRADE_COST".getBytes();
  private static final String ACCOUNT_UPGRADE_COST_STR = "ACCOUNT_UPGRADE_COST";

  private static final byte[] WITNESS_PAY_PER_BLOCK = "WITNESS_PAY_PER_BLOCK".getBytes();
  private static final String WITNESS_PAY_PER_BLOCK_STR = "WITNESS_PAY_PER_BLOCK";

  private static final byte[] WITNESS_127_PAY_PER_BLOCK = "WITNESS_127_PAY_PER_BLOCK".getBytes();
  private static final String WITNESS_127_PAY_PER_BLOCK_STR = "WITNESS_127_PAY_PER_BLOCK";

  private static final byte[] WITNESS_STANDBY_ALLOWANCE = "WITNESS_STANDBY_ALLOWANCE".getBytes();
  private static final String WITNESS_STANDBY_ALLOWANCE_STR = "WITNESS_STANDBY_ALLOWANCE";

  private static final byte[] ENERGY_FEE = "ENERGY_FEE".getBytes();
  private static final String ENERGY_FEE_STR = "ENERGY_FEE";

  private static final long DEFAULT_ENERGY_FEE = 100L;
  public static final String DEFAULT_ENERGY_PRICE_HISTORY = "0:" + DEFAULT_ENERGY_FEE;
  private static final byte[] MAX_CPU_TIME_OF_ONE_TX = "MAX_CPU_TIME_OF_ONE_TX".getBytes();
  private static final String MAX_CPU_TIME_OF_ONE_TX_STR = "MAX_CPU_TIME_OF_ONE_TX";

  //abandon
  private static final byte[] CREATE_ACCOUNT_FEE = "CREATE_ACCOUNT_FEE".getBytes();
  private static final String CREATE_ACCOUNT_FEE_STR = "CREATE_ACCOUNT_FEE";


  private static final byte[] CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT
      = "CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT".getBytes();
  private static final String CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT_STR
      = "CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT";

  private static final byte[] CREATE_NEW_ACCOUNT_BANDWIDTH_RATE =
      "CREATE_NEW_ACCOUNT_BANDWIDTH_RATE".getBytes();
  private static final String CREATE_NEW_ACCOUNT_BANDWIDTH_RATE_STR =
      "CREATE_NEW_ACCOUNT_BANDWIDTH_RATE";

  private static final byte[] TRANSACTION_FEE = "TRANSACTION_FEE".getBytes(); // 1 byte
  private static final String TRANSACTION_FEE_STR = "TRANSACTION_FEE";

  private static final long DEFAULT_TRANSACTION_FEE = 10L;
  public static final String DEFAULT_BANDWIDTH_PRICE_HISTORY = "0:" + DEFAULT_TRANSACTION_FEE;

  private static final byte[] ASSET_ISSUE_FEE = "ASSET_ISSUE_FEE".getBytes();
  private static final String ASSET_ISSUE_FEE_STR = "ASSET_ISSUE_FEE";

  private static final byte[] UPDATE_ACCOUNT_PERMISSION_FEE = "UPDATE_ACCOUNT_PERMISSION_FEE"
      .getBytes();
  private static final String UPDATE_ACCOUNT_PERMISSION_FEE_STR = "UPDATE_ACCOUNT_PERMISSION_FEE";

  private static final byte[] MULTI_SIGN_FEE = "MULTI_SIGN_FEE"
      .getBytes();
  private static final String MULTI_SIGN_FEE_STR = "MULTI_SIGN_FEE";

  private static final byte[] SHIELDED_TRANSACTION_FEE = "SHIELDED_TRANSACTION_FEE".getBytes();
  private static final byte[] SHIELDED_TRANSACTION_CREATE_ACCOUNT_FEE =
      "SHIELDED_TRANSACTION_CREATE_ACCOUNT_FEE".getBytes();
  //This value should be not negative
  private static final byte[] TOTAL_SHIELDED_POOL_VALUE = "TOTAL_SHIELDED_POOL_VALUE".getBytes();
  private static final byte[] EXCHANGE_CREATE_FEE = "EXCHANGE_CREATE_FEE".getBytes();
  private static final String EXCHANGE_CREATE_FEE_STR = "EXCHANGE_CREATE_FEE";

  private static final byte[] EXCHANGE_BALANCE_LIMIT = "EXCHANGE_BALANCE_LIMIT".getBytes();
  private static final byte[] TOTAL_TRANSACTION_COST = "TOTAL_TRANSACTION_COST".getBytes();
  private static final byte[] TOTAL_CREATE_ACCOUNT_COST = "TOTAL_CREATE_ACCOUNT_COST".getBytes();
  private static final byte[] TOTAL_CREATE_WITNESS_COST = "TOTAL_CREATE_WITNESS_FEE".getBytes();
  private static final byte[] TOTAL_STORAGE_POOL = "TOTAL_STORAGE_POOL".getBytes();
  private static final byte[] TOTAL_STORAGE_TAX = "TOTAL_STORAGE_TAX".getBytes();
  private static final byte[] TOTAL_STORAGE_RESERVED = "TOTAL_STORAGE_RESERVED".getBytes();
  private static final byte[] STORAGE_EXCHANGE_TAX_RATE = "STORAGE_EXCHANGE_TAX_RATE".getBytes();
  private static final String FORK_CONTROLLER = "FORK_CONTROLLER";
  private static final String FORK_PREFIX = "FORK_VERSION_";
  //This value is only allowed to be 0, 1, -1
  private static final byte[] REMOVE_THE_POWER_OF_THE_GR = "REMOVE_THE_POWER_OF_THE_GR".getBytes();
  private static final String REMOVE_THE_POWER_OF_THE_GR_STR = "REMOVE_THE_POWER_OF_THE_GR";

  //This value is only allowed to be 0, 1, -1
  private static final byte[] ALLOW_DELEGATE_RESOURCE = "ALLOW_DELEGATE_RESOURCE".getBytes();
  private static final String ALLOW_DELEGATE_RESOURCE_STR = "ALLOW_DELEGATE_RESOURCE";
  //This value is only allowed to be 0, 1, -1
  private static final byte[] ALLOW_ADAPTIVE_ENERGY = "ALLOW_ADAPTIVE_ENERGY".getBytes();
  private static final String ALLOW_ADAPTIVE_ENERGY_STR = "ALLOW_ADAPTIVE_ENERGY";
  //This value is only allowed to be 0, 1, -1
  private static final byte[] ALLOW_UPDATE_ACCOUNT_NAME = "ALLOW_UPDATE_ACCOUNT_NAME".getBytes();
  private static final String ALLOW_UPDATE_ACCOUNT_NAME_STR = "ALLOW_UPDATE_ACCOUNT_NAME";
  //This value is only allowed to be 0, 1, -1
  //Note: there is a space in this key name. This space must not be deleted.
  private static final byte[] ALLOW_SAME_TOKEN_NAME = " ALLOW_SAME_TOKEN_NAME".getBytes();
  private static final String ALLOW_SAME_TOKEN_NAME_STR = " ALLOW_SAME_TOKEN_NAME";

  //If the parameter is larger than 0, the contract is allowed to be created.
  private static final byte[] ALLOW_CREATION_OF_CONTRACTS = "ALLOW_CREATION_OF_CONTRACTS"
      .getBytes();
  private static final String ALLOW_CREATION_OF_CONTRACTS_STR = "ALLOW_CREATION_OF_CONTRACTS";

  //Used only for multi sign
  private static final byte[] TOTAL_SIGN_NUM = "TOTAL_SIGN_NUM".getBytes();
  //Used only for multi sign, once，value is {0,1}
  private static final byte[] ALLOW_MULTI_SIGN = "ALLOW_MULTI_SIGN".getBytes();
  private static final String ALLOW_MULTI_SIGN_STR = "ALLOW_MULTI_SIGN";

  //token id,Incremental，The initial value is 1000000
  @Getter
  private static final byte[] TOKEN_ID_NUM = "TOKEN_ID_NUM".getBytes();
  //Used only for token updates, once，value is {0,1}
  private static final byte[] TOKEN_UPDATE_DONE = "TOKEN_UPDATE_DONE".getBytes();
  //Used only for abi moves, once，value is {0,1}
  private static final byte[] ABI_MOVE_DONE = "ABI_MOVE_DONE".getBytes();
  //This value is only allowed to be 0, 1, -1
  private static final byte[] ALLOW_TVM_TRANSFER_TRC10 = "ALLOW_TVM_TRANSFER_TRC10".getBytes();
  private static final String ALLOW_TVM_TRANSFER_TRC10_STR = "ALLOW_TVM_TRANSFER_TRC10";
  //If the parameter is larger than 0, allow ZKsnark Transaction
  private static final byte[] ALLOW_SHIELDED_TRANSACTION = "ALLOW_SHIELDED_TRANSACTION".getBytes();
  private static final byte[] ALLOW_SHIELDED_TRC20_TRANSACTION =
      "ALLOW_SHIELDED_TRC20_TRANSACTION"
          .getBytes();
  private static final String ALLOW_SHIELDED_TRC20_TRANSACTION_STR =
      "ALLOW_SHIELDED_TRC20_TRANSACTION";

  private static final byte[] ALLOW_TVM_ISTANBUL = "ALLOW_TVM_ISTANBUL".getBytes();
  private static final String ALLOW_TVM_ISTANBUL_STR = "ALLOW_TVM_ISTANBUL";

  private static final byte[] ALLOW_TVM_CONSTANTINOPLE = "ALLOW_TVM_CONSTANTINOPLE".getBytes();
  private static final String ALLOW_TVM_CONSTANTINOPLE_STR = "ALLOW_TVM_CONSTANTINOPLE";

  private static final byte[] ALLOW_TVM_SOLIDITY_059 = "ALLOW_TVM_SOLIDITY_059".getBytes();
  private static final String ALLOW_TVM_SOLIDITY_059_STR = "ALLOW_TVM_SOLIDITY_059";

  private static final byte[] FORBID_TRANSFER_TO_CONTRACT = "FORBID_TRANSFER_TO_CONTRACT"
      .getBytes();
  private static final String FORBID_TRANSFER_TO_CONTRACT_STR = "FORBID_TRANSFER_TO_CONTRACT";

  //Used only for protobuf data filter , once，value is 0,1
  private static final byte[] ALLOW_PROTO_FILTER_NUM = "ALLOW_PROTO_FILTER_NUM"
      .getBytes();
  private static final String ALLOW_PROTO_FILTER_NUM_STR = "ALLOW_PROTO_FILTER_NUM";

  private static final byte[] AVAILABLE_CONTRACT_TYPE = "AVAILABLE_CONTRACT_TYPE".getBytes();
  private static final byte[] ACTIVE_DEFAULT_OPERATIONS = "ACTIVE_DEFAULT_OPERATIONS".getBytes();
  //Used only for account state root, once，value is {0,1} allow is 1
  private static final byte[] ALLOW_ACCOUNT_STATE_ROOT = "ALLOW_ACCOUNT_STATE_ROOT".getBytes();
  private static final String ALLOW_ACCOUNT_STATE_ROOT_STR = "ALLOW_ACCOUNT_STATE_ROOT";

  private static final byte[] CURRENT_CYCLE_NUMBER = "CURRENT_CYCLE_NUMBER".getBytes();
  private static final byte[] CHANGE_DELEGATION = "CHANGE_DELEGATION".getBytes();
  private static final String CHANGE_DELEGATION_STR = "CHANGE_DELEGATION";

  private static final byte[] ALLOW_PBFT = "ALLOW_PBFT".getBytes();
  private static final String ALLOW_PBFT_STR = "ALLOW_PBFT";

  private static final byte[] ALLOW_MARKET_TRANSACTION = "ALLOW_MARKET_TRANSACTION".getBytes();
  private static final String ALLOW_MARKET_TRANSACTION_STR = "ALLOW_MARKET_TRANSACTION";

  private static final byte[] MARKET_SELL_FEE = "MARKET_SELL_FEE".getBytes();
  private static final String MARKET_SELL_FEE_STR = "MARKET_SELL_FEE";

  private static final byte[] MARKET_CANCEL_FEE = "MARKET_CANCEL_FEE".getBytes();
  private static final String MARKET_CANCEL_FEE_STR = "MARKET_CANCEL_FEE";
  
  private static final byte[] MARKET_QUANTITY_LIMIT = "MARKET_QUANTITY_LIMIT".getBytes();

  private static final byte[] ALLOW_TRANSACTION_FEE_POOL = "ALLOW_TRANSACTION_FEE_POOL".getBytes();
  private static final String ALLOW_TRANSACTION_FEE_POOL_STR = "ALLOW_TRANSACTION_FEE_POOL";

  private static final byte[] TRANSACTION_FEE_POOL = "TRANSACTION_FEE_POOL".getBytes();

  private static final byte[] MAX_FEE_LIMIT = "MAX_FEE_LIMIT".getBytes();
  private static final String MAX_FEE_LIMIT_STR = "MAX_FEE_LIMIT";

  private static final byte[] BURN_TRX_AMOUNT = "BURN_TRX_AMOUNT".getBytes();
  private static final byte[] ALLOW_BLACKHOLE_OPTIMIZATION =
      "ALLOW_BLACKHOLE_OPTIMIZATION".getBytes();
  private static final String ALLOW_BLACKHOLE_OPTIMIZATION_STR =
      "ALLOW_BLACKHOLE_OPTIMIZATION";

  private static final byte[] ALLOW_NEW_RESOURCE_MODEL = "ALLOW_NEW_RESOURCE_MODEL".getBytes();
  private static final String ALLOW_NEW_RESOURCE_MODEL_STR = "ALLOW_NEW_RESOURCE_MODEL";

  private static final byte[] ALLOW_TVM_FREEZE = "ALLOW_TVM_FREEZE".getBytes();
  private static final String ALLOW_TVM_FREEZE_STR = "ALLOW_TVM_FREEZE";

  private static final byte[] ALLOW_TVM_VOTE = "ALLOW_TVM_VOTE".getBytes();
  private static final String ALLOW_TVM_VOTE_STR = "ALLOW_TVM_VOTE";

  private static final byte[] ALLOW_TVM_LONDON = "ALLOW_TVM_LONDON".getBytes();
  private static final String ALLOW_TVM_LONDON_STR = "ALLOW_TVM_LONDON";

  private static final byte[] ALLOW_TVM_COMPATIBLE_EVM = "ALLOW_TVM_COMPATIBLE_EVM".getBytes();
  private static final String ALLOW_TVM_COMPATIBLE_EVM_STR = "ALLOW_TVM_COMPATIBLE_EVM";

  private static final byte[] NEW_REWARD_ALGORITHM_EFFECTIVE_CYCLE =
      "NEW_REWARD_ALGORITHM_EFFECTIVE_CYCLE".getBytes();
  //This value is only allowed to be 1
  private static final byte[] ALLOW_ACCOUNT_ASSET_OPTIMIZATION =
      "ALLOW_ACCOUNT_ASSET_OPTIMIZATION".getBytes();
  private static final String ALLOW_ACCOUNT_ASSET_OPTIMIZATION_STR =
      "ALLOW_ACCOUNT_ASSET_OPTIMIZATION";

  private static final byte[] ALLOW_ASSET_OPTIMIZATION =
      "ALLOW_ASSET_OPTIMIZATION".getBytes();
  private static final String ALLOW_ASSET_OPTIMIZATION_STR =
      "ALLOW_ASSET_OPTIMIZATION";


  private static final byte[] ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX =
      "ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX".getBytes();
  private static final String ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX_STR =
      "ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX";

  private static final byte[] ALLOW_NEW_REWARD = "ALLOW_NEW_REWARD".getBytes();
  private static final String ALLOW_NEW_REWARD_STR = "ALLOW_NEW_REWARD";

  private static final byte[] MEMO_FEE = "MEMO_FEE".getBytes();
  private static final String MEMO_FEE_STR = "MEMO_FEE";

  private static final byte[] MEMO_FEE_HISTORY = "MEMO_FEE_HISTORY".getBytes();
  private static final byte[] ALLOW_DELEGATE_OPTIMIZATION =
      "ALLOW_DELEGATE_OPTIMIZATION".getBytes();
  private static final String ALLOW_DELEGATE_OPTIMIZATION_STR =
      "ALLOW_DELEGATE_OPTIMIZATION";

  private static final byte[] ALLOW_DYNAMIC_ENERGY =
      "ALLOW_DYNAMIC_ENERGY".getBytes();
  private static final String ALLOW_DYNAMIC_ENERGY_STR =
      "ALLOW_DYNAMIC_ENERGY";

  private static final byte[] DYNAMIC_ENERGY_THRESHOLD =
      "DYNAMIC_ENERGY_THRESHOLD".getBytes();
  private static final String DYNAMIC_ENERGY_THRESHOLD_STR =
      "DYNAMIC_ENERGY_THRESHOLD";

  private static final byte[] DYNAMIC_ENERGY_INCREASE_FACTOR =
      "DYNAMIC_ENERGY_INCREASE_FACTOR".getBytes();
  private static final String DYNAMIC_ENERGY_INCREASE_FACTOR_STR =
      "DYNAMIC_ENERGY_INCREASE_FACTOR";

  private static final byte[] DYNAMIC_ENERGY_MAX_FACTOR =
      "DYNAMIC_ENERGY_MAX_FACTOR".getBytes();
  private static final String DYNAMIC_ENERGY_MAX_FACTOR_STR =
      "DYNAMIC_ENERGY_MAX_FACTOR";

  private static final byte[] UNFREEZE_DELAY_DAYS = "UNFREEZE_DELAY_DAYS".getBytes();
  private static final String UNFREEZE_DELAY_DAYS_STR = "UNFREEZE_DELAY_DAYS";

  private static final byte[] ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID =
      "ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID".getBytes();
  private static final String ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID_STR =
      "ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID";

  private static final byte[] ALLOW_TVM_SHANGHAI = "ALLOW_TVM_SHANGHAI".getBytes();
  private static final String ALLOW_TVM_SHANGHAI_STR = "ALLOW_TVM_SHANGHAI";

  private static final byte[] ALLOW_CANCEL_UNFREEZE_V2 = "ALLOW_CANCEL_UNFREEZE_V2"
      .getBytes();
  private static final String ALLOW_CANCEL_UNFREEZE_V2_STR = "ALLOW_CANCEL_UNFREEZE_V2";

  private static final byte[] ALLOW_OPTIMIZE_LOCK_DELEGATE_RESOURCE =
      "ALLOW_OPTIMIZE_LOCK_DELEGATE_RESOURCE".getBytes();
  private static final String ALLOW_OPTIMIZE_LOCK_DELEGATE_RESOURCE_STR =
      "ALLOW_OPTIMIZE_LOCK_DELEGATE_RESOURCE";

  private Map<String, Long> cacheValues = new HashMap<String, Long>();

  public void resetCache() {
    this.cacheValues.clear();
  }

  @Autowired
  private ProposalSettingStore(@Value("proposal_setting") String dbName) {
    super(dbName);
  }

  public String intArrayToString(int[] a) {
    StringBuilder sb = new StringBuilder();
    for (int i : a) {
      sb.append(i);
    }
    return sb.toString();
  }

  public int[] stringToIntArray(String s) {
    int length = s.length();
    int[] result = new int[length];
    for (int i = 0; i < length; ++i) {
      result[i] = Integer.parseInt(s.substring(i, i + 1));
    }
    return result;
  }

  public void saveTokenIdNum(long num) {
    this.put(TOKEN_ID_NUM,
        new BytesCapsule(ByteArray.fromLong(num)));
  }

  public long getTokenIdNum() {
    return Optional.ofNullable(getUnchecked(TOKEN_ID_NUM))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOKEN_ID_NUM"));
  }

  public void saveBlockFilledSlotsIndex(int blockFilledSlotsIndex) {
    logger.debug("BlockFilledSlotsIndex: {}.", blockFilledSlotsIndex);
    this.put(BLOCK_FILLED_SLOTS_INDEX,
        new BytesCapsule(ByteArray.fromInt(blockFilledSlotsIndex)));
  }

  public int getBlockFilledSlotsIndex() {
    return Optional.ofNullable(getUnchecked(BLOCK_FILLED_SLOTS_INDEX))
        .map(BytesCapsule::getData)
        .map(ByteArray::toInt)
        .orElseThrow(
            () -> new IllegalArgumentException("not found BLOCK_FILLED_SLOTS_INDEX"));
  }

  public int getMinFrozenTime() {
    return Optional.ofNullable(getUnchecked(MIN_FROZEN_TIME))
        .map(BytesCapsule::getData)
        .map(ByteArray::toInt)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MIN_FROZEN_TIME"));
  }

  public void saveMaintenanceTimeInterval(long timeInterval) {
    logger.debug("MAINTENANCE_TIME_INTERVAL:" + timeInterval);
    this.put(MAINTENANCE_TIME_INTERVAL,
        new BytesCapsule(ByteArray.fromLong(timeInterval)));
    this.cacheValues.put(MAINTENANCE_TIME_INTERVAL_STR, timeInterval);
  }

  public long getMaintenanceTimeInterval() {
    Long maintenanceTimeInterval =  this.cacheValues.get(MAINTENANCE_TIME_INTERVAL_STR);
    if (maintenanceTimeInterval !=null) {
      return maintenanceTimeInterval;
    }
    return getMaintenanceTimeIntervalFromDB();
  }

  public long getMaintenanceTimeIntervalFromDB() {
    return Optional.ofNullable(getUnchecked(MAINTENANCE_TIME_INTERVAL))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MAINTENANCE_TIME_INTERVAL"));
  }

  public void saveAccountUpgradeCost(long accountUpgradeCost) {
    logger.debug("ACCOUNT_UPGRADE_COST:" + accountUpgradeCost);
    this.put(ACCOUNT_UPGRADE_COST,
        new BytesCapsule(ByteArray.fromLong(accountUpgradeCost)));
    this.cacheValues.put(ACCOUNT_UPGRADE_COST_STR, accountUpgradeCost);
  }

  public long getAccountUpgradeCost() {
    Long accountUpgradeCost =  this.cacheValues.get(ACCOUNT_UPGRADE_COST_STR);
    if (accountUpgradeCost !=null) {
      return accountUpgradeCost;
    }
    return getAccountUpgradeCostFromDB();
  }

  public long getAccountUpgradeCostFromDB() {
    return Optional.ofNullable(getUnchecked(ACCOUNT_UPGRADE_COST))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ACCOUNT_UPGRADE_COST"));
  }

  public void saveWitnessPayPerBlock(long pay) {
    logger.debug("WITNESS_PAY_PER_BLOCK:" + pay);
    this.put(WITNESS_PAY_PER_BLOCK,
        new BytesCapsule(ByteArray.fromLong(pay)));
    this.cacheValues.put(WITNESS_PAY_PER_BLOCK_STR, pay);
  }

  public long getWitnessPayPerBlock() {
    Long witnessPayPerBlock =  this.cacheValues.get(WITNESS_PAY_PER_BLOCK_STR);
    if (witnessPayPerBlock !=null) {
      return witnessPayPerBlock;
    }
    return getWitnessPayPerBlockFromDB();
  }

  public long getWitnessPayPerBlockFromDB() {
    return Optional.ofNullable(getUnchecked(WITNESS_PAY_PER_BLOCK))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found WITNESS_PAY_PER_BLOCK"));
  }

  public void saveWitness127PayPerBlock(long pay) {
    logger.debug("WITNESS_127_PAY_PER_BLOCK:" + pay);
    this.put(WITNESS_127_PAY_PER_BLOCK,
        new BytesCapsule(ByteArray.fromLong(pay)));
    this.cacheValues.put(WITNESS_127_PAY_PER_BLOCK_STR, pay);
  }

  public long getWitness127PayPerBlock() {
    Long witness127PayPerBlock =  this.cacheValues.get(WITNESS_127_PAY_PER_BLOCK_STR);
    if (witness127PayPerBlock !=null) {
      return witness127PayPerBlock;
    }
    return getWitness127PayPerBlockFromDB();
  }
  public long getWitness127PayPerBlockFromDB() {
    return Optional.ofNullable(getUnchecked(WITNESS_127_PAY_PER_BLOCK))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElse(16000000L);
  }

  public void saveWitnessStandbyAllowance(long allowance) {
    logger.debug("WITNESS_STANDBY_ALLOWANCE:" + allowance);
    this.put(WITNESS_STANDBY_ALLOWANCE,
        new BytesCapsule(ByteArray.fromLong(allowance)));
    this.cacheValues.put(WITNESS_STANDBY_ALLOWANCE_STR, allowance);
  }

  public long getWitnessStandbyAllowance() {
    Long witnessStandbyAllowance =  this.cacheValues.get(WITNESS_STANDBY_ALLOWANCE_STR);
    if (witnessStandbyAllowance !=null) {
      return witnessStandbyAllowance;
    }
    return getWitnessStandbyAllowanceFromDB();
  }

  public long getWitnessStandbyAllowanceFromDB() {
    return Optional.ofNullable(getUnchecked(WITNESS_STANDBY_ALLOWANCE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found WITNESS_STANDBY_ALLOWANCE"));
  }

  public void saveFreeNetLimit(long freeNetLimit) {
    this.put(DynamicResourceProperties.FREE_NET_LIMIT,
        new BytesCapsule(ByteArray.fromLong(freeNetLimit)));
    this.cacheValues.put(DynamicResourceProperties.FREE_NET_LIMIT_STR, freeNetLimit);
  }

  public long getFreeNetLimit() {
    Long freeNetLimit =  this.cacheValues.get(DynamicResourceProperties.FREE_NET_LIMIT_STR);
    if (freeNetLimit !=null) {
      return freeNetLimit;
    }
    return getFreeNetLimitFromDB();
  }
  public long getFreeNetLimitFromDB() {
    return Optional.ofNullable(getUnchecked(DynamicResourceProperties.FREE_NET_LIMIT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found FREE_NET_LIMIT"));
  }

  public void saveTotalNetWeight(long totalNetWeight) {
    this.put(DynamicResourceProperties.TOTAL_NET_WEIGHT,
        new BytesCapsule(ByteArray.fromLong(totalNetWeight)));
  }

  public long getTotalNetWeight() {
    return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_NET_WEIGHT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_NET_WEIGHT"));
  }

  public void saveTotalEnergyWeight(long totalEnergyWeight) {
    this.put(DynamicResourceProperties.TOTAL_ENERGY_WEIGHT,
        new BytesCapsule(ByteArray.fromLong(totalEnergyWeight)));
  }

  public long getTotalEnergyWeight() {
    return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_ENERGY_WEIGHT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_ENERGY_WEIGHT"));
  }

  public void saveTotalTronPowerWeight(long totalEnergyWeight) {
    this.put(DynamicResourceProperties.TOTAL_TRON_POWER_WEIGHT,
        new BytesCapsule(ByteArray.fromLong(totalEnergyWeight)));
  }

  public long getTotalTronPowerWeight() {
    return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_TRON_POWER_WEIGHT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_TRON_POWER_WEIGHT"));
  }

  public void saveTotalNetLimit(long totalNetLimit) {
    this.put(DynamicResourceProperties.TOTAL_NET_LIMIT,
        new BytesCapsule(ByteArray.fromLong(totalNetLimit)));
    this.cacheValues.put(DynamicResourceProperties.TOTAL_NET_LIMIT_STR, totalNetLimit);
  }

  public long getTotalNetLimit() {
    Long totalNetLimit =  this.cacheValues.get(DynamicResourceProperties.TOTAL_NET_LIMIT_STR);
    if (totalNetLimit !=null) {
      return totalNetLimit;
    }
    return getTotalNetLimitFromDB();
  }
  public long getTotalNetLimitFromDB() {
    return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_NET_LIMIT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_NET_LIMIT"));
  }

  @Deprecated
  public void saveTotalEnergyLimit(long totalEnergyLimit) {
    this.put(DynamicResourceProperties.TOTAL_ENERGY_LIMIT,
        new BytesCapsule(ByteArray.fromLong(totalEnergyLimit)));

    long ratio = getAdaptiveResourceLimitTargetRatio();
    saveTotalEnergyTargetLimit(totalEnergyLimit / ratio);
  }

  public void saveTotalEnergyLimit2(long totalEnergyLimit) {
    this.put(DynamicResourceProperties.TOTAL_ENERGY_LIMIT,
        new BytesCapsule(ByteArray.fromLong(totalEnergyLimit)));
    this.cacheValues.put(DynamicResourceProperties.TOTAL_ENERGY_LIMIT_STR, totalEnergyLimit);
  }

  public long getTotalEnergyLimit() {
    return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_ENERGY_LIMIT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_ENERGY_LIMIT"));
  }

  public void saveTotalEnergyCurrentLimit(long totalEnergyCurrentLimit) {
    this.put(DynamicResourceProperties.TOTAL_ENERGY_CURRENT_LIMIT,
        new BytesCapsule(ByteArray.fromLong(totalEnergyCurrentLimit)));
    this.cacheValues.put(DynamicResourceProperties.TOTAL_ENERGY_CURRENT_LIMIT_STR, totalEnergyCurrentLimit);
  }

  public long getTotalEnergyCurrentLimit() {
    Long totalEnergyCurrentLimit =  this.cacheValues.get(DynamicResourceProperties.TOTAL_ENERGY_CURRENT_LIMIT_STR);
    if (totalEnergyCurrentLimit !=null) {
      return totalEnergyCurrentLimit;
    }
    return getTotalEnergyCurrentLimitFromDB();
  }

  public long getTotalEnergyCurrentLimitFromDB() {
    return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_ENERGY_CURRENT_LIMIT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_ENERGY_CURRENT_LIMIT"));
  }

  public void saveTotalEnergyTargetLimit(long targetTotalEnergyLimit) {
    this.put(DynamicResourceProperties.TOTAL_ENERGY_TARGET_LIMIT,
        new BytesCapsule(ByteArray.fromLong(targetTotalEnergyLimit)));
    this.cacheValues.put(DynamicResourceProperties.TOTAL_ENERGY_TARGET_LIMIT_STR, targetTotalEnergyLimit);
  }

  public long getTotalEnergyTargetLimit() {
    Long totalEnergyTargetLimit =  this.cacheValues.get(DynamicResourceProperties.TOTAL_ENERGY_TARGET_LIMIT_STR);
    if (totalEnergyTargetLimit !=null) {
      return totalEnergyTargetLimit;
    }
    return getTotalEnergyTargetLimitFromDB();
  }
  public long getTotalEnergyTargetLimitFromDB() {
    return Optional.ofNullable(getUnchecked(DynamicResourceProperties.TOTAL_ENERGY_TARGET_LIMIT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_ENERGY_TARGET_LIMIT"));
  }

  public void saveAdaptiveResourceLimitMultiplier(long adaptiveResourceLimitMultiplier) {
    this.put(DynamicResourceProperties.ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER,
        new BytesCapsule(ByteArray.fromLong(adaptiveResourceLimitMultiplier)));
    this.cacheValues.put(DynamicResourceProperties.ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER_STR, adaptiveResourceLimitMultiplier);
  }

  public long getAdaptiveResourceLimitMultiplier() {
    Long adaptiveResourceLimitMultiplier =  this.cacheValues
        .get(DynamicResourceProperties.ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER_STR);
    if (adaptiveResourceLimitMultiplier !=null) {
      return adaptiveResourceLimitMultiplier;
    }
    return getAdaptiveResourceLimitMultiplierFromDB();
  }
  public long getAdaptiveResourceLimitMultiplierFromDB() {
    return Optional
        .ofNullable(getUnchecked(DynamicResourceProperties.ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER"));
  }

  public void saveAdaptiveResourceLimitTargetRatio(long adaptiveResourceLimitTargetRatio) {
    this.put(DynamicResourceProperties.ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO,
        new BytesCapsule(ByteArray.fromLong(adaptiveResourceLimitTargetRatio)));
    this.cacheValues.put(DynamicResourceProperties.ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO_STR,
        adaptiveResourceLimitTargetRatio);
  }

  public long getAdaptiveResourceLimitTargetRatio() {
    Long adaptiveResourceLimitTargetRatio =  this.cacheValues
        .get(DynamicResourceProperties.ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO_STR);
    if (adaptiveResourceLimitTargetRatio !=null) {
      return adaptiveResourceLimitTargetRatio;
    }
    return getAdaptiveResourceLimitTargetRatioFromDB();
  }
  public long getAdaptiveResourceLimitTargetRatioFromDB() {
    return Optional
        .ofNullable(getUnchecked(DynamicResourceProperties.ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO"));
  }


  public void saveEnergyFee(long totalEnergyFee) {
    this.put(ENERGY_FEE,
        new BytesCapsule(ByteArray.fromLong(totalEnergyFee)));
    this.cacheValues.put(ENERGY_FEE_STR, totalEnergyFee);
  }

  public long getEnergyFee() {
    Long energyFee =  this.cacheValues.get(ENERGY_FEE_STR);
    if (energyFee !=null) {
      return energyFee;
    }
    return getEnergyFeeFromDB();
  }

  public long getEnergyFeeFromDB() {
    return Optional.ofNullable(getUnchecked(ENERGY_FEE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ENERGY_FEE"));
  }

  public void saveMaxCpuTimeOfOneTx(long time) {
    this.put(MAX_CPU_TIME_OF_ONE_TX,
        new BytesCapsule(ByteArray.fromLong(time)));
    this.cacheValues.put(MAX_CPU_TIME_OF_ONE_TX_STR, time);
  }

  public long getMaxCpuTimeOfOneTx() {
    Long maxCpuTimeOfOneTx =  this.cacheValues.get(MAX_CPU_TIME_OF_ONE_TX_STR);
    if (maxCpuTimeOfOneTx !=null) {
      return maxCpuTimeOfOneTx;
    }
    return getMaxCpuTimeOfOneTxFromDB();
  }

  public long getMaxCpuTimeOfOneTxFromDB() {
    return Optional.ofNullable(getUnchecked(MAX_CPU_TIME_OF_ONE_TX))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MAX_CPU_TIME_OF_ONE_TX"));
  }

  public void saveCreateAccountFee(long fee) {
    this.put(CREATE_ACCOUNT_FEE,
        new BytesCapsule(ByteArray.fromLong(fee)));
    this.cacheValues.put(CREATE_ACCOUNT_FEE_STR, fee);
  }


  public long getShieldedTransactionFee() {
    return Optional.ofNullable(getUnchecked(SHIELDED_TRANSACTION_FEE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found SHIELD_TRANSACTION_FEE"));
  }

  public long getCreateAccountFee() {
    Long createAccountFee =  this.cacheValues.get(CREATE_ACCOUNT_FEE_STR);
    if (createAccountFee !=null) {
      return createAccountFee;
    }
    return getMaintenanceTimeIntervalFromDB();
  }


  public void saveCreateNewAccountFeeInSystemContract(long fee) {
    this.put(CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT,
        new BytesCapsule(ByteArray.fromLong(fee)));
    this.cacheValues.put(CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT_STR, fee);
  }

  public long getCreateNewAccountFeeInSystemContract() {
    Long createNewAccountFeeInSystemContract =  this.cacheValues
        .get(CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT_STR);
    if (createNewAccountFeeInSystemContract !=null) {
      return createNewAccountFeeInSystemContract;
    }
    return getCreateNewAccountFeeInSystemContractFromDB();
  }

  public long getCreateNewAccountFeeInSystemContractFromDB() {
    return Optional.ofNullable(getUnchecked(CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException(
                "not found CREATE_NEW_ACCOUNT_FEE_IN_SYSTEM_CONTRACT"));
  }

  public void saveCreateNewAccountBandwidthRate(long rate) {
    this.put(CREATE_NEW_ACCOUNT_BANDWIDTH_RATE,
        new BytesCapsule(ByteArray.fromLong(rate)));
    this.cacheValues.put(CREATE_NEW_ACCOUNT_BANDWIDTH_RATE_STR, rate);
  }

  public long getCreateNewAccountBandwidthRate() {
    Long createNewAccountBandwidthRate =  this.cacheValues.get(CREATE_NEW_ACCOUNT_BANDWIDTH_RATE_STR);
    if (createNewAccountBandwidthRate !=null) {
      return createNewAccountBandwidthRate;
    }
    return getCreateNewAccountBandwidthRateFromDB();
  }

  public long getCreateNewAccountBandwidthRateFromDB() {
    return Optional.ofNullable(getUnchecked(CREATE_NEW_ACCOUNT_BANDWIDTH_RATE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException(
                "not found CREATE_NsEW_ACCOUNT_BANDWIDTH_RATE2"));
  }


  public void saveTransactionFee(long fee) {
    this.cacheValues.put(TRANSACTION_FEE_STR, fee);
    this.put(TRANSACTION_FEE,
        new BytesCapsule(ByteArray.fromLong(fee)));
  }

  public long getTransactionFee() {
    Long transactionFee =  this.cacheValues.get(TRANSACTION_FEE_STR);
    if (transactionFee !=null) {
      return transactionFee;
    }
    return getTransactionFeeFromDB();
  }

  public long getTransactionFeeFromDB() {
    return Optional.ofNullable(getUnchecked(TRANSACTION_FEE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TRANSACTION_FEE"));
  }

  public void saveAssetIssueFee(long fee) {
    this.put(ASSET_ISSUE_FEE,
        new BytesCapsule(ByteArray.fromLong(fee)));
    this.cacheValues.put(ASSET_ISSUE_FEE_STR, fee);
  }

  public void saveUpdateAccountPermissionFee(long fee) {
    this.put(UPDATE_ACCOUNT_PERMISSION_FEE,
        new BytesCapsule(ByteArray.fromLong(fee)));
    this.cacheValues.put(UPDATE_ACCOUNT_PERMISSION_FEE_STR, fee);
  }

  public void saveMultiSignFee(long fee) {
    this.put(MULTI_SIGN_FEE,
        new BytesCapsule(ByteArray.fromLong(fee)));
    this.cacheValues.put(MULTI_SIGN_FEE_STR, fee);
  }

  public long getAssetIssueFee() {
    Long assetIssueFee =  this.cacheValues.get(ASSET_ISSUE_FEE_STR);
    if (assetIssueFee !=null) {
      return assetIssueFee;
    }
    return getAssetIssueFeeFromDB();
  }

  public long getAssetIssueFeeFromDB() {
    return Optional.ofNullable(getUnchecked(ASSET_ISSUE_FEE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ASSET_ISSUE_FEE"));
  }

  public long getUpdateAccountPermissionFee() {
    Long updateAccountPermissionFee =  this.cacheValues.get(UPDATE_ACCOUNT_PERMISSION_FEE_STR);
    if (updateAccountPermissionFee !=null) {
      return updateAccountPermissionFee;
    }
    return getUpdateAccountPermissionFeeFromDB();
  }
  public long getUpdateAccountPermissionFeeFromDB() {
    return Optional.ofNullable(getUnchecked(UPDATE_ACCOUNT_PERMISSION_FEE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found UPDATE_ACCOUNT_PERMISSION_FEE"));
  }

  public long getMultiSignFee() {
    Long multiSignFee =  this.cacheValues.get(MULTI_SIGN_FEE_STR);
    if (multiSignFee !=null) {
      return multiSignFee;
    }
    return getMultiSignFeeFromDB();
  }
  public long getMultiSignFeeFromDB() {
    return Optional.ofNullable(getUnchecked(MULTI_SIGN_FEE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MULTI_SIGN_FEE"));
  }

  public void saveExchangeCreateFee(long fee) {
    this.put(EXCHANGE_CREATE_FEE,
        new BytesCapsule(ByteArray.fromLong(fee)));
    this.cacheValues.put(EXCHANGE_CREATE_FEE_STR, fee);
  }

  public long getExchangeCreateFee() {
    Long exchangeCreateFee =  this.cacheValues.get(EXCHANGE_CREATE_FEE_STR);
    if (exchangeCreateFee !=null) {
      return exchangeCreateFee;
    }
    return getExchangeCreateFeeFromDB();
  }

  public long getExchangeCreateFeeFromDB() {
    return Optional.ofNullable(getUnchecked(EXCHANGE_CREATE_FEE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found EXCHANGE_CREATE_FEE"));
  }


  public long getExchangeBalanceLimit() {
    return Optional.ofNullable(getUnchecked(EXCHANGE_BALANCE_LIMIT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found EXCHANGE_BALANCE_LIMIT"));
  }

  public void saveAllowMarketTransaction(long allowMarketTransaction) {
    this.put(ALLOW_MARKET_TRANSACTION,
        new BytesCapsule(ByteArray.fromLong(allowMarketTransaction)));
    this.cacheValues.put(ALLOW_MARKET_TRANSACTION_STR, allowMarketTransaction);
  }

  public long getAllowMarketTransaction() {
    Long allowMarketTransaction =  this.cacheValues.get(ALLOW_MARKET_TRANSACTION_STR);
    if (allowMarketTransaction !=null) {
      return allowMarketTransaction;
    }
    return getAllowMarketTransactionFromDB();
  }
  public long getAllowMarketTransactionFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_MARKET_TRANSACTION))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_MARKET_TRANSACTION"));
  }

  public void saveMarketSellFee(long fee) {
    this.put(MARKET_SELL_FEE,
        new BytesCapsule(ByteArray.fromLong(fee)));
    this.cacheValues.put(MARKET_SELL_FEE_STR, fee);
  }

  public long getMarketSellFee() {
    Long marketSellFee =  this.cacheValues.get(MARKET_SELL_FEE_STR);
    if (marketSellFee !=null) {
      return marketSellFee;
    }
    return getMarketSellFeeFromDB();
  }
  public long getMarketSellFeeFromDB() {
    return Optional.ofNullable(getUnchecked(MARKET_SELL_FEE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MARKET_SELL_FEE"));
  }

  public void saveMarketCancelFee(long fee) {
    this.put(MARKET_CANCEL_FEE,
        new BytesCapsule(ByteArray.fromLong(fee)));
    this.cacheValues.put(MARKET_CANCEL_FEE_STR, fee);
  }

  public long getMarketCancelFee() {
    Long marketCancelFee =  this.cacheValues.get(MARKET_CANCEL_FEE_STR);
    if (marketCancelFee !=null) {
      return marketCancelFee;
    }
    return getMarketCancelFeeFromDB();
  }
  public long getMarketCancelFeeFromDB() {
    return Optional.ofNullable(getUnchecked(MARKET_CANCEL_FEE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MARKET_CANCEL_FEE"));
  }

  public void saveAllowTransactionFeePool(long value) {
    this.put(ALLOW_TRANSACTION_FEE_POOL,
        new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_TRANSACTION_FEE_POOL_STR, value);
  }

  public long getAllowTransactionFeePool() {
    Long allowTransactionFeePool =  this.cacheValues.get(ALLOW_TRANSACTION_FEE_POOL_STR);
    if (allowTransactionFeePool !=null) {
      return allowTransactionFeePool;
    }
    return getAllowTransactionFeePoolFromDB();
  }
  public long getAllowTransactionFeePoolFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_TRANSACTION_FEE_POOL))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_TRANSACTION_FEE_POOL"));
  }


  public void saveTotalTransactionCost(long value) {
    this.put(TOTAL_TRANSACTION_COST,
        new BytesCapsule(ByteArray.fromLong(value)));
  }

  public long getTotalTransactionCost() {
    return Optional.ofNullable(getUnchecked(TOTAL_TRANSACTION_COST))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found TOTAL_TRANSACTION_COST"));
  }

  public void saveRemoveThePowerOfTheGr(long rate) {
    this.put(REMOVE_THE_POWER_OF_THE_GR,
        new BytesCapsule(ByteArray.fromLong(rate)));
    this.cacheValues.put(REMOVE_THE_POWER_OF_THE_GR_STR, rate);
  }

  public long getRemoveThePowerOfTheGr() {
    Long removeThePowerOfTheGr =  this.cacheValues.get(REMOVE_THE_POWER_OF_THE_GR_STR);
    if (removeThePowerOfTheGr !=null) {
      return removeThePowerOfTheGr;
    }
    return getRemoveThePowerOfTheGrFromDB();
  }

  public long getRemoveThePowerOfTheGrFromDB() {
    return Optional.ofNullable(getUnchecked(REMOVE_THE_POWER_OF_THE_GR))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found REMOVE_THE_POWER_OF_THE_GR"));
  }

  public void saveAllowDelegateResource(long value) {
    this.put(ALLOW_DELEGATE_RESOURCE,
        new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_DELEGATE_RESOURCE_STR, value);
  }

  public long getAllowDelegateResource() {
    Long allowDelegateResource =  this.cacheValues.get(ALLOW_DELEGATE_RESOURCE_STR);
    if (allowDelegateResource !=null) {
      return allowDelegateResource;
    }
    return getAllowDelegateResourceFromDB();
  }

  public long getAllowDelegateResourceFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_DELEGATE_RESOURCE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_DELEGATE_RESOURCE"));
  }

  public void saveAllowAdaptiveEnergy(long value) {
    this.put(ALLOW_ADAPTIVE_ENERGY,
        new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_ADAPTIVE_ENERGY_STR, value);
  }

  public long getAllowAdaptiveEnergy() {
    Long allowAdaptiveEnergy =  this.cacheValues.get(ALLOW_ADAPTIVE_ENERGY_STR);
    if (allowAdaptiveEnergy !=null) {
      return allowAdaptiveEnergy;
    }
    return getAllowAdaptiveEnergyFromDB();
  }
  public long getAllowAdaptiveEnergyFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_ADAPTIVE_ENERGY))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_ADAPTIVE_ENERGY"));
  }

  public void saveAllowTvmTransferTrc10(long value) {
    this.put(ALLOW_TVM_TRANSFER_TRC10,
        new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_TVM_TRANSFER_TRC10_STR, value);
  }

  public long getAllowTvmTransferTrc10() {
    Long allowTvmTransferTrc10 =  this.cacheValues.get(ALLOW_TVM_TRANSFER_TRC10_STR);
    if (allowTvmTransferTrc10 !=null) {
      return allowTvmTransferTrc10;
    }
    return getAllowTvmTransferTrc10FromDB();
  }

  public long getAllowTvmTransferTrc10FromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_TVM_TRANSFER_TRC10))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_TVM_TRANSFER_TRC10"));
  }

  public void saveAllowTvmConstantinople(long value) {
    this.put(ALLOW_TVM_CONSTANTINOPLE,
        new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_TVM_CONSTANTINOPLE_STR, value);
  }

  public long getAllowTvmConstantinople() {
    Long allowTvmConstantinople =  this.cacheValues.get(ALLOW_TVM_CONSTANTINOPLE_STR);
    if (allowTvmConstantinople !=null) {
      return allowTvmConstantinople;
    }
    return getAllowTvmConstantinopleFromDB();
  }
  public long getAllowTvmConstantinopleFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_TVM_CONSTANTINOPLE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_TVM_CONSTANTINOPLE"));
  }

  public void saveAllowTvmSolidity059(long value) {
    this.put(ALLOW_TVM_SOLIDITY_059,
        new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_TVM_SOLIDITY_059_STR, value);
  }

  public long getAllowTvmSolidity059() {
    Long allowTvmSolidity059 =  this.cacheValues.get(ALLOW_TVM_SOLIDITY_059_STR);
    if (allowTvmSolidity059 !=null) {
      return allowTvmSolidity059;
    }
    return getAllowTvmSolidity059FromDB();
  }
  public long getAllowTvmSolidity059FromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_TVM_SOLIDITY_059))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found ALLOW_TVM_SOLIDITY_059"));
  }

  public void saveForbidTransferToContract(long value) {
    this.put(FORBID_TRANSFER_TO_CONTRACT,
        new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(FORBID_TRANSFER_TO_CONTRACT_STR, value);
  }

  public long getForbidTransferToContract() {
    Long forbidTransferToContract =  this.cacheValues.get(FORBID_TRANSFER_TO_CONTRACT_STR);
    if (forbidTransferToContract !=null) {
      return forbidTransferToContract;
    }
    return getForbidTransferToContractFromDB();
  }
  public long getForbidTransferToContractFromDB() {
    return Optional.ofNullable(getUnchecked(FORBID_TRANSFER_TO_CONTRACT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found FORBID_TRANSFER_TO_CONTRACT"));
  }

  public void saveAvailableContractType(byte[] value) {
    this.put(AVAILABLE_CONTRACT_TYPE,
        new BytesCapsule(value));
  }

  public byte[] getAvailableContractType() {
    return Optional.ofNullable(getUnchecked(AVAILABLE_CONTRACT_TYPE))
        .map(BytesCapsule::getData)
        .orElseThrow(
            () -> new IllegalArgumentException("not found AVAILABLE_CONTRACT_TYPE"));
  }

  public void saveActiveDefaultOperations(byte[] value) {
    this.put(ACTIVE_DEFAULT_OPERATIONS,
        new BytesCapsule(value));
  }

  public byte[] getActiveDefaultOperations() {
    return Optional.ofNullable(getUnchecked(ACTIVE_DEFAULT_OPERATIONS))
        .map(BytesCapsule::getData)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ACTIVE_DEFAULT_OPERATIONS"));
  }

  public void saveAllowUpdateAccountName(long rate) {
    this.put(ALLOW_UPDATE_ACCOUNT_NAME,
        new BytesCapsule(ByteArray.fromLong(rate)));
    this.cacheValues.put(ALLOW_UPDATE_ACCOUNT_NAME_STR, rate);
  }

  public long getAllowUpdateAccountName() {
    Long allowUpdateAccountName =  this.cacheValues.get(ALLOW_UPDATE_ACCOUNT_NAME_STR);
    if (allowUpdateAccountName !=null) {
      return allowUpdateAccountName;
    }
    return getAllowUpdateAccountNameFromDB();
  }

  public long getAllowUpdateAccountNameFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_UPDATE_ACCOUNT_NAME))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_UPDATE_ACCOUNT_NAME"));
  }

  public void saveAllowSameTokenName(long rate) {
    this.put(ALLOW_SAME_TOKEN_NAME,
        new BytesCapsule(ByteArray.fromLong(rate)));
    this.cacheValues.put(ALLOW_SAME_TOKEN_NAME_STR, rate);
  }

  public long getAllowSameTokenName() {
    Long allowSameTokenName =  this.cacheValues.get(MAINTENANCE_TIME_INTERVAL_STR);
    if (allowSameTokenName !=null) {
      return allowSameTokenName;
    }
    return getAllowSameTokenNameFromDB();
  }

  public long getAllowSameTokenNameFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_SAME_TOKEN_NAME))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_SAME_TOKEN_NAME"));
  }

  public void saveAllowCreationOfContracts(long allowCreationOfContracts) {
    this.put(ALLOW_CREATION_OF_CONTRACTS,
        new BytesCapsule(ByteArray.fromLong(allowCreationOfContracts)));
    this.cacheValues.put(ALLOW_CREATION_OF_CONTRACTS_STR, allowCreationOfContracts);
  }

  public void saveAllowMultiSign(long allowMultiSing) {
    this.put(ALLOW_MULTI_SIGN,
        new BytesCapsule(ByteArray.fromLong(allowMultiSing)));
    this.cacheValues.put(ALLOW_MULTI_SIGN_STR, allowMultiSing);
  }

  public long getAllowMultiSign() {
    Long allowMultiSign =  this.cacheValues.get(ALLOW_MULTI_SIGN_STR);
    if (allowMultiSign !=null) {
      return allowMultiSign;
    }
    return getMaintenanceTimeIntervalFromDB();
  }

  public long getAllowCreationOfContracts() {
    Long allowCreationOfContracts =  this.cacheValues.get(ALLOW_CREATION_OF_CONTRACTS_STR);
    if (allowCreationOfContracts !=null) {
      return allowCreationOfContracts;
    }
    return getAllowCreationOfContractsFromDB();
  }

  public long getAllowCreationOfContractsFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_CREATION_OF_CONTRACTS))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_CREATION_OF_CONTRACTS"));
  }

  public void saveAllowShieldedTransaction(long allowShieldedTransaction) {
    this.put(ALLOW_SHIELDED_TRANSACTION,
        new BytesCapsule(ByteArray.fromLong(allowShieldedTransaction)));
  }


  public void saveAllowShieldedTRC20Transaction(long allowShieldedTRC20Transaction) {
    this.put(ALLOW_SHIELDED_TRC20_TRANSACTION,
        new BytesCapsule(ByteArray.fromLong(allowShieldedTRC20Transaction)));
    this.cacheValues.put(ALLOW_SHIELDED_TRC20_TRANSACTION_STR, allowShieldedTRC20Transaction);
  }

  public long getAllowShieldedTRC20Transaction() {
    Long allowShieldedTRC20Transaction =  this.cacheValues.get(ALLOW_SHIELDED_TRC20_TRANSACTION_STR);
    if (allowShieldedTRC20Transaction !=null) {
      return allowShieldedTRC20Transaction;
    }
    return getAllowShieldedTRC20TransactionFromDB();
  }
  public long getAllowShieldedTRC20TransactionFromDB() {
    String msg = "not found ALLOW_SHIELDED_TRC20_TRANSACTION";
    return Optional.ofNullable(getUnchecked(ALLOW_SHIELDED_TRC20_TRANSACTION))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException(msg));
  }

  public void saveAllowTvmIstanbul(long allowTVMIstanbul) {
    this.put(ALLOW_TVM_ISTANBUL,
        new BytesCapsule(ByteArray.fromLong(allowTVMIstanbul)));
    this.cacheValues.put(ALLOW_TVM_ISTANBUL_STR, allowTVMIstanbul);
  }

  public long getAllowTvmIstanbul() {
    Long allowTvmIstanbul =  this.cacheValues.get(ALLOW_TVM_ISTANBUL_STR);
    if (allowTvmIstanbul !=null) {
      return allowTvmIstanbul;
    }
    return getAllowTvmIstanbulFromDB();
  }
  public long getAllowTvmIstanbulFromDB() {
    String msg = "not found ALLOW_TVM_ISTANBUL";
    return Optional.ofNullable(getUnchecked(ALLOW_TVM_ISTANBUL))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException(msg));
  }


  public void saveBlockFilledSlots(int[] blockFilledSlots) {
    logger.debug("BlockFilledSlots: {}.", intArrayToString(blockFilledSlots));
    this.put(BLOCK_FILLED_SLOTS,
        new BytesCapsule(ByteArray.fromString(intArrayToString(blockFilledSlots))));
  }

  public int[] getBlockFilledSlots() {
    return Optional.ofNullable(getUnchecked(BLOCK_FILLED_SLOTS))
        .map(BytesCapsule::getData)
        .map(ByteArray::toStr)
        .map(this::stringToIntArray)
        .orElseThrow(
            () -> new IllegalArgumentException(
                "not found latest SOLIDIFIED_BLOCK_NUM timestamp"));
  }

  public int getBlockFilledSlotsNumber() {
    return ChainConstant.BLOCK_FILLED_SLOTS_NUMBER;
  }

  public void applyBlock(boolean fillBlock) {
    int[] blockFilledSlots = getBlockFilledSlots();
    int blockFilledSlotsIndex = getBlockFilledSlotsIndex();
    blockFilledSlots[blockFilledSlotsIndex] = fillBlock ? 1 : 0;
    saveBlockFilledSlotsIndex((blockFilledSlotsIndex + 1) % getBlockFilledSlotsNumber());
    saveBlockFilledSlots(blockFilledSlots);
  }

  public long getLatestSolidifiedBlockNum() {
    return Optional.ofNullable(getUnchecked(LATEST_SOLIDIFIED_BLOCK_NUM))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found latest SOLIDIFIED_BLOCK_NUM"));
  }


  public long getLatestProposalNum() {
    return Optional.ofNullable(getUnchecked(LATEST_PROPOSAL_NUM))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found latest PROPOSAL_NUM"));
  }

  /**
   * get timestamp of creating global latest block.
   */
  public long getLatestBlockHeaderTimestamp() {
    return Optional.ofNullable(getUnchecked(LATEST_BLOCK_HEADER_TIMESTAMP))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found latest block header timestamp"));
  }

  /**
   * get number of global latest block.
   */
  public long getLatestBlockHeaderNumber() {
    return Optional.ofNullable(getUnchecked(LATEST_BLOCK_HEADER_NUMBER))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found latest block header number"));
  }

  /**
   * get id of global latest block.
   */

  public Sha256Hash getLatestBlockHeaderHash() {
    byte[] blockHash = Optional.ofNullable(getUnchecked(LATEST_BLOCK_HEADER_HASH))
        .map(BytesCapsule::getData)
        .orElseThrow(() -> new IllegalArgumentException("not found block hash"));
    return Sha256Hash.wrap(blockHash);
  }

  /**
   * save timestamp of creating global latest block.
   */
  public void saveLatestBlockHeaderTimestamp(long t) {
    logger.info("Update latest block header timestamp = {}.", t);
    this.put(LATEST_BLOCK_HEADER_TIMESTAMP, new BytesCapsule(ByteArray.fromLong(t)));
  }

  /**
   * save number of global latest block.
   */
  public void saveLatestBlockHeaderNumber(long n) {
    logger.info("Update latest block header number = {}.", n);
    this.put(LATEST_BLOCK_HEADER_NUMBER, new BytesCapsule(ByteArray.fromLong(n)));
  }



  public long getNextMaintenanceTime() {
    return Optional.ofNullable(getUnchecked(NEXT_MAINTENANCE_TIME))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found NEXT_MAINTENANCE_TIME"));
  }


  public void saveNextMaintenanceTime(long nextMaintenanceTime) {
    this.put(NEXT_MAINTENANCE_TIME,
        new BytesCapsule(ByteArray.fromLong(nextMaintenanceTime)));
  }


  //The unit is trx
  public void addTotalNetWeight(long amount) {
    if (amount == 0) {
      return;
    }
    long totalNetWeight = getTotalNetWeight();
    totalNetWeight += amount;
    if (allowNewReward()) {
      totalNetWeight = Math.max(0, totalNetWeight);
    }
    saveTotalNetWeight(totalNetWeight);
  }

  //The unit is trx
  public void addTotalEnergyWeight(long amount) {
    if (amount == 0) {
      return;
    }
    long totalEnergyWeight = getTotalEnergyWeight();
    totalEnergyWeight += amount;
    if (allowNewReward()) {
      totalEnergyWeight = Math.max(0, totalEnergyWeight);
    }
    saveTotalEnergyWeight(totalEnergyWeight);
  }

  //The unit is trx
  public void addTotalTronPowerWeight(long amount) {
    if (amount == 0) {
      return;
    }
    long totalWeight = getTotalTronPowerWeight();
    totalWeight += amount;
    if (allowNewReward()) {
      totalWeight = Math.max(0, totalWeight);
    }
    saveTotalTronPowerWeight(totalWeight);
  }


  public void addTotalTransactionCost(long fee) {
    long newValue = getTotalTransactionCost() + fee;
    saveTotalTransactionCost(newValue);
  }

  public void forked(int version, boolean value) {
    String forkKey = FORK_CONTROLLER + version;
    put(forkKey.getBytes(), new BytesCapsule(Boolean.toString(value).getBytes()));
  }

  /**
   * get allow protobuf number.
   */
  public long getAllowProtoFilterNum() {
    Long allowProtoFilterNum =  this.cacheValues.get(ALLOW_PROTO_FILTER_NUM_STR);
    if (allowProtoFilterNum !=null) {
      return allowProtoFilterNum;
    }
    return getAllowProtoFilterNumFromDB();
  }
  public long getAllowProtoFilterNumFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_PROTO_FILTER_NUM))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found allow protobuf number"));
  }

  /**
   * save allow protobuf  number.
   */
  public void saveAllowProtoFilterNum(long num) {
    logger.info("Update allow protobuf number = {}.", num);
    this.put(ALLOW_PROTO_FILTER_NUM, new BytesCapsule(ByteArray.fromLong(num)));
    this.cacheValues.put(ALLOW_PROTO_FILTER_NUM_STR, num);
  }

  public void saveAllowAccountStateRoot(long allowAccountStateRoot) {
    this.put(ALLOW_ACCOUNT_STATE_ROOT,
        new BytesCapsule(ByteArray.fromLong(allowAccountStateRoot)));
    this.cacheValues.put(ALLOW_ACCOUNT_STATE_ROOT_STR, allowAccountStateRoot);
  }

  public long getAllowAccountStateRoot() {
    Long allowAccountStateRoot =  this.cacheValues.get(ALLOW_ACCOUNT_STATE_ROOT_STR);
    if (allowAccountStateRoot !=null) {
      return allowAccountStateRoot;
    }
    return getAllowAccountStateRootFromDB();
  }
  public long getAllowAccountStateRootFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_ACCOUNT_STATE_ROOT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_ACCOUNT_STATE_ROOT"));
  }

  public long getCurrentCycleNumber() {
    return Optional.ofNullable(getUnchecked(CURRENT_CYCLE_NUMBER))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElse(0L);
  }

  public void saveChangeDelegation(long number) {
    this.put(CHANGE_DELEGATION,
        new BytesCapsule(ByteArray.fromLong(number)));
    this.cacheValues.put(CHANGE_DELEGATION_STR, number);
  }

  public long getChangeDelegation() {
    Long changeDelegation =  this.cacheValues.get(CHANGE_DELEGATION_STR);
    if (changeDelegation !=null) {
      return changeDelegation;
    }
    return getChangeDelegationFromDB();
  }
  public long getChangeDelegationFromDB() {
    return Optional.ofNullable(getUnchecked(CHANGE_DELEGATION))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found CHANGE_DELEGATION"));
  }

  public void saveAllowPBFT(long number) {
    this.put(ALLOW_PBFT,
        new BytesCapsule(ByteArray.fromLong(number)));
    this.cacheValues.put(ALLOW_PBFT_STR, number);
  }

  public long getAllowPBFT() {
    Long allowPBFT =  this.cacheValues.get(ALLOW_PBFT_STR);
    if (allowPBFT !=null) {
      return allowPBFT;
    }
    return getAllowPBFTFromDB();
  }

  public long getAllowPBFTFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_PBFT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found ALLOW_PBFT"));
  }

  public long getMaxFeeLimit() {
    Long maxFeeLimit =  this.cacheValues.get(MAX_FEE_LIMIT_STR);
    if (maxFeeLimit !=null) {
      return maxFeeLimit;
    }
    return getMaxFeeLimitFromDB();
  }
  public long getMaxFeeLimitFromDB() {
    return Optional.ofNullable(getUnchecked(MAX_FEE_LIMIT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found MAX_FEE_LIMIT"));
  }

  public void saveMaxFeeLimit(long maxFeeLimit) {
    this.put(MAX_FEE_LIMIT,
        new BytesCapsule(ByteArray.fromLong(maxFeeLimit)));
    this.cacheValues.put(MAX_FEE_LIMIT_STR, maxFeeLimit);
  }

  public long getBurnTrxAmount() {
    return Optional.ofNullable(getUnchecked(BURN_TRX_AMOUNT))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found BURN_TRX_AMOUNT"));
  }

  public void burnTrx(long amount) {
    if (amount <= 0) {
      return;
    }
    amount += getBurnTrxAmount();
    saveBurnTrx(amount);
  }

  private void saveBurnTrx(long amount) {
    this.put(BURN_TRX_AMOUNT, new BytesCapsule(ByteArray.fromLong(amount)));
  }

  public boolean supportBlackHoleOptimization() {
    return getAllowBlackHoleOptimization() == 1L;
  }

  public void saveAllowBlackHoleOptimization(long value) {
    this.put(ALLOW_BLACKHOLE_OPTIMIZATION, new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_BLACKHOLE_OPTIMIZATION_STR, value);
  }

  public long getAllowBlackHoleOptimization() {
    Long allowBlackHoleOptimization =  this.cacheValues.get(ALLOW_BLACKHOLE_OPTIMIZATION_STR);
    if (allowBlackHoleOptimization !=null) {
      return allowBlackHoleOptimization;
    }
    return getAllowBlackHoleOptimizationFromDB();
  }
  public long getAllowBlackHoleOptimizationFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_BLACKHOLE_OPTIMIZATION))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_BLACKHOLE_OPTIMIZATION"));
  }


  public boolean supportAllowNewResourceModel() {
    return getAllowNewResourceModel() == 1L;
  }


  public void saveAllowNewResourceModel(long value) {
    this.put(ALLOW_NEW_RESOURCE_MODEL, new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_NEW_RESOURCE_MODEL_STR, value);
  }

  public long getAllowNewResourceModel() {
    Long allowNewResourceModel =  this.cacheValues.get(ALLOW_NEW_RESOURCE_MODEL_STR);
    if (allowNewResourceModel !=null) {
      return allowNewResourceModel;
    }
    return getAllowNewResourceModelFromDB();
  }
  public long getAllowNewResourceModelFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_NEW_RESOURCE_MODEL))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_NEW_RESOURCE_MODEL"));
  }

  public void saveAllowTvmFreeze(long allowTvmFreeze) {
    this.put(ALLOW_TVM_FREEZE,
        new BytesCapsule(ByteArray.fromLong(allowTvmFreeze)));
    this.cacheValues.put(ALLOW_TVM_FREEZE_STR, allowTvmFreeze);
  }

  public long getAllowTvmFreeze() {
    Long allowTvmFreeze =  this.cacheValues.get(ALLOW_TVM_FREEZE_STR);
    if (allowTvmFreeze !=null) {
      return allowTvmFreeze;
    }
    return getAllowTvmFreezeFromDB();
  }
  public long getAllowTvmFreezeFromDB() {
    String msg = "not found ALLOW_TVM_FREEZE";
    return Optional.ofNullable(getUnchecked(ALLOW_TVM_FREEZE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException(msg));
  }

  public void saveAllowTvmVote(long allowTvmVote) {
    this.put(ALLOW_TVM_VOTE,
        new BytesCapsule(ByteArray.fromLong(allowTvmVote)));
    this.cacheValues.put(ALLOW_TVM_VOTE_STR, allowTvmVote);
  }

  public long getAllowTvmVote() {
    Long allowTvmVote =  this.cacheValues.get(ALLOW_TVM_VOTE_STR);
    if (allowTvmVote !=null) {
      return allowTvmVote;
    }
    return getAllowTvmVoteFromDB();
  }
  public long getAllowTvmVoteFromDB() {
    String msg = "not found ALLOW_TVM_VOTE";
    return Optional.ofNullable(getUnchecked(ALLOW_TVM_VOTE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException(msg));
  }

  public void saveAllowTvmLondon(long allowTvmLondon) {
    this.put(ALLOW_TVM_LONDON,
        new BytesCapsule(ByteArray.fromLong(allowTvmLondon)));
    this.cacheValues.put(ALLOW_TVM_LONDON_STR, allowTvmLondon);
  }

  public long getAllowTvmLondon() {
    Long allowTvmLondon =  this.cacheValues.get(ALLOW_TVM_LONDON_STR);
    if (allowTvmLondon !=null) {
      return allowTvmLondon;
    }
    return getAllowTvmLondonFromDB();
  }
  public long getAllowTvmLondonFromDB() {
    String msg = "not found ALLOW_TVM_LONDON";
    return Optional.ofNullable(getUnchecked(ALLOW_TVM_LONDON))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException(msg));
  }

  public void saveAllowTvmCompatibleEvm(long allowTvmCompatibleEvm) {
    this.put(ALLOW_TVM_COMPATIBLE_EVM,
        new BytesCapsule(ByteArray.fromLong(allowTvmCompatibleEvm)));
    this.cacheValues.put(ALLOW_TVM_COMPATIBLE_EVM_STR, allowTvmCompatibleEvm);
  }

  public long getAllowTvmCompatibleEvm() {
    Long allowTvmCompatibleEvm =  this.cacheValues.get(ALLOW_TVM_COMPATIBLE_EVM_STR);
    if (allowTvmCompatibleEvm !=null) {
      return allowTvmCompatibleEvm;
    }
    return getAllowTvmCompatibleEvmFromDB();
  }
  public long getAllowTvmCompatibleEvmFromDB() {
    String msg = "not found ALLOW_TVM_COMPATIBLE_EVM";
    return Optional.ofNullable(getUnchecked(ALLOW_TVM_COMPATIBLE_EVM))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException(msg));
  }

  // 1: enable
  public long getAllowAccountAssetOptimization() {
    Long allowAccountAssetOptimization =  this.cacheValues.get(ALLOW_ACCOUNT_ASSET_OPTIMIZATION_STR);
    if (allowAccountAssetOptimization !=null) {
      return allowAccountAssetOptimization;
    }
    return getAllowAccountAssetOptimizationFromDB();
  }
  public long getAllowAccountAssetOptimizationFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_ACCOUNT_ASSET_OPTIMIZATION))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_ACCOUNT_ASSET_OPTIMIZATION"));
  }

  public void setAllowAccountAssetOptimization(long value) {
    this.put(ALLOW_ACCOUNT_ASSET_OPTIMIZATION, new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_ACCOUNT_ASSET_OPTIMIZATION_STR, value);
  }

  // 1: enable
  public long getAllowAssetOptimization() {
    Long allowAssetOptimization =  this.cacheValues.get(ALLOW_ASSET_OPTIMIZATION_STR);
    if (allowAssetOptimization !=null) {
      return allowAssetOptimization;
    }
    return getAllowAssetOptimizationFromDB();
  }
  public long getAllowAssetOptimizationFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_ASSET_OPTIMIZATION))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_ASSET_OPTIMIZATION"));
  }

  public void setAllowAssetOptimization(long value) {
    this.put(ALLOW_ASSET_OPTIMIZATION, new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_ASSET_OPTIMIZATION_STR, value);
  }

  public void saveAllowHigherLimitForMaxCpuTimeOfOneTx(long value) {
    this.put(ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX,
        new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX_STR, value);
  }

  public long getAllowHigherLimitForMaxCpuTimeOfOneTx() {
    Long allowHigherLimitForMaxCpuTimeOfOneTx =  this.cacheValues.get(ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX_STR);
    if (allowHigherLimitForMaxCpuTimeOfOneTx !=null) {
      return allowHigherLimitForMaxCpuTimeOfOneTx;
    }
    return getAllowHigherLimitForMaxCpuTimeOfOneTxFromDB();
  }
  public long getAllowHigherLimitForMaxCpuTimeOfOneTxFromDB() {
    String msg = "not found ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX";
    return Optional.ofNullable(getUnchecked(ALLOW_HIGHER_LIMIT_FOR_MAX_CPU_TIME_OF_ONE_TX))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException(msg));
  }

  public long getMemoFee() {
    Long memoFee =  this.cacheValues.get(MEMO_FEE_STR);
    if (memoFee !=null) {
      return memoFee;
    }
    return getMemoFeeFromDB();
  }
  public long getMemoFeeFromDB() {
    return Optional.ofNullable(getUnchecked(MEMO_FEE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found MEMO_FEE"));
  }


  public void saveMemoFee(long value) {
    this.put(MEMO_FEE, new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(MEMO_FEE_STR, value);
  }


  public long getAllowNewReward() {
    Long allowNewReward =  this.cacheValues.get(ALLOW_NEW_REWARD_STR);
    if (allowNewReward !=null) {
      return allowNewReward;
    }
    return getAllowNewRewardFromDB();
  }
  public long getAllowNewRewardFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_NEW_REWARD))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found AllowNewReward"));
  }

  public void saveAllowNewReward(long newReward) {
    this.put(ALLOW_NEW_REWARD, new BytesCapsule(ByteArray.fromLong(newReward)));
    this.cacheValues.put(ALLOW_NEW_REWARD_STR, newReward);
  }

  public long getAllowDelegateOptimization() {
    Long allowDelegateOptimization =  this.cacheValues.get(ALLOW_DELEGATE_OPTIMIZATION_STR);
    if (allowDelegateOptimization !=null) {
      return allowDelegateOptimization;
    }
    return getAllowDelegateOptimizationFromDB();
  }
  public long getAllowDelegateOptimizationFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_DELEGATE_OPTIMIZATION))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_DELEGATE_OPTIMIZATION"));
  }


  public void saveAllowDelegateOptimization(long value) {
    this.put(ALLOW_DELEGATE_OPTIMIZATION, new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_DELEGATE_OPTIMIZATION_STR, value);
  }

  public long getAllowDynamicEnergy() {
    Long allowDynamicEnergy =  this.cacheValues.get(ALLOW_DYNAMIC_ENERGY_STR);
    if (allowDynamicEnergy !=null) {
      return allowDynamicEnergy;
    }
    return getAllowDynamicEnergyFromDB();
  }
  public long getAllowDynamicEnergyFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_DYNAMIC_ENERGY))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found ALLOW_DYNAMIC_ENERGY"));
  }


  public void saveAllowDynamicEnergy(long value) {
    this.put(ALLOW_DYNAMIC_ENERGY, new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_DYNAMIC_ENERGY_STR, value);
  }

  public long getDynamicEnergyThreshold() {
    Long dynamicEnergyThreshold =  this.cacheValues.get(DYNAMIC_ENERGY_THRESHOLD_STR);
    if (dynamicEnergyThreshold !=null) {
      return dynamicEnergyThreshold;
    }
    return getDynamicEnergyThresholdFromDB();
  }
  public long getDynamicEnergyThresholdFromDB() {
    return Optional.ofNullable(getUnchecked(DYNAMIC_ENERGY_THRESHOLD))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found DYNAMIC_ENERGY_THRESHOLD"));
  }

  public void saveDynamicEnergyThreshold(long value) {
    this.put(DYNAMIC_ENERGY_THRESHOLD, new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(DYNAMIC_ENERGY_THRESHOLD_STR, value);
  }

  public long getDynamicEnergyIncreaseFactor() {
    Long dynamicEnergyIncreaseFactor =  this.cacheValues.get(DYNAMIC_ENERGY_INCREASE_FACTOR_STR);
    if (dynamicEnergyIncreaseFactor !=null) {
      return dynamicEnergyIncreaseFactor;
    }
    return getDynamicEnergyIncreaseFactorFromDB();
  }
  public long getDynamicEnergyIncreaseFactorFromDB() {
    return Optional.ofNullable(getUnchecked(DYNAMIC_ENERGY_INCREASE_FACTOR))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found DYNAMIC_ENERGY_INCREASE_FACTOR"));
  }

  public void saveDynamicEnergyIncreaseFactor(long value) {
    this.put(DYNAMIC_ENERGY_INCREASE_FACTOR, new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(DYNAMIC_ENERGY_INCREASE_FACTOR_STR, value);
  }

  public long getDynamicEnergyMaxFactor() {
    Long dynamicEnergyMaxFactor =  this.cacheValues.get(DYNAMIC_ENERGY_MAX_FACTOR_STR);
    if (dynamicEnergyMaxFactor !=null) {
      return dynamicEnergyMaxFactor;
    }
    return getDynamicEnergyMaxFactorFromDB();
  }
  public long getDynamicEnergyMaxFactorFromDB() {
    return Optional.ofNullable(getUnchecked(DYNAMIC_ENERGY_MAX_FACTOR))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException("not found DYNAMIC_ENERGY_MAX_FACTOR"));
  }

  public void saveDynamicEnergyMaxFactor(long value) {
    this.put(DYNAMIC_ENERGY_MAX_FACTOR, new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(DYNAMIC_ENERGY_MAX_FACTOR_STR, value);
  }

  public boolean allowNewReward() {
    return getAllowNewReward() == 1;
  }

  public long getUnfreezeDelayDays() {
    Long unfreezeDelayDays =  this.cacheValues.get(UNFREEZE_DELAY_DAYS_STR);
    if (unfreezeDelayDays !=null) {
      return unfreezeDelayDays;
    }
    return getUnfreezeDelayDaysFromDB();
  }
  public long getUnfreezeDelayDaysFromDB() {
    return Optional.ofNullable(getUnchecked(UNFREEZE_DELAY_DAYS))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(() -> new IllegalArgumentException("not found UNFREEZE_DELAY_DAYS"));
  }

  public boolean supportUnfreezeDelay() {
    return getUnfreezeDelayDays() > 0;
  }

  public void saveUnfreezeDelayDays(long value) {
    this.put(UNFREEZE_DELAY_DAYS, new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(UNFREEZE_DELAY_DAYS_STR, value);
  }

  public void saveAllowOptimizedReturnValueOfChainId(long value) {
    this.put(ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID,
        new BytesCapsule(ByteArray.fromLong(value)));
    this.cacheValues.put(ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID_STR, value);
  }

  public long getAllowOptimizedReturnValueOfChainId() {
    Long allowOptimizedReturnValueOfChainId =  this.cacheValues.get(ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID_STR);
    if (allowOptimizedReturnValueOfChainId !=null) {
      return allowOptimizedReturnValueOfChainId;
    }
    return getAllowOptimizedReturnValueOfChainIdFromDB();
  }
  public long getAllowOptimizedReturnValueOfChainIdFromDB() {
    String msg = "not found ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID";
    return Optional.ofNullable(getUnchecked(ALLOW_OPTIMIZED_RETURN_VALUE_OF_CHAIN_ID))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElseThrow(
            () -> new IllegalArgumentException(msg));
  }

  public void saveAllowTvmShangHai(long allowTvmShangHai) {
    this.put(ALLOW_TVM_SHANGHAI,
        new BytesCapsule(ByteArray.fromLong(allowTvmShangHai)));
    this.cacheValues.put(ALLOW_TVM_SHANGHAI_STR, allowTvmShangHai);
  }

  public long getAllowTvmShangHai() {
    Long allowTvmShangHai =  this.cacheValues.get(ALLOW_TVM_SHANGHAI_STR);
    if (allowTvmShangHai !=null) {
      return allowTvmShangHai;
    }
    return getAllowTvmShangHaiFromDB();
  }
  public long getAllowTvmShangHaiFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_TVM_SHANGHAI))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElse(CommonParameter.getInstance().getAllowTvmShangHai());
  }


  public void saveAllowCancelUnfreezeV2(long allowCancelUnfreezeV2) {
    this.put(ALLOW_CANCEL_UNFREEZE_V2,
        new BytesCapsule(ByteArray.fromLong(allowCancelUnfreezeV2)));
    this.cacheValues.put(ALLOW_CANCEL_UNFREEZE_V2_STR, allowCancelUnfreezeV2);
  }

  public long getAllowCancelUnfreezeV2() {
    Long allowCancelUnfreezeV2 =  this.cacheValues.get(ALLOW_CANCEL_UNFREEZE_V2_STR);
    if (allowCancelUnfreezeV2 !=null) {
      return allowCancelUnfreezeV2;
    }
    return getAllowCancelUnfreezeV2FromDB();
  }
  public long getAllowCancelUnfreezeV2FromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_CANCEL_UNFREEZE_V2))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElse(CommonParameter.getInstance().getAllowCancelUnfreezeV2());
  }


  public void saveAllowOptimizeLockDelegateResource(long allowOptimizeLockDelegateResource) {
    this.put(ALLOW_OPTIMIZE_LOCK_DELEGATE_RESOURCE,
        new BytesCapsule(ByteArray.fromLong(allowOptimizeLockDelegateResource)));
    this.cacheValues.put(ALLOW_OPTIMIZE_LOCK_DELEGATE_RESOURCE_STR, allowOptimizeLockDelegateResource);
  }

  public long getAllowOptimizeLockDelegateResource() {
    Long allowOptimizeLockDelegateResource =  this.cacheValues.get(ALLOW_OPTIMIZE_LOCK_DELEGATE_RESOURCE_STR);
    if (allowOptimizeLockDelegateResource !=null) {
      return allowOptimizeLockDelegateResource;
    }
    return getAllowOptimizeLockDelegateResourceFromDB();
  }
  public long getAllowOptimizeLockDelegateResourceFromDB() {
    return Optional.ofNullable(getUnchecked(ALLOW_OPTIMIZE_LOCK_DELEGATE_RESOURCE))
        .map(BytesCapsule::getData)
        .map(ByteArray::toLong)
        .orElse(CommonParameter.getInstance().getAllowOptimizeLockDelegateResource());
  }


  private static class DynamicResourceProperties {

    private static final byte[] ONE_DAY_NET_LIMIT = "ONE_DAY_NET_LIMIT".getBytes();
    //public free bandwidth
    private static final byte[] PUBLIC_NET_USAGE = "PUBLIC_NET_USAGE".getBytes();
    //fixed
    private static final byte[] PUBLIC_NET_LIMIT = "PUBLIC_NET_LIMIT".getBytes();
    private static final byte[] PUBLIC_NET_TIME = "PUBLIC_NET_TIME".getBytes();
    private static final byte[] FREE_NET_LIMIT = "FREE_NET_LIMIT".getBytes();
    private static final String FREE_NET_LIMIT_STR = "FREE_NET_LIMIT";

    private static final byte[] TOTAL_NET_WEIGHT = "TOTAL_NET_WEIGHT".getBytes();
    //ONE_DAY_NET_LIMIT - PUBLIC_NET_LIMIT，current TOTAL_NET_LIMIT
    private static final byte[] TOTAL_NET_LIMIT = "TOTAL_NET_LIMIT".getBytes();
    private static final String TOTAL_NET_LIMIT_STR = "TOTAL_NET_LIMIT";

    private static final byte[] TOTAL_ENERGY_TARGET_LIMIT = "TOTAL_ENERGY_TARGET_LIMIT".getBytes();
    private static final String TOTAL_ENERGY_TARGET_LIMIT_STR = "TOTAL_ENERGY_TARGET_LIMIT";

    private static final byte[] TOTAL_ENERGY_CURRENT_LIMIT = "TOTAL_ENERGY_CURRENT_LIMIT"
        .getBytes();
    private static final String TOTAL_ENERGY_CURRENT_LIMIT_STR = "TOTAL_ENERGY_CURRENT_LIMIT";

    private static final byte[] TOTAL_ENERGY_AVERAGE_USAGE = "TOTAL_ENERGY_AVERAGE_USAGE"
        .getBytes();
    private static final byte[] TOTAL_ENERGY_AVERAGE_TIME = "TOTAL_ENERGY_AVERAGE_TIME".getBytes();
    private static final byte[] TOTAL_ENERGY_WEIGHT = "TOTAL_ENERGY_WEIGHT".getBytes();
    private static final byte[] TOTAL_TRON_POWER_WEIGHT = "TOTAL_TRON_POWER_WEIGHT".getBytes();
    private static final byte[] TOTAL_ENERGY_LIMIT = "TOTAL_ENERGY_LIMIT".getBytes();
    private static final String TOTAL_ENERGY_LIMIT_STR = "TOTAL_ENERGY_LIMIT";

    private static final byte[] BLOCK_ENERGY_USAGE = "BLOCK_ENERGY_USAGE".getBytes();
    private static final byte[] ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER =
        "ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER"
            .getBytes();
    private static final String ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER_STR =
        "ADAPTIVE_RESOURCE_LIMIT_MULTIPLIER";
    private static final byte[] ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO =
        "ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO"
            .getBytes();
    private static final String ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO_STR =
        "ADAPTIVE_RESOURCE_LIMIT_TARGET_RATIO";

  }

}
