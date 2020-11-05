package com.taosdata.jdbc;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Enumeration;
import java.util.Properties;

import static org.junit.Assert.*;

public class TSDBDriverTest {

    private static final String[] validURLs = {
            "jdbc:TAOS://localhost:0",
            "jdbc:TAOS://localhost",
            "jdbc:TAOS://localhost:6030/test",
            "jdbc:TAOS://localhost:6030",
            "jdbc:TAOS://localhost:6030/",
            "jdbc:TSDB://localhost:6030",
            "jdbc:TSDB://localhost:6030/",
            "jdbc:TAOS://127.0.0.1:0/db?user=root&password=taosdata",
            "jdbc:TAOS://:",
            "jdbc:TAOS://:/",
            "jdbc:TAOS://:/test",
            "jdbc:TAOS://localhost:0/?user=root&password=taosdata"
    };
    private static boolean islibLoaded = false;
    private static boolean isTaosdActived;

    private Connection conn;

    @BeforeClass
    public static void before() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.equals("linux"))
            return;
        // try to load taos lib
        try {
            System.loadLibrary("taos");
            islibLoaded = true;
        } catch (UnsatisfiedLinkError error) {
            System.out.println("load tdengine lib failed.");
            error.printStackTrace();
        }
        // check taosd is activated
        try {
            String[] cmd = {"/bin/bash", "-c", "ps -ef | grep taosd | grep -v \"grep\""};
            Process exec = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(exec.getInputStream()));
            int lineCnt = 0;
            while (reader.readLine() != null) {
                lineCnt++;
            }
            if (lineCnt > 0)
                isTaosdActived = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Class.forName("com.taosdata.jdbc.TSDBDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConnectWithJdbcURL() {
        final String url = "jdbc:TAOS://localhost:6030/log?user=root&password=taosdata";
        try {
            if (islibLoaded && isTaosdActived) {
                conn = DriverManager.getConnection(url);
                assertNotNull("failure - connection should not be null", conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail("failure - should not throw Exception");
        }
    }

    @Test
    public void testConnectWithProperties() {
        final String jdbcUrl = "jdbc:TAOS://localhost:6030/log?user=root&password=taosdata";
        Properties connProps = new Properties();
        connProps.setProperty(TSDBDriver.PROPERTY_KEY_CHARSET, "UTF-8");
        connProps.setProperty(TSDBDriver.PROPERTY_KEY_LOCALE, "en_US.UTF-8");
        connProps.setProperty(TSDBDriver.PROPERTY_KEY_TIME_ZONE, "UTC-8");
        try {
            if (islibLoaded && isTaosdActived) {
                conn = DriverManager.getConnection(jdbcUrl, connProps);
                assertNotNull("failure - connection should not be null", conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail("failure - should not throw Exception");
        }
    }

    @Test
    public void testConnectWithConfigFile() {
        String jdbcUrl = "jdbc:TAOS://:/log?user=root&password=taosdata";
        Properties connProps = new Properties();
        connProps.setProperty(TSDBDriver.PROPERTY_KEY_CHARSET, "UTF-8");
        connProps.setProperty(TSDBDriver.PROPERTY_KEY_LOCALE, "en_US.UTF-8");
        connProps.setProperty(TSDBDriver.PROPERTY_KEY_TIME_ZONE, "UTC-8");
        try {
            if (islibLoaded && isTaosdActived) {
                conn = DriverManager.getConnection(jdbcUrl, connProps);
                assertNotNull("failure - connection should not be null", conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail("failure - should not throw Exception");
        }
    }

    @Test(expected = SQLException.class)
    public void testAcceptsURL() throws SQLException {
        Driver driver = new TSDBDriver();
        for (String url : validURLs) {
            assertTrue("failure - acceptsURL(\" " + url + " \") should be true", driver.acceptsURL(url));
        }
        driver.acceptsURL(null);
        fail("acceptsURL throws exception when parameter is null");
    }

    @Test
    public void testParseURL() {
        TSDBDriver driver = new TSDBDriver();

        String url = "jdbc:TAOS://127.0.0.1:0/db?user=root&password=taosdata&charset=UTF-8";
        Properties config = new Properties();
        Properties actual = driver.parseURL(url, config);
        assertEquals("failure - host should be 127.0.0.1", "127.0.0.1", actual.get("host"));
        assertEquals("failure - port should be 0", "0", actual.get("port"));
        assertEquals("failure - dbname should be db", "db", actual.get("dbname"));
        assertEquals("failure - user should be root", "root", actual.get("user"));
        assertEquals("failure - password should be taosdata", "taosdata", actual.get("password"));
        assertEquals("failure - charset should be UTF-8", "UTF-8", actual.get("charset"));

        url = "jdbc:TAOS://127.0.0.1:0";
        config = new Properties();
        actual = driver.parseURL(url, config);
        assertEquals("failure - host should be 127.0.0.1", "127.0.0.1", actual.getProperty("host"));
        assertEquals("failure - port should be 0", "0", actual.get("port"));
        assertNull("failure - dbname should be null", actual.get("dbname"));

        url = "jdbc:TAOS://127.0.0.1:0/db";
        config = new Properties();
        actual = driver.parseURL(url, config);
        assertEquals("failure - host should be 127.0.0.1", "127.0.0.1", actual.getProperty("host"));
        assertEquals("failure - port should be 0", "0", actual.get("port"));
        assertEquals("failure - dbname should be db", "db", actual.get("dbname"));

        url = "jdbc:TAOS://:/?";
        config = new Properties();
        config.setProperty(TSDBDriver.PROPERTY_KEY_USER, "root");
        config.setProperty(TSDBDriver.PROPERTY_KEY_PASSWORD, "taosdata");
        actual = driver.parseURL(url, config);
        assertEquals("failure - user should be root", "root", actual.getProperty("user"));
        assertEquals("failure - password should be taosdata", "taosdata", actual.getProperty("password"));
        assertNull("failure - host should be null", actual.getProperty("host"));
        assertNull("failure - port should be null", actual.getProperty("port"));
        assertNull("failure - dbname should be null", actual.getProperty("dbname"));
    }

    @Test
    public void testGetPropertyInfo() throws SQLException {
        Driver driver = new TSDBDriver();
        final String url = "jdbc:TAOS://localhost:6030/log?user=root&password=taosdata";
        Properties connProps = new Properties();
        DriverPropertyInfo[] propertyInfo = driver.getPropertyInfo(url, connProps);
        for (DriverPropertyInfo info : propertyInfo) {
            if (info.name.equals(TSDBDriver.PROPERTY_KEY_HOST))
                assertEquals("failure - host should be localhost", "localhost", info.value);
            if (info.name.equals(TSDBDriver.PROPERTY_KEY_PORT))
                assertEquals("failure - port should be 6030", "6030", info.value);
            if (info.name.equals(TSDBDriver.PROPERTY_KEY_DBNAME))
                assertEquals("failure - dbname should be test", "log", info.value);
            if (info.name.equals(TSDBDriver.PROPERTY_KEY_USER))
                assertEquals("failure - user should be root", "root", info.value);
            if (info.name.equals(TSDBDriver.PROPERTY_KEY_PASSWORD))
                assertEquals("failure - password should be root", "taosdata", info.value);
        }
    }

    @Test
    public void testGetMajorVersion() {
        assertEquals("failure - getMajorVersion should be 2", 2, new TSDBDriver().getMajorVersion());
    }

    @Test
    public void testGetMinorVersion() {
        assertEquals("failure - getMinorVersion should be 0", 0, new TSDBDriver().getMinorVersion());
    }

    @Test
    public void testJdbcCompliant() {
        assertFalse("failure - jdbcCompliant should be false", new TSDBDriver().jdbcCompliant());
    }

    @Test
    public void testGetParentLogger() throws SQLFeatureNotSupportedException {
        assertNull("failure - getParentLogger should be be null", new TSDBDriver().getParentLogger());
    }

    public void printRs() {
        Statement statement = null;
        try {
            statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("show databases");
            ResultSetMetaData metaData = resultSet.getMetaData();
            while (resultSet.next()) {
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    System.out.print(metaData.getColumnLabel(i) + " : " + resultSet.getString(i) + "\t");
                }
                System.out.println();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null)
                    statement.close();
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}