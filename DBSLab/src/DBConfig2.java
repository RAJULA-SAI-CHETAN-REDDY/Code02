import java.sql.*;

public class DBConfig2 {

    // ── Change these three to match your Oracle setup ──
    private static final String URL  = "jdbc:oracle:thin:@localhost:1521/XE";
    private static final String USER = "SYSTEM";
    private static final String PASS = "240905164";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("oracle.jdbc.OracleDriver");
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("oracle.jdbc.driver.OracleDriver");
                } catch (ClassNotFoundException e2) {
                    throw new SQLException("Oracle JDBC driver not found. Run app with lib/ojdbc11.jar on classpath.", e2);
                }
            }
            connection = DriverManager.getConnection(URL, USER, PASS);
        }
        return connection;
    }
}
