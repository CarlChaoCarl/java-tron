package org.tron.common.runtime;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.tron.core.vm.program.Program;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RuntimeImpl.class})
@Slf4j
public class RuntimeImplMockTest {
  private RuntimeImpl runtime = new RuntimeImpl();

  @Before
  public void init() {

  }

  @After
  public void  clearMocks() {
    Mockito.framework().clearInlineMocks();
  }

  @Test
  public void testSetResultCode1() throws Exception {
    ProgramResult programResult = new ProgramResult();

    // exception instanceof BadJumpDestinationException
    Program.BadJumpDestinationException badJumpDestinationException
        = new Program.BadJumpDestinationException("Operation with pc isn't 'JUMPDEST': PC[%d];", 0);
    programResult.setException(badJumpDestinationException);
    //power mock private method：setResultCode
    Whitebox.invokeMethod(runtime,"setResultCode", programResult);

    // exception instanceof OutOfTimeException
    Program.OutOfTimeException outOfTimeException
        = new Program.OutOfTimeException("CPU timeout for 0x0a executing");
    programResult.setException(outOfTimeException);
    //power mock private method：setResultCode
    Whitebox.invokeMethod(runtime,"setResultCode", programResult);


    // exception instanceof PrecompiledContractException
    Program.PrecompiledContractException precompiledContractException
        = new Program.PrecompiledContractException("precompiled contract exception");
    programResult.setException(precompiledContractException);
    //power mock private method：setResultCode
    Whitebox.invokeMethod(runtime,"setResultCode", programResult);

    // exception instanceof StackTooSmallException
    Program.StackTooSmallException stackTooSmallException
        = new Program.StackTooSmallException("Expected stack size %d but actual %d;", 100, 10);
    programResult.setException(stackTooSmallException);
    //power mock private method：setResultCode
    Whitebox.invokeMethod(runtime,"setResultCode", programResult);

    // exception instanceof JVMStackOverFlowException
    Program.JVMStackOverFlowException jvmStackOverFlowException
        = new Program.JVMStackOverFlowException();
    programResult.setException(jvmStackOverFlowException);
    //power mock private method：setResultCode
    Whitebox.invokeMethod(runtime,"setResultCode", programResult);

    Assert.assertTrue(true);
  }

}

