# Copyright 2013 In-Q-Tel/Lab41
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

require "rubygems"
require "test/unit"
require "watir-webdriver"
 
class DendriteTest < Test::Unit::TestCase 
  def setup 
    @browser ||= Watir::Browser.new :firefox
    @browser.goto "http://localhost:8080/dendrite"
  end
   
  def teardown 
    @browser.close
  end

  def test_login_success
    @browser.text_field(:name => "username").set "user"
    @browser.text_field(:name => "password").set "password"
    @browser.button(:id => "button-login").click 
    assert @browser.text.include? 'titanexample'
  end

  def test_logout_success
    @browser.text_field(:name => "username").set "user"
    @browser.text_field(:name => "password").set "password"
    @browser.button(:id => "button-login").click 
    assert @browser.text.include?('titanexample')
    @browser.button(:id => "button-logout").click 
    @browser.alert.close
    assert @browser.text.include?('titanexample') != true
  end
  
  def test_login_failure
    @browser.text_field(:name => "username").set "USER"
    @browser.text_field(:name => "password").set "FAIL"
    @browser.button(:id => "button-login").click 
    @browser.alert.close
    assert @browser.text.include?('titanexample') != true
  end
end
