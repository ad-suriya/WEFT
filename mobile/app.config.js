const fs = require('node:fs');
const path = require('node:path');
const appJson = require('./app.json');

module.exports = () => {
  const firebaseConfig = path.join(__dirname, 'google-services.json');
  const expo = { ...appJson.expo };

  if (fs.existsSync(firebaseConfig)) {
    expo.android = { ...expo.android, googleServicesFile: './google-services.json' };
  }

  return { expo };
};
