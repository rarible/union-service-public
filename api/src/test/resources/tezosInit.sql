create domain zarith as numeric;

create table order_activities
(
    id                  varchar               not null,
    match_left          varchar,
    match_right         varchar,
    hash                varchar,
    transaction         varchar,
    block               varchar,
    level               integer,
    main                boolean default false not null,
    date                timestamp             not null,
    order_activity_type varchar               not null
);

create table orders
(
    maker                    varchar   not null,
    maker_edpk               varchar   not null,
    taker                    varchar,
    taker_edpk               varchar,
    make_asset_type_class    varchar   not null,
    make_asset_type_contract varchar,
    make_asset_type_token_id varchar,
    make_asset_value         zarith    not null,
    make_asset_decimals      integer,
    take_asset_type_class    varchar   not null,
    take_asset_type_contract varchar,
    take_asset_type_token_id varchar,
    take_asset_value         zarith    not null,
    take_asset_decimals      integer,
    start_date               timestamp,
    end_date                 timestamp,
    salt                     varchar   not null,
    signature                varchar   not null,
    created_at               timestamp not null,
    last_update_at           timestamp not null,
    hash                     varchar   not null
        primary key
);

create table if not exists nft_activities
(
    activity_type varchar               not null,
    transaction   varchar               not null,
    index         integer               not null,
    block         varchar               not null,
    level         integer               not null,
    main          boolean default false not null,
    date          timestamp             not null,
    contract      varchar               not null,
    token_id      varchar               not null,
    owner         varchar               not null,
    amount        zarith                not null,
    tr_from       varchar,
    primary key (transaction, block)
);

-- list
insert into order_activities (id, match_left, match_right, hash, "transaction", block, level, main, "date",order_activity_type)
values ('d432fb5d00706c54a8fe7b1132d65a0ef2e3306bfe484d39ee7bd6ba3e4dd4f3d930750b1aecdda8854925917c0c99b1ee957f9d0fcb2e3c2652abd3c1737783dbc1d94d1d51ffb12e56dda15a9cb5f40c4be139c2e46b351c3bee9572b12c2b57bc354f5162a5add470ec0961c86e27dab5aa8b2825dd63bcde062a64fde6ff', null, null, 'c2e72a459d006fb63d4de1ce234748b9ae40be96541c4bd275e355589d952aa3', null, null, null, true, '2022-04-26 15:39:10.000000', 'list');

insert into orders (maker, maker_edpk, taker, taker_edpk, make_asset_type_class, make_asset_type_contract,
                    make_asset_type_token_id, make_asset_value, make_asset_decimals, take_asset_type_class,
                    take_asset_type_contract, take_asset_type_token_id, take_asset_value, take_asset_decimals,
                    start_date, end_date, salt, signature, created_at, last_update_at, hash)
values ('tz1aSkwEot3L2kmUvcoxzjMomb9mvBNuzFK6', 'edpkurPsQ8eUApnLUJ9ZPDvu98E8VNj4KtJa1aZr16Cr5ow5VHKnz4', null, null, 'MT', 'KT1REVBNMiz2QuQjgGM9UsoPDmu7Jcm9gX6y', 3, 111, null, 'XTZ', null, null, 111000000, 6, null, null, '405710724614117503492054822924481144577865105167182072266619118516213521319313023927', 'edsigtc7bBoPMr8Yhqees78qSLpDE3Ch7vuFsy1K6Mf8ffXUazpRhocsAAjfeXBbuUC3fv7uk7idVTXt2Egyank4BsUChWgTux6', '2022-04-26 15:39:10.000000', '2022-04-26 15:39:10.000000', 'c2e72a459d006fb63d4de1ce234748b9ae40be96541c4bd275e355589d952aa3');

-- match

