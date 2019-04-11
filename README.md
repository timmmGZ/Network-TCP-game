# TCP chatroom with private chat function and a small betting game  
# Chat room
1. start the server(Server.java)
2. open some client(Player.java), before joining the chatroom, you have to enter the IP of the Server e.g.127.0.0.1 (chatroom Server)  
2.1.and then if you are first one join the chatroom, then set your nickname e.g. Jay, and then your playname in chatroom becomes"Jay 127.0.0.1:53248", unique "ip:port" is necessary in my game, just incase of two member have same name "Jay".  
2.2.if you are not the first one, then enter who e.g. C(Introducing Agent) invited you to this chatroom, and then set your nickname  
3. now you can chat in public chatroom, if you want to send private msg to some one, do like this @[Jay 127.0.0.1:53248]--hi how are you?  
4. try to send 'COMMAND' to learn how to play with this simple game  
# Join Game
The game's idea is: each player can play with other player only once, it is a counting-backwards game, for example Player A and Player B
match in a duel, counting is set to be starting from A or B randomly, and both A,B players will know who start counting first, player A 
sends to player B number 3, B sends to A number 2, count backwards from 5(3+2), if counting starts from A, then A wins(start--A B A B A--end),
in the case of cheating(because both Players know who start first), so when A send 3 to B, B will receive "8(encrypted)", they will only
get each others real number after the duel finish, and will be told the encryptFunction for fairness.

1. Members in this chatroom have two identities(either only chat member or both player and chat member), if some chat member want to 
   join the game, send JOIN to its Introducing Agent e.g. @[Jay 127.0.0.1:53248]--JOIN, for the first one who join the chatroom, since
   your Introducing Agent is yourself, also send like @[yournickname 127.0.0.1:53248]--JOIN to yourself.
2. For play room, players will be matching automatically to the one who is not in a duel, to send number to current enemy, send e.g."$168"
  sending 'COMMAND' will show you more about the rules of the game.
# HTTP Server
Try to create more players and play with yourself, and then go to http://127.0.0.1:8080/ to check to result of the whole play room.
