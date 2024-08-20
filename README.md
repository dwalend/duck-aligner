# duck-aligner
Duck Aligner is a tool for tracking GPS locations while preserving privacy.

Duck Aligner enables a small group of (two?) people to share their locations with each other for a brief period of time, presumably to help them find each other - without sharing either location with a giant corporation for the life of their devices.

Here's the plan sketch:

* Mostly eat what amazon puts on the plate, but use Constellation for a network and subnet.

* (2) Use scalajs to build a web app
* * (6) Make it look like an app https://matt.might.net/articles/how-to-native-iphone-ipad-apps-in-javascript/
* (3) Use the dom to get location from the device http://docs.glngn.com/latest/api/org.scala-js.scalajs-dom_sjs1_2.12/org/scalajs/dom/raw/Geolocation.html https://stackoverflow.com/questions/40483880/geolocation-in-scala-js 
* (Find some other javascript API to see what tech is providing the location. If it's IP-based instead of GPS, for example)
* (4) Send that to some URL, receive locations back from other users

* (1) Use a Constellation network of people willing to share with each other
* * Create a subnet of people to share with each other right now
* * Let "share location right now" be the protected, valued asset

* Alternative to Constellation - Use AWS Lambda to wake up and start a supremely cheap EC2 instance (t4g ?) for an hour - and as a stable server - to share the data. Compile all the scala code to native to make for quick lambda starts and something that fits on the t4g tiny computer. Only store the data in memory on the EC2 instance so that the location gets scrubbed with the EC2 machine. https://www.youtube.com/watch?v=SBJNAf-OGQw 

* (5) Use AWS SMS to send a URL via text to let others know someone wants to share their location with them

* (6) Use the scala js AWS API to get map sections from location data https://docs.aws.amazon.com/location/latest/APIReference/welcome.html https://github.com/exoego/aws-sdk-scalajs-facade/blob/master/aws-sdk-v2/services/location/src/main/scala/facade/amazonaws/services/Location.scala https://docs.aws.amazon.com/location/latest/developerguide/samples.html#example-draw-markers . (Possibly offset the location or enter some false locations to put off AWS tracking if they are storing too much data. Amazon is pretty open about which information they keep from AWS API calls.)

When a user wants to share a location with another user she starts the app. The app lets her pick from contacts and choose who to share with, and for how long. The app makes the initial request to central. Central starts the required resources, and sends SMS messages to alert other users to touch their duck aligner URLs. Her app periodically sends location updates until time expires. The other users duck aligners do the same. Each person's app displays all the locations of people who have agreed to share with them. 

The graph of users at any given time is likely bipartite, and very much disconnected. The whole graph will likely have disconnected islands of ~Dunbar's number or fewer people - likely far fewer.

Implicit - if you agree to see someone's location then you are agreeing to share your own.



