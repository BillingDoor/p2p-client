#include <iostream>

#include "Client.h"
#include "Server.h"


int main() {
  char c;
  std::cin >> c;
  if(c == 'c') {
    std::cout << "Creating client..." << std::endl;
    Client client = Client("127.0.0.1", 8999);
    client.run();
//    while(std::cin >> c) {
//      if(c == 'x') {
//        client.stop();
//        break;
//      }
//    }
  } else if(c == 's') {
    std::cout << "Starting server..." << std::endl;
    Server server = Server(8999);
//    std::thread server_thread(&Server::run, server);
    std::thread server_thread([&] (Server * server) { server->run(); }, &server);
    pthread_t handle = server_thread.native_handle();
    server_thread.detach();
    std::cout << "waiting for end signal" << std::endl;
    while(std::cin >> c) {
      if(c == 'x') {
        std::cout << "kill accepting" << std::endl;
        pthread_cancel(handle);
        std::cout << "kill handlers" << std::endl;
        server.stop();
        break;
      }
      std::cout << "waiting for end signal" << std::endl;
    }
  }
  return 0;
}