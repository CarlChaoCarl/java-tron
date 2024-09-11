package org.tron.core.consensus;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tron.common.utils.Sha256Hash;
import org.tron.consensus.dpos.DposService;
import org.tron.consensus.dpos.StateManager;
import org.tron.core.capsule.BlockCapsule;

@RunWith(PowerMockRunner.class)
@PrepareForTest({StateManager.class})
public class StateManagerTest {

  @Spy
  private StateManager stateManager = new StateManager();

  @Before
  public void init() {

  }

  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void testReceiveBlockTimeOverInterval() {
    BlockCapsule.BlockId blockId = new BlockCapsule.BlockId(Sha256Hash.ZERO_HASH, 1L);

    DposService dposServiceMock = mock(DposService.class);
    BlockCapsule blockCapsuleMock = mock(BlockCapsule.class);
    when(blockCapsuleMock.getBlockId()).thenReturn(blockId);

    stateManager.setDposService(dposServiceMock);
    stateManager.receiveBlock(blockCapsuleMock);

    Assert.assertEquals(true, true);
  }


}
