var person = {};
var hockeyTeam = { name: 'WolfPack' };

person.first = 'Ian';
person['last'] = 'Bull';
person.hockeyTeam = hockeyTeam;
console.log(hockeyTeam);
console.log(person);

var ptName = person.hockeyTeam.name;
console.log(ptName);

var name = hockeyTeam.name;

hockeyTeam.captain = person;

var same = person === hockeyTeam.captain;
console.log(same);

