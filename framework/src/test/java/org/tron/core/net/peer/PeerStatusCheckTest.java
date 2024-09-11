package org.tron.core.net.peer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PeerStatusCheck.class})
public class PeerStatusCheckTest {
  @InjectMocks
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
    PeerStatusCheck peerStatusCheckMock = spy(peerStatusCheck);

    doThrow(new RuntimeException("test exception")).when(peerStatusCheckMock).statusCheck();
    peerStatusCheckMock.init();

    // the initialDelay of scheduleWithFixedDelay is 5s
    Thread.sleep(5000L);
    Assert.assertEquals(true, true);
  }

}
