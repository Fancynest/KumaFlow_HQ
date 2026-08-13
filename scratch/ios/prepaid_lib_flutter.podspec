#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint unik_lib_flutter.podspec' to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'prepaid_lib_flutter'
  s.version          = '0.0.6'
  s.summary          = 'This is plugin for get balance and update balance.'
  s.description      = <<-DESC
A Library Prepaid Flutter
                       DESC
  s.homepage         = 'http://example.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '13.0'
  s.preserve_paths = 'prepaid_lib_ios.framework'
  s.xcconfig = { 'OTHER_LDFLAGS' => '-framework prepaid_lib_ios' }
  s.vendored_frameworks = 'prepaid_lib_ios.framework'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
