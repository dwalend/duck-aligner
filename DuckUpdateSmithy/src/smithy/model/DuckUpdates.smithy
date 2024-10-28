$version: "2.0"

namespace net.walend.duckaligner.duckupdates.v0

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

    //Incremented every update
    @required  //I might back off requiring the snapshot version if not needed for recovering tracks
    snapshot: Integer

    @required
    position:GeoPoint
}