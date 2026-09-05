const { withAndroidManifest } = require('@expo/config-plugins');

module.exports = function withFocusBridge(config) {
  return withAndroidManifest(config, config => {
    const permissions = config.modResults.manifest['uses-permission'] || [];
    if (!permissions.some(p => p.$?.['android:name'] === 'android.permission.ACCESS_NOTIFICATION_POLICY')) {
      permissions.push({ $: { 'android:name': 'android.permission.ACCESS_NOTIFICATION_POLICY' } });
    }
    config.modResults.manifest['uses-permission'] = permissions;
    return config;
  });
};
