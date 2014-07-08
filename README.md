# Callisto #

Callisto is a fan-made app for JupiterBroadcasting.


## Differences in v2 ##

Version 1 of Callisto was my very first Android app. As a result, much of it was more "Can I do this?" rather than having a vision and fulfilling it. Version 2 is going to be a complete re-write with some recycled code from v1.

Here are the main differences planned for v2:

 * _More modern_. v1 was aimed at all versions of Android and was thus limited in many ways. v2 is (currently) aimed at running on 2.3+, which still covers the majority of the market but will allow much easier and cleaner implementations of many features.
 * _More organized_. Since v1 was kind of put together as I went, the code was rather muddled. It lacked organization and clarity and I myself frequently got lost trying to remember what did what.
 * _Cleaner UI_. In addition to adhering to the modern Android UX practices, v2 will remove a lot of buggy interfaces that simply did not work in v1.
 * _Less centralized_. I'm planning on introducing features in v2 to keep me from being a choking point for events such as JB releasing a new show.
 * _Less buggy_. I'm hoping that -as a result of everything above- the code will be much more pure and have far fewer bugs.


## Getting going ##

I use [Intellij](http://www.jetbrains.com/idea/). It is free and somewhat open source. You can use whatever the hell you want but don't expect me to help get you up and running.

If you use IntelliJ, getting set up is incredibly simple: open the project.


### Support Library v7 ###

This is the first time I've used v7 of the support library. Here are the steps I had to do to get it to work with the project:

1. Import appcompat as a module (File > Import Module)

2. Add module as lib for Callisto (in Project Structure > Modules, click the little '+' and choose "Module Dependency")

3. Edit the iml file for appcompat to https://gist.github.com/hgoebl/8261396

#### Minifying ####

The only other thing I would recommend doing is setting up whatever dev environment you use to auto-minify the JS, CSS, and JSON assets before building.
The package I personally use is [minify](https://www.npmjs.org/package/minify) which is a node module.

If you want to do what I do, first install node (google how, this is not a node project), then run:

`npm install -g minify`
`npm install -g json-minify`

From there it's as simple as telling your IDE to run it on JS, CSS, and JSON files in the asset directory before the build. I wrote a shell script to assist in it because IntelliJ sucks at External Tools.


## Technologies Used ##

This list of technologies is where the project stands _currently_ in v2; because it is rapidly re-constructing, the list is very likely to change.



## General Architecture ##


### Database schema ###

TBA

### License ###

Callisto is released under the [Open Software License v3](http://opensource.org/licenses/OSL-3.0), which is an OSI-approved open-source copyleft license.

The icons, however, are NOT released under this license. Further information is found in the `LICENSE_ICONS.txt` file.

