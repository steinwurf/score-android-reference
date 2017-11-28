score-android-reference
=======================

**Score** is a library which implements the
**S**\ imple **Co**\ ded **Re**\ liable protocol for reliable unicast and
multicast communication over UDP.

This repository contains reference designs using the score-android library.

Setup
-----
The score-android library is a dependency of this project.
The library is deployed at Steinwurf's private maven repository.

You need to be an authorized user to accessing this.
To become authorized you need a commercial license, please contact
contact@steinwurf.com.

If you have a commercial license please contact Steinwurf at
support@steinwurf.com to have us make you a user for our maven repository.

Once you have a user you need to put the following lines in your global gradle
file (This is located in ``$HOME/.gradle/gradle.properties``):

.. code-block:: groovy

    artifactory_username=[YOUR USERNAME HERE]
    artifactory_password=[YOUR PASSWORD HERE]

For security reasons it's recommended to use an encrypted password.
You can get that on your users page on our Artifactory web interface.

http://artifactory.steinwurf.com/artifactory/webapp/#/home

After this you should be able to build the project using Android Studio or
`./gradlew`.

Apps
----
The repository contains several apps, in this section each of them will be
described:

Score Client
............
The Score Client app is located in the client module. As the name implies, this
app is the client app which is used for receiving the stream from either of the
server apps.

Camera Server
.............
The Camera Server app is located in the server module. This app creates a score
video stream of the first camera of the device. This is usually the back camera.

Screen Capture Server
.....................
The Screen Capture Server app is located in the server module. This app creates
a score video stream of the screen of the device.

Shortcomings
------------
Note, these apps are simple reference designs which means they have several
shortcomings a real application would probably need to address in most cases.

Some of these are listed here:
1. The IP and port is hard-coded.
2. The video encoding configuration is hard-coded.
3. The score source is not configured, which means the performance may be
compromised.
4. If only a small amount of data is being transmitted it can take a long time
   before enough data is available in the score source for it to create data
   packets. This can lead to dropped video frames on the client.
   To prevent this issue, one need to configure the score source properly and
   use the source's flush method.

License
-------
THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF.
