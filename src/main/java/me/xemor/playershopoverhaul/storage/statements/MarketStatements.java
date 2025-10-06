package me.xemor.playershopoverhaul.storage.statements;

import me.xemor.playershopoverhaul.storage.uuidsupport.UUIDArgumentFactory;
import me.xemor.playershopoverhaul.storage.uuidsupport.UUIDColumnMapper;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

@RegisterConstructorMapper(MarketDTO.class)
@RegisterConstructorMapper(MarketStatements.ListingIdToMarketDTO.class)
@RegisterArgumentFactory(UUIDArgumentFactory.class)
@RegisterColumnMapper(UUIDColumnMapper.class)
public interface MarketStatements {

    @SqlQuery(
            """
            SELECT markets.id, markets.item, markets.name
            FROM markets
            WHERE markets.id = :id
            FOR UPDATE
            """
    )
    MarketDTO getMarketById(@Bind("id") long id);

    @SqlQuery(
            """
            SELECT markets.id, markets.item, markets.name
            FROM markets
            WHERE markets.item = :item
            FOR UPDATE
            """
    )
    MarketDTO getMarketByItem(@Bind("item") byte[] item);

    @SqlUpdate(
            """
            INSERT IGNORE INTO markets(item, name)
            VALUES (:item, :name)
            """
    )
    void insertOrReplaceMarket(@Bind("item") byte[] item, @Bind("name") String name);

    @SqlQuery("""
            SELECT listings.id AS listingID, markets.id AS marketID, markets.item
            FROM markets
            JOIN listings ON markets.id = listings.marketID
            WHERE listings.id IN (<listingIDs>)
            FOR UPDATE
            """)
    List<ListingIdToMarketDTO> getMarketsForListings(@BindList("listingIDs") List<Integer> listingIDs);

    record ListingIdToMarketDTO(int listingID, int marketID, byte[] item) {}

}
