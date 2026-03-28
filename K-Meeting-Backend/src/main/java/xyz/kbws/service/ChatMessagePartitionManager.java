package xyz.kbws.service;

import org.springframework.stereotype.Component;
import xyz.kbws.utils.ChatMessageTableUtil;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理 chatMessage 历史分表元数据和建表逻辑。
 */
@Component
public class ChatMessagePartitionManager {

    @Resource
    private DataSource dataSource;

    public List<String> listPartitionTables() {
        List<String> tableNames = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    if (ChatMessageTableUtil.isValidTableName(tableName)
                            && !ChatMessageTableUtil.BASE_TABLE_NAME.equalsIgnoreCase(tableName)) {
                        tableNames.add(tableName);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询 chatMessage 分表失败", ex);
        }
        tableNames.sort(String::compareToIgnoreCase);
        return tableNames;
    }

    public void ensurePartitionTable(String tableName) {
        ChatMessageTableUtil.validateTableName(tableName);
        if (exists(tableName)) {
            return;
        }
        String suffix = tableName.substring(ChatMessageTableUtil.BASE_TABLE_NAME.length() + 1);
        String idxMeetingTime = "idx_chatMessage_" + suffix + "_meetingId_sendTime";
        String idxSendTime = "idx_chatMessage_" + suffix + "_sendTime";
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(buildCreateTableSql(tableName));
            statement.execute("CREATE INDEX IF NOT EXISTS " + idxMeetingTime + " ON " + tableName + " (meetingId, sendTime)");
            statement.execute("CREATE INDEX IF NOT EXISTS " + idxSendTime + " ON " + tableName + " (sendTime)");
        } catch (SQLException ex) {
            throw new IllegalStateException("创建 chatMessage 分表失败: " + tableName, ex);
        }
    }

    private boolean exists(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
                while (resultSet.next()) {
                    String currentTableName = resultSet.getString("TABLE_NAME");
                    if (tableName.equalsIgnoreCase(currentTableName)) {
                        return true;
                    }
                }
                return false;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("检查 chatMessage 分表失败", ex);
        }
    }

    private String buildCreateTableSql(String tableName) {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "id BIGINT PRIMARY KEY,"
                + "meetingId INT NOT NULL,"
                + "type TINYINT NOT NULL,"
                + "content VARCHAR(500),"
                + "sendUserId INT NOT NULL,"
                + "sendUserNickName VARCHAR(20),"
                + "sendTime BIGINT NOT NULL,"
                + "receiveType TINYINT NOT NULL,"
                + "receiveUserId INT,"
                + "fileSize BIGINT,"
                + "fileName VARCHAR(200),"
                + "fileType TINYINT,"
                + "fileSuffix VARCHAR(10),"
                + "status TINYINT"
                + ")";
    }
}
