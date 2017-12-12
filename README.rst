=======================
score-android-reference
=======================
|buildbot| **client:** |API-16| **server:** |API-21|

**Score** is a library which implements the
**S**\ imple **Co**\ ded **Re**\ liable protocol for reliable unicast and
multicast communication over UDP.

This repository contains reference which illustrates how to use the
`score-android <https://github.com/steinwurf/score-android>`_ library.


.. contents:: Table of Contents:
   :local:

Setup
=====
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
You can get that on your user's page on our Artifactory web interface.

http://artifactory.steinwurf.com/artifactory/webapp/#/home

After this you should be able to build the project using Android Studio, or by
running the following gradle command:

.. code-block:: cmd

    ./gradlew assembleDebug

Apps
====
The examples in this project are split into two modules - `server` and `client`.
In these modules each activity is contains a separate examples. The example can
either be audio or video related.

Each example will be described in this section.

Audio
-----
This section describes the audio related examples.

Score Audio
...........

|Score Audio|

This example is the client side of the audio examples.

Score Microphone
................

|Score Microphone|

This example is the server side of the audio examples. It streams the audio
recorded from the device's microphone.

The audio encoding is uncompressed linear PCM with a sample size of 16 bits.

Streaming audio differs from streaming video because the data rate is
somewhat lower. To combat this the symbol size should be lowered so that even
though very little data is generated, data packets are still produced by
the Score Source in a timely manner.

Video
-----
This seconds describes the Apps related to video streaming.

Score Video
...........

|Score Video|

This example is the client side of the video examples.
It uses Steinwurf's `mediaplayer library <https://github.com/steinwurf/mediaplayer-android>`_ for playback of the incoming video data.
The before the video player can start the playback of video, it needs the SPS
and PPS buffers. For this reason these buffers are provided as messages injected
to the stream before each I-frame (NaluType.IdrSlice) in the stream.
Additionally the last 8 bytes of the received message is a long representing the
time stamp of the received video sample.

Score Camera
............

|Score Camera|

This example is one of the server sides of the video example. This app creates a
score video stream from the device's first camera. This is usually the back
camera.

Score Screen Capture
....................

|Score Screen Capture|

This example is another server side example of the video example.
This app creates a score video stream based on the screen of the device.
It uses the same encoder and encoding configuration as the Score Camera example.

Limitations
===========
Note, these apps are simple reference designs which means they have several
limitations would likely need to be addressed in a real application.

Some of these are listed here:

#. The IP and port is hard-coded.
#. The media encoding configuration is hard-coded.
#. If only a small amount of data is being transmitted it can take a long time
   before enough data is available in the score source for it to create data
   packets. This can lead to dropped frames on the client.
   To prevent this issue, one need to configure the score source properly and
   use the source's flush method.
#. The clients are not notified when the server has stopped. And when restarting
   the server all clients must be restarted as well.

License
=======
THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF STEINWURF.

.. |buildbot| image:: http://buildbot.steinwurf.dk/svgstatus?project=score-android-reference
    :target: http://buildbot.steinwurf.dk/stats?projects=score-android-reference

.. |API-16| image:: https://img.shields.io/badge/API-16%2B-brightgreen.svg?style=flat
    :target: https://android-arsenal.com/api?level=16

.. |API-21| image:: https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat
    :target: https://android-arsenal.com/api?level=21

.. |Score Video| image:: https://github.com/steinwurf/score-android-reference/raw/master/client/src/main/res/mipmap-mdpi/ic_launcher_round.png
    :width: 24
    :target: https://github.com/steinwurf/score-android-reference/blob/master/client/src/main/java/com/steinwurf/score/client_reference/video/VideoClientActivity.java

.. |Score Screen Capture| image:: https://github.com/steinwurf/score-android-reference/raw/master/server/src/main/res/mipmap-mdpi/ic_launcher1_round.png
    :width: 24
    :target: https://github.com/steinwurf/score-android-reference/blob/master/server/src/main/java/com/steinwurf/score/server_reference/video/ScreenCaptureActivity.java

.. |Score Camera| image:: https://github.com/steinwurf/score-android-reference/raw/master/server/src/main/res/mipmap-mdpi/ic_launcher2_round.png
    :width: 24
    :target: https://github.com/steinwurf/score-android-reference/blob/master/server/src/main/java/com/steinwurf/score/server_reference/video/CameraActivity.java

.. |Score Audio| image:: https://github.com/steinwurf/score-android-reference/raw/master/client/src/main/res/mipmap-mdpi/ic_launcher2_round.png
    :width: 24
    :target: https://github.com/steinwurf/score-android-reference/blob/master/client/src/main/java/com/steinwurf/score/client_reference/audio/AudioClientActivity.java

.. |Score Microphone| image:: https://github.com/steinwurf/score-android-reference/raw/master/server/src/main/res/mipmap-mdpi/ic_launcher3_round.png
    :width: 24
    :target: https://github.com/steinwurf/score-android-reference/blob/master/server/src/main/java/com/steinwurf/score/server_reference/audio/MicrophoneActivity.java
