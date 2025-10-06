package me.xemor.playershopoverhaul.storage.statements;

import me.xemor.playershopoverhaul.storage.uuidsupport.UUIDArgumentFactory;
import me.xemor.playershopoverhaul.storage.uuidsupport.UUIDColumnMapper;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.UUID;

@RegisterConstructorMapper(PaymentStatements.PaymentDTO.class)
@RegisterArgumentFactory(UUIDArgumentFactory.class)
@RegisterColumnMapper(UUIDColumnMapper.class)
public interface PaymentStatements {

    @SqlQuery("""
            SELECT id, sellerID, serverID, toPay
            FROM payment
            WHERE sellerID = :sellerId
            AND serverID = :serverId
            FOR UPDATE
            """)
    List<PaymentDTO> getPayments(@Bind("sellerId") UUID sellerId, @Bind("serverId") int serverId);

    @SqlUpdate("""
            DELETE FROM payment
            WHERE id in (<ids>)
            """)
    void deletePayments(@BindList("ids") int... ids);

    @SqlUpdate(
            """
            INSERT INTO payment(sellerID, serverID, toPay) VALUES (:sellerId, :serverId, :toPay)
            """
    )
    void insertPayment(@Bind("sellerId") UUID sellerId, @Bind("serverId") int serverId, @Bind("toPay") double toPay);

    record PaymentDTO(int id, UUID sellerId, int serverId, double toPay) {}
}
