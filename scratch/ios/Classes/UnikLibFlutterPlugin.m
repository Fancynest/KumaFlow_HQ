#import "UnikLibFlutterPlugin.h"
#if __has_include(<prepaid_lib_flutter/prepaid_lib_flutter-Swift.h>)
#import <prepaid_lib_flutter/prepaid_lib_flutter-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "prepaid_lib_flutter-Swift.h"
#endif

@implementation UnikLibFlutterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftUnikLibFlutterPlugin registerWithRegistrar:registrar];
}
@end
