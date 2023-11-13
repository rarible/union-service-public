# Rarible Protocol

Rarible Protocol is a decentralized multichain toolset that simplifies the way developers can work with NFTs. Protocol builds an abstraction layer for several blockchains and isolates the developer from their specifics with Multichain SDK.

You can find detailed documentation at [docs.rarible.org](https://docs.rarible.org).

## API Reference

Use these base URLs to access our API on different networks:

| Documentation                                                                | Base URL                             | Environments                          |
|:-----------------------------------------------------------------------------|:-------------------------------------|:--------------------------------------|
| [multichain-api.rarible.org](https://multichain-api.rarible.org)             |                                      | For all environments                  |
| [api.rarible.org/v0.1/doc](https://api.rarible.org/v0.1/doc)                 | https://api.rarible.org/v0.1         | Production (Mainnet)                  |
| [api-staging.rarible.org/v0.1/doc](https://api-staging.rarible.org/v0.1/doc) | https://api-staging.rarible.org/v0.1 | Staging (Rinkeby, Mumbai)             |
| [dev-api.rarible.org/v0.1/doc](https://dev-api.rarible.org/v0.1/doc)         | https://dev-api.rarible.org/v0.1     | Development (Ropsten, Mumbai, Ithaca) |

Also see additional information and usage examples on the [API Reference](https://docs.rarible.org/api-reference/) and [Search Capabilities](https://docs.rarible.org/reference/search-capabilities/) pages.
Rarible Protocol SDK

Protocol SDK is available on GitHub: [https://github.com/rarible/sdk](https://github.com/rarible/sdk)

## Activating a new evm blockchain

1. Activate enabled flag, for example: `integration.eth.arbitrum.enabled: true`
2. Restart union services: indexer, worker, api, listener
3. Create tasks for enabled blockchain:
    - `ITEM_REINDEX` with param `{"versionData":1,"settingsHash":"19e94d6d897e42206c57b2d451f51cbc","blockchain":"ARBITRUM","index":"protocol_union_testnet_item_1"}`
    - `SYNC_OWNERSHIP_TASK` with param `{"blockchain" : "ARBITRUM", "scope" : "EVENT"}`
    - `SYNC_ACTIVITY_TASK` with param `{"blockchain" : "ARBITRUM", "scope" : "EVENT", "type" : "NFT"}`


## Suggestions

You are welcome to [suggest features](https://github.com/rarible/protocol/discussions) and [report bugs found](https://github.com/rarible/protocol/issues)!

## Contributing

The codebase is maintained using the "contributor workflow" where everyone without exception contributes patch proposals using "pull requests" (PRs). This facilitates social contribution, easy testing, and peer review.

See more information on [CONTRIBUTING.md](https://github.com/rarible/protocol/blob/main/CONTRIBUTING.md).

## License

Rarible Protocol is available under [GPL v3](LICENSE.md).

SDK and openapi (with generated clients) are available under [MIT](MIT-LICENSE.md).
