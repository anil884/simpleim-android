# Introduction #

The server part of this Simple Instant Messaging program is Called ServerSimpleIM.
On this repository there is a project called ServerSimpleIM - it can be imported into Eclipse IDE. Unfortunately there are no ant build files.

To turn down the debug messages you can set the **`DEBUG`** variable in _serversimpleim.BaseServer.java_.

# Details #

As said before, all communication is made by using UDP packets - that is a **stateless** protocol (opposite to TCP).

The basic functions of the server are:
  * Accepting _registration_ requests;
  * Accepting _login_ requests;
  * Maintaining the the list of the users (together with their status - that is online or offline) registered to the system.

The user list is maintained in memory during the execution of server program. It is also written onto file (_users.txt_) so that the subsequent execution of the server the user list is restored from the file.


## Accepting _registration_ requests ##

If the client comes to the server with a registration request (_username_, password) then the server checks whether another user is registered with the same _username_:
  * if not, then the server accepts and saves this new user information in its memory and sends a _OK_ answer;
  * otherwise, that is there's already a user registered with the same username, the servers sends a _REFUSED_ answer.


## Accepting _login_ requests ##

If the client comes to the server with a login request (_username_, _password_) then the server checks whether there exists a user with these credentials (_username_, _password_):
  * if not, then the server sends a _REFUSE_ answer meaning that the server refused to the user to login the system;
  * otherwise, that is there exists a user with username = _username_ and password = _password_, the server sends back to the client a _OK_ answer and then the list of all users (together with their status and _contact information_) registered on the system.


## Maintaining the the list of the users ##

As already said, the server keeps the list of all registered users. This list is updated each time:
  * a new user registers;
  * a user logs in;
  * a user logs out;

When a user logs in, of course, she gets the fresh copy of user list!

Since all the communication is done using UDP protocol some packet could be lost - this packet could be a log out packet.
If a user **A** logs out and the server does not receive the logout packet then the user list contains wrong data - that is for the server the user **A** is still logged in. And from now on, all users that login will get the wrong information about the user **A**.
To remedy this problem the server sends each logged user every _SEC_ seconds a packet requesting an answer. If for some user, after requesting a number of times she's information, there's no answer - then she's maybe offline.

Using this _polling_ the server keeps the users information up to date.