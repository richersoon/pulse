package com.zutubi.pulse.upgrade.tasks;

import com.zutubi.pulse.upgrade.UpgradeException;
import com.zutubi.pulse.util.JDBCUtils;
import org.acegisecurity.providers.encoding.Md5PasswordEncoder;

import java.sql.*;
import java.util.List;

/**
 * <class-comment/>
 */
public class EncodePasswordUpgradeTaskTest extends BaseUpgradeTaskTestCase
{
    public EncodePasswordUpgradeTaskTest()
    {
    }

    public EncodePasswordUpgradeTaskTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    protected List<String> getTestMappings()
    {
        return getMappings("1020");
    }

    public void testUpgrade() throws SQLException, UpgradeException
    {
        Connection con = null;
        try
        {
            con = dataSource.getConnection();

            // insert data into table.
            insertTestData(con, 1L, "password");
            insertTestData(con, 2L, "~!@#$%^&*()_+");
            insertTestData(con, 3L, "`1234567890-=");
            insertTestData(con, 4L, "      ");

            // upgrade
            EncodePasswordUpgradeTask upgradeTask = new EncodePasswordUpgradeTask();
            upgradeTask.setDataSource(dataSource);
            upgradeTask.execute();

            assertEquals(0, upgradeTask.getErrors().size());

            // verify that it is as expected.
            Md5PasswordEncoder encoder = new Md5PasswordEncoder();
            assertTrue(encoder.isPasswordValid(selectPassword(con, 1L), "password", null));
            assertTrue(encoder.isPasswordValid(selectPassword(con, 2L), "~!@#$%^&*()_+", null));
            assertTrue(encoder.isPasswordValid(selectPassword(con, 3L), "`1234567890-=", null));
            assertTrue(encoder.isPasswordValid(selectPassword(con, 4L), "      ", null));
        }
        finally
        {
            JDBCUtils.close(con);
        }
    }

    private String selectPassword(Connection con, Long id) throws SQLException
    {
        CallableStatement stmt = null;
        ResultSet rs = null;
        try
        {
            stmt = con.prepareCall("SELECT password FROM user WHERE id = ?");
            JDBCUtils.setLong(stmt, 1, id);
            rs = stmt.executeQuery();
            if (rs.next())
            {
                return JDBCUtils.getString(rs, "password");
            }
            return null;
        }
        finally
        {
            JDBCUtils.close(rs);
            JDBCUtils.close(stmt);
        }
    }

    private void insertTestData(Connection con, Long id, String password) throws SQLException
    {
        PreparedStatement ps = null;
        try
        {
            ps = con.prepareStatement("insert into USER (id, password) values (?, ?)");
            JDBCUtils.setLong(ps, 1, id);
            JDBCUtils.setString(ps, 2, password);
            ps.executeUpdate();
        }
        finally
        {
            JDBCUtils.close(ps);
        }
    }
}
