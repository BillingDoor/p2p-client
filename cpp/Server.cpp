#include "Server.h"

Server::Server(uint16_t port) {
  setup_socket(port);
}

Server::~Server() {
  close_socket();
}

void Server::setup_socket(uint16_t port) {
  //For setsock opt (REUSEADDR)
  int yes = 1;

  // setup socket address structure
  memset(&server_address, 0, sizeof(sockaddr_in));
  server_address.sin_family = AF_INET;
  server_address.sin_addr.s_addr = INADDR_ANY;
  server_address.sin_port = htons(port); // port

  //Avoid bind error if the socket was not close()'d last time;
  setsockopt(connection_socket, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int));


  connection_socket = socket(AF_INET, SOCK_STREAM, 0);
  if(!connection_socket) {
    std::cerr << "ERROR: Failed to create socket." << std::endl;
    exit(-1);
  }

  // set to nonblocking
  fcntl(connection_socket, F_SETFL, O_NONBLOCK);

  if(bind(connection_socket, (struct sockaddr *) &server_address, sizeof(sockaddr_in)) < 0) {
    std::cerr << "ERROR: Failed to bind socket." << std::endl;
    exit(-1);
  }

  if(listen(connection_socket, SOMAXCONN) < 0) {
    std::cerr << "ERROR: Failed to listen on socket." << std::endl;
    exit(-1);
  }
}

void Server::run() {
  ClientConnection client;
  socklen_t cliSize = sizeof(sockaddr_in);

  while(true) {

    //Blocks here;
    // TODO: change to select()
    client.socket = accept(connection_socket, (struct sockaddr *) &client_address, &cliSize);

    if(client.socket < 0) {
//      std::cerr << "Error: Failed to accept client." << std::endl;
    } else {
      std::thread conn([&](ClientConnection client) { Server::handle_client(client); }, client);
      client_connections.emplace_back(conn.native_handle());
      conn.detach();
      std::cout << "Client connected!" << std::endl;
    }
  }
}

void Server::stop() {
  for(auto &&thread : client_connections) {
    pthread_cancel(thread);
  }
}

void Server::close_socket() {
  close(connection_socket);
}

void Server::handle_client(ClientConnection client) {
  // loop to handle_client all requests
  while(true) {
    // get a request
    std::string request = get_request(client);
    // break if client is done or an error occurred
    if(request.empty()) break;
    // send response
    std::cout << "Client sent: " << request << std::endl;
    bool success = send_response(client, request);
    // break if an error occurred
    if(not success) break;
  }
  // remove client
  close(client.socket);
//  mutex.lock();
  client_connections.erase(std::remove(client_connections.begin(), client_connections.end(), pthread_self()), client_connections.end());
//  mutex.unlock();
}

std::string Server::get_request(ClientConnection client) {
  std::string request;
  // read until we get a newline
  while(request.find("\n") == std::string::npos) {
    int nread = recv(client.socket, client.buffer, sizeof(client.buffer), 0);
    if(nread < 0) {
      if(errno == EINTR)
        // the socket call was interrupted -- try again
        continue;
      else
        // an error occurred, so break out
        return "";
    } else if(nread == 0) {
      // the socket is closed
      return "";
    }
    // be sure to use append in case we have binary data
    request.append(client.buffer, nread);
  }
  // a better server would cut off anything after the newline and
  // save it in a cache
  return request;
}

bool Server::send_response(ClientConnection client, std::string response) {
  // prepare to send response
  const char *ptr = response.c_str();
  int nleft = response.length();
  int nwritten;
  // loop to be sure it is all sent
  while(nleft) {
    if((nwritten = send(client.socket, ptr, nleft, 0)) < 0) {
      if(errno == EINTR) {
        // the socket call was interrupted -- try again
        continue;
      } else {
        // an error occurred, so break out
        return false;
      }
    } else if(nwritten == 0) {
      // the socket is closed
      return false;
    }
    nleft -= nwritten;
    ptr += nwritten;
  }
  return true;
}
