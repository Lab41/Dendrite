basePath = '../../../../';

files = [
  JASMINE,
  JASMINE_ADAPTER,
  './src/main/webapp/lib/angular/angular.js',
  './src/main/webapp/lib/angular/angular-*.js',
  './src/test/javascript/test/lib/angular/angular-mocks.js',
  './src/main/webapp/js/**/*.js',
  './src/test/javascript/test/unit/**/*.js'
];

autoWatch = true;

browsers = ['Chrome'];

junitReporter = {
  outputFile: './target/js_test_out/unit.xml',
  suite: 'unit'
};
