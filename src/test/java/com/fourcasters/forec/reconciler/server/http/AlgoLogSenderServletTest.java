package com.fourcasters.forec.reconciler.server.http;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Created by ivan on 10/01/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class AlgoLogSenderServletTest {

    private static String oldProp;
    private AlgoLogSenderServlet servlet;
    @Mock private HttpParser httpParser;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        oldProp = System.getProperty("ENV");
        System.setProperty("ENV", "test");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (oldProp != null) {
            System.setProperty("ENV", oldProp);
        } else {
            System.clearProperty("ENV");
        }
    }

    @Before
    public void setUp() throws Exception {
        servlet = new AlgoLogSenderServlet(httpParser);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void validateFailsIfMethodIsNotGet() throws Exception {
        when(httpParser.getParam("magic")).thenReturn("iamawizard");
        when(httpParser.getMethod()).thenReturn("POST");
        assertFalse(servlet.validate(null));
        when(httpParser.getMethod()).thenReturn("PUT");
        assertFalse(servlet.validate(null));
        when(httpParser.getMethod()).thenReturn("GET");
        assertTrue(servlet.validate(null));
    }

    @Test
    public void validateFailsIfMagicIsNotThere() throws Exception {
        when(httpParser.getMethod()).thenReturn("GET");
        assertFalse(servlet.validate(null));
        when(httpParser.getParam("magic")).thenReturn("iamawizard");
        assertTrue(servlet.validate(null));

    }

    @Test
    public void validateSucceedIfCrossIsNotThere() throws Exception {
        when(httpParser.getMethod()).thenReturn("GET");
        when(httpParser.getParam("magic")).thenReturn("iamawizard");
        assertTrue(servlet.validate(null));
        when(httpParser.getParam("cross")).thenReturn("kings");
        assertTrue(servlet.validate(null));

    }

    @Test
    public void respond() throws Exception {

    }

    @Test
    public void getAlgoFolderName() throws Exception {
        String logFolder = AlgoLogSenderServlet.getAlgoFolderName("imawizard","kings");
        assertEquals("imawizard_kings", logFolder);
        String anotherOne = AlgoLogSenderServlet.getAlgoFolderName("imawizard",null);
        assertEquals("imawizard", anotherOne);
    }

    @Test
    public void getLogFolderName() throws Exception {
        String logFolderName = AlgoLogSenderServlet.getLogFolderName("imawitch", null);
        assertEquals("logfile_imawitch", logFolderName);
        String anotherOne = AlgoLogSenderServlet.getLogFolderName("imawitch", "kings");
        assertEquals("logfile_imawitch_kings", anotherOne);
    }

    @Test
    public void getLogFileName() throws Exception {
        String fileName = AlgoLogSenderServlet.getLogFileName("imaturtle", null);
        assertEquals("logfileimaturtle_1.txt", fileName);
        String anotherOne = AlgoLogSenderServlet.getLogFileName("imaturtle", "splinter");
        assertEquals("logfileimaturtle_splinter_1.txt", anotherOne);
    }

    @Test
    public void findLogFile() throws Exception {
        File logFile = AlgoLogSenderServlet.findLogFile("algo_100204", "audcad");
        assertTrue(logFile.exists());
        assertTrue(logFile.getTotalSpace() > 0);
    }

}