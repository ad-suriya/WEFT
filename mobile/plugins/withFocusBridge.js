const { withAndroidManifest, withMainActivity } = require('@expo/config-plugins');
const { mergeContents } = require('@expo/config-plugins/build/utils/generateCode');

module.exports = function withFocusBridge(config) {
  config = withAndroidManifest(config, config => {
    const permissions = config.modResults.manifest['uses-permission'] || [];
    for (const name of ['android.permission.ACCESS_NOTIFICATION_POLICY', 'android.permission.SYSTEM_ALERT_WINDOW']) {
      if (!permissions.some(p => p.$?.['android:name'] === name)) permissions.push({ $: { 'android:name': name } });
    }
    config.modResults.manifest['uses-permission'] = permissions;
    return config;
  });

  // MainActivity is singleTask (for the weft:// deep link scheme), so a repeat launch calls
  // onNewIntent instead of onCreate — without forwarding the new intent here, React Native's
  // Linking module never sees deep links sent while the app is already running, such as the
  // "weft://blocked?..." link the app-blocking Accessibility service opens. This mod re-applies
  // that override on every prebuild, since android/ itself isn't checked into git.
  return withMainActivity(config, config => {
    config.modResults.contents = mergeContents({
      tag: 'weft-focus-bridge-onNewIntent',
      src: config.modResults.contents,
      newSrc: [
        '  override fun onNewIntent(intent: Intent) {',
        '    super.onNewIntent(intent)',
        '    setIntent(intent)',
        '  }',
        '',
      ].join('\n'),
      anchor: /override fun getMainComponentName/,
      offset: 0,
      comment: '//',
    }).contents.replace(
      /^import android\.os\.Bundle$/m,
      'import android.content.Intent\nimport android.os.Bundle',
    );
    return config;
  });
};
