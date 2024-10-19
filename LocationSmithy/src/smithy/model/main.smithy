$version: "2.0"

namespace net.walend.duckaligner

use alloy#simpleRestJson

@title("Duck Aligner Location Service")
@simpleRestJson
service LocationService {
    version: "0.0.0"  //todo link to the build system
    operations: [
        UpdatePosition
    ]
}

@http(method: "POST", uri: "/update")
operation UpdatePosition {
    input := {positinoUpdate:PositionUpdate}
    output := {tracks:Tracks}
}
