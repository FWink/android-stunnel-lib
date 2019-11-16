# android-stunnel-lib
Android/Java wrapper around Stunnel (https://www.stunnel.org/). Creation of Stunnel configurations by using high-level Java methods instead of config files.

Add TLS support to anything.

# Usage and examples

The main class to use is `StunnelBuilder`. You can add _services_ by calling `addService()`. A service is one single proxy redirecting traffic from a TCP server socket to a destination. Services run either in client mode (incoming connections are plaintext, outgoing connections are secured with TLS) or in server mode (the other way around).

This is how a client configuration might look like to connect to google.com using TLS 1.3:
```java
new StunnelBuilder(context)
  .addService()
      .client()
      .acceptLocal(80)
      .connect("google.com", 443)
      .sslVersion(SSLVersion.TLSv1_3)
      .apply()
  .start();
```
Note: you can use this to add TLS 1.3 support to apps running on older Android platforms relatively easily.


Using TLS PSK instead:
```java
new StunnelBuilder(context)
  .addService()
      .client()
      .acceptLocal(80)
      .connect("google.com", 443)
      .sslVersion(SSLVersion.TLSv1_2)
      .ciphers(SSLCipher.PSK)
      .pskSecrets(new PreSharedKey(
              "MyName",
              "MyPreSharedKey"
      ))
      .apply()
  .start();
```

Once `start()` is called a temporary configuration file is created which is used to run a Stunnel process. To kill the process (thus closing all connections) you need to `close()` the returned `Stunnel` object.

## Installation

Add jitpack.io to your project .gradle file like this:

```
    allprojects {
        repositories {
            jcenter()
            maven { url "https://jitpack.io" }
        }
   }
```

Then add the library to your dependencies in your module .gradle file:
````
dependencies {
    implementation 'com.github.FWink:android-stunnel-lib:master-SNAPSHOT'
}
````

## Example app

You can see this library in action here: https://github.com/FWink/home-assistant-android-stunnel

That app adds PSK client authentication to HomeAssistant (https://www.home-assistant.io) by embedding a WebView that connects to a local Stunnel proxy. The `StunnelTask` class in https://github.com/FWink/home-assistant-android-stunnel/blob/master/app/src/main/java/de/fwinkel/homeassistantstunnel/HomeAssistantActivity.java creates a `Stunnel` object. Once that is done the WebView connects to `localhost:PORT` and displays the HomeAssistant web UI.

# Limitations

Currently only a small handfull of Stunnel options is implemented. You can manually use other options by calling `ServiceBuilder#setOption(String, String)`. Full list of Stunnel options: https://www.stunnel.org/static/stunnel.html
