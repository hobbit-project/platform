Set-Variable -Name "VOS_CONTAINER_ID" -Value "$(docker ps -aqf 'name=vos')"
Set-Variable -Name "VOS_CONTAINER_NUMBER" -Value "$( (docker ps -aqf 'name=vos' | Measure-Object -line).Lines)"


if ( "$VOS_CONTAINER_NUMBER" -eq 1 ){
  # dos TO unix 
  docker exec -it $VOS_CONTAINER_ID bash -c "tr -d '\15\32' < ./storage-init.sh > ./storage-init.sh"
  docker exec -it $VOS_CONTAINER_ID bash ./storage-init.sh
}else{
  echo "Can not determine vos container name..."
  echo "Use docker ps to determine vos container name and execute the following command manually:"
  echo "docker exec -it yourvoscontainername bash ./storage-init.sh"
}

