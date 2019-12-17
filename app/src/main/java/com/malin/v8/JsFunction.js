var hockeyTeam = {
     name: 'WolfPack',
     players: [],
     addPlayer: function (player) {
          this.players.push(player);
          return this.players.size;
     }
}