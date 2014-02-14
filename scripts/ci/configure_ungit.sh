#!/bin/bash
_targetDir=$1

if [ -z "$_targetDir" ]
then
  echo "Usage: $0 <targetDir>"
else
  echo "Default GitHub User:"
  read _gitUser

  echo "GitHub Server Address:"
  read _gitServer

  echo "Local Directory for GitHub Repos:"
  read _gitDir

  # escape variables
  _gitDir=${_gitDir//\//\\\/}

  echo "Updating configuration variables"
  sed -i "" "s/user: <FIXME.*>/user: '$_gitUser'/g" $_targetDir/public/source/app.js
  sed -i "" "s/local: <FIXME.*>/local: '$_gitServer'/g" $_targetDir/public/source/app.js
  sed -i "" "s/remote: <FIXME.*>/remote: '$_gitDir'/g" $_targetDir/public/source/app.js
fi
