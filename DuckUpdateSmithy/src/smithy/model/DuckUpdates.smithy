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

//todo not used yet 
structure DuckIdFinder {
    @required
    v: String
}

list Positions {
    member: GeoPoint
}
structure DuckInfo {
    @required
    id:DuckId

    @required
    duckName:String

    @required
    lastChanged:Long
}

structure Track {
    @required
    duckInfo:DuckInfo

    @required
    positions: Positions
}

list DuckTracks {
    member: Track
}

//All the data needed to show all the duck tracks
structure DuckSitRepUpdate {
    @required
    protocol: Integer = 0

    //Incremented every update
    @required
    snapshot: Integer

    @required
    tracks: DuckTracks
}

//all the info needed to update the track of a single duck
structure DuckUpdate {
    @required
    protocol: Integer = 0

    @required
    id:DuckId

    @required
    duckName:String

    @required
    position:GeoPoint
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

list DuckEventWishList {
    member: Integer
}