sudo docker stop $(sudo docker ps -a | grep -v "hobbit_" | awk '{print $1}')
sudo docker service rm $(sudo docker service ls | grep -v "hobbit_")