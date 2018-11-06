package org.tron.common.zksnark.merkle;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.utils.ByteArray;
import org.tron.common.zksnark.SHA256CompressCapsule;
import org.tron.core.db.Manager;

@Slf4j
public class MerkleContainer {

  @Setter
  @Getter
  private Manager manager;

  public static MerkleContainer createInstance(Manager manager) {
    MerkleContainer instance = new MerkleContainer();
    instance.setManager(manager);
    return instance;
  }

  //need persist
//  public static HashMap<String, IncrementalMerkleTreeContainer> this.manager.getMerkleTreeStore() = new HashMap();
//  public static HashMap<String, IncrementalMerkleWitnessContainer> this.manager.getMerkleWitnessStore() = new HashMap();

  public static IncrementalMerkleTreeContainer lastTree;


  public static IncrementalMerkleTreeContainer getBestMerkleRoot() {
    return lastTree;
  }


  public boolean rootIsExist(byte[] rt) {
    return this.manager.getMerkleTreeStore().contain(rt);
  }


  public IncrementalMerkleTreeCapsule getMerkleTree(byte[] rt) {
    return this.manager.getMerkleTreeStore().get(rt);
  }

  public IncrementalMerkleTreeContainer saveCm(byte[] rt, byte[] cm1, byte[] cm2) {
    IncrementalMerkleTreeContainer tree = this.manager.getMerkleTreeStore().get(rt)
        .toMerkleTreeContainer();

    SHA256CompressCapsule sha256CompressCapsule1 = new SHA256CompressCapsule();
    sha256CompressCapsule1.setContent(ByteString.copyFrom(cm1));
    tree.append(sha256CompressCapsule1.getInstance());

    SHA256CompressCapsule sha256CompressCapsule2 = new SHA256CompressCapsule();
    sha256CompressCapsule2.setContent(ByteString.copyFrom(cm2));
    tree.append(sha256CompressCapsule2.getInstance());

    this.manager.getMerkleTreeStore().put(tree.getRootKey(), tree.getTreeCapsule());
    return tree;
  }

  public void putMerkleTree(byte[] key, IncrementalMerkleTreeCapsule capsule) {
    this.manager.getMerkleTreeStore().put(key, capsule);
  }


  public void putMerkleWitness(byte[] key, IncrementalMerkleWitnessCapsule capsule) {
    this.manager.getMerkleWitnessStore().put(key, capsule);
  }

  public MerklePath path(byte[] rt) {
    IncrementalMerkleTreeContainer tree = this.manager.getMerkleTreeStore().get(rt)
        .toMerkleTreeContainer();
    return tree.path();
  }

  private byte[] createWitnessKey(String txHash, int index) {
    return ByteArray.fromString(txHash + index);
  }

  private IncrementalMerkleWitnessContainer getWitness(String txHash, int index) {
    return this.manager.getMerkleWitnessStore().get(createWitnessKey(txHash, index))
        .toMerkleWitnessContainer();
  }

  public void saveWitness(String txHash, int index,
      IncrementalMerkleWitnessContainer witness) {
    this.manager.getMerkleWitnessStore()
        .put(createWitnessKey(txHash, index), witness.getWitnessCapsule());
  }


}
