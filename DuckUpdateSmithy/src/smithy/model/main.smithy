$version: "2.0"

namespace net.walend.duckaligner.duckupdates.v0

use alloy#simpleRestJson

@title("Duck Aligner Update Service")
@simpleRestJson
service DuckUpdateService {
    version: "0.0.0"  //todo link to the build system
    operations: [
        UpdatePosition,
        MapLibreGlKey
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