VOLUMES=("openems_chronografStorage" "openems_edgeData" "openems_pgdata" "openems_influxData" "openems_influxConfig" "openems_grafanaStorage" "openems_chronografStorage" "openems_backendData")

mkdir ~/backups
for volume in "${VOLUMES[@]}"
do
  docker run --rm -v "$volume:/volume" -v "$HOME/backups:/backup" ubuntu tar cvf "/backup/$volume-backup.tar" /volume
done