sudo docker build -t dockerfile .
sudo docker run -v /etc/timezone:/etc/timezone:ro -v /home/tioaki02/MEGAsync/eclipseWorkspace/solarparkemulator-app/data:/code/src/main/resources:rw -v /home/tioaki02/MEGAsync/eclipseWorkspace/solarparkemulator-app/configs/ucy.conf:/code/configs/solarpark.conf:rw -d -p 4567:4567 dockerfile:latest
sudo docker ps
