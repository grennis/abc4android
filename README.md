# abc4android (fork)

This is a fork of the [abc4j](https://github.com/Sciss/abc4j) library adapter for Android use:

* Remove desktop / UI code and dependencies (Swing, etc.)
* Change to gradle build system

## usage

To use the library add this to your root `build.gradle`:

    allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

And this to your project `build.gradle`:

    compile 'com.github.grennis:abc4android:v0.6.0_Android'

## overview

Please refer to the [original fork](https://github.com/Sciss/abc4j) of the [original project](https://code.google.com/p/abc4j/) for documentation.
