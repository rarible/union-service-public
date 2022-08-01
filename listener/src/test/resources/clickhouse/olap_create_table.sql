CREATE TABLE IF NOT EXISTS marketplace_all_stats
(
    date                        DateTime('UTC'),
    collectionId                String,
    blockchain                  LowCardinality(String),
    nativeCurrency              LowCardinality(String),
    totalItemSupply             Int256,
    ownersCount                 Int256,
    highestSaleUsd              Float64,
    highestSaleNative           Float64,
    totalGmvUsd                 Float64,
    totalGmvNative              Float64,
    gmvUsd_1d                   Float64,
    gmvUsd_1d_changePercentage  Nullable(Float64),
    gmvNative_1d                Float64,
    gmvUsd_7d                   Float64,
    gmvUsd_7d_changePercentage  Nullable(Float64),
    gmvNative_7d                Float64,
    gmvUsd_30d                  Float64,
    gmvUsd_30d_changePercentage Nullable(Float64),
    gmvNative_30d               Float64,
    floorPriceUsd               Nullable(Float64),
    floorPriceNative            Nullable(Float64)
) ENGINE MergeTree()
PARTITION BY tuple()
ORDER BY collectionId
SETTINGS index_granularity = 128;
