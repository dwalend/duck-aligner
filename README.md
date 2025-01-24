# duck-aligner
Duck Aligner is a tool for sharing GPS locations among a group for about an hour while preserving everyone's privacy.

Duck Aligner enables a small group of people to share their locations with each other for a brief period of time, presumably to help them find each other - without sharing locations with a giant corporation for the life of their devices. I need a way to coordinate with my fiercely independent adolescent kids, but not tracking them all the time. The name is a reference to a verse from James Taylor's "Sun on the Moon."
             
What's here now:

* A quick proof that I could build a scalajs project in bleep (not terribly interesting) in FrontEnd
* The http4s starter project from `sbt new http4s/http4s.g8 --branch 0.23-scala3` in ShareLocationService 
* * Converted to a bleep build
* * To get some practice handling geographic coordinates I added a service to check the weather
* * To run the server use 

```shell
bleep run DuckupdateService
```
And curl it with   

```shell
curl -i http://localhost:8080/forecast/42.338032,-71.211578
```

Here's the sketch of a plan:

* Mostly eat what amazon puts on the plate, build the system up by getting things going, then take on the biggest risks - least familiarity for me - first. Numbers indicate order I plan to chip away at the project.
          
* - [x] (1) Get a server up and running, and sharing location data
* Get it running on some tiny machine on AWS (t4 I think is the current generation)
* * It'll only serve about 2 requests per minute per user ~ 240 requests an hour for most uses
* * That might mean compiling it to scala native to fit on the tiny server
* * - [ ] (4.2) smithy source code should go to a bleep-managed and bleep-cleaned directory to compile
* * - [ ] (4.4) share about smithy in bleep

* - [ ] (3) Use scalajs to build a web app
* *  Make it look like an app https://matt.might.net/articles/how-to-native-iphone-ipad-apps-in-javascript/
* - [x] Use the dom to get location from the device http://docs.glngn.com/latest/api/org.scala-js.scalajs-dom_sjs1_2.12/org/scalajs/dom/raw/Geolocation.html https://stackoverflow.com/questions/40483880/geolocation-in-scala-js
* * (Find some other javascript API to see what tech is providing the location. If it's IP-based instead of GPS, for example)
* * - [ ] (4.6) Bleep copy static javascript and html to a (bleep-managed) resource directory
* * - [ ] (4.8) Test javascript in bleep - how to access dom (or not use dom)
* * Add an optional stealth zone - maybe the first quarter of the trip - to hide origin
* - [x] (4) Send that to some URL, receive locations (stored in memory) back from other users (blocked bleep vs scalajs)
* - [ ] (5) Get map sections from location data from AWS' location service
** - [ ] Use smithy4s' AWS API to get map sections from location data https://docs.aws.amazon.com/location/latest/APIReference/welcome.html https://github.com/exoego/aws-sdk-scalajs-facade/blob/master/aws-sdk-v2/services/location/src/main/scala/facade/amazonaws/services/Location.scala https://docs.aws.amazon.com/location/latest/developerguide/samples.html#example-draw-markers . (blocked smithy4s compile error building AWS location)
* * - [ ] (6) Build the smithy4s aws code using Bleep instead of mill
* * - [ ] (7) Wait for the location service form smithy4s to get fixed (or figure out how to do it). Broken in the 2023 version - maybe don't need it after all - just the key
* * Possibly offset the location or enter some false locations to put off AWS tracking if they are storing too much data. Amazon is pretty open about which information they keep from AWS API calls.        
* Set up an ephemeral service (S3 and AWS lambda ?) behind an AWS Gateway endpoint to 
* * serve the static bits - javascript, html, images, etc
* * start the service - see https://www.youtube.com/watch?v=SBJNAf-OGQw - smithy4s - maybe - not sure it's the right critter.
* * * compile to scala native again
* * handle the https cert work (which the geolocation javascript API requires)
* * - [ ] (8) send SMS text messages to invite other people to the location sharing service
* * handle user login and authentication 

---

When a user wants to share a location with another user she starts the app. The app lets her pick from contacts and choose who to share with, and for how long. The app makes the initial request to central. Central starts the required resources, and sends SMS messages to alert other users to touch their duck aligner URLs. Her app periodically sends location updates until time expires. The other users duck aligners do the same. Each person's app displays all the locations of people who have agreed to share with them. 

The graph of users at any given time is likely bipartite, and very much disconnected. The whole graph will likely have disconnected islands of ~Dunbar's number or fewer people - likely far fewer.

Implicit - if you agree to see someone's location then you are agreeing to share your own.


---

Dev Setup

Install bleep for your system. Likely set up a special machine's account on AWS to handle the permissions there, but not just yet.