insert into order_activities (id, match_left, match_right, hash, "transaction", block, level, main, "date",order_activity_type)
values ('BKpJX4yv2JsxezPcvgnavyjJZBZVbQ5hicMwQLEkxv9516Qz27N_46', 'f1dad99bd88f47cecb6f6124c80f726f7b42a7ddb9e6ded3e68e40c20f49ff13', '8c3b89e350b767fa625165050abe157e6fcdd511981d238f6cbe8e47e43e603c', 'f1dad99bd88f47cecb6f6124c80f726f7b42a7ddb9e6ded3e68e40c20f49ff13', null, null, null, true, '2022-04-28 16:58:44.000000', 'match');

-- SNDBX-240
INSERT INTO order_activities (id, match_left, match_right, hash, transaction, block, level, main, date, order_activity_type)
VALUES ('BMdUs3hgMfEHLyXtJVRaMem6kq9JAaJmhVfV69zHVpnkygrLiNi_59', '606d6c3322000cfffbed0a41c4c641241cc5a93d68f220bb7acb6f06a3c4810c', '688a049b1695af56a01e3d64d9632e32a13ca5d36dd2ed68b2e28370b38800f7', '606d6c3322000cfffbed0a41c4c641241cc5a93d68f220bb7acb6f06a3c4810c', 'op84xXAS4xXDFdt9RwAUqoe9gd7wMmMZyYoepQndsdPBSoETH8D', 'BMdUs3hgMfEHLyXtJVRaMem6kq9JAaJmhVfV69zHVpnkygrLiNi', 2264477, true, '2022-04-08 21:34:44.000000', 'match');
INSERT INTO orders (maker, maker_edpk, taker, taker_edpk, make_asset_type_class, make_asset_type_contract, make_asset_type_token_id, make_asset_value, make_asset_decimals, take_asset_type_class, take_asset_type_contract, take_asset_type_token_id, take_asset_value, take_asset_decimals, start_date, end_date, salt, signature, created_at, last_update_at, hash)
VALUES ('tz1ZzkVVRaip6kEmUXeWdQf7aTcyzMg9fA3w', 'edpkuveXFUfu161TZbESe4YmfuHiwMbRKMZYTEJcHNM2sHbPMYRvZX', null, null, 'MT', 'KT18c8n9jbpDRZStQRvZUC1GPBEDEcVgwGFC', '25', 1, null, 'XTZ', null, null, 5000000, 6, null, null, '20210317215322916846141579976212017611669124120135979150119163101107117962510214239', 'edsigu5zhj8WLHqZe1HrDwRqs4NKu7dD87kdQBu8JdbvD7ukuWWKfEgavbvAdjAcbXUk89Tnb7b9MwDyqGBV7xaViRmMsTE2FHP', '2022-03-27 16:33:43.000000', '2022-04-08 21:34:44.000000', '606d6c3322000cfffbed0a41c4c641241cc5a93d68f220bb7acb6f06a3c4810c');
INSERT INTO orders (maker, maker_edpk, taker, taker_edpk, make_asset_type_class, make_asset_type_contract, make_asset_type_token_id, make_asset_value, make_asset_decimals, take_asset_type_class, take_asset_type_contract, take_asset_type_token_id, take_asset_value, take_asset_decimals, start_date, end_date, salt, signature, created_at, last_update_at, hash)
VALUES ('tz1iLaTq7cFBgELhTzBZtRSP76AXKPCqbYGe', 'edpkv9NAho1VUAZhZhxYvy7AqJELmm2jXVYV31z6gvUJHU4w4SFB7r', null, null, 'XTZ', null, null, 5000000, 6, 'MT', 'KT18c8n9jbpDRZStQRvZUC1GPBEDEcVgwGFC', '25', 1, null, null, null, '0', 'NO_SIGNATURE', '2022-04-08 21:34:54.000000', '2022-04-08 21:34:44.000000', '688a049b1695af56a01e3d64d9632e32a13ca5d36dd2ed68b2e28370b38800f7');

