package com.rarible.protocol.union.api.controller.test

import com.rarible.protocol.flow.nft.api.client.*
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import kotlinx.coroutines.FlowPreview
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@FlowPreview
abstract class AbstractIntegrationTest {

    //--------------------- ETHEREUM ---------------------//
    @Autowired
    @Qualifier("ethereum.item.api")
    lateinit var testEthereumItemApi: NftItemControllerApi

    @Autowired
    @Qualifier("ethereum.ownership.api")
    lateinit var testEthereumOwnershipApi: NftOwnershipControllerApi

    @Autowired
    @Qualifier("ethereum.collection.api")
    lateinit var testEthereumCollectionApi: NftCollectionControllerApi

    @Autowired
    @Qualifier("ethereum.order.api")
    lateinit var testEthereumOrderApi: com.rarible.protocol.order.api.client.OrderControllerApi

    /*    
    @Autowired
    @Qualifier("ethereum.activity.api")
    lateinit var testEthereumOrderActivityApi: NftOrderActivityControllerApi
    */

    //--------------------- POLYGON ---------------------//    
    @Autowired
    @Qualifier("polygon.item.api")
    lateinit var testPolygonItemApi: NftItemControllerApi

    @Autowired
    @Qualifier("polygon.ownership.api")
    lateinit var testPolygonOwnershipApi: NftOwnershipControllerApi

    @Autowired
    @Qualifier("polygon.collection.api")
    lateinit var testPolygonCollectionApi: NftCollectionControllerApi

    @Autowired
    @Qualifier("polygon.order.api")
    lateinit var testPolygonOrderApi: com.rarible.protocol.order.api.client.OrderControllerApi

    /*    
    @Autowired
    @Qualifier("polygon.activity.api")
    lateinit var testPolygonOrderActivityApi: NftOrderActivityControllerApi
    */

    //--------------------- FLOW ---------------------//    
    @Autowired
    lateinit var testFlowItemApi: FlowNftItemControllerApi

    @Autowired
    lateinit var testFlowOwnershipApi: FlowNftOwnershipControllerApi

    @Autowired
    lateinit var testFlowCollectionApi: FlowNftCollectionControllerApi

    @Autowired
    lateinit var testFlowOrderApi: FlowOrderControllerApi

    @Autowired
    lateinit var testFlowActivityApi: FlowNftOrderActivityControllerApi


}