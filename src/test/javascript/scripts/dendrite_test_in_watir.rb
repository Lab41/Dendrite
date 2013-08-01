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
