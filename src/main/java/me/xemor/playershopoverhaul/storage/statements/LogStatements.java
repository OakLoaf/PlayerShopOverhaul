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
public interface LogStatements {

    @SqlUpdate("INSERT INTO log (sellerID, buyerID, amountPurchased, amountPaid, marketID) VALUES (:sellerID, :buyerID, :amountPurchased, :amountPaid, :marketID)")
    void logPayment(
            @Bind("sellerID") UUID sellerID,
            @Bind("buyerID") UUID buyerID,
            @Bind("amountPurchased") int amountPurchased,
            @Bind("amountPaid") double amountPaid,
            @Bind("marketID") int marketID
    );

}