INSERT INTO orders (maker, maker_edpk, taker, taker_edpk, make_asset_type_class, make_asset_type_contract, make_asset_type_token_id, make_asset_value, make_asset_decimals, take_asset_type_class, take_asset_type_contract, take_asset_type_token_id, take_asset_value, take_asset_decimals, start_date, end_date, salt, signature, created_at, last_update_at, hash) VALUES ('tz1XzWGWhX8mKfqggyHt4NnoYwuNZ7qiLoNG', 'edpktiYxnjdK5PR9NbYbNNLBs3txocoNupKTNPx54d5qaN3yeoFNyC', null, null, 'XTZ', null, null, 1000000, 6, 'MT', 'KT1RuoaCbnZpMgdRpSoLfJUzSkGz1ZSiaYwj', '320', 1, null, null, null, '0', 'NO_SIGNATURE', '2022-06-10 13:28:00.000000', '2022-06-10 13:27:55.000000', '91fadb78e8df52f83695efc16d325696c829f982ce3e4206489fef4f9b0742d6');
INSERT INTO orders (maker, maker_edpk, taker, taker_edpk, make_asset_type_class, make_asset_type_contract, make_asset_type_token_id, make_asset_value, make_asset_decimals, take_asset_type_class, take_asset_type_contract, take_asset_type_token_id, take_asset_value, take_asset_decimals, start_date, end_date, salt, signature, created_at, last_update_at, hash) VALUES ('tz1ePNX2ys72LrEiLoKRTHukjjWbUu2QaWG3', 'edpkui38uEABPWwy4zVXde5fevaBwjkdSEwpmoghb1isTvcAo2J43F', null, null, 'MT', 'KT1RuoaCbnZpMgdRpSoLfJUzSkGz1ZSiaYwj', '320', 11, null, 'XTZ', null, null, 11000000, 6, null, null, '1487621613119170120202081301751835914820576108134221121128328112901180203154021456177', 'edsigtbnQhHJh7LYWyKdqAz3Pa3Lmk6oxcVWEJB3toWULe3R5vnL7E4e2yoLsE6hsSyTatjQ7Xi9ANbXAnPeNmNGQAFNwUKGYFC', '2022-06-10 13:27:13.000000', '2022-06-10 13:28:40.000000', '0e69a1b2d5f705aff75c46570b2ed37449be83136c88d2493381d1fa365e45df');
INSERT INTO order_activities (id, match_left, match_right, hash, transaction, block, level, main, date, order_activity_type) VALUES ('BLZqXaBVGh6WCEZNFTHgspfDbRxdQNGfZtkMXnhmGdipcqm7bxN_2', '0e69a1b2d5f705aff75c46570b2ed37449be83136c88d2493381d1fa365e45df', '91fadb78e8df52f83695efc16d325696c829f982ce3e4206489fef4f9b0742d6', '0e69a1b2d5f705aff75c46570b2ed37449be83136c88d2493381d1fa365e45df', 'onzPDwZF2mjaQKEpKfxfPUZ3fSyMHwCNURQJLKig6P6m4GUhAQh', 'BLZqXaBVGh6WCEZNFTHgspfDbRxdQNGfZtkMXnhmGdipcqm7bxN', 671229, true, '2022-06-10 13:27:55.000000', 'match');


insert into orders (maker, maker_edpk, taker, taker_edpk, make_asset_type_class, make_asset_type_contract,
                    make_asset_type_token_id, make_asset_value, make_asset_decimals, take_asset_type_class,
                    take_asset_type_contract, take_asset_type_token_id, take_asset_value, take_asset_decimals,
                    start_date, end_date, salt, signature, created_at, last_update_at, hash)
