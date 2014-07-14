// Tour templates vary according to the presence/absence of navigation buttons,
var tourTemplateDefault = function() {
  return "<div class='popover tour'>\
  <div class='arrow'></div>\
  <h3 class='popover-title'></h3>\
  <div class='popover-content'></div>\
  <div class='popover-navigation'>\
    <button class='btn btn-default' data-role='end'>End tour</button>\
  </div>\
</div>"
};

var tourTemplateWithNav = function() {
  return "<div class='popover tour'>\
  <div class='arrow'></div>\
  <h3 class='popover-title'></h3>\
  <div class='popover-content'></div>\
  <div class='popover-navigation'>\
    <button class='btn btn-default' data-role='end'>End tour</button>\
    <div class='prev-next-wrapper'>\
      <button class='btn btn-primary' data-role='prev'>« Prev</button>\
      <button class='btn btn-primary' data-role='next'>Next »</button>\
    </div>\
  </div>\
</div>"
};

var tourTemplateNextOnly = function(labelStop, labelNext) {
  return "<div class='popover tour'>\
  <div class='arrow'></div>\
  <h3 class='popover-title'></h3>\
  <div class='popover-content'></div>\
  <div class='popover-navigation'>\
    <button class='btn btn-default' data-role='end'>"+labelStop+"</button>\
    <div class='prev-next-wrapper'>\
      <button class='btn btn-primary' data-role='next'>"+labelNext+"</button>\
    </div>\
  </div>\
</div>"
};

// catch-all function in case user restarts tour after logging in
var tourRestart = function(tour) {
  if ($('input[name="username"]').length === 0) {
    window.location.href = 'j_spring_security_logout';
  }
  tour.goTo(0);
  tour.restart();
};

