# Callisto #

Callisto is a fan-made app for JupiterBroadcasting.


## Getting going ##

I use [Intellij](http://www.jetbrains.com/idea/). It is free and somewhat open source. You can use whatever the hell you want but don't expect me to help get you up and running.

If you use IntelliJ, getting set up is incredibly simple: open the project.


### Support Library v7 ###

This is the first time I've used v7 of the support library. I've _tried_ to set it up where it would automatically import the required module,
but IntelliJ seems to forget every time I close the project.

_Ideally_, all you should need to do is set the ANDROID_HOME Path Variable (Preferences > Path Variables).


However, if Intellij is being stupid and not finding it, here are the steps I have to do every time I open the project.


1. Import appcompat as a module (File > Import Module). Path is $ANDROID_HOMEZ$/extras/android/support/v7/appcompat. (Hit "Yes" if it says anything about "reusing".)

2. Add module as lib for Callisto (in Project Structure > Modules, click the little '+' and choose "Module Dependency")

3. Edit the iml file for appcompat to be [https://gist.github.com/hgoebl/8261396]


#### Minifying ####

The only other thing I would recommend doing is setting up whatever dev environment you use to auto-minify the JS, CSS, and JSON assets before building.
The package I personally use is [minify](https://www.npmjs.org/package/minify) which is a node module.

If you want to do what I do, first install node (google how, this is not a node project), then run:

`npm install -g minify`
`npm install -g json-minify`

From there it's as simple as telling your IDE to run it on JS, CSS, and JSON files in the asset directory before the build. I wrote a shell script to assist in it because IntelliJ sucks at External Tools.


## Technologies Used ##

This list of technologies is where the project stands _currently_ in v2; because it is rapidly re-constructing, the list is very likely to change.

 * [android-smart-image-view](https://github.com/loopj/android-smart-image-view) is a small little tool to easily set ImageViews to internet resources asyncronously.
 * [HockeyApp](http://hockeyapp.net/features/) is a beta distribution & testing service. I'm still trying it out.


### License ###

Callisto is released under the [Open Software License v3](http://opensource.org/licenses/OSL-3.0), which is an OSI-approved open-source copyleft license.

The icons, however, are NOT released under this license. Further information is found in the `LICENSE_ICONS.txt` file.

