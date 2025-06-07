CREATE TABLE IF NOT EXISTS markets
(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    item VARBINARY(65500) NOT NULL UNIQUE,
    name TEXT NOT NULL
);
|
CREATE TABLE IF NOT EXISTS listings
(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    sellerID BINARY(16) NOT NULL,
    serverID INT NOT NULL,
    stock INT NOT NULL,
    pricePer DOUBLE NOT NULL,
    marketID INTEGER NOT NULL REFERENCES markets(id),
    CHECK (stock > 0 AND pricePer > 0)
);
|
CREATE TABLE IF NOT EXISTS payment
(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    sellerID BINARY(16) NOT NULL,
    serverID INTEGER NOT NULL,
    toPay DOUBLE NOT NULL,
    CHECK (toPay > 0)
);
|
CREATE TABLE IF NOT EXISTS log
    @SqlQuery
(
    "INSERT INTO log (sellerID, buyerID, amountPurchased, amountPaid, marketID) VALUES (:sellerID, :buyerID, :amountPurchased, :amountPaid, :marketID)")
(
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    sellerID BINARY(16) NOT NULL,
    buyerID BINARY(16) NOT NULL,
    amountPurchased INT NOT NULL,
    amountPaid DOUBLE NOT NULL,
    marketID INTEGER REFERENCES markets(id)
);
|
CREATE TABLE IF NOT EXISTS nameToUUID
(
    uuid BINARY(16) PRIMARY KEY,
    username CHAR(17) UNIQUE
);