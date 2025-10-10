$version: "2.0"

namespace net.walend.duckaligner.duckupdates.v0

use smithy.api#required
use smithy4s.meta#adt

structure GeoPoint {
    @required
    latitude: Double

    @required
    longitude: Double

    @required
    timestamp: Long
}

structure DuckId {
    @required
    v: Long
}

structure DuckInfo {
    @required
    id:DuckId  //todo not needed

    @required
    duckName:String

    @required
    lastChanged:Long
}

/////
@mixin
structure DuckEventBits {
    @required
    protocol: Integer = 0
    @required
    order: Integer
    @required
    id:DuckId
}

structure DuckPositionEvent with [
    DuckEventBits
] {
    @required
    position:GeoPoint
}

structure DuckInfoEvent with [
    DuckEventBits
] {
    @required
    duckInfo:DuckInfo
}

@adt
union DuckEvent {
    position:DuckPositionEvent
    info:DuckInfoEvent
}

list DuckEvents {
    member: DuckEvent
}

union NewDuckEventsResponse {
    eventsForClient: DuckEvents
    rescueServer: Unit
}