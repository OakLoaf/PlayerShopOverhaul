package me.xemor.playershopoverhaul.storage.statements;

import me.xemor.playershopoverhaul.Listing;
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

@RegisterConstructorMapper(Listing.class)
@RegisterArgumentFactory(UUIDArgumentFactory.class)
@RegisterColumnMapper(UUIDColumnMapper.class)
public interface ListingStatements {

    @SqlQuery("SELECT * FROM listings WHERE id = :id FOR UPDATE")
    Listing getListing(@Bind("id") int id);

    @SqlUpdate("DELETE FROM listings WHERE id = :id")
    void deleteListing(@Bind("id") int id);

    @SqlUpdate("DELETE FROM listings WHERE id IN (<ids>)")
    void deleteListings(@BindList("ids") int... ids);

    @SqlUpdate("""
    INSERT INTO listings(sellerID, serverID, stock, pricePer, marketID)
    SELECT :sellerID, :serverID, :stock, :pricePer, id
    FROM markets
    WHERE item = :item;
    """)
    void insertListing(@Bind("sellerID") UUID sellerID,
                       @Bind("serverID") int serverID,
                       @Bind("stock") int stock,
                       @Bind("pricePer") double pricePer,
                       @Bind("item") byte[] item);

    @SqlQuery("""
            SELECT * FROM listings WHERE sellerID = :sellerID AND serverID = :serverID LIMIT 21 OFFSET :offset FOR UPDATE
            """)
    List<Listing> getPlayerListings(@Bind("sellerID") UUID sellerID, @Bind("serverID") int serverID, @Bind("offset") int offset);

    @SqlQuery("""
        WITH selected AS (
            SELECT id, sellerID, serverID, stock, pricePer, marketID,
                   SUM(stock) OVER (ORDER BY pricePer, id) AS cumulative_stock
            FROM listings
            WHERE marketID = :marketId
        )
        SELECT id, serverID, sellerID, marketID, stock, pricePer
        FROM selected
        WHERE cumulative_stock - stock < :stockToPurchase  -- only fully or partially used rows
        ORDER BY pricePer
        FOR UPDATE;
    """)
    List<Listing> listingsToPurchaseForStockAmount(@Bind("marketId") int marketId, @Bind("stockToPurchase") int stockToPurchase);

}
