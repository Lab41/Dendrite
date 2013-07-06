#!/usr/bin/env watchr

require 'fileutils'

# config file for watchr http://github.com/mynyml/watchr
# install: gem install watchr
# run: watch watchr.rb
# note: make sure that you have jstd server running (server.sh) and a browser captured

log_file = File.expand_path(File.dirname(__FILE__) + '/../../../target/logs/jstd.log')

Dir.chdir '../../..'

FileUtils.mkdir_p(File.dirname log_file)
FileUtils.touch log_file

puts "String watchr... log file: #{log_file}"

watch( '(src/main/webapp/js|src/test/javascript/test/unit)' )  do
  `echo "\n\ntest run started @ \`date\`" > #{log_file}`
  `scripts/test.sh &> #{log_file}`
end

