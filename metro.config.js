const { getDefaultConfig } = require('expo/metro-config');
const path = require('path');

const config = getDefaultConfig(__dirname);

config.resolver.blockList = [
  new RegExp(`${path.resolve(__dirname, 'android-legacy').replace(/\\/g, '\\\\')}/.*`),
  new RegExp(`${path.resolve(__dirname, 'baselineprofile').replace(/\\/g, '\\\\')}/.*`),
  new RegExp(`${path.resolve(__dirname, '.gradle').replace(/\\/g, '\\\\')}/.*`),
];

module.exports = config;
