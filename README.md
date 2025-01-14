# Universal/App Links

This plugin makes it easy to respond to deeplinks through custom URL schemes and Universal/App Links on iOS and Android.

## Installation

```bash
cordova plugin add ionic-plugin-deeplinks
--variable URL_SCHEME=myapp --variable DEEPLINK_SCHEME=https --variable DEEPLINK_HOST=example.com
--variable ANDROID_PATH_PREFIX=/
```

## iOS Configuration

As of iOS 9.2, Universal Links *must* be enabled in order to deep link to your app. Custom URL schemes are no longer supported.

Follow the official [Universal Links](https://developer.apple.com/library/ios/documentation/General/Conceptual/AppSearch/UniversalLinks.html) guide on the Apple Developer docs
to set up your domain to allow Universal Links.

### How to set up top-level domains (TLD's)
#### Set up Associated Domains
First you must enable the `Associated Domains` capability in your [provisioning profile](https://developer.apple.com/account/resources/profiles/list).
After that you must enable it in the Xcode project, too.
For automated builds you can do it easily by adding this to your `config.xml`.

    <config-file target="*-Debug.plist" parent="com.apple.developer.associated-domains">
        <array>
            <string>applinks:example.org</string>
        </array>
    </config-file>

    <config-file target="*-Release.plist" parent="com.apple.developer.associated-domains">
        <array>
            <string>applinks:example.org</string>
        </array>
    </config-file>

Instead of `applinks` only you could use `<string>webcredentials:example.org</string>` or `<string>activitycontinuation:example.org</string>`, too.

#### Set up Apple App Site Association (AASA)
Your website (i.e. `example.org`) must provide this both files.

* /apple-app-site-association
* /.well-known/apple-app-site-association

The content should contain your app.

    {
      "applinks": {
        "apps": [],
        "details": [
          {
            "appID": "1A234BCD56.org.example",
            "paths": [
              "NOT \/api\/*",
              "NOT \/",
              "*"
            ]
          }
        ]
      }
    }

This means that all your requests - except /api and / - will be redirected to your app.
Please replace `1A234BCD56` with your TEAM ID and `org.example` with your Bundle-ID. (the `id=""` of your `<widget />`)

## Android Configuration

Android supports Custom URL Scheme links, and as of Android 6.0 supports a similar feature to iOS' Universal Links called App Links.

Follow the App Links documentation on [Declaring Website Associations](https://developer.android.com/training/app-links/index.html#web-assoc) to enable your domain to
deeplink to your Android app.

To prevent Android from creating multiple app instances when opening deeplinks, you can add the following preference in Cordova config.xml file:

```xml
 <preference name="AndroidLaunchMode" value="singleTask" />
```

### How to set up top-level domains (TLD's)
#### Set up [Android App Links](https://developer.android.com/training/app-links)
Your website (i.e. `example.org`) must provide this file.

* /.well-known/assetlinks.json

The content should contain your app.

    [
      {
        "relation": [
          "delegate_permission\/common.handle_all_urls"
        ],
        "target": {
          "namespace": "android_app",
          "package_name": "org.example",
          "sha256_cert_fingerprints": [
            "12:A3:BC:D4:56:E7:89:F0:12:34:5A:B6:78:90:C1:23:45:DE:67:FA:89:01:2B:C3:45:67:8D:9E:0F:1A:2B:C3"
          ]
        }
      }
    ]

Replace `org.example` with your app package. (the `id=""` of your `<widget />`)
The fingerprints you can get via `$ keytool -list -v -keystore my-release-key.keystore`.
You can test it via https://developers.google.com/digital-asset-links/tools/generator.
