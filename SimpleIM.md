# Introduction #

The client part of this Simple Instant Messaging program is Called SimpleIM. On this repository there is a project called SimpleIM - it can be imported into Eclipse IDE. Unfortunately there are no ant build files.

To turn down the debug messages you can set the **`DEBUG`** variable in _com.tolmms.simpleim.MainActivity.java_.

# Details #

As said almost everywhere - the communication is made by using UDP protocol. It is a state-less protocol. The greatest problem using this protocol (in this application) is to provide a mechanism to check if a message reached the destination or if it didn't then attempt to re-send the packet. The messages can be:
  * log in messages;
  * log out messages;
  * communication messages - that is a text message that a user A sends to another user B.

The communication is made by contacting **directly** the destination. [Here](http://code.google.com/p/simpleim-android/wiki/ServerSimpleIM) I said that the [ServerSimpleIM](http://code.google.com/p/simpleim-android/wiki/ServerSimpleIM) gives to every logged user the list of current registered users (with some additional information per each user - _contact information_, _status_, _localization coordinates_).

So, to make a point, it is a kind of p2p application. Every message is sent directly to the destination user and not through the server.

There's a "polling" to check whether some user logged out without sending a logout message (maybe due to communication error, etc.). In this way also each client has a fresh, up to date, view of the current users (whether they are offline or not and their localization coordinates).


## Basic Functions Provided ##

The android client application provides the following features (described at high level):
  * possibility to register / login to the chat system (that is a server - [ServerSimpleIM](http://code.google.com/p/simpleim-android/wiki/ServerSimpleIM));
  * possibility to view all the registered users - also to view their status (online or offline);
  * possibility to view on the map the users that are on line. A user can choose whether others can view him on the map - note if it chooses to not to show him on the others map then he won't be able to see others on his map! _I used OSMDroid api to get map data from OpenStreetMap_.
**Note that it is necessary that you either run this app on a device with an sd card (or memory - like the nexus s) or an emulator "with an SD card" in order to view the map**.
  * possibility to send text messages to a on line user;
  * possibility to send a message to all the on line users of the system;

## Things to Know about ##

### Connection to server parameters ###
In order to run this program you should write manually in _com.tolmms.simpleim.communication.Communication.java_ the address of the computer where the server program [ServerSimpleIM](http://code.google.com/p/simpleim-android/wiki/ServerSimpleIM) runs. In my configuration it is `serverIpString = "192.168.1.7"` and the port is `serverUdpPort = 4445`. Leave the port as it is if you do not set any specific port in [ServerSimpleIM](http://code.google.com/p/simpleim-android/wiki/ServerSimpleIM).

If you want to play with this program using emulators - be aware that the _loopback_ interface does not work as it should (by now I don't know why) - so the workaround is to put the address of other interface connected to some network (in my case it is a wifi with a 192.168.1.7 ip address).

NOTE: Yes, it would be very handy to have an UI where you can put this data in. But for now... it can be only edited directly from source code.

### Parameters to tune to keep up to date the user list information ###
Another thing you may want to tailor is the refresh time of user state (that is if the information of a user _U_ is older than a certain amount of time it is refreshed - the program requests new information by sending a request message to _U_). The variable you may want to change is `SECONDS_TO_CHECK_USER_INFO` in _com.tolmms.simpleim.interfaces.IAppManager.java_.
The `NUMBER_USER_INFO_REQUEST_RETRIES` variable that you can find in the same file - _com.tolmms.simpleim.interfaces.IAppManager.java_ is the number of retries below which, if no answer is received after this number of requests from a certain user _U_, the user _U_ is marked as **offline**.

NOTE: Also these parameters could be set by uses of a handy UI... for now this _handy UI_ is not implemented.

### Parameters to tune to keep trace whether a message has reached destination ###
The same concept applies with text messages sent to a user. At this point, if you read the previous paragraph, the variables, that are in _com.tolmms.simpleim.interfaces.IAppManager.java_, are self explained: `SECONDS_TO_CHECK_SENT_MESSAGES`, `NUMBER_MESSAGE_SENT_RETRIES`.
Just to be clear - when a user _U_ sends a text message to _W_ (both must be on line) the user _U_ waits for a message from _W_ that confirms it received its message. If after `SECONDS_TO_CHECK_SENT_MESSAGES` x `NUMBER_MESSAGE_SENT_RETRIES` seconds _U_ does not receive any confirmation message from _W_ then the message is marked as **lost** message.

NOTE: see the above notes.