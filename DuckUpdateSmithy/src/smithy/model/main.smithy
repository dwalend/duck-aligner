$version: "2.0"

namespace net.walend.duckaligner.duckupdates.v0

use alloy#simpleRestJson

@title("Duck Aligner Update Service")
@simpleRestJson
service DuckUpdateService {
    version: "0.0.0"  //todo link to the build system
    operations: [
        ProposeEvents,
        MapLibreGlKey,
        //changed to an event and CQRS model
        //todo add method put ProposeEvents - reply is server's events to add to the client
        //todo that reply can include a list of events the server wants to know about (is missing due to ec2 reboot)
        //events are numbered. Proposed event number is next from the client's point of view N
        // server's behavior is
        //1) accept event - likely with the wrong number n+1 - and reply with events N through M\
        //1a) client restart recovery: if M = 1 then the client gets all the events the server knows
        //2) server error recovery: observe if the client knows more events than the server - and request any uknown events 1 through N -1
        //3) client error recovery: client updates its event list with the new events
        //4) server error recovery: client proposes any events it knows that the server requested 

    ]
}
////
@http(method: "POST", uri: "/propose", code: 200)
operation ProposeEvents {
    input := {
        @required
        proposal:DuckEvents
    }
    output := {
        @required
        updates:DuckEvents
        @required
        pleaseSend:DuckEventWishList
    }
}

@http(method: "GET", uri: "/mapKey", code: 200)
operation MapLibreGlKey {
    output := {
        @required
        key:String
    }
}