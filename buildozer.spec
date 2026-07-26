[app]

# (str) Title of your application
title = My Application

# (str) Package name
package.name = myapp

# (str) Package domain (needed for android/ios packaging)
package.domain = org.example

# (str) Source code where the main.py live
source.dir = .

# (list) Source files to include (let empty to include all the files)
source.include_exts = py,png,jpg,kv,atlas,ttf,txt,json,mp3,mp4

# (list) List of inclusions using pattern matching
# source.include_patterns = assets/*,images/*.png

# (list) Source files to exclude (let empty to not exclude anything)
# source.exclude_exts = spec

# (list) List of directory names to exclude
# source.exclude_dirs = tests, bin, __pycache__

# (list) List of exclusions using pattern matching
# source.exclude_patterns = license,images/*/.jpg

# (str) Application versioning (method 1)
version = 0.1

# (str) Application versioning (method 2)
# version.regex = __version__ = ['"](.*)['"]
# version.filename = %(source.dir)s/main.py

# (list) Application requirements
requirements = python3,kivy

# (str) Custom source folders for requirements
# requirements.source.kivy = ./kivy

# (list) Garden requirements
# garden_requirements =

# (str) Presplash of the application
# presplash.filename = %(source.dir)s/data/presplash.png

# (str) Icon of the application
# icon.filename = %(source.dir)s/data/icon.png

# (str) Supported orientation (one of landscape, portrait or all)
orientation = portrait

# (list) List of service to declare
# services = NAME:ENTRYPOINT_TO_PY,NAME2:ENTRYPOINT2_TO_PY

#
# OS X Specific
#

#
# author = © Copyright Info

# change the major version of python used by the app
osx.python_version = 3

# Kivy version to use
osx.kivy_version = 2.1.0

#
# Android specific
#

# (bool) Indicate if the application should be fullscreen or not
fullscreen = 0

# (string) Presplash background color (for android toolchain)
# Supported formats are: #RRGGBB #AARRGGBB or one of the following names:
# red, blue, green, black, white, gray, cyan, magenta, yellow, lightgray,
# darkgray, grey, lightgrey, darkgrey, aqua, fuchsia, lime, maroon, navy,
# olive, purple, silver, teal.
# android.presplash_color = #FFFFFF

# (str) Adaptive icon of the application (can be file/dir)
# android.adaptive-icon.filename = %(source.dir)s/data/icon.png

# (str) Intent filters for the activity
# android.manifest.intent_filters =
#     --action android.intent.action.VIEW
#     --category android.intent.category.DEFAULT
#     --data scheme=myapp

# (list) Android additionnal libraries to copy into the APK
# android.add_src =

# (list) Gradle dependencies to add to the gradle dependencies
# android.gradle_dependencies =

# (bool) Enable AndroidX support
android.use_androidx = true

# (str) android.named_permissions = android.permission.INTERNET

# (list) Permissions to add to the manifest
android.permissions = INTERNET

# (int) Target Android API, should be as high as possible.
android.api = 31

# (int) Minimum API your APK will support.
android.minapi = 21

# (int) Android SDK version to use
android.sdk = 33

# (str) Android NDK version to use
android.ndk = 25b

# (int) Android NDK API to use. This is the minimum API your app will support, it should usually match android.minapi.
android.ndk_api = 21

# (bool) Use --private data storage (True) or --dir public storage (False)
# android.private_storage = True

# (str) Android Java SDK (JDK) to use
java.home = /usr/lib/jvm/java-11-openjdk-amd64

# (str) Android entry point, default is ok for Kivy-based app
# android.entrypoint = org.kivy.android.PythonActivity

# (str) Full name of the Java class which extends PythonActivity
# android.main_class = org.kivy.android.PythonActivity

# (list) Android AAR archives to add (contains .jar and .so)
# android.aar_archives =

# (list) Gradle repositories to add {’url’:’…’, ‘username’:’…’, ‘password’:’…’}
# android.gradle_repositories =

# (list) Java classes to add as sources
# android.java_src =

# (list) Java jars to add
# android.jar =

# (list) Java jars to exclude from the APK
# android.jar_exclude =

# (list) Java jars to include in the APK, to be used with android.jar_exclude
# android.jar_include =

# (bool) Indicates whether the APK should be built with the debugger.
# android.debug = False

# (bool) Indicates whether the APK should be built with the profiler.
# android.profiler = False

# (bool) Indicates whether the APK should be built with the strict mode.
# android.strict = False

#
# iOS specific
#

# (str) Path to a custom kivy-ios folder
# ios.kivy_ios_dir = ../kivy-ios
# (str) Path to the iOS SDK from Xcode to use
# ios.sdk = iphonesimulator
# (str) Path to the iOS SDK version to use
# ios.sdk_version = 15.0
# (str) Path to the iOS deployment target
# ios.deployment_target = 13.0
# (bool) Whether to enable the iOS debug mode
# ios.debug = False

[buildozer]

# (int) Log level (0 = error only, 1 = info, 2 = debug (with command output))
log_level = 2

# (int) Display warning if buildozer is run as root (0 = False, 1 = True)
warn_on_root = 1

# (str) Path to build artifact storage
# build_dir = ./.buildozer

# (str) Path to build output (i.e. .apk, .ipa, etc.)
# bin_dir = ./bin

#    -----------------------------------------------------------------------------
#    List as sections
#
#    You can define all the sections list here.
#    Each section will generate a specific APK when the buildozer is run.
#    Each section should overwrite the previous configuration.
#    For more details, check the documentation
#    https://buildozer.readthedocs.io/en/latest/#configuration-file
#
#    [app]
#    title = My First App
#    package.name = myfirstapp
#
#    [app]
#    title = My Second App
#    package.name = mysecondapp
#

# (str) Android application entry point
# android.entrypoint = org.kivy.android.PythonActivity

# (list) Permissions
# android.permissions = INTERNET

# (int) Android API to use
# android.api = 31

# (int) Minimum API version
# android.minapi = 21

# (int) Android SDK version
# android.sdk = 33

# (str) Android NDK version
# android.ndk = 25b

# (int) Android NDK API version
# android.ndk_api = 21

# (str) Java JDK path
# java.home = /usr/lib/jvm/java-11-openjdk-amd64
