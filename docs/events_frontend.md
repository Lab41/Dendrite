### Frontend Events
AngularJS `$broadcast` and `$emit`  messages, paired with `$on` triggers, enables modular frontend components to communicate state changes and other required actions to each other.

#### Authentication
event | meaning
------|--------|---------
loginRequired | User needs to login
loggedIn | Successful user login
logoutConfirmed | User successfully logged out
returnHome | Redirect user to homepage
loginRequest | (deprecated) Request for user login
loginConfirmed | (deprecated) Successful user login
logoutRequest | (deprecated) Request for user logout

#### Data Changes
event | meaning
------|--------|---------
graphFileParsed | Graph file/keys parsed on frontend
graphFileImported | Successful file upload/import
projectHasData | DB has a graph for the current project
reloadGraph | Graph (nodes;edges) has changed and frontend needs to reload/redraw
reloadProjectNeeded | Project (branch; graph) has changed and frontend needs to reload/redraw
pollBranches | Re-poll available branches for current project
pollActiveAnalytics | Re-poll jobs and pending/complete/error status
