package me.xemor.playershopoverhaul.storage.statements;

import me.xemor.playershopoverhaul.storage.uuidsupport.UUIDArgumentFactory;
import me.xemor.playershopoverhaul.storage.uuidsupport.UUIDColumnMapper;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.UUID;

@RegisterArgumentFactory(UUIDArgumentFactory.class)
@RegisterColumnMapper(UUIDColumnMapper.class)
public interface NameToUUIDStatements {

    @SqlUpdate(
            """
            REPLACE INTO nameToUUID(uuid, username) VALUES(:uuid, :name)
            """
    )
    void setUsername(@Bind("uuid") UUID uuid, @Bind("name") String name);

    @SqlQuery(
            """
            SELECT username FROM nameToUUID WHERE uuid = :uuid
            """
    )
    String getUsername(@Bind("uuid") UUID uuid);

    @SqlQuery(
            """
            SELECT uuid FROM nameToUUID WHERE username = :username
            """
    )
    UUID getUUID(@Bind("username") String username);

}
