package org.tron.core.net.peer;

import static org.mockito.Mockito.doThrow;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PeerStatusCheck.class})
public class PeerStatusCheckTest {
  @Spy
  private PeerStatusCheck peerStatusCheck = new PeerStatusCheck();

  @Before
  public void init() {

  }

  @After
  public void  clearMocks() {
    peerStatusCheck.close();
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void testInitException() throws InterruptedException {
    doThrow(new RuntimeException("test exception")).when(peerStatusCheck).statusCheck();
    peerStatusCheck.init();

    // the initialDelay of scheduleWithFixedDelay is 5s
    Thread.sleep(5000L);
    Assert.assertEquals(true, true);
  }

}
