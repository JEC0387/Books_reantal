package DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtill {

    private static final String URL =
            "jdbc:mariadb://localhost:3306/library_db" +
                    "?useUnicode=true&characterEncoding=utf8" +
                    "&serverTimezone=Asia/Seoul";

    private static final String USER = "root";
    private static final String PASSWORD = "1111";

    static {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MariaDB JDBC Driver 로드 실패", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
