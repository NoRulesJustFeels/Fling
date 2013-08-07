Fling
=====

<p><img src="http://chromecast.entertailion.com/chromecastanimation100.gif"/></p>

<p>The Fling application allows you to beam video files from computers to <a href="https://www.google.com/intl/en/chrome/devices/chromecast/">ChromeCast</a> devices.</p>

<p>To run the application, you need a <a href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">Java runtime environment (JRE)</a> for your operating system. 
There is a dependency on a web app that needs to be <a href="https://developers.google.com/cast/whitelisting#whitelist-receiver">registered with Google</a>, so currently this solution will only work for developers until Google opens up their ChromeCast developer program.</p>

<p>When the application starts, it will search for ChromeCast devices on your local network. Select the device that you want to beam the videos to.
Then drag-and-drop a video file onto the application to beam it to the ChromeCast device.</p>

<p>Playback is limited to the <a href="https://developers.google.com/cast/supported_media_types">video formats supported by ChromeCast</a>.
Fling does not do any conversions of the video.</p>

<p>There are basic playback controls to play, pause and stop the video (pause and stop behaves the same).</p>

<p>The computer running the Fling application needs to be on the same network as the ChromeCast device. 
To play the video, a web server is created on port 8080 on your computer and you might have to configure your firewall to allow incoming connections to access the video.</p>

<p>Note for developers: You need to put your own app ID in the FlingFrame.java and receiver index.html files. Also, take a look a the <a href="https://github.com/entertailion/DIAL">DIAL Android app</a> which also discovers ChromeCast devices and supports YouTube videos.</p>

<p>Watch this <a href="http://youtu.be/fehncl0nTAE">video</a> to see the application in action.</p>

<p>Other apps developed by Entertailion:
<ul>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.tvremote">Able Remote for Google TV</a>: The ultimate Google TV remote</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.launcher">Open Launcher for Google TV</a>: The ultimate Google TV launcher</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.overlay">Overlay for Google TV</a>: Live TV effects for Google TV</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.overlaynews">Overlay News for Google TV</a>: News headlines over live TV</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.videowall">Video Wall</a>: Wall-to-Wall Youtube videos</li>
<li><a href="https://play.google.com/store/apps/details?id=com.entertailion.android.tasker">GTV Tasker Plugin</a>: Control your Google TV with Tasker actions</li>
</ul>
</p>

<p>
<h2>RAMP Protocol for ChromeCast Explained (unofficial):</h2>

Google is using a proprietary protocol called RAMP (Remote Application Media Protocol) to control media playback on a ChromeCast device.</p>

<p>The RAMP commands are sent over a web socket connection to the ChromeCast device using JSON notation.
Look at my DIAL app that I have open sourced for the sequence of HTTP actions to get the URL for creating the web socket: https://github.com/entertailion/DIAL</p>

<p>Associated with your app you need a receiver HTML page that has a video HTML tag and the Cast receiver javascript library included (same as the Cast SDK sample).
You have to start the app associated with the receiver HTML.</p>

<p>Once you have the web socket connection open to the ChromeCast device, there are various commands that allow you to load and control media playback.</p>

<p>The first message you have to deal with is a ping from the device: 
<blockquote>
["cm",{"type":"ping"}]
</blockquote>
The client needs to respond with a "pong" on an interval that is specified with the web socket URL payload: 
<blockquote>
["cm",{"type":"pong"}]
</blockquote>
</p>

<p>To load a video use the following RAMP command:
<blockquote>
["ramp",{"title":"Video title","src":"http://some/url/to/a/video.mp4","type":"LOAD","cmd_id":0,"autoplay":true}]
</blockquote>
</p>

<p>The "cmd_id" parameter increments with each command sent from the client. The associated response from the ChromeCast device will have the same "cmd_id" value.</p>

<p>You can use the following command to explicitly ask for status:
<blockquote>
["ramp",{"type":"INFO","cmd_id":1}]
</blockquote>
</p>

<p>However, during my testing the device automatically provided regular status responses during media playback to track the playback progress:
<blockquote>
["ramp",{"cmd_id":2,"type":"STATUS","status":{"event_sequence":7768, "state":2, "content_id":"http://some/url/to/a/video.mp4", "current_time":9.208321571350098, "duration":596.5013427734375, "volume":0.5, "muted":false, "time_progress":true, "title":"Video title"}}]
</blockquote>
</p>

<p>To stop the video:
<blockquote>
["ramp",{"type":"STOP","cmd_id":3}]
</blockquote>
</p>

<p>There doesn't appear to be a pause command. The stop commands actually pauses the video and the user will see a frozen video frame.</p>

<p>To play the video again, you can just play from the current position:
<blockquote>
["ramp",{"type":"PLAY","cmd_id":4}]
</blockquote>
</p>

<p>Or you can start from another position like the beginning of the video:
<blockquote>
["ramp",{"position":0,"type":"PLAY","cmd_id":5}]
</blockquote>
</p>

<p>You can set the volume using the following command:
<blockquote>
["ramp",{"volume":0.5,"type":"VOLUME","cmd_id":6}]
</blockquote>
</p>

<p>Or mute the volume:
<blockquote>
["ramp",{"type":"VOLUME","cmd_id":7,"muted":true}]
</blockquote>
</p>

