# SimpleIM - Simple Instant Messaging - for Android #
#### A project for Multimedia and Mobile Systems course at DIBRIS (which include Computer Science) of Genova, Italy - 2012 ####


There are two projects:
  * [ServerSimpleIM](http://code.google.com/p/simpleim-android/wiki/ServerSimpleIM) - the server that responds to requests of SimpleIM app;
  * [SimpleIM](http://code.google.com/p/simpleim-android/wiki/SimpleIM) - the Android app;

All communication is made using UDP.

_There are no security mechanisms provided - no encryption of communication between server and client or between clients. Someone could intercept a message, modify it, and send it to someone else._

The server [ServerSimpleIM](http://code.google.com/p/simpleim-android/wiki/ServerSimpleIM) is responding only to registration and login requests coming from the [SimpleIM](http://code.google.com/p/simpleim-android/wiki/SimpleIM) app. Also [ServerSimpleIM](http://code.google.com/p/simpleim-android/wiki/ServerSimpleIM) sends a list of all registered users (containing per each user the contact information, that is ip, port etc).

The [SimpleIM](http://code.google.com/p/simpleim-android/wiki/SimpleIM) app offers the following features:
  * Register;
  * Login;
  * Listing of all users (with their status - online or not);
  * Chat with a user;
  * Send message to all online users (pushing only one button);
  * View the on line users on the map.

Coming soon some screenshots and maybe demo videos.

### First Video ###
In this video it is showed how to:
start the server;
tune the server parameter for the Android client application;
how users interact, by sending text messages, messages that can be lost.

It is, also, showed how the system reacts to unexpected exit. How it keeps up to date the users status (online, offline):

<a href='http://www.youtube.com/watch?feature=player_embedded&v=M9pdWN6gFIU' target='_blank'><img src='http://img.youtube.com/vi/M9pdWN6gFIU/0.jpg' width='425' height=344 /></a>

### Second Video ###
In this video it is showed how the map functionality works.

<a href='http://www.youtube.com/watch?feature=player_embedded&v=Kfrg0qR6Cgs' target='_blank'><img src='http://img.youtube.com/vi/Kfrg0qR6Cgs/0.jpg' width='425' height=344 /></a>