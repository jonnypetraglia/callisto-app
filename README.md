# Callisto #

Callisto is a fan-made app for JupiterBroadcasting.


## Differences in v2 ##

Version 1 of Callisto was my very first Android app. As a result, much of it was more "Can I do this?" rather than having a vision and fulfilling it. Version 2 is going to be a complete re-write with some recycled code from v1.

Here are the main differences planned for v2:

 * _More modern_. v1 was aimed at all versions of Android and was thus limited in many ways. v2 is (currently) aimed at running on 2.3+, which still covers the majority of the market but will allow much easier and cleaner implementations of many features.
 * _More organized_. Since v1 was kind of put together as I went, the code was rather muddled. It lacked organization and clarity and I myself frequently got lost trying to remember what did what.
 * _Cleaner UI_. In addition to adhering to the modern Android UX practices, v2 will remove a lot of buggy interfaces that simply did not work in v1.
 * _Less deprecated_. My goal this time is to have _zero_ warnings when compiling the project.
 * _Less centralized_. I'm planning on introducing features in v2 to keep me from being a choking point for events such as JB releasing a new show.
 * _Less buggy_. I'm hoping that -as a result of everything above- the code will be much more pure and have far fewer bugs.


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


## General Architecture ##

The nice thing about v2 of Callisto is that it is very compartmentalized. Ideally, each fragment should function more or less completely independent of each other.

- **catalog**: Used for fetching the back catalog, i.e. the podcatcher portion of the app.
  - **playback/queue**: TBA
- **live**: Used for the live streaming.
- **chat**: Used for the IRC portion of the app.
- **schedule**: Used to pull in the calendar feed & set alarms for upcoming shows.
- **contact**: Used for the JB Wufoo contact form.
- **donate**: Used for donations for JB (Currently Paypal, eventually Patreon.)
- **settings**: Settings for the app.
- **about**: An about page for the app, listing version# for the app & release changelog and whatnot.


Each package will have it's own fragment that is inherited from **CallistoFragment**, which is just an abstract class with some niceties like a reference back to the Master Activity.
They will also have, obviously, other classes and possibly subpackages.

**MasterActivitity** is the...well, the master activity. It is the through which all fragments are served. It is essentially just a shell to facilitate moving between fragments.

**DatabaseConnector** is the class used, internally, to access the database (i.e. it handles it being created, upgraded, and contains the SqliteDatabase variable).
To actually do anything with the database, files called **DatabaseMates** are available per package. (DatabaseMates interally call the DatabaseConnector.)
This keeps DatabaseConnector from growing unseemingly large and keeps with the idea of separation of features by package.

**SplashFragment** is a fragment to show upon opening the app for the first time. Because the new JB logo is sexy.


There is a class called **PRIVATE** filled with things that I don't want to check into the repo, things like API keys and whatnot.
There should be no logic in there, and if there ever is, I will try to comment on why I put it in PRIVATE on a case-by-case basis.

### Database schema ###

TBA

### License ###

Callisto is released under the [Open Software License v3](http://opensource.org/licenses/OSL-3.0), which is an OSI-approved open-source copyleft license.

The icons, however, are NOT released under this license. Further information is found in the `LICENSE_ICONS.txt` file.

