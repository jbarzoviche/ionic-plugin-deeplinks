#import <Cordova/CDVPlugin.h>

@interface DeeplinkPlugin : CDVPlugin {
  NSMutableArray *_handlers;
  CDVPluginResult *_lastEvent;
}

- (void)canOpenApp:(CDVInvokedUrlCommand *)command;
- (void)onDeepLink:(CDVInvokedUrlCommand *)command;
- (void)getHardwareInfo:(CDVInvokedUrlCommand *)command;

- (BOOL)handleLink:(NSURL *)url;
- (BOOL)handleContinueUserActivity:(NSUserActivity *)userActivity;

- (void)sendToJs;

- (CDVPluginResult*)createResult:(NSURL *)url;

@end