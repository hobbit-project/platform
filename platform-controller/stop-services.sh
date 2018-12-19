#sudo docker stop $(docker ps -a | grep -v "hobbit_" | awk '{print $1}')
docker service rm $(docker service ls | grep -v "hobbit_")