values ('tz1UQMvLFm8xnf7CcSjJwWWa2ibbtrnrsAne', 'edpkuxoJEL4tMtiyde96VejnydaGPpUumcCa9b5D6TGkFXMXEHEzHB', null, null, 'MT', 'KT1WGYTJRzMUoJHcZ62jRAMGGqVWQrLSMBza', 3, 2, null, 'XTZ', null, null, 100000, 6, null, null, '1622411590619113022323025777416417319716025010520511110216604154218462334819015419640', 'edsigtehu2GUYVV4bBobDRvqtH8rXTLAsPf7kvE2fYsESy7swoCj5eAGdb2NfRcvSC2HAbNfSkLZ8jrc9qiYgMRbPLR7dTzMHsq', '2022-03-12 14:44:13.000000', '2022-04-28 16:58:44.000000', 'f1dad99bd88f47cecb6f6124c80f726f7b42a7ddb9e6ded3e68e40c20f49ff13');

insert into orders (maker, maker_edpk, taker, taker_edpk, make_asset_type_class, make_asset_type_contract,
                    make_asset_type_token_id, make_asset_value, make_asset_decimals, take_asset_type_class,
                    take_asset_type_contract, take_asset_type_token_id, take_asset_value, take_asset_decimals,
                    start_date, end_date, salt, signature, created_at, last_update_at, hash)
values ('tz1YuZGjEMmfBxGtXSxYgU8VDRH847wbt7nP', 'edpku1PUTiu6CmnxVqQiDp183ks7vgPrQj4SdvxU7hsG53hRos6Nc3', null, null, 'XTZ', null, null, 50000, 6, 'MT', 'KT1WGYTJRzMUoJHcZ62jRAMGGqVWQrLSMBza', 3, 1, null, null, null, 0, 'NO_SIGNATURE', '2022-04-28 16:58:53.000000', '2022-04-28 16:58:44.000000', '8c3b89e350b767fa625165050abe157e6fcdd511981d238f6cbe8e47e43e603c');

-- cancel
insert into order_activities (id, match_left, match_right, hash, "transaction", block, level, main, "date",order_activity_type)
values ('BMHowFJeRxfD5hUKKe2K64tbcNMS33CdgFoKTJXvWt7DumS4WJR_8', null, null, '9c1cae8b763962949e25c687d16026f803d3d6bc9b3b730445d2b411982c355b', null, null, null, true, '2022-04-28 18:49:44.000000', 'cancel_l');

insert into orders (maker, maker_edpk, taker, taker_edpk, make_asset_type_class, make_asset_type_contract,
                    make_asset_type_token_id, make_asset_value, make_asset_decimals, take_asset_type_class,
                    take_asset_type_contract, take_asset_type_token_id, take_asset_value, take_asset_decimals,
                    start_date, end_date, salt, signature, created_at, last_update_at, hash)
values ('tz1ea4AkZ44BDZS9SFpdEw5cTfYGYNnE22Bd', 'edpku9De2Fzpo6WGwxbYYDsNpjYoEvjoqgqyLHsb5HL5PWZ9mevJex', null, null, 'MT', 'KT18pVpRXKPY2c4U2yFEGSH3ZnhB2kL8kwXS', 32980, 100, null, 'XTZ', null, null, 100000000, null, null, null, 10814509376088188194226193851546825417291381178711720175401574925124511017216951228, 'edsigu398YBLMp7BXDa6b6Nz8J9kEAeP9xAgHSU7m7JdXPgMvtFRMbZHf8aUrQcw2erwSqAvNqJX9YiXBjaGLnXdo4Ka4Mpm8Pq', '2022-01-24 16:39:12.000000', '2022-04-28 18:49:44.000000', '9c1cae8b763962949e25c687d16026f803d3d6bc9b3b730445d2b411982c355b');

