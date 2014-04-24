# Spydroid-ipcamera

* Spydroid is a little app. that *streams the camera and microphone of your phone to your browser or to VLC !*

* It is *a fun app for pulling off pranks*: you can remotly trigger funny sounds on your phone or toggle its flash.

* **Developers, start [by reading this](https://github.com/fyhertz/libstreaming) to find out how streaming is achieved in Spydroid.**

* Check out the feature list to see what Spydroid is capable of :) And If you have questions, check out the [FAQ](https://github.com/fyhertz/spydroid-ipcamera/wiki/FAQ).

* If you enjoyed Spydroid, or its source code, [please rate the app on Google Play](https://market.android.com/details?id=net.majorkernelpanic.spydroid), I would really appreciate :) And if you go and like [the facebook page](http://www.facebook.com/spydroidipcamera), you will be rewarded with all my gratitude :p

# Features

 * The stream can be directly read by VLC which is great because VLC is a very powerful tool, for example *you can really easily record the stream in a file*. [FAQ See the FAQ to find out how].
 * You can enable/disable sound or video streaming
 * The resolution, the bitrate and the framerate of the stream can be configured... Two video encoders are available for the video streaming: H.263 and H.264. For sound streaming AMR and AAC are available.
 * The flash can be controlled remotly !
 * You can choose between the back facing camera and the front facing camera (if your phone has one)
 * Funny sounds can be played on the phone from the HTTP interface ! Awesome isn't it ? :p
 * You can make the phone vibrate remotely
 * You can see the battery level of the phone
 * Some more advanced features are shown [here](https://github.com/fyhertz/spydroid-ipcamera/wiki/Advanced-Use-of-Spydroid)

# Requirements

 * Ice Cream Sandwich or better (API level >=14)
 * *H.263:* should work on phones that supports h263
 * *H.264:* should work on phones that supports h264
 * *AMR:* should work everywhere!
 * *AAC:* should work everywhere!

There may be some glitches on some phones. Some resolution, frame rate, bit rate may be supported on some phones and not on other.

* The VLC plugin for firefox or chrome need to work properly if you want to use the http interface. 

# Dear developers

**The streaming stack used in Spydroid is available as a standalone library [here](https://github.com/fyhertz/libstreaming) with its Javadoc and explanations about how it works and how to use it. It is available under two licenses, the GPL, and a commercial licence.**

# Licensing

**If you are interested in using the libstreaming or parts of Spydroid in a close source commercial application, you can contact me at the mail address specified below.**

_Folks, please note that i'm a french dude, so if you see something that doesn't make any sense do not hesitate to correct me :) (fyhertz at gmail dot com)._

_Enjoy_

By [Simon Guigui](https://simon.guigui.us/) aka fyhertz 
