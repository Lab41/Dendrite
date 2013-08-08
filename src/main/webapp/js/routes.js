(function(exports){

    var config = {
      // all roles (max of 31 before the bit shift pushes the accompanying integer out of the memory footprint for an integer
      roles :[
          'ROLE_PUBLIC',
          'ROLE_USER',
          'ROLE_ADMIN'],

      // access levels referencing roles above
      accessLevels : {
        'ROLE_PUBLIC' : "*",
        'ROLE_ANON': ['ROLE_PUBLIC'],
        'ROLE_USER' : ['ROLE_USER', 'ROLE_ADMIN'],
        'ROLE_ADMIN': ['ROLE_ADMIN']
      }
    }

    exports.userRoles = buildRoles(config.roles);
    exports.accessLevels = buildAccessLevels(config.accessLevels, exports.userRoles);

    // distinct bit mask for each role
    // begin with "1" and shifts the bit to the left for each element in the roles array parameter
    function buildRoles(roles){

      var bitMask = "01";
      var userRoles = {};

      for(var role in roles){
          var intCode = parseInt(bitMask, 2);
          userRoles[roles[role]] = {
              bitMask: intCode,
              title: roles[role]
          };
          bitMask = (intCode << 1 ).toString(2)
      }

      return userRoles;
    }

    // build access level bit masks
    // masks based on the accessLevelDeclaration parameter, which must contain an array for each access level containing the allowed roles
    function buildAccessLevels(accessLevelDeclarations, userRoles){

      var accessLevels = {};
      for(var level in accessLevelDeclarations){

        if(typeof accessLevelDeclarations[level] == 'string'){
          if(accessLevelDeclarations[level] == '*'){

            var resultBitMask = '';

            for( var role in userRoles){
                resultBitMask += "1"
            }
            //accessLevels[level] = parseInt(resultBitMask, 2);
            accessLevels[level] = {
                bitMask: parseInt(resultBitMask, 2),
                title: accessLevelDeclarations[level]
            };
          }
          else console.log("Access Control Error: Could not parse '" + accessLevelDeclarations[level] + "' as access definition for level '" + level + "'")

        }
        else {

          var resultBitMask = 0;
          for(var role in accessLevelDeclarations[level]){
            if(userRoles.hasOwnProperty(accessLevelDeclarations[level][role]))
                resultBitMask = resultBitMask | userRoles[accessLevelDeclarations[level][role]].bitMask
            else console.log("Access Control Error: Could not find role '" + accessLevelDeclarations[level][role] + "' in registered roles while building access for '" + level + "'")
          }
          accessLevels[level] = {
            bitMask: resultBitMask,
            title: accessLevelDeclarations[level][role]
          };
        }
      }

      return accessLevels;
    }

})(typeof exports === 'undefined' ? this['routingConfig'] = {} : exports);
