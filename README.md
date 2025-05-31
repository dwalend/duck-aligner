# duck-aligner
Duck Aligner is a tool for sharing GPS locations among a group for about an hour while preserving everyone's privacy.

Duck Aligner enables a small group of people to share their locations with each other for a brief period of time, presumably to help them find each other - without sharing locations with a giant corporation for the life of their devices. I need a way to coordinate with my fiercely independent adolescent kids, but not tracking them all the time. The name is a reference to a verse from James Taylor's "Sun on the Moon."
             
What's here now:

* Proof of concept of 
* * tracking GPS from browsers, 
* * sharing with a server, 
* * and displaying all locations in all browsers
* Mostly in a bleep build 

To build it all:

```shell
cd MapLibreGlScalablyTyped
mill app.something
cd ..
rm -rf DuckUpdateSmithy/src/resources/ .bleep/projects-jars/DuckUpdateSmithy@jvm/my.org/DuckUpdateSmithy_3/0.1.0-SNAPSHOT/jars/DuckUpdateSmithy_3.jar
bleep gen-smithy-code DuckUpdateSmithy
bleep compile DuckUpdateSmithy
bleep link DuckUpdateBrowser
bleep run DuckUpdateService 
```

Here's the sketch of a plan:

* Mostly eat what amazon puts on the plate, build the system up by getting things going, then take on the biggest risks - least familiarity for me - first. Numbers indicate order I plan to chip away at the project.
          
* - [x] Get a server up and running, and sharing location data
* - [ ] (2) Get it running on some tiny machine on AWS (t4 I think is the current generation)
* * - [x] (1) Package up all the bits for the server in a jar using a new bleep plugin
* * - [ ] (3) Run the server from the jar via bleep plugin
* * - [ ] (4) smithy source code should go to a bleep-managed and bleep-cleaned directory to compile
* * - [ ] (5) Bleep smithy AWS plugin - and update it to generate the code using the new facilities
* * - [ ] (6) Bleep plugin to create an AMI with the jar
* * - [ ] (7) Bleep plugin to start the AMI with the jar on EC2
* * - [ ] (8) Start it from an AWS Lambda
* * send SMS text messages to invite other people to the location sharing service
* * handle user login and authentication

* * It'll only serve about 2 requests per minute per user ~ 240 requests an hour for most uses
* * That might mean compiling it to scala native to fit on the tiny server
* * - [ ] (4.4) share about smithy in bleep

* - [x] Use scalajs to build a web app
* *  Make it look like an app https://matt.might.net/articles/how-to-native-iphone-ipad-apps-in-javascript/
* - [x] Use the dom to get location from the device http://docs.glngn.com/latest/api/org.scala-js.scalajs-dom_sjs1_2.12/org/scalajs/dom/raw/Geolocation.html https://stackoverflow.com/questions/40483880/geolocation-in-scala-js
* * - [ ] (1.1) Bleep copy static javascript and html to a (bleep-managed) resource directory
* * - [ ] (4.8) Test javascript in bleep - how to access dom (or not use dom)
* * Add an optional stealth zone - maybe the first quarter of the trip - to hide origin
* - [x] Send that to some URL, receive locations (stored in memory) back from other users (blocked bleep vs scalajs)
* - [x] Get map sections from location data from AWS' location service
* * - Get MapLibra parts from our own server - possibly caching data. (Lots of duplicate calls.)
* * - [ ] Build the smithy4s aws code using Bleep instead of mill
* * - [ ] Use Scalablytyped for MapLibreGL from Bleep instead of mill
* * Possibly offset the location or enter some false locations to put off AWS tracking if they are storing too much data. Amazon is pretty open about which information they keep from AWS API calls.        
* Set up an ephemeral service (S3 and AWS lambda ?) behind an AWS Gateway endpoint to 
* * serve the static bits - javascript, html, images, etc
* * start the service - see https://www.youtube.com/watch?v=SBJNAf-OGQw - smithy4s - maybe - not sure it's the right critter.
* * * compile to scala native again
* * handle the https cert work (which the geolocation javascript API requires)

---

When a user wants to share a location with another user she starts the app. The app lets her pick from contacts and choose who to share with, and for how long. The app makes the initial request to central. Central starts the required resources, and sends SMS messages to alert other users to touch their duck aligner URLs. Her app periodically sends location updates until time expires. The other users duck aligners do the same. Each person's app displays all the locations of people who have agreed to share with them. 

The graph of users at any given time is likely bipartite, and very much disconnected. The whole graph will likely have disconnected islands of ~Dunbar's number or fewer people - likely far fewer.

Implicit - if you agree to see someone's location then you are agreeing to share your own.


---

Dev Setup

Install bleep for your system. Likely set up a special machine's account on AWS to handle the permissions there, but not just yet.