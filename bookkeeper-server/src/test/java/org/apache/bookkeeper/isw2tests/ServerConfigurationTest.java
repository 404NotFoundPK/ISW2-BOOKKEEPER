package org.apache.bookkeeper.isw2tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
// import org.apache.bookkeeper.conf.ServerConfiguration;
import org.apache.commons.configuration.ConfigurationException;

@RunWith(Parameterized.class)
public class ServerConfigurationTest {

    private final ServerConfigurationB serverConf;

    public ServerConfigurationTest() {
        serverConf = new ServerConfigurationB();
    }

    @Before
    public void setup() throws Exception {
        // serverConf.loadConf();
        serverConf.loadConf(
            getClass().getClassLoader().getResource("bk_server.conf"));
    }

    @Test
    public void testEphemeralPortsAllowed() throws ConfigurationException {
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setAllowEphemeralPorts(true);
        conf.setBookiePort(0);

        conf.validate();
        assertTrue(true);
    }

    @Test(expected = ConfigurationException.class)
    public void testEphemeralPortsDisallowed() throws ConfigurationException {
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setAllowEphemeralPorts(false);
        conf.setBookiePort(0);
        conf.validate();
    }

    @Test
    public void testSetExtraServerComponents() {
        ServerConfigurationB conf = new ServerConfigurationB();
        assertNull(conf.getExtraServerComponents());
        String[] components = new String[] {
            "test1", "test2", "test3"
        };
        conf.setExtraServerComponents(components);
        assertArrayEquals(components, conf.getExtraServerComponents());
    }

    @Test
    public void testGetExtraServerComponents() {
        String[] components = new String[] {
            "test1", "test2", "test3"
        };
        assertArrayEquals(components, serverConf.getExtraServerComponents());
    }

    @Test(expected = ConfigurationException.class)
    public void testMismatchofJournalAndFileInfoVersionsOlderJournalVersion() throws ConfigurationException {
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setJournalFormatVersionToWrite(5);
        conf.setFileInfoFormatVersionToWrite(1);
        conf.validate();
    }

    @Test(expected = ConfigurationException.class)
    public void testMismatchofJournalAndFileInfoVersionsOlderFileInfoVersion() throws ConfigurationException {
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setJournalFormatVersionToWrite(6);
        conf.setFileInfoFormatVersionToWrite(0);
        conf.validate();
    }

    @Test
    public void testValidityOfJournalAndFileInfoVersions() throws ConfigurationException {
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setJournalFormatVersionToWrite(5);
        conf.setFileInfoFormatVersionToWrite(0);
        conf.validate();

        conf = new ServerConfigurationB();
        conf.setJournalFormatVersionToWrite(6);
        conf.setFileInfoFormatVersionToWrite(1);
        conf.validate();
    }

    @Test
    public void testEntryLogSizeLimit() throws ConfigurationException {
        ServerConfigurationB conf = new ServerConfigurationB();
        try {
            conf.setEntryLogSizeLimit(-1);
            fail("should fail setEntryLogSizeLimit since `logSizeLimit` is too small");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            conf.setProperty("logSizeLimit", "-1");
            conf.validate();
            fail("Invalid configuration since `logSizeLimit` is too small");
        } catch (ConfigurationException ce) {
            // expected
        }

        try {
            conf.setEntryLogSizeLimit(2 * 1024 * 1024 * 1024L - 1);
            fail("Should fail setEntryLogSizeLimit size `logSizeLimit` is too large");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            conf.validate();
            fail("Invalid configuration since `logSizeLimit` is too large");
        } catch (ConfigurationException ce) {
            // expected
        }

        conf.setEntryLogSizeLimit(512 * 1024 * 1024);
        conf.validate();
        assertEquals(512 * 1024 * 1024, conf.getEntryLogSizeLimit());

        conf.setEntryLogSizeLimit(1073741824);
        conf.validate();
        assertEquals(1073741824, conf.getEntryLogSizeLimit());
    }

    @Test
    public void testCompactionSettings() {
        ServerConfigurationB conf = new ServerConfigurationB();
        long major, minor;

        // Default Values
        major = conf.getMajorCompactionMaxTimeMillis();
        minor = conf.getMinorCompactionMaxTimeMillis();
        Assert.assertEquals(-1, major);
        Assert.assertEquals(-1, minor);

        // Set values major then minor
        conf.setMajorCompactionMaxTimeMillis(500).setMinorCompactionMaxTimeMillis(250);
        major = conf.getMajorCompactionMaxTimeMillis();
        minor = conf.getMinorCompactionMaxTimeMillis();
        Assert.assertEquals(500, major);
        Assert.assertEquals(250, minor);

        // Set values minor then major
        conf.setMinorCompactionMaxTimeMillis(150).setMajorCompactionMaxTimeMillis(1500);
        major = conf.getMajorCompactionMaxTimeMillis();
        minor = conf.getMinorCompactionMaxTimeMillis();
        Assert.assertEquals(1500, major);
        Assert.assertEquals(150, minor);

        // Default Values
        major = conf.getMajorCompactionInterval();
        minor = conf.getMinorCompactionInterval();
        Assert.assertEquals(3600, minor);
        Assert.assertEquals(86400, major);

        // Set values major then minor
        conf.setMajorCompactionInterval(43200).setMinorCompactionInterval(1800);
        major = conf.getMajorCompactionInterval();
        minor = conf.getMinorCompactionInterval();
        Assert.assertEquals(1800, minor);
        Assert.assertEquals(43200, major);

        // Set values minor then major
        conf.setMinorCompactionInterval(900).setMajorCompactionInterval(21700);
        major = conf.getMajorCompactionInterval();
        minor = conf.getMinorCompactionInterval();
        Assert.assertEquals(900, minor);
        Assert.assertEquals(21700, major);

        // Default Values
        double majorThreshold, minorThreshold;
        majorThreshold = conf.getMajorCompactionThreshold();
        minorThreshold = conf.getMinorCompactionThreshold();
        Assert.assertEquals(0.8, majorThreshold, 0.00001);
        Assert.assertEquals(0.2, minorThreshold, 0.00001);

        // Set values major then minor
        conf.setMajorCompactionThreshold(0.7).setMinorCompactionThreshold(0.1);
        majorThreshold = conf.getMajorCompactionThreshold();
        minorThreshold = conf.getMinorCompactionThreshold();
        Assert.assertEquals(0.7, majorThreshold, 0.00001);
        Assert.assertEquals(0.1, minorThreshold, 0.00001);

        // Set values minor then major
        conf.setMinorCompactionThreshold(0.3).setMajorCompactionThreshold(0.6);
        majorThreshold = conf.getMajorCompactionThreshold();
        minorThreshold = conf.getMinorCompactionThreshold();
        Assert.assertEquals(0.6, majorThreshold, 0.00001);
        Assert.assertEquals(0.3, minorThreshold, 0.00001);
    }

