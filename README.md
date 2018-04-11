# SoundChan: The Discord Voice Bot

## How to Run

Currently the only way to run Sound Chan is to build the bot from source. As such you'll need the JRE and JDK.

## Instructions For Use

To run the sound bot on your Server, you'll need a developer's bot token from Discord. Full instructions for use will be coming, but in the meantime for development purposes, place your token and the corresponding settings in a file named, `soundchan.properties`. You can see an example of how the file should be formatted in `soundchan.properties.example`.

## Requirements to Run

* Java Runtime Environment

## Requirements to Develop

* Java Runtime Environment
* Java Development Kit
* `net.dv8tion:JDA:3.5.1_339`
* `com.sedmelluq:lavaplayer:1.2.53`

More information to come.

## Todo

* Move the ExtendedListenerAdapter outside of main
* Add a monitor for updated files (refresh every few minutes or on command?)
* Add pre-emption support for sound effects (without disturbing the queue)
* Support seeking (starting a track at any point, or skipping ahead in the track)
* User roles
* Help command
* Document run steps properly
* Package a batch file / bash file for easy use
* List currently playing song
* Like or dislike support for currently playing track
* Have SoundChan reply with a voice bot
* Turn features on and off using config files
