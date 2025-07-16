local data = redis.call('HGETALL', KEYS[1])
redis.call('DEL', KEYS[1])
return data