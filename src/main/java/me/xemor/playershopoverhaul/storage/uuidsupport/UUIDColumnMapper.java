package me.xemor.playershopoverhaul.storage.uuidsupport;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class UUIDColumnMapper implements ColumnMapper<UUID> {
    @Override
    public UUID map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
        byte[] bytes = rs.getBytes(columnNumber);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long mostSigBits = buffer.getLong();
        long leastSigBits = buffer.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }
}
