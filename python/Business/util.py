import os

def get_file_size(path):
    return os.stat(path).st_size

def get_file_binary_data(path, size=-1):
    with open(path, 'rb') as file:
        if size <= 0:
            file_data = file.read()
        else:
            file_data = file.read(size)
    return file_data

def write_file_from_binary_data(data, path):
    with open(path, 'wb') as file:
        file.write(data)

def chunks_generator(path, chunk_size=8192):
    with open(path, 'rb') as file:
        chunk = file.read(chunk_size)
        while chunk:
            yield chunk
            chunk = file.read(chunk_size)