-- mint
insert into nft_activities (activity_type, "transaction", index, block, level, main, "date", contract, token_id, owner, amount, tr_from)
values ('mint', 'opNoeB9ub52E9yEcUv6nYhTYHJ8R9SsN21S7n9zpS8qcFckc21i', 9, 'BKsJSiEfxzdSsuagvyLQfnZ7AQPbtjnmMkNoXmdnz8dadM45dE2', 2320097, true, '2022-04-28 19:14:59.000000',
        'KT1RJ6PbjHpwc3M5rw5s2Nbmefwbuwbdxton', 726245, 'tz1QFyjHEp3kfeyEuMAAcuc4oYZjmrMjn9vx', 3, null);

-- transfer
insert into nft_activities (activity_type, "transaction", index, block, level, main, "date", contract, token_id, owner, amount, tr_from)
values ('transfer', 'oo22KG27pptFtKEGJ8JcwN44teZoRMTigRaFsKiKPnwvWcg8xB6', 8, 'BKmg87vCWK7uwRF4avDWj7KAmcuLCrVv4gMLCfg3sHdS82rsEfq', 2320168, true, '2022-04-28 19:51:59.000000',
        'KT1MxDwChiDwd6WBVs24g1NjERUoK622ZEFp', 16625, 'KT1BvWGFENd4CXW5F3u4n31xKfJhmBGipoqF', 1, 'tz1LUDkKvKpoz2iXtih9SpiPwiVsBoxvtsTh');

-- burn
insert into nft_activities (activity_type, "transaction", index, block, level, main, "date", contract, token_id, owner, amount, tr_from)
values ('burn', 'ooAYipzS3MU83RKQMXVhmwcC9jgtpPpENQqNGUw9CDMyQbo13wj', 4, 'BLgNWpfjc5xWzvbY7neaE4DPFWGUZvJT77dUAHxb3YubYzMWQCA', 2320179, true, '2022-04-28 19:58:14.000000',
        'KT18pVpRXKPY2c4U2yFEGSH3ZnhB2kL8kwXS', 61026, 'tz1hFesk6GV6fT3vak68zz5JxdZ5kK81rvRB', 1, null);

-- contracts
create table if not exists contracts
(
    kind              varchar                      not null,
    address           varchar                      not null
    primary key,
    owner             varchar,
    block             varchar                      not null,
    level             integer                      not null,
    tsp               timestamp                    not null,
    main              boolean default false        not null,
    last_block        varchar                      not null,
    last_level        integer                      not null,
    last              timestamp                    not null,
    tokens_number     bigint  default 0            not null,
    last_token_id     zarith  default '0'::numeric not null,
    ledger_id         varchar                      not null,
    ledger_key        jsonb,
    ledger_value      jsonb,
    metadata          jsonb   default '{}'::jsonb  not null,
    minters           character varying[],
    uri_pattern       varchar,
    counter           zarith  default 0            not null,
    token_metadata_id varchar,
    metadata_id       varchar,
    royalties_id      varchar,
    crawled           boolean default true         not null
);
create table if not exists tzip16_metadata
(
    contract    varchar               not null
    primary key,
    block       varchar               not null,
    level       integer               not null,
    tsp         timestamp             not null,
    main        boolean default false not null,
    name        varchar,
    description varchar,
    version     varchar,
    license     jsonb,
    authors     character varying[],
    homepage    varchar,
    source      jsonb,
    interfaces  character varying[],
    errors      jsonb,
    views       jsonb
);

