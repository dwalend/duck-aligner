$version: "2.0"

namespace net.walend.duckaligner.duckupdates.v0

use alloy#simpleRestJson

@title("Duck Aligner Update Service")
@simpleRestJson
service DuckUpdateService {
    version: "0.0.0"  //todo link to the build system
    operations: [
        UpdatePosition,
        MapLibreGlKey,
        GetDuckId
        //todo add a "put all my state to rescue the server" - and have the client detect the problem and push the fix

    ]
}

@http(method: "POST", uri: "/update", code: 200)
operation UpdatePosition {
    input := {
        @required
        positionUpdate:DuckUpdate
    }
    output := {
        @required
        sitRep:DuckSitRepUpdate
    }
}

@http(method: "GET", uri: "/mapKey", code: 200)
operation MapLibreGlKey {
    output := {
        @required
        key:String
    }
}

//todo this should probably use a POST and something more sophisticated - from an identity server
@http(method: "GET", uri: "/duckId/{duckIdFinder}", code: 200)
operation GetDuckId {
    input := {
        @required
        @httpLabel
        duckIdFinder: String
    }

    output := {
        @required
        duckId:DuckId
    }
}