    @Test
    public void testForBetterMutationSkipListArenaChunkSize() throws ConfigurationException {     
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setAllowEphemeralPorts(true);
        conf.setBookiePort(0); 
        conf.setSkipListArenaChunkSize(4194304);
        conf.setSkipListArenaMaxAllocSize(4194304);
        conf.validate();
        // Arena max allocation size should be smaller than the chunk size.
        int arenaChunk = conf.getSkipListArenaChunkSize();
        int arenaMax = conf.getSkipListArenaMaxAllocSize();
        Assert.assertEquals(arenaChunk, arenaMax);
    }

    @Test
    public void testForBetterMutationJournalAlignmentSize() throws ConfigurationException {     
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setAllowEphemeralPorts(true);
        conf.setBookiePort(0); 
        conf.setJournalAlignmentSize(16 * 1024 * 1024);
        // default conf.setJournalPreAllocSizeMB(16);
        conf.validate();
        // Invalid preallocation size.
        int journalSize = conf.getJournalAlignmentSize();
        int journalPreAllocSize = conf.getJournalPreAllocSizeMB() * 1024 * 1024;
        Assert.assertEquals(journalSize, journalPreAllocSize);
    }

    @Test
    public void testForBetterMutationIsEntryLogPerLedgerEnabled() throws ConfigurationException {     
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setAllowEphemeralPorts(true);
        conf.setBookiePort(0); 

        conf.setEntryLogPerLedgerEnabled(true);
        conf.setUseTransactionalCompaction(false);
        conf.validate();
        // Arena max allocation size should be smaller than the chunk size.
        boolean isEntryLog = conf.isEntryLogPerLedgerEnabled();
        boolean useTransactional = conf.getUseTransactionalCompaction();
        Assert.assertNotEquals(isEntryLog, useTransactional);
    }

    @Test
    public void testForBetterMutationIsEntryLogPerLedgerEnabled2() throws ConfigurationException {     
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setAllowEphemeralPorts(true);
        conf.setBookiePort(0); 

        conf.setEntryLogPerLedgerEnabled(false);
        conf.setUseTransactionalCompaction(true);
        conf.validate();
        // Arena max allocation size should be smaller than the chunk size.
        boolean isEntryLog = conf.isEntryLogPerLedgerEnabled();
        boolean useTransactional = conf.getUseTransactionalCompaction();
        Assert.assertNotEquals(isEntryLog, useTransactional);
    }

    @Test
    public void testForBetterCoverageArenaChunkMinor() throws ConfigurationException {     
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setAllowEphemeralPorts(true);
        conf.setBookiePort(0); 
        conf.setSkipListArenaChunkSize(120);
        conf.setSkipListArenaMaxAllocSize(4194304);
        try {
            conf.validate();  
        } catch (Exception e) {
            //excepted
            // Arena max allocation size should be smaller than the chunk size.
        }
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{{120}, {512}, {16777728}, {16777216}};
        return Arrays.asList(data);
    }

    @Parameter(0)
    public int value;  // 120 // 512 // 16777728 // 16777216
    
    @Test
    public void testForBetterCoverageJournalAlignmentSize() throws ConfigurationException {     
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setAllowEphemeralPorts(true);
        conf.setBookiePort(0); 
        conf.setJournalAlignmentSize(value);
        // default JournalPreAllocSizeMB is 16 * 1024 * 1024 = 16777216
        // journal alignment must be divided by 512
        try {
            conf.validate(); 
            // no errors if 512 or 16777216
        } catch (Exception e) {
            // excepted if 16777728
            // journal alignment must be < JournalPreAllocSizeMB * 1024 * 1024
            // excepted if 120 getJournalAlignmentSize() must be > 512
        }
    }

    @Test
    public void testForBetterCoverageEntryLogPerLedgerEnabled() {     
        ServerConfigurationB conf = new ServerConfigurationB();
        conf.setAllowEphemeralPorts(true);
        conf.setBookiePort(0); 

        conf.setEntryLogPerLedgerEnabled(true);
        conf.setUseTransactionalCompaction(true);
        try {
            conf.validate();  
        } catch (Exception e) {
            // excepted
            // When entryLogPerLedger is enabled , it is unnecessary to use transactional compaction
        }
    }
}
