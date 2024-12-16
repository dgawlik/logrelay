docker run -d \
  --user=root \
  --network=host \
  --volume="/home/john/Code/logrelay/logs:/data" \
  --volume="/home/john/Code/logrelay/filebeat.docker.yml:/usr/share/filebeat/filebeat.yml:ro" \
  --volume="/var/lib/docker/containers:/var/lib/docker/containers:ro" \
  --volume="/var/run/docker.sock:/var/run/docker.sock:ro" \
  --volume="registry:/usr/share/filebeat/data:rw" \
  docker.elastic.co/beats/filebeat:8.17.0 filebeat -e --strict.perms=false \
  -E output.elasticsearch.hosts=["localhost:9200"]