$version: "2.0"

namespace net.walend.duckaligner

use smithy.api#required

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

list Positions {
    member: GeoPoint
}

structure Track {
    @required
    id:DuckId

    @required
    positions: Positions
}

map TrackMap {
    key: String //really a DuckId
    value: Track
}

structure Tracks {
    //Incremented every update
    @required
    snapshotVersion: Integer

    @required
    tracks: TrackMap
}

structure PositionUpdate {
    @required
    id:DuckId

    @required
    snapshotVersion: Integer

    @required
    position:GeoPoint
}