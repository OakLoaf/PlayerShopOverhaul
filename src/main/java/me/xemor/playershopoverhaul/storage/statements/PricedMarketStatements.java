package me.xemor.playershopoverhaul.storage.statements;

import me.xemor.playershopoverhaul.PricedMarket;
import me.xemor.playershopoverhaul.itemserialization.ItemSerialization;
import me.xemor.playershopoverhaul.storage.uuidsupport.UUIDArgumentFactory;
import me.xemor.playershopoverhaul.storage.uuidsupport.UUIDColumnMapper;
import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.v3.sqlobject.config.RegisterColumnMapper;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;
import java.util.UUID;

@RegisterArgumentFactory(UUIDArgumentFactory.class)
@RegisterColumnMapper(UUIDColumnMapper.class)
@RegisterConstructorMapper(PricedMarketStatements.PricedMarketDTO.class)
public interface PricedMarketStatements {

    @SqlQuery("""
            SELECT markets.id, markets.item AS item, prices.price AS price, prices.stock AS stock, users.sellerID
            FROM markets
            JOIN (SELECT min(pricePer) AS price, sum(stock) AS stock, marketID FROM listings GROUP BY marketID) AS prices
            ON prices.marketID = markets.id
            JOIN (SELECT sellerID, pricePer, marketID FROM listings) AS users
            ON users.pricePer = prices.price AND users.marketID = prices.marketID
            WHERE markets.id = :marketID
            FOR UPDATE
            """)
    PricedMarketDTO getPricedMarketById(@Bind("marketID") int marketID);

    @SqlQuery("""
            SELECT
                markets.id,
                markets.item,
                prices.stock AS stock,
                prices.price AS price,
                prices.sellerID AS sellerID
            FROM markets
            JOIN (
                SELECT
                    marketID,
                    pricePer AS price,
                    sellerID,
                    stock
                FROM (
                    SELECT
                        listings.marketID,
                        listings.pricePer,
                        listings.sellerID,
                        SUM(listings.stock) OVER (PARTITION BY listings.marketID) AS stock,
                        ROW_NUMBER() OVER (PARTITION BY listings.marketID ORDER BY listings.pricePer ASC) AS rn
                    FROM listings
                ) ranked
                WHERE ranked.rn = 1
            ) AS prices
            ON prices.marketID = markets.id
            WHERE markets.name LIKE :search
            ORDER BY prices.stock DESC
            LIMIT 21 OFFSET :offset
            FOR UPDATE
            """)
    List<PricedMarketDTO> getPricedMarkets(@Bind("offset") int offset, @Bind("search") String search);


    record PricedMarketDTO(int id, byte[] item, double price, int stock, UUID sellerID) {
        public PricedMarket asPricedMarket() {
            return new PricedMarket(id, ItemSerialization.binaryToItemStack(item), price, sellerID, stock);
        }
    }

}