var tour = new Tour({
  storage: window.localStorage,
  keyboard: true,
  name: 'dendrite-tour',
  template: tourTemplateDefault(),
  defaultTimeout: 250,
  defaultSelectorPrefix: '#tour-step-',
  steps: [
  {
    path: '/dendrite/#/home',
    title: "Welcome!",
    content: '<p>To begin using Dendrite, take a few minutes with this guided tour.</p>\
              <p>It doesn\'t cover everything, but provides a good foundation to:</p>\
              <ul>\
                <li>Import graph data</li>\
                <li>Explore a social network</li>\
                <li>Identify key people through graph analytics</li>\
              </ul>\
              <p class="well">\
                <strong>We will call out actions you need to take in this highlighted box.</strong>\
              </p>',
    backdrop: true,
    orphan: true,
    template: tourTemplateNextOnly("No Thanks", "Start")
  },
  {
    element: "#tour-step-1",
    placement: "bottom",
    title: "Login",
    content: '<p>To login, enter:</p>\
              <ul class="well tour-well">\
                <li>Username: <strong>user</strong></li>\
                <li>Password: <strong>password</strong></li>\
              </ul>',
    onShown: function (tour) {
      $('input[name="username"]').focus();
    },
    onNext: function(tour) {
      var userIn = $('input[name="username"').val();
      var passIn = $('input[name="password"').val();
      if (userIn !== "user" || passIn !== "password") {
        this.next = tour.getCurrentStep();
      }
      else {
        tourLoggedIn();
      }
    },
    reflex: true
  },
  {
    element: "#tour-step-2",
    placement: "bottom",
    title: "Project List",
    content: 'Organize your work into different projects.  That way, you can more easily track different networks or types of calculations, but still share work among your team.\
              <p class="well">\
                Go ahead and <strong>Create</strong> a new project.\
              </p>',
    reflex: true
  },
  {
    element: "#tour-step-3",
    placement: "bottom",
    title: "New Project",
    content: 'Project names help you track the type of data or purpose behind the project.\
              <p class="well">\
                Give your new project a name.\
              </p>',
    reflex: true
  },
  {
    title: "Project Overview",
    content: '<p>This page is where you will:\
              <ul>\
                <li>Manage data</li>\
                <li>Work with different versions of your project</li>\
                <li>Get a few quick visual overviews of the network</li>\
              </ul>\
              <p class="well">\
                Download the <strong><a target="_blank" href="https://raw.githubusercontent.com/Lab41/Dendrite/master/src/test/javascript/data/les-mis3.json">test dataset</a></strong> before continuing.\
              </p>',
    backdrop: true,
    orphan: true,
    template: tourTemplateNextOnly("End Tour", "Continue")
  },
  {
    element: "#tour-step-5",
    placement: 'bottom',
    title: "Import Graph",
    content: '<p>Dendrite can handle four standard graph formats to import and export.  To import the test dataset...</p>\
              <ol class="well tour-well">\
                <li>Select the <strong>GraphSON</strong> format</li>\
                <li>Click the <strong>Select file</strong> button and choose the downloaded file</li>\
              </ol>',
    onNext: function(tour) {
      tourGoToNextStep();
    }
  },
  {
    element: "#tour-step-6",
    placement: 'bottom',
    title: "Index Data",
    content: '<p>To index data for better search, Dendrite inspected the file and lets you select the correct data type for each field(s).</p>\
              <p class="well">\
                Change the <strong>location</strong> field to a <strong>geocoordinate</strong> type before clicking the <strong>Load Graph</strong> button.\
              </p>',
    reflex: true,
    onNext: function(tour) {
      tourGoToNextStep();
    },
  },
  {
    element: "#tour-step-7",
    placement: 'bottom',
    title: "Project Versions",
    content: '<p>You can organize your project into different versions (also called Branches).  These allow you to experiment with ideas, such as removing data or running different analytics, without affecting other versions.</p>',
    template: tourTemplateWithNav()
  },
  {
    element: "#tour-step-8",
    placement: 'top',
    title: "Quick Visuals",
    content: '<p>You can also get a quick feel for your graph with several basic visualizations of the data.\
              <p class="well">\
                Go ahead and click through the different tabs before continuing.\
              </p>',
    template: tourTemplateWithNav()
  },
  {
    element: "#tour-step-9",
    placement: 'bottom',
    title: "Data Explorer",
    content: '<p>The main way to edit your data and run analytics will be through the data explorer.\
              <p class="well">\
                Click the <strong>People</strong> button to begin exploring.\
              </p>',
    reflex: true,
    onNext: function(tour) {
      tourGoToNextStep();
    },
  },
  {
    element: "#tour-step-10",
    placement: 'bottom',
    title: "Data Explorer",
    content: '<p>The data explorer organizes your graph in a familiar spreadsheet format. You can click on any column heading to sort that field.  You can also search for specific data.\
              <p class="well">\
                Go ahead and type <strong>Valj*</strong> into the search box to test search features.\
              </p>',
    template: tourTemplateWithNav()
  },
  {
    element: "#tour-step-11",
    placement: 'bottom',
    title: "Analytics",
    content: '<p>Dendrite makes it easy to perform a variety of graph analytics.\
              <p class="well">\
                Click the <strong>Analytics</strong> button to begin the analytics process.\
              </p>',
    reflex: true,
    onNext: function(tour) {
      tourGoToNextStep();
    },
  },
  {
    element: "#tour-step-12",
    placement: 'bottom',
    title: "Analytics",
    content: '<p>Analytic calculations are categorized according to each type of purpose.</p>  <p>You can look through the tabs to get a feel for each category, analytics available, and how some require specific input parameters.</p>\
              <p class="well">\
                Choose the <strong>PageRank</strong> calculation.  You can leave the default parameters or change them if you prefer.\
              </p>',
    reflex: true
  },
  {
    element: "#tour-step-13",
    placement: 'top',
    title: "PageRank",
    content: '<p>With the click of a button, Dendrite performs the calculation using the appropriate analytic engine, which could be GraphLab, JUNG, Faunus, or GraphX.\
              <p class="well">\
                Go ahead and submit the calculation.\
              </p>',
    reflex: true
  },
  {
    element: "#tour-step-14",
    placement: 'bottom',
    title: "Analytic Results",
    content: '<p>The output of each calculation becomes part of your dataset.\
              <p class="well">\
                Click on the <strong>PageRank</strong> column heading to sort the data according to each node\'s calculated score.\
              </p>',
    template: tourTemplateWithNav()
  },
  {
    title: "Congratulations!",
    content: '<p>You\'re done with the tour!  We only walked you through a small subset of features, so be sure to continue exploring.</p>\
              <p class="well">\
                Visit the <strong><a target="_blank" href="http://www.github.com/lab41/Dendrite">Dendrite</a></strong> project on GitHub if you want to contribute. Thanks!\
              </p>',
    backdrop: true,
    orphan: true
  }
]});

var tourTimeout = function() {
  return tour._options.defaultTimeout;
};

var tourSelectorPrefix = function() {
  return tour._options.defaultSelectorPrefix;
};

// function necessary to sync JQuery tour with AngularJS $scope refreshes
var tourGoToNextStep = function() {

  // end the tour to prevent tour from continuing in the absence of a step's on-page selector
  tour.end();
  tourHide();

  // set the next step and calculate the on-page selector
  //  (IMPORTANT: this requires selectors of the format: id="tour-step-X"
  var stepNum = tour.getCurrentStep()+1;
  var el = tourSelectorPrefix() + stepNum;

  // block until element exists on page
  //  (IMPORTANT: in AngularJS, use ng-if directives instead of ng-show/hide to block
  //    rendering until backend actions complete)
  if ($(el).length === 0) {
    setTimeout(function() {
        tourGoToNextStep(stepNum);
    }, 2 * tourTimeout());
  }
  else {
    tour.setCurrentStep(stepNum);
    setTimeout(function() {
      tour.start(true);
    }, tourTimeout());
  }
};

var tourLoggedIn = function() {
  tour.setCurrentStep(1);
  tourGoToNextStep();
};

var tourHide = function() { $('.tour').hide(); };
var tourShow = function() { $('.tour').show(); };

var tourFileSelected = function() {
  setTimeout(function() {
    tourGoToNextStep();
  }, tourTimeout());
};


// Initialize and start the tour
$(document).ready(function() {
  tour.init();
  tour.start();
});

// bind to AngularJS
$(document).on("click", ".tour-start", function() {
  tourRestart(tour);
});
