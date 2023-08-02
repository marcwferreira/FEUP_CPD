# Game Description

Our project is a unique twist on the popular game "Cards Against Humanity." In this turn-based game, players are challenged to complete part of a given sentence. Each player, except for one designated as the 'judge,' submits their phrase. The judge's role is to determine the most creative response among all the players. At the end of each round, the winning player earns a point, the judge role rotates to another player, and the process repeats. The player that reaches a score of 10 points first wins the game.

We offer two different game types for players: ranked and casual gameplay. In the casual play version, players are randomly assigned to games from a queue, this is just for fun and will not update the player rank. In ranked play, players are matched with games based on their rank and the game's rankc (which is determined by the player who created the game). If there are no games available that match a player's rank, we increase the rank difference tolerance in intervals of approximately 20 seconds. As players win more games, their ranks will also go up.

## Running The Program

To run the game, follow these steps:

1. From the `assign2` folder, execute the appropriate bash files according to your operating system:
   - **Server:**
     - Linux: `sudo ./run_server.sh`
     - Windows: `run_server.bat` (if the above command doesn't work)
   - **Client:**
     - Linux: `sudo ./run_single_client.sh`
     - Windows: `run_server.bat` (if the above command doesn't work)

2. For testing purposes, you can run multiple clients simultaneously by executing the `run_four_clients.sh` bash file. Please note that this option is only available on Linux and requires gnome-terminal. You may also need to install `dbus-x11` manually using the command: `sudo apt-get install dbus-x11`.


The bash files automatically compile and run the server and client. When running the client using the bash file, you need to specify a file to store the game data. This prevents conflicts between multiple clients accessing the same JSON file. You can choose from the following pre-prepared files: `info`, `info1`, `info2`, and `info3`. If you specify a filename that doesn't exist, the client will attempt to create a new file.

**OBS:** The windows files might not work if javac does not work, in this case please compile and run manually (instructions below).

If you prefer to manually compile and run the programs, here are the commands:

### Server:
From the `assign2` folder, execute the following commands:
1. `cd src/server_src`
2. `javac -cp ../lib/json-simple-1.1.1.jar -d classes server/*.java ../myutils/*.java`
3. `java -classpath ../lib/json-simple-1.1.1.jar:classes:.:classes.myutils:classes.server server.GameServer [SERVER PORT]`

### Client:
From the `assign2` folder, execute the following commands:
1. `cd src/client_src`
2. `javac -cp ../lib/json-simple-1.1.1.jar -d classes client/*.java ../myutils/*.java`
3. `java -classpath ../lib/json-simple-1.1.1.jar:classes:.:classesmyutils:classes.client client.GameClient [SERVER IP] [SERVER PORT] <FILE NAME> <unsafe>`

**Explanation:**

- `[ARG]` indicates a required argument.
- `<ARG>` indicates an optional argument.
- `<unsafe>` should be replaced with the word "unsafe." Using this option skips the restore part of the game, allowing direct access to the login menu without automatic login using a token.

## Menu Options

1. **Join Casual Queue**: Join the casual queue and wait for a new game to start.
2. **Join Ranked Queue**: Join the ranked queue and wait for a new game to start.
3. **Create Game**: Create a new game with 4 to 12 players.
4. **Rejoin Game**: Rejoin a previous game if possible. This option is useful if you lost connection or left the game but want to join again. Note that rejoining is not guaranteed and depends on the server's response.
5. **Show Rank**: Display the current number of wins, representing the player's ranked status.
6. **Change Password**: Change your current password to a new one.
0. **Log Out**: Log out from the server.

### Extra Options

- 1: **Leave Queue**: While in a queue, you can choose to leave it by sending the command "1". The server will verify if the action is possible and remove you from the queue. Please note that this may fail if the server has already assigned you to a game.
- **@leave**: At any point during the game, you can leave by sending this command to the server.

## Game Rules

- Each round has a submission time limit of 1 minute for players to enter their phrases.
- Each round also has a voting time limit of 1 minute for players to choose the most creative response from the submissions. 

These time limits ensure that the game progresses smoothly and prevent players from delaying the game indefinitely.

## Queue and Player Disconnection Handling

During the game, if a player loses connection, we have implemented a system to handle their disconnection appropriately:

- If a player loses connection, we keep them in the queue for joining a game for a period of 5 minutes.
- If the player reconnects within this 5-minute window, they will be able to rejoin the queue and keep their place in it.
- However, if the 5-minute time limit expires without the player reconnecting, they will be removed from the queue. While the player remains offline, but within the 5 minutes they will still remain in the queue, but the matchmaking system will skip over them when assigning games.

This ensures that players who experience temporary disconnections have the opportunity to rejoin without losing their spot at the queue while also preventing problems for other players due to inactive or disconnected participants.

## Client Restore System and Rejoining a Game

The client program includes a restore system that automatically attempts to log in using a token when the program is run again. If the login is successful and the server confirms that the player still has a spot in the queue, the player will be redirected to the queue. If the player doesn't have a spot in the queue, they will be taken to the main menu. If login is not possible they will go to the login menu.

From the main menu, players also have the option to attempt to rejoin a game they previously left or got disconnected from. This option is useful if a player wants to rejoin a game after a wrongly leaving it or after a connection loss. Note that rejoining a game is not guaranteed and depends on the server's response.

 The necessary tokens and login information will be stored by the client into a json file to enable the automatic login and rejoin functionality.

## Connection Handler in the Server

The server incorporates a connection handler to effectively manages player connections during the game. Here's an overview of how it works:

- **Tracking Player Sockets**: The connection handler keeps track of the players' sockets to verify their connection status. By maintaining a record of active sockets, the server can ensure communication with the players.

- **Last Online Detection System**: Monitor player activity and ensure they are still online, the server uses a last online detection system. This system periodically checks whether players are still active and update their timestamps. If a player is found to be inactive, their socket entry is removed, indicating that they are offline.

- **Token Validation**: The timestamp associated with each player's connection is also utilized to validate the login token's validity. The token remains valid for 5 minutes after the player goes offline. By comparing timestamps, the server verifies whether a player's token is still valid.

The connection handler plays a crucial role in ensuring that players' clients can seamlessly recuperate in the event of temporary disconnections.

## Game Recovery and JSON Files

Our game has a way of not losing games if the server itself goes down, it stores each game as a JSON file. This approach allows us to recover games in progress, although the current round in progress may be lost.

When a game is created, a corresponding JSON file is generated to store the game state. This file serves as a snapshot of the game at a specific point in time.

In the event of the server going down or unexpected shutdown, we can utilize these JSON files to recover the game. Upon restarting the server, the system will attempt to load the most recent game files and allow players to rejoin the game (from the game code the client stores for them).

To prevent infiite incomplete games if the game does not start in 5 minutes the server will assume the game was abandoned and close the game and delete its files.

However, it's important to note that while game recovery enables players to continue where they left off, the round that was being played at the time of the server closing will be lost.

## Made with love ❤️ by

- Fábio Morais - up202008052
- Filipe Fonseca - up202003474
- Marcos Ferreira - up201800177
