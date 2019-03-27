# SoundChan: The Discord Voice Bot

## How to Run

Currently the only way to run Sound Chan is to build the bot from source. As such you'll need the JRE and JDK.

## Instructions For Use

To run the sound bot on your Server, you'll need a developer's bot token from Discord.
As the exact way to get a token may change over time, checkout the [Discord documentation](https://discordapp.com/developers/docs/reference).
Once you get a token, place your token and the corresponding settings in a file named, `soundchan.properties`.
You can see an example of how the file should be formatted in `soundchan.properties.example`.

In your `soundchan.properties`, you can specify all sorts of settings that SoundChan will read on startup.
An example of a setting you may want to set is the directory to sounds you want SoundChan to play as an interjection.
Other things you can set is if SoundChan should watch this directory for changes so you can add and remove files on the fly.
See `soundchan.properties.example` for a full list of currently adjustable things.

Another file you can optionally add is `usersounds.properties`.
Here you can specify a sound from the sound directory (set in `soundchan.properties`) that will play when a user joins the voice channel SoundChan is currently in.
See `usersounds.properties.example` for more information about how to set this up.

## Requirements to Run

* Java Runtime Environment

## Requirements to Develop

* Java Runtime Environment
* Java Development Kit
* `net.dv8tion:JDA:3.5.1_339`
* `com.sedmelluq:lavaplayer:1.2.53`

More information to come.

## Todo

- [x] Move the ExtendedListenerAdapter outside of main
- [x] Add a monitor for updated files (refresh every few minutes)
- [x] Add pre-emption support for sound effects (without disturbing the queue)
- [ ] Support seeking (starting a track at any point, or skipping ahead in the track)
- [ ] User roles
- [x] Help command
- [ ] Document run steps properly
- [ ] Package a batch file / bash file for easy use
- [x] List currently playing song
- [ ] Like or dislike support for currently playing track
- [ ] Have SoundChan reply with a voice bot
- [x] Turn features on and off using config files
