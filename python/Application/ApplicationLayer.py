from python.utils.StatusMessage import StatusMessage
import asyncio
import logging.handlers
import os
import sys
import re

logging.basicConfig(
    level=logging.DEBUG,
    format='%(name)s: %(message)s',
)
handler = logging.handlers.RotatingFileHandler(
    os.path.abspath("./logs/log.txt"),
)

log = logging.getLogger(__name__)

class Application:
    def __init__(self, lower_layer):
        self._lower_layer = lower_layer

    def run(self):
        """
        Run application
        """
        asyncio.get_event_loop().run_until_complete(self._aio_readline())
        self._shutdown()

    def _shutdown(self):
        asyncio.get_event_loop().run_until_complete(self._lower_layer.stop_server())
        loop = asyncio.get_event_loop()

        pending = asyncio.Task.all_tasks()
        for task in pending:
            task.cancel()
            loop.run_until_complete(task)

    def _print_main_menu(self):
        print("="*30)
        print("{:^30}".format('P2P Application'))

    def _print_connected_menu(self):
        self._print_main_menu()
        print("1. Print routing table")
        print("2. Send file request")
        print("3. Send command")
        print("4. Ping")
        print("5. Ping all")
        print("quit. Quit the application")

    def _print_not_connected_menu(self):
        self._print_main_menu()
        print("1. Join the network")
        print("quit. Quit the application")

    async def _print_routing_table(self):
        routing_table_info = await self._lower_layer.get_routing_table_info()
        print("|{:^3}|{:^30}|{:^19}|{:^5}|{:^5}".format("ID","ID", "IP", "Port", "Is NAT"))
        for index, peer_info in zip(range(len(routing_table_info)),routing_table_info):
            print("|{:^3}|{:^30}|{:^19}|{:^5}|{:^5}".format(index, *peer_info))

    async def _send_file_request(self):
        print("Index: ")
        index = (await asyncio.get_event_loop().run_in_executor(None, sys.stdin.readline)).strip().lower()
        index = int(index)
        print("Filename: ")
        filename = (await asyncio.get_event_loop().run_in_executor(None, sys.stdin.readline)).strip()
        routing_table_info = await self._lower_layer.get_routing_table_info()
        if index >= len(routing_table_info):
            print("Wrong index")
            return
        await self._lower_layer.file_request(routing_table_info[index][0], filename)

    async def _ping(self):
        print("Index: ")
        index = (await asyncio.get_event_loop().run_in_executor(None, sys.stdin.readline)).strip().lower()
        index = int(index)
        routing_table_info = await self._lower_layer.get_routing_table_info()
        if index >= len(routing_table_info):
            print("Wrong index")
            return
        await self._lower_layer.ping(routing_table_info[index][0])

    async def _ping_all(self):
        await self._lower_layer.ping_all()

    async def _send_command(self):
        print("Index: ")
        index = (await asyncio.get_event_loop().run_in_executor(None, sys.stdin.readline)).strip().lower()
        index = int(index)
        print("Command: ")
        command = (await asyncio.get_event_loop().run_in_executor(None, sys.stdin.readline)).strip()
        print("Should respond?[y/n]: ")
        should_respond = (await asyncio.get_event_loop().run_in_executor(None, sys.stdin.readline)).strip()
        if should_respond.lower() == 'y':
            should_respond = True
        else:
            should_respond = False

        routing_table_info = await self._lower_layer.get_routing_table_info()
        if index >= len(routing_table_info):
            print("Wrong index")
            return
        await self._lower_layer.command(routing_table_info[index][0], command, should_respond)

    async def _aio_readline(self):
        connected = False
        try:
            while True:
                if connected:
                    self._print_connected_menu()
                    line = (await asyncio.get_event_loop().run_in_executor(None, sys.stdin.readline)).strip().lower()
                    if line == '1':
                        # Print routing table
                        await self._print_routing_table()
                    elif line == '2':
                        await self._print_routing_table()
                        await self._send_file_request()
                    elif line == '3':
                        await self._print_routing_table()
                        await self._send_command()
                    elif line == '4':
                        await self._print_routing_table()
                        await self._ping()
                    elif line == '5':
                        await self._ping_all()
                    elif line == 'quit':
                        await self._lower_layer.leave()
                        print("Quiting...")
                        await asyncio.sleep(3)
                        await self._lower_layer.stop_server()
                        break
                else:
                    self._print_not_connected_menu()
                    line = (await asyncio.get_event_loop().run_in_executor(None, sys.stdin.readline)).strip().lower()
                    if line == '1':
                        print("=" * 30)
                        print("{:^25}".format("Joining the network"))
                        print("Input IP of the bootstrap node(or None if this is a bootstrap node):")
                        ip = await asyncio.get_event_loop().run_in_executor(None, sys.stdin.readline)
                        if ip.strip() == 'None':
                            status = await self._lower_layer.join_network(None)
                            if status is StatusMessage.SUCCESS:
                                connected = True
                        elif re.match(
                                r'^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$',
                                ip.strip()):
                            print("Input port of the bootstrap node: ")
                            port = await asyncio.get_event_loop().run_in_executor(None, sys.stdin.readline)
                            if re.match(
                                    r'^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$',
                                    port.strip()):
                                status = await self._lower_layer.join_network((ip.strip(), int(port.strip())))
                                if status is StatusMessage.SUCCESS:
                                    connected = True
                                else:
                                    print("Failed to connect to the bootstrap node")
                            else:
                                print("Invalid port number: {}".format(port.strip()))
                        else:
                            print("Invalid ip address {}".format(ip.strip()))
                    if line == 'quit':
                        await self._lower_layer.stop_server()
                        break
        except asyncio.CancelledError:
            return