INSERT INTO public.contracts (kind, address, owner, block, level, tsp, main, last_block, last_level, last, tokens_number, last_token_id, ledger_id, ledger_key, ledger_value, metadata, minters, uri_pattern, counter, token_metadata_id, metadata_id, royalties_id, crawled) VALUES ('fa2', 'KT18bKocekp5ei8LvBbMjUEEc8KRpLHsJHRj', null, 'BLjCp3knpNPM4aEXUbWRgr6H6G9KZD662ZUuqoYK6EMoPf8ueLm', 2097947, '2022-02-07 17:35:14.000000', true, 'BLjCp3knpNPM4aEXUbWRgr6H6G9KZD662ZUuqoYK6EMoPf8ueLm', 2097947, '2022-02-07 17:35:14.000000', 0, 0, '101464', '"nat"', '"address"', '{"": "ipfs://QmPAdeiHxAo8f8rNq32mZvPcPsSPqbrr5irC77uDehgdVA"}', null, null, 0, '101466', '101467', null, true);
INSERT INTO public.tzip16_metadata (contract, block, level, tsp, main, name, description, version, license, authors, homepage, source, interfaces, errors, views) VALUES ('KT18bKocekp5ei8LvBbMjUEEc8KRpLHsJHRj', 'BLjCp3knpNPM4aEXUbWRgr6H6G9KZD662ZUuqoYK6EMoPf8ueLm', 2097947, '2022-02-07 17:35:14.000000', true, 'STATION 020: SKULL CANDY OFFERINGS', 'Do you feel this skull is trying to trick us? I don’t know.', '1', '[]', null, 'https://collectible.sweet.io/series/1206', '[]', '{TZIP-012,TZIP-016,TZIP-021}', '[]', '[]');

INSERT INTO public.contracts (kind, address, owner, block, level, tsp, main, last_block, last_level, last, tokens_number, last_token_id, ledger_id, ledger_key, ledger_value, metadata, minters, uri_pattern, counter, token_metadata_id, metadata_id, royalties_id, crawled) VALUES ('rarible', 'KT18exmo82JomAcRGTNpUvp1wATSqT63AYmF', 'tz1e6t4FD8zV46wsBndy5qcx1ch87hn6F98H', 'BMbA9sDJXDt98g9rJ7LCSRY6UXNQwnahoUegxYaBtFmQ1kLb3YF', 2194928, '2022-03-14 06:49:34.000000', true, 'BMbA9sDJXDt98g9rJ7LCSRY6UXNQwnahoUegxYaBtFmQ1kLb3YF', 2194928, '2022-03-14 06:49:34.000000', 0, 3, '129488', '["nat", "address"]', '"nat"', '{"name": "Spiral", "symbol": "S22", "contractURI": "https://api-mainnet.rarible.com/contractMetadata/{address}"}', null, null, 3, '129490', '129493', '129487', true);
INSERT INTO public.tzip16_metadata (contract, block, level, tsp, main, name, description, version, license, authors, homepage, source, interfaces, errors, views) VALUES ('KT18exmo82JomAcRGTNpUvp1wATSqT63AYmF', 'BMbA9sDJXDt98g9rJ7LCSRY6UXNQwnahoUegxYaBtFmQ1kLb3YF', 2194928, '2022-03-14 06:49:34.000000', true, 'Spiral', null, null, '[]', null, null, '[]', null, '[]', '[]');

INSERT INTO public.contracts (kind, address, owner, block, level, tsp, main, last_block, last_level, last, tokens_number, last_token_id, ledger_id, ledger_key, ledger_value, metadata, minters, uri_pattern, counter, token_metadata_id, metadata_id, royalties_id, crawled) VALUES ('rarible', 'KT1Gg9cdNu3cFUT2UrUZKBSvTRH8N8AsPk2i', 'tz1RahNvmiJqbymNWJt3Ss9ooNjKRvEKEhpB', 'BM4smaru1v5QWrYSjCX2aFw5UyYk51msRZdU3AwvVHAX8wtDqvp', 2244926, '2022-04-01 20:46:59.000000', true, 'BMVVWg85TKCyexEsu8c4KbWKatqWVaMnjV4yYgbUbGMtzWuZfnx', 2272072, '2022-04-11 14:35:44.000000', 0, 1663, '141400', '"nat"', '"address"', '{"": ""}', '{KT1NF8YjHkJPEfFqfQtcWAjnnrqtMHCbKWqb,KT1NLutkkpwLZ7HWKpWbYPW6FZkzZAkZPXvX}', null, 0, '141402', '141405', '141399', true);
