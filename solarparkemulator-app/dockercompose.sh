sudo docker rm $(sudo docker ps -a -q)
sudo docker-compose up --remove-